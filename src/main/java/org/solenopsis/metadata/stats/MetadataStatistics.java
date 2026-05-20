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
package org.solenopsis.metadata.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;
import org.solenopsis.metadata.list.ListMetadata;
import org.solenopsis.soap.metadata.DescribeMetadataObject;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.FileProperties;
import org.solenopsis.soap.metadata.MetadataPortType;

/**
 * Generates statistics about metadata in a Salesforce org.
 *
 * @author Scot P. Floess
 */
public class MetadataStatistics {

    private static final Logger logger = LoggerFactory.getLogger(MetadataStatistics.class);

    /**
     * Default number of top types to include in statistics.
     */
    public static final int DEFAULT_TOP_COUNT = 10;

    /**
     * Generate comprehensive statistics about org metadata.
     *
     * @param port the metadata port
     * @param apiVersion the Salesforce API version
     * @return statistics about the org
     * @throws Exception if statistics generation fails
     */
    public static OrgStats generateStats(
        final MetadataPortType port,
        final String apiVersion
    ) throws Exception {
        return generateStats(port, apiVersion, DEFAULT_TOP_COUNT);
    }

    /**
     * Generate comprehensive statistics about org metadata.
     *
     * @param port the metadata port
     * @param apiVersion the Salesforce API version
     * @param topCount number of top types to include
     * @return statistics about the org
     * @throws Exception if statistics generation fails
     */
    public static OrgStats generateStats(
        final MetadataPortType port,
        final String apiVersion,
        final int topCount
    ) throws Exception {
        logger.info("Generating metadata statistics for API version {}", apiVersion);

        final double apiVersionDouble = Double.parseDouble(apiVersion);
        final DescribeMetadataResult describe = port.describeMetadata(apiVersionDouble);

        final OrgStats stats = new OrgStats();
        stats.setApiVersion(apiVersion);
        stats.setOrganizationNamespace(describe.getOrganizationNamespace());

        // Count components by type
        final Map<String, Integer> typeCounts = new HashMap<>();
        int totalComponents = 0;

        for (DescribeMetadataObject obj : describe.getMetadataObjects()) {
            try {
                final List<FileProperties> items = ListMetadata.listType(
                    port,
                    obj.getXmlName(),
                    apiVersionDouble
                );
                final int count = items.size();
                typeCounts.put(obj.getXmlName(), count);
                totalComponents += count;

                logger.debug("Type {}: {} components", obj.getXmlName(), count);
            } catch (Exception e) {
                logger.warn("Failed to list metadata type: {} - {}", obj.getXmlName(), e.getMessage());
                typeCounts.put(obj.getXmlName(), 0);
            }
        }

        stats.setTypeCounts(typeCounts);
        stats.setTotalComponents(totalComponents);
        stats.setTotalTypes(typeCounts.size());

        // Calculate top types
        final List<OrgStats.TypeCount> topTypes = typeCounts.entrySet().stream()
            .map(entry -> new OrgStats.TypeCount(entry.getKey(), entry.getValue()))
            .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
            .limit(topCount)
            .collect(Collectors.toList());

        stats.setTopTypes(topTypes);

        logger.info("Statistics generated: {} components across {} types", totalComponents, typeCounts.size());

        return stats;
    }

    /**
     * Generate statistics for specific metadata types only.
     *
     * @param port the metadata port
     * @param metadataTypes list of metadata type names to include
     * @param apiVersion the API version
     * @return statistics for the specified types
     * @throws Exception if statistics generation fails
     */
    public static OrgStats generateStatsForTypes(
        final MetadataPortType port,
        final List<String> metadataTypes,
        final String apiVersion
    ) throws Exception {
        logger.info("Generating statistics for {} metadata types", metadataTypes.size());

        final double apiVersionDouble = Double.parseDouble(apiVersion);
        final DescribeMetadataResult describe = port.describeMetadata(apiVersionDouble);

        final OrgStats stats = new OrgStats();
        stats.setApiVersion(apiVersion);
        stats.setOrganizationNamespace(describe.getOrganizationNamespace());

        // Count components for specified types only
        final Map<String, Integer> typeCounts = new HashMap<>();
        int totalComponents = 0;

        for (String typeName : metadataTypes) {
            try {
                final List<FileProperties> items = ListMetadata.listType(
                    port,
                    typeName,
                    apiVersionDouble
                );
                final int count = items.size();
                typeCounts.put(typeName, count);
                totalComponents += count;

                logger.debug("Type {}: {} components", typeName, count);
            } catch (Exception e) {
                logger.warn("Failed to list metadata type: {} - {}", typeName, e.getMessage());
                typeCounts.put(typeName, 0);
            }
        }

        stats.setTypeCounts(typeCounts);
        stats.setTotalComponents(totalComponents);
        stats.setTotalTypes(typeCounts.size());

        // All types are included in topTypes since we're filtering
        final List<OrgStats.TypeCount> topTypes = typeCounts.entrySet().stream()
            .map(entry -> new OrgStats.TypeCount(entry.getKey(), entry.getValue()))
            .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
            .collect(Collectors.toList());

        stats.setTopTypes(topTypes);

        logger.info("Statistics generated: {} components across {} types", totalComponents, typeCounts.size());

        return stats;
    }
}
