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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WsdlSubUrlEnum.
 *
 * @author Scot P. Floess
 */
class WsdlSubUrlEnumTest {

    @Test
    void testApexSubUrl() {
        assertEquals("/services/wsdl/apex", WsdlSubUrlEnum.APEX.getSubUrl());
    }

    @Test
    void testCustomSubUrl() {
        assertEquals("/services/wsdl/class/", WsdlSubUrlEnum.CUSTOM.getSubUrl());
    }

    @Test
    void testEnterpriseSubUrl() {
        assertEquals("/soap/wsdl.jsp?type=*", WsdlSubUrlEnum.ENTERPRISE.getSubUrl());
    }

    @Test
    void testMetadataSubUrl() {
        assertEquals("/services/wsdl/metadata", WsdlSubUrlEnum.METADATA.getSubUrl());
    }

    @Test
    void testPartnerSubUrl() {
        assertEquals("/soap/wsdl.jsp", WsdlSubUrlEnum.PARNTER.getSubUrl());
    }

    @Test
    void testToolingSubUrl() {
        assertEquals("/services/wsdl/tooling", WsdlSubUrlEnum.TOOLING.getSubUrl());
    }

    @Test
    void testComputeUrlApex() {
        String baseUrl = "https://example.salesforce.com";
        String result = WsdlSubUrlEnum.APEX.computeUrl(baseUrl);
        // StringUtil.concatWithSeparator adds separator between parts
        assertEquals("https://example.salesforce.com//services/wsdl/apex", result);
    }

    @Test
    void testComputeUrlMetadata() {
        String baseUrl = "https://test.salesforce.com";
        String result = WsdlSubUrlEnum.METADATA.computeUrl(baseUrl);
        assertEquals("https://test.salesforce.com//services/wsdl/metadata", result);
    }

    @Test
    void testComputeUrlWithTrailingSlash() {
        String baseUrl = "https://example.salesforce.com/";
        String result = WsdlSubUrlEnum.APEX.computeUrl(baseUrl);
        // StringUtil.concatWithSeparator adds separator
        assertTrue(result.contains("/services/wsdl/apex"));
    }

    @Test
    void testComputeUrlCustom() {
        String baseUrl = "https://custom.salesforce.com";
        String result = WsdlSubUrlEnum.CUSTOM.computeUrl(baseUrl);
        assertEquals("https://custom.salesforce.com//services/wsdl/class/", result);
    }

    @Test
    void testAllEnumValues() {
        WsdlSubUrlEnum[] values = WsdlSubUrlEnum.values();
        assertEquals(6, values.length);

        assertNotNull(WsdlSubUrlEnum.valueOf("APEX"));
        assertNotNull(WsdlSubUrlEnum.valueOf("CUSTOM"));
        assertNotNull(WsdlSubUrlEnum.valueOf("ENTERPRISE"));
        assertNotNull(WsdlSubUrlEnum.valueOf("METADATA"));
        assertNotNull(WsdlSubUrlEnum.valueOf("PARNTER"));
        assertNotNull(WsdlSubUrlEnum.valueOf("TOOLING"));
    }
}
