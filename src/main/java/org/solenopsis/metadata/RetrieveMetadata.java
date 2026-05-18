package org.solenopsis.metadata;

import java.util.logging.Logger;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.MetadataPortType;

/**
 * Can retrieve metadata in it's entirety or condensed (meaning
 *
 * @author Scot P. Floess
 */
public final class RetrieveMetadata {
    /**
     * Our logger.
     */
    private static final Logger logger = Logger.getLogger(RetrieveMetadata.class.getName());

    /**
     * Default constructor not allowed.
     */
    private RetrieveMetadata() {
    }

    /**
     * Return our logger.
     */
    private static Logger getLogger() {
        return logger;
    }

    static DescribeMetadataResult retrieveMetadata(final MetadataPortType port, final Double apiVersion, final boolean isCondensed) throws Exception {
        final DescribeMetadataResult describeMetadata = port.describeMetadata(apiVersion);

        return describeMetadata;
    }

    static DescribeMetadataResult retrieveMetadata(final MetadataPortType port, final String apiVersion, final boolean isCondensed) throws Exception {
        return retrieveMetadata(port, Double.parseDouble(apiVersion), isCondensed);
    }

    public static DescribeMetadataResult getMetadataComplete(final MetadataPortType port, final String apiVersion) throws Exception {
        return retrieveMetadata(port, apiVersion, false);
    }

    public static DescribeMetadataResult getMetadataCondensed(final MetadataPortType port, final String apiVersion) throws Exception {
        return retrieveMetadata(port, apiVersion, false);
    }
}
