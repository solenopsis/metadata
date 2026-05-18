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
import org.flossware.jcommons.util.StringUtil;
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
            System.err.println("\nYou must provide either --solenopsis or --creds parameters!\n");
            System.exit(1);
        }
    }

    Context(final String[] args) {
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            switch (args[argIndex]) {
                case "--solenopsis":
                    setSolenopsisCredentials(args[++argIndex]);
                    break;

                case "--creds":
                    setCredentials(args[++argIndex]);
                    break;

                case "--prefix":
                    prefix = args[++argIndex];
                    break;

                case "--dir":
                    outputDir = args[++argIndex];
                    new File(outputDir).mkdirs();
                    break;
            }
        }

        ensureCredentials();
    }
}
