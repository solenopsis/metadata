/*
 * Copyright (C) 2024 Scot P. Floess
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
package org.solenopsis.metadata.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solenopsis.soap.metadata.DescribeMetadataObject;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.FileProperties;
import org.solenopsis.soap.metadata.ListMetadataQuery;
import org.solenopsis.soap.metadata.MetadataPortType;

/**
 * Lists metadata components from a Salesforce org.
 *
 * @author Scot P. Floess
 */
public class ListMetadata {

    private static final Logger logger = LoggerFactory.getLogger(ListMetadata.class);

    /**
     * List all components of a specific metadata type.
     *
     * @param port the metadata port
     * @param metadataType the type to list (e.g., "ApexClass", "CustomObject")
     * @param apiVersion the Salesforce API version
     * @return list of file properties for the metadata type
     * @throws Exception if listing fails
     */
    public static List<FileProperties> listType(
        final MetadataPortType port,
        final String metadataType,
        final double apiVersion
    ) throws Exception {
        logger.debug("Listing metadata type: " + metadataType);

        ListMetadataQuery query = new ListMetadataQuery();
        query.setType(metadataType);

        List<FileProperties> result = port.listMetadata(
            Arrays.asList(query),
            apiVersion
        );

        if (result == null) {
            result = new ArrayList<>();
        }

        logger.info("Found {} components for type: {}", result.size(), metadataType);
        return result;
    }

    /**
     * List all components of a specific metadata type.
     *
     * @param port the metadata port
     * @param metadataType the type to list
     * @param apiVersion the API version as string
     * @return list of file properties
     * @throws Exception if listing fails
     */
    public static List<FileProperties> listType(
        final MetadataPortType port,
        final String metadataType,
        final String apiVersion
    ) throws Exception {
        return listType(port, metadataType, Double.parseDouble(apiVersion));
    }

    /**
     * List all components of multiple metadata types.
     *
     * @param port the metadata port
     * @param metadataTypes list of types to query
     * @param apiVersion the API version
     * @return map of metadata type to list of file properties
     * @throws Exception if listing fails
     */
    public static Map<String, List<FileProperties>> listTypes(
        final MetadataPortType port,
        final List<String> metadataTypes,
        final double apiVersion
    ) throws Exception {
        logger.info("Listing {} metadata types", metadataTypes.size());

        Map<String, List<FileProperties>> results = new HashMap<>();

        for (String metadataType : metadataTypes) {
            List<FileProperties> props = listType(port, metadataType, apiVersion);
            results.put(metadataType, props);
        }

        return results;
    }

    /**
     * List all metadata types available in the org.
     *
     * @param port the metadata port
     * @param describeResult the describe metadata result
     * @param apiVersion the API version
     * @return map of all metadata types to their components
     * @throws Exception if listing fails
     */
    public static Map<String, List<FileProperties>> listAll(
        final MetadataPortType port,
        final DescribeMetadataResult describeResult,
        final double apiVersion
    ) throws Exception {
        logger.info("Listing all metadata types from org");

        Map<String, List<FileProperties>> results = new HashMap<>();
        int totalComponents = 0;

        for (DescribeMetadataObject obj : describeResult.getMetadataObjects()) {
            try {
                List<FileProperties> props = listType(port, obj.getXmlName(), apiVersion);
                results.put(obj.getXmlName(), props);
                totalComponents += props.size();
            } catch (Exception e) {
                logger.warn("Failed to list metadata type: {} - {}", obj.getXmlName(), e.getMessage());
                results.put(obj.getXmlName(), new ArrayList<>());
            }
        }

        logger.info("Listed {} total components across {} metadata types", totalComponents, results.size());
        return results;
    }

    /**
     * List metadata components in a specific folder.
     *
     * @param port the metadata port
     * @param metadataType the type to list
     * @param folder the folder name
     * @param apiVersion the API version
     * @return list of file properties in the folder
     * @throws Exception if listing fails
     */
    public static List<FileProperties> listFolder(
        final MetadataPortType port,
        final String metadataType,
        final String folder,
        final double apiVersion
    ) throws Exception {
        logger.debug("Listing metadata type: " + metadataType + " in folder: " + folder);

        ListMetadataQuery query = new ListMetadataQuery();
        query.setType(metadataType);
        query.setFolder(folder);

        List<FileProperties> result = port.listMetadata(
            Arrays.asList(query),
            apiVersion
        );

        if (result == null) {
            result = new ArrayList<>();
        }

        logger.info("Found {} components for type: {} in folder: {}", result.size(), metadataType, folder);
        return result;
    }
}
