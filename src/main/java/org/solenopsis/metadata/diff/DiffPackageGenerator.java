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
package org.solenopsis.metadata.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solenopsis.soap.metadata.Package;
import org.solenopsis.soap.metadata.PackageTypeMembers;

/**
 * Generates package.xml and destructiveChanges.xml from git diff results.
 *
 * @author Scot P. Floess
 */
public class DiffPackageGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DiffPackageGenerator.class);

    /**
     * Generate a package from changed metadata components.
     *
     * @param components list of metadata components
     * @param apiVersion the Salesforce API version
     * @return the package
     */
    public static Package generatePackage(List<MetadataComponent> components, String apiVersion) {
        return generatePackage(components, apiVersion, false);
    }

    /**
     * Generate a package from metadata components.
     *
     * @param components list of metadata components
     * @param apiVersion the Salesforce API version
     * @param includeDeleted if true, include deleted components; if false, exclude them
     * @return the package
     */
    public static Package generatePackage(List<MetadataComponent> components, String apiVersion, boolean includeDeleted) {
        logger.info("Generating package from {} components (API {})", components.size(), apiVersion);

        // Group components by type
        Map<String, List<String>> componentsByType = new HashMap<>();

        for (MetadataComponent component : components) {
            // Skip deleted components if not including them
            if (component.isDeleted() && !includeDeleted) {
                continue;
            }

            // Skip non-deleted if only including deleted
            if (!component.isDeleted() && includeDeleted) {
                continue;
            }

            componentsByType
                .computeIfAbsent(component.getType(), k -> new ArrayList<>())
                .add(component.getName());
        }

        Package pkg = new Package();
        pkg.setVersion(apiVersion);

        for (Map.Entry<String, List<String>> entry : componentsByType.entrySet()) {
            PackageTypeMembers typeMembers = new PackageTypeMembers();
            typeMembers.setName(entry.getKey());
            typeMembers.getMembers().addAll(entry.getValue());

            pkg.getTypes().add(typeMembers);

            logger.debug("Added type {} with {} members", entry.getKey(), entry.getValue().size());
        }

        logger.info("Generated package with {} types", pkg.getTypes().size());
        return pkg;
    }

    /**
     * Generate package for added/modified components only.
     *
     * @param components list of all components
     * @param apiVersion the API version
     * @return package containing only non-deleted components
     */
    public static Package generateDeployPackage(List<MetadataComponent> components, String apiVersion) {
        return generatePackage(components, apiVersion, false);
    }

    /**
     * Generate destructiveChanges package for deleted components only.
     *
     * @param components list of all components
     * @param apiVersion the API version
     * @return package containing only deleted components
     */
    public static Package generateDestructiveChanges(List<MetadataComponent> components, String apiVersion) {
        return generatePackage(components, apiVersion, true);
    }

    /**
     * Generate package from git diff.
     *
     * @param fromRef the starting git reference
     * @param toRef the ending git reference
     * @param apiVersion the API version
     * @return package for deployment
     * @throws Exception if git parsing fails
     */
    public static Package generateFromGitDiff(String fromRef, String toRef, String apiVersion) throws Exception {
        List<MetadataComponent> components = GitDiffParser.parseChangedComponents(fromRef, toRef);
        return generateDeployPackage(components, apiVersion);
    }

    /**
     * Generate both deploy and destructive packages from git diff.
     *
     * @param fromRef the starting git reference
     * @param toRef the ending git reference
     * @param apiVersion the API version
     * @return array with [deployPackage, destructivePackage]
     * @throws Exception if git parsing fails
     */
    public static Package[] generateBothFromGitDiff(String fromRef, String toRef, String apiVersion) throws Exception {
        List<MetadataComponent> components = GitDiffParser.parseChangedComponents(fromRef, toRef);

        Package deployPackage = generateDeployPackage(components, apiVersion);
        Package destructivePackage = generateDestructiveChanges(components, apiVersion);

        return new Package[]{deployPackage, destructivePackage};
    }

    /**
     * Check if there are any deleted components.
     *
     * @param components list of components
     * @return true if any component is deleted
     */
    public static boolean hasDeletedComponents(List<MetadataComponent> components) {
        return components.stream().anyMatch(MetadataComponent::isDeleted);
    }

    /**
     * Count components by type.
     *
     * @param components list of components
     * @return map of type to count
     */
    public static Map<String, Integer> countByType(List<MetadataComponent> components) {
        Map<String, Integer> counts = new HashMap<>();

        for (MetadataComponent component : components) {
            counts.merge(component.getType(), 1, Integer::sum);
        }

        return counts;
    }
}
