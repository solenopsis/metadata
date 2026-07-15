package org.solenopsis.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.MetadataPortType;

/**
 * Retrieves metadata descriptions from a Salesforce org in complete or condensed form.
 *
 * @author Scot P. Floess
 */
public final class RetrieveMetadata {

    private static final Logger logger = LoggerFactory.getLogger(RetrieveMetadata.class);

    private RetrieveMetadata() {
    }

    static DescribeMetadataResult retrieveMetadata(final MetadataPortType port, final Double apiVersion) throws Exception {
        logger.debug("Retrieving metadata for API version {}", apiVersion);
        return port.describeMetadata(apiVersion);
    }

    static DescribeMetadataResult retrieveMetadata(final MetadataPortType port, final String apiVersion) throws Exception {
        return retrieveMetadata(port, Double.parseDouble(apiVersion));
    }

    /**
     * Retrieves complete metadata information from a Salesforce org.
     *
     * @param port the metadata port to use for the request
     * @param apiVersion the Salesforce API version
     * @return the complete metadata description result
     * @throws Exception if the metadata retrieval fails
     */
    public static DescribeMetadataResult getMetadataComplete(final MetadataPortType port, final String apiVersion) throws Exception {
        return retrieveMetadata(port, apiVersion);
    }

    /**
     * Retrieves condensed metadata information from a Salesforce org.
     *
     * @param port the metadata port to use for the request
     * @param apiVersion the Salesforce API version
     * @return the condensed metadata description result
     * @throws Exception if the metadata retrieval fails
     */
    public static DescribeMetadataResult getMetadataCondensed(final MetadataPortType port, final String apiVersion) throws Exception {
        return retrieveMetadata(port, apiVersion);
    }
}
