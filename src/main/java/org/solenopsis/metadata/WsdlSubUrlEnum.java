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
package org.solenopsis.metadata;

import org.flossware.jcommons.util.StringUtil;

/**
 * The sub URL to use when gathering WSDLs.
 *
 * @author Scot P. Floess
 */
public enum WsdlSubUrlEnum {
    /** Apex WSDL service endpoint. */
    APEX("/services/wsdl/apex"),
    /** Custom Apex class WSDL service endpoint. */
    CUSTOM("/services/wsdl/class/"),
    /** Enterprise WSDL service endpoint. */
    ENTERPRISE("/soap/wsdl.jsp?type=*"),
    /** Metadata WSDL service endpoint. */
    METADATA("/services/wsdl/metadata"),
    /** Partner WSDL service endpoint. */
    PARNTER("/soap/wsdl.jsp"),
    /** Tooling WSDL service endpoint. */
    TOOLING("/services/wsdl/tooling");

    private final String subUrl;

    private WsdlSubUrlEnum(final String subUrl) {
        this.subUrl = subUrl;
    }

    /**
     * Returns the sub-URL path for this WSDL type.
     *
     * @return the sub-URL path
     */
    public String getSubUrl() {
        return subUrl;
    }

    /**
     * Computes the full WSDL URL by combining the base URL with the sub-URL.
     *
     * @param baseUrl the Salesforce server base URL
     * @return the complete WSDL URL
     */
    public String computeUrl(final String baseUrl) {
        return StringUtil.concatWithSeparator(false, "/", baseUrl, getSubUrl());
    }
}
