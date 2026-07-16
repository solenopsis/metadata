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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Set;
import org.solenopsis.metadata.WsdlSubUrlEnum;
import org.solenopsis.session.Credentials;
import org.solenopsis.session.SessionContext;
import org.solenopsis.session.credentials.CredentialsUtil;
import org.solenopsis.session.soap.PortEnum;
import org.solenopsis.session.soap.login.LoginServiceEnum;
import org.solenopsis.soap.metadata.MetadataService;
import org.solenopsis.soap.metadata.MetadataPortType;

/**
 * Context for command line options.
 *
 * @author Scot P. Floess
 */
final class Context {
    Credentials credentials;

    String outputDir = System.getProperty("user.home");

    String prefix = "";

    MetadataPortType port;

    SessionContext sessionContext;

    /**
     * Explicit set of standard WSDLs to retrieve, or null for default behavior.
     * Populated by the --wsdl flag.
     */
    Set<WsdlSubUrlEnum> wsdls;

    /**
     * When true, skip custom Apex web service WSDL discovery.
     * Set by the --skip-custom flag.
     */
    boolean skipCustom = false;

    /**
     * When true and wsdls is null, include the tooling WSDL in the default set.
     * Set by the --include-tooling flag.
     */
    boolean includeTooling = false;

    /**
     * Package-private no-args constructor for testing.
     */
    Context() {
    }

    /**
     * Returns the set of standard WSDLs to retrieve. If an explicit set was
     * provided via --wsdl, that set is returned. Otherwise, the default set
     * (apex, enterprise, metadata, partner) is returned, optionally including
     * tooling if --include-tooling was specified.
     *
     * @return the set of standard WSDLs to retrieve
     */
    Set<WsdlSubUrlEnum> getStandardWsdls() {
        if (wsdls != null) {
            return wsdls;
        }

        final Set<WsdlSubUrlEnum> defaults = EnumSet.of(
            WsdlSubUrlEnum.APEX, WsdlSubUrlEnum.ENTERPRISE,
            WsdlSubUrlEnum.METADATA, WsdlSubUrlEnum.PARTNER
        );

        if (includeTooling) {
            defaults.add(WsdlSubUrlEnum.TOOLING);
        }

        return defaults;
    }

    void setCredentials(final String fileName) {
        credentials = CredentialsUtil.fromFile(fileName);
        sessionContext = LoginServiceEnum.DEFAULT_LOGIN_SERVICE.getLoginService().login(credentials);
        port = PortEnum.METADATA.createPortForService(MetadataService.class, sessionContext);
    }

    void setSolenopsisCredentials(final String env) {
        final Path credPath = Paths.get(System.getProperty("user.home"), ".solenopsis", "credentials", env + ".properties");
        setCredentials(credPath.toString());
    }

    void ensureCredentials() {
        if (null == credentials) {
            throw new IllegalArgumentException("You must provide either --solenopsis or --creds parameters!");
        }
    }

    Context(final String[] args) {
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            switch (args[argIndex]) {
                case "--solenopsis":
                    if (argIndex + 1 >= args.length) {
                        throw new IllegalArgumentException("--solenopsis requires an environment argument");
                    }
                    setSolenopsisCredentials(args[++argIndex]);
                    break;

                case "--creds":
                    if (argIndex + 1 >= args.length) {
                        throw new IllegalArgumentException("--creds requires a file path argument");
                    }
                    setCredentials(args[++argIndex]);
                    break;

                case "--prefix":
                    if (argIndex + 1 >= args.length) {
                        throw new IllegalArgumentException("--prefix requires a value argument");
                    }
                    prefix = args[++argIndex];
                    break;

                case "--dir":
                    if (argIndex + 1 >= args.length) {
                        throw new IllegalArgumentException("--dir requires a directory argument");
                    }
                    outputDir = args[++argIndex];
                    new File(outputDir).mkdirs();
                    break;

                case "--wsdl":
                    if (argIndex + 1 >= args.length) {
                        throw new IllegalArgumentException("--wsdl requires a comma-separated list of WSDL names");
                    }
                    wsdls = EnumSet.noneOf(WsdlSubUrlEnum.class);
                    for (final String name : args[++argIndex].split(",")) {
                        final String trimmed = name.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        try {
                            final WsdlSubUrlEnum wsdlType = WsdlSubUrlEnum.valueOf(trimmed.toUpperCase());
                            if (wsdlType != WsdlSubUrlEnum.CUSTOM) {
                                wsdls.add(wsdlType);
                            }
                        } catch (final IllegalArgumentException e) {
                            throw new IllegalArgumentException(
                                "Unknown WSDL type: " + trimmed
                                + ". Valid types: apex, enterprise, metadata, partner, tooling"
                            );
                        }
                    }
                    break;

                case "--skip-custom":
                    skipCustom = true;
                    break;

                case "--include-tooling":
                    includeTooling = true;
                    break;
            }
        }

        ensureCredentials();
    }
}
