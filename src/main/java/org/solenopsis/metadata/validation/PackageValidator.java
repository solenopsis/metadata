/*
 * Copyright (C) 2026 Scot P. Floess
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
package org.solenopsis.metadata.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solenopsis.soap.metadata.DescribeMetadataObject;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.FileProperties;
import org.solenopsis.soap.metadata.MetadataPortType;
import org.solenopsis.soap.metadata.Package;
import org.solenopsis.soap.metadata.PackageTypeMembers;
import org.solenopsis.metadata.list.ListMetadata;

/**
 * Validates package.xml files for correctness and compliance.
 *
 * @author Scot P. Floess
 */
public class PackageValidator {

    private static final Logger logger = LoggerFactory.getLogger(PackageValidator.class);

    /**
     * Validate a package for basic structural issues.
     *
     * @param pkg the package to validate
     * @return validation result
     */
    public static ValidationResult validateStructure(final Package pkg) {
        logger.debug("Validating package structure");

        final ValidationResult result = new ValidationResult();

        // Check version
        if (pkg.getVersion() == null || pkg.getVersion().isEmpty()) {
            result.addError("Missing or empty version");
        } else {
            try {
                Double.parseDouble(pkg.getVersion());
            } catch (NumberFormatException e) {
                result.addError("Invalid version format: " + pkg.getVersion());
            }
        }

        // Check for types
        if (pkg.getTypes() == null || pkg.getTypes().isEmpty()) {
            result.addError("No types defined in package");
            return result;
        }

        // Validate each type
        final Set<String> typeNames = new HashSet<>();

        for (PackageTypeMembers type : pkg.getTypes()) {
            String typeName = type.getName();

            if (typeName == null || typeName.isEmpty()) {
                result.addError("Type missing name");
                continue;
            }

            // Check for duplicate types
            if (typeNames.contains(typeName)) {
                result.addError("Duplicate type definition", typeName);
            } else {
                typeNames.add(typeName);
            }

            // Check for members
            if (type.getMembers() == null || type.getMembers().isEmpty()) {
                result.addWarning("Type has no members", typeName);
            } else {
                // Check for duplicate members within type
                final Set<String> memberNames = new HashSet<>();

                for (String member : type.getMembers()) {
                    if (member == null || member.isEmpty()) {
                        result.addError("Empty member name", typeName);
                    } else if (memberNames.contains(member)) {
                        result.addError("Duplicate member: " + member, typeName);
                    } else {
                        memberNames.add(member);
                    }
                }
            }
        }

        logger.info("Structure validation complete: {} errors, {} warnings", result.getErrorCount(), result.getWarningCount());

        return result;
    }

    /**
     * Validate a package against an org's metadata.
     *
     * @param pkg the package to validate
     * @param port the metadata port
     * @param apiVersion the API version
     * @return validation result
     * @throws Exception if org validation fails
     */
    public static ValidationResult validateAgainstOrg(
        final Package pkg,
        final MetadataPortType port,
        final String apiVersion
    ) throws Exception {
        logger.info("Validating package against org");

        final ValidationResult result = validateStructure(pkg);

        // If structure validation failed, don't proceed
        if (!result.isValid()) {
            logger.warn("Skipping org validation due to structure errors");
            return result;
        }

        final double apiVersionDouble = Double.parseDouble(apiVersion);
        final DescribeMetadataResult describe = port.describeMetadata(apiVersionDouble);

        // Build set of valid type names
        final Set<String> validTypes = new HashSet<>();
        for (DescribeMetadataObject obj : describe.getMetadataObjects()) {
            validTypes.add(obj.getXmlName());
        }

        // Validate each type exists in org
        for (PackageTypeMembers type : pkg.getTypes()) {
            final String typeName = type.getName();

            if (!validTypes.contains(typeName)) {
                result.addError("Type not found in org", typeName);
                continue;
            }

            // Check if members are wildcards
            if (type.getMembers().contains("*")) {
                if (type.getMembers().size() > 1) {
                    result.addWarning("Wildcard mixed with specific members", typeName);
                }
                continue; // Skip member validation for wildcards
            }

            // Validate members exist
            try {
                final List<FileProperties> orgComponents = ListMetadata.listType(
                    port,
                    typeName,
                    apiVersionDouble
                );

                final Set<String> orgMemberNames = new HashSet<>();
                for (FileProperties prop : orgComponents) {
                    orgMemberNames.add(prop.getFullName());
                }

                for (String member : type.getMembers()) {
                    if (!orgMemberNames.contains(member)) {
                        result.addWarning("Member not found in org: " + member, typeName);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to list org components for type {}: {}", typeName, e.getMessage());
                result.addWarning("Could not verify members against org", typeName);
            }
        }

        logger.info("Org validation complete: {} errors, {} warnings", result.getErrorCount(), result.getWarningCount());

        return result;
    }

    /**
     * Validate package version compatibility.
     *
     * @param pkg the package
     * @param orgApiVersion the org's API version
     * @return validation result
     */
    public static ValidationResult validateVersion(
        final Package pkg,
        final String orgApiVersion
    ) {
        final ValidationResult result = new ValidationResult();

        try {
            final double pkgVersion = Double.parseDouble(pkg.getVersion());
            final double orgVersion = Double.parseDouble(orgApiVersion);

            if (pkgVersion > orgVersion) {
                result.addWarning(
                    "Package version (" + pkgVersion + ") is higher than org version (" + orgVersion + ")",
                    "version"
                );
            }
        } catch (NumberFormatException e) {
            result.addError("Invalid version format");
        }

        return result;
    }
}
