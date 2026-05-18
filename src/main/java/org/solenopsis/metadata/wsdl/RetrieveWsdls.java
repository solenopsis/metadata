/*
 * Copyright (C) 2017 Scot P. Floess
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.solenopsis.metadata.wsdl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.flossware.jcommons.util.StringUtil;
import org.solenopsis.soap.metadata.AsyncResult;
import org.solenopsis.soap.metadata.MetadataPortType;
import org.solenopsis.soap.metadata.Package;
import org.solenopsis.soap.metadata.PackageTypeMembers;
import org.solenopsis.soap.metadata.RetrieveRequest;
import org.solenopsis.soap.metadata.RetrieveResult;
import org.solenopsis.metadata.WsdlSubUrlEnum;

/**
 * Can retrieve the "stock" API WSDLs (apex, enterprise, metadata, partner and
 * tooling).
 *
 * @author Scot P. Floess
 */
public class RetrieveWsdls {

    static RetrieveRequest createRetrieveRequest(final String apiVersion) throws Exception {
        final RetrieveRequest retrieveRequest = new RetrieveRequest();
        retrieveRequest.setApiVersion(Double.parseDouble(apiVersion));

        final PackageTypeMembers types = new PackageTypeMembers();
        types.setName("ApexClass");
        types.getMembers().add("*");

        final Package pack = new Package();
        pack.setVersion(apiVersion);
        pack.getTypes().add(types);

        retrieveRequest.setUnpackaged(pack);

        return retrieveRequest;
    }

    static RetrieveResult ensureRetrieveSuccess(final RetrieveResult result) {
        if (result.isSuccess()) {
            return result;
        }

        throw new RuntimeException("Trouble retrieving apex classes:  " + result.getErrorMessage() + " -> " + result.getErrorStatusCode());
    }

    static byte[] retrieveApexClasses(final MetadataPortType metadataPort, final String apiVersion) throws Exception {
        final AsyncResult asyncResult = metadataPort.retrieve(createRetrieveRequest(apiVersion));

        RetrieveResult result = null;

        do {
            Thread.sleep(3000);
            result = metadataPort.checkRetrieveStatus(asyncResult.getId(), true);

            System.out.println("    Polling...  ");
        } while (!result.isDone());

        return ensureRetrieveSuccess(result).getZipFile();
    }

    static List<String> findCustomWsdls(final Context context) throws Exception {
        System.out.println("Retrieving Custom WSDLs...");

        final List<String> retVal = new ArrayList<>();

        final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(retrieveApexClasses(context.port, context.credentials.version())));

        ZipEntry zipEntry = null;

        final byte[] rawData = new byte[2048000];

        do {
            zipEntry = zis.getNextEntry();

            if (null == zipEntry || zipEntry.isDirectory() || !zipEntry.getName().endsWith(".cls")) {
                continue;
            }

            int len;

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            do {
                len = zis.read(rawData);

                if (len < 1) {
                    continue;
                }

                baos.write(rawData, 0, len);

            } while (len > 0);

            final String str = new String(baos.toByteArray());

            if (str.contains("WebService")) {
                retVal.add(new File(zipEntry.getName()).getName().split("\\.")[0]);
            }

        } while (null != zipEntry);

        return retVal;
    }

    static void retrieveWsdl(final Context context, final HttpGet httpGet, final String wsdlFileName) throws Exception {
        final BasicClientCookie cookie = new BasicClientCookie("sid", context.sessionContext.sessionId());
        cookie.setDomain(".salesforce.com");
        cookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");

        CookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(cookie);

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCookieStore(cookieStore);

        final String outputFile = StringUtil.concatWithSeparator(false, System.getProperty("file.separator"), context.outputDir, context.prefix + wsdlFileName);

        System.out.println("Retreiving [" + outputFile + "]");

        CloseableHttpResponse loginResponse = HttpClients.createDefault().execute(httpGet, localContext);

        final FileWriter writer = new FileWriter(outputFile);

        IOUtils.copy(loginResponse.getEntity().getContent(), writer);

        writer.close();

        loginResponse.close();

    }

    static void retrieveWsdls(final Context context, final List<String> customWsdls) throws Exception {
        retrieveWsdl(context, new HttpGet(WsdlSubUrlEnum.APEX.computeUrl(context.sessionContext.serverUrl())), "apex.wsdl");
        retrieveWsdl(context, new HttpGet(WsdlSubUrlEnum.ENTERPRISE.computeUrl(context.sessionContext.serverUrl())), "enterprise.wsdl");
        retrieveWsdl(context, new HttpGet(WsdlSubUrlEnum.METADATA.computeUrl(context.sessionContext.serverUrl())), "metadata.wsdl");
        retrieveWsdl(context, new HttpGet(WsdlSubUrlEnum.PARNTER.computeUrl(context.sessionContext.serverUrl())), "partner.wsdl");
//        retrieveWsdl(context, new HttpGet(WsdlSubUrlEnum.TOOLING.computeUrl(context.sessionContext.serverUrl())), "tooling.wsdl");

        for (final String wsdl : customWsdls) {
            retrieveWsdl(context, new HttpGet(WsdlSubUrlEnum.CUSTOM.computeUrl(context.sessionContext.serverUrl()) + "/" + wsdl), wsdl + ".wsdl");
        }
    }

    public static void main(final String[] args) throws Exception {
        final Context context = new Context(args);

        retrieveWsdls(context, findCustomWsdls(context));
    }
}
