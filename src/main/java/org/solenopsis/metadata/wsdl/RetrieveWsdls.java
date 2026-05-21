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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(RetrieveWsdls.class);

    /**
     * Default buffer size for reading zip entries (8KB).
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Pattern to match single-line comments (//).
     */
    private static final Pattern SINGLE_LINE_COMMENT_PATTERN = Pattern.compile(
        "//.*?$",
        Pattern.MULTILINE
    );

    /**
     * Pattern to match multi-line comments (/* ... *\/).
     */
    private static final Pattern MULTI_LINE_COMMENT_PATTERN = Pattern.compile(
        "/\\*.*?\\*/",
        Pattern.DOTALL
    );

    /**
     * Pattern to match string literals (both single and double quotes).
     * Handles basic escaped quotes.
     */
    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile(
        "'(?:[^'\\\\]|\\\\.)*'|\"(?:[^\"\\\\]|\\\\.)*\"",
        Pattern.DOTALL
    );

    /**
     * Pattern to match the 'webservice' keyword in Apex method declarations.
     * Matches case-insensitively and requires it to be followed by method signature:
     * - Simple types: webservice static String method()
     * - Void: WebService void method()
     * - Complex types: webservice static List<String> method()
     * - Nested generics: webservice static Map<String, List<Integer>> method()
     * - Arrays: webservice static String[] method()
     * Must be followed by return type and method name with opening parenthesis.
     */
    private static final Pattern WEBSERVICE_PATTERN = Pattern.compile(
        "\\bwebservice\\s+(?:static\\s+)?\\S+.*?\\w+\\s*\\(",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

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

            logger.debug("Polling retrieve status for async ID: {}", asyncResult.getId());
        } while (!result.isDone());

        return ensureRetrieveSuccess(result).getZipFile();
    }

    /**
     * Finds all custom web service classes in the Salesforce org.
     * Retrieves all Apex classes and scans them for the 'webservice' keyword
     * in method declarations.
     *
     * @param context the context containing credentials and connection info
     * @return list of class names that contain web service methods
     * @throws Exception if retrieval or parsing fails
     */
    static List<String> findCustomWsdls(final Context context) throws Exception {
        logger.info("Retrieving Custom WSDLs for org");

        final List<String> webServiceClasses = new ArrayList<>();
        final byte[] zipData = retrieveApexClasses(context.port, context.credentials.version());

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry zipEntry;
            final byte[] buffer = new byte[BUFFER_SIZE];

            while ((zipEntry = zis.getNextEntry()) != null) {
                // Skip directories and non-.cls files
                if (zipEntry.isDirectory() || !zipEntry.getName().endsWith(".cls")) {
                    logger.debug("Skipping non-class entry: {}", zipEntry.getName());
                    continue;
                }

                // Read the class file content
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }

                // Convert to string with explicit UTF-8 encoding
                final String classContent = baos.toString(StandardCharsets.UTF_8.name());

                // Check if this class contains webservice methods
                if (isWebServiceClass(classContent)) {
                    // Extract class name from file path (e.g., "classes/MyClass.cls" -> "MyClass")
                    final String className = new File(zipEntry.getName())
                        .getName()
                        .replaceFirst("\\.cls$", "");

                    webServiceClasses.add(className);
                    logger.info("Found web service class: {}", className);
                }
            }
        }

        logger.info("Discovered {} web service class(es)", webServiceClasses.size());

        return webServiceClasses;
    }

    /**
     * Determines if an Apex class contains web service methods by looking for
     * the 'webservice' keyword in method declarations. Removes comments and
     * string literals first to avoid false positives, and requires the keyword
     * to be followed by a method signature.
     *
     * @param classContent the Apex class source code
     * @return true if the class contains web service methods, false otherwise
     */
    static boolean isWebServiceClass(final String classContent) {
        if (classContent == null || classContent.isEmpty()) {
            return false;
        }

        // Remove comments and string literals to avoid false positives
        String cleaned = SINGLE_LINE_COMMENT_PATTERN.matcher(classContent).replaceAll("");
        cleaned = MULTI_LINE_COMMENT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = STRING_LITERAL_PATTERN.matcher(cleaned).replaceAll("");

        // Check for webservice keyword followed by method signature
        final Matcher matcher = WEBSERVICE_PATTERN.matcher(cleaned);
        return matcher.find();
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

        logger.info("Retrieving WSDL: {}", outputFile);

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

    /**
     * Main entry point for WSDL retrieval utility.
     * Retrieves all standard Salesforce WSDLs (Apex, Enterprise, Metadata, Partner)
     * and custom Apex class WSDLs from the org.
     *
     * @param args command-line arguments (--solenopsis, --creds, --prefix, --dir)
     * @throws Exception if WSDL retrieval fails
     */
    public static void main(final String[] args) throws Exception {
        final Context context = new Context(args);

        retrieveWsdls(context, findCustomWsdls(context));
    }
}
