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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.solenopsis.soap.metadata.DescribeMetadataObject;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.FileProperties;
import org.solenopsis.soap.metadata.MetadataPortType;

/**
 * Tests for MetadataStatistics.
 *
 * @author Scot P. Floess
 */
class MetadataStatisticsTest {

    private MetadataPortType port;
    private DescribeMetadataResult describeResult;

    @BeforeEach
    void setUp() {
        port = mock(MetadataPortType.class);
        describeResult = mock(DescribeMetadataResult.class);
    }

    @Test
    void testGenerateStats_BasicCounts() throws Exception {
        // Setup metadata objects
        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("ApexClass");

        DescribeMetadataObject obj2 = mock(DescribeMetadataObject.class);
        when(obj2.getXmlName()).thenReturn("CustomObject");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj1, obj2));
        when(describeResult.getOrganizationNamespace()).thenReturn("testorg");

        // Setup file properties
        FileProperties prop1 = mock(FileProperties.class);
        FileProperties prop2 = mock(FileProperties.class);
        FileProperties prop3 = mock(FileProperties.class);

        when(port.describeMetadata(60.0)).thenReturn(describeResult);
        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(Arrays.asList(prop1, prop2))  // ApexClass: 2
            .thenReturn(Arrays.asList(prop3));         // CustomObject: 1

        OrgStats stats = MetadataStatistics.generateStats(port, "60.0");

        assertNotNull(stats);
        assertEquals("60.0", stats.getApiVersion());
        assertEquals("testorg", stats.getOrganizationNamespace());
        assertEquals(3, stats.getTotalComponents());
        assertEquals(2, stats.getTotalTypes());
        assertEquals(2, stats.getTypeCounts().get("ApexClass"));
        assertEquals(1, stats.getTypeCounts().get("CustomObject"));
    }

    @Test
    void testGenerateStats_TopTypes() throws Exception {
        // Setup metadata objects
        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("ApexClass");

        DescribeMetadataObject obj2 = mock(DescribeMetadataObject.class);
        when(obj2.getXmlName()).thenReturn("Layout");

        DescribeMetadataObject obj3 = mock(DescribeMetadataObject.class);
        when(obj3.getXmlName()).thenReturn("CustomObject");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj1, obj2, obj3));
        when(describeResult.getOrganizationNamespace()).thenReturn("");

        // Create different sized lists
        List<FileProperties> apexProps = createMockFilePropertiesList(100);
        List<FileProperties> layoutProps = createMockFilePropertiesList(75);
        List<FileProperties> objectProps = createMockFilePropertiesList(50);

        when(port.describeMetadata(60.0)).thenReturn(describeResult);
        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(apexProps)
            .thenReturn(layoutProps)
            .thenReturn(objectProps);

        OrgStats stats = MetadataStatistics.generateStats(port, "60.0", 3);

        assertNotNull(stats.getTopTypes());
        assertEquals(3, stats.getTopTypes().size());

        // Verify sorting (descending order)
        assertEquals("ApexClass", stats.getTopTypes().get(0).getType());
        assertEquals(100, stats.getTopTypes().get(0).getCount());

        assertEquals("Layout", stats.getTopTypes().get(1).getType());
        assertEquals(75, stats.getTopTypes().get(1).getCount());

        assertEquals("CustomObject", stats.getTopTypes().get(2).getType());
        assertEquals(50, stats.getTopTypes().get(2).getCount());
    }

    @Test
    void testGenerateStats_TopTypesLimit() throws Exception {
        // Setup 5 metadata objects but request only top 3
        List<DescribeMetadataObject> objects = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            DescribeMetadataObject obj = mock(DescribeMetadataObject.class);
            when(obj.getXmlName()).thenReturn("Type" + i);
            objects.add(obj);
        }

        when(describeResult.getMetadataObjects()).thenReturn(objects);
        when(describeResult.getOrganizationNamespace()).thenReturn("");

        when(port.describeMetadata(60.0)).thenReturn(describeResult);

        // Return different counts for each type
        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(createMockFilePropertiesList(10))
            .thenReturn(createMockFilePropertiesList(20))
            .thenReturn(createMockFilePropertiesList(5))
            .thenReturn(createMockFilePropertiesList(15))
            .thenReturn(createMockFilePropertiesList(8));

        OrgStats stats = MetadataStatistics.generateStats(port, "60.0", 3);

        assertNotNull(stats.getTopTypes());
        assertEquals(3, stats.getTopTypes().size()); // Limited to top 3
        assertEquals(5, stats.getTotalTypes()); // But total types is 5
    }

    @Test
    void testGenerateStats_EmptyOrg() throws Exception {
        when(describeResult.getMetadataObjects()).thenReturn(new ArrayList<>());
        when(describeResult.getOrganizationNamespace()).thenReturn("");

        when(port.describeMetadata(60.0)).thenReturn(describeResult);

        OrgStats stats = MetadataStatistics.generateStats(port, "60.0");

        assertNotNull(stats);
        assertEquals(0, stats.getTotalComponents());
        assertEquals(0, stats.getTotalTypes());
        assertTrue(stats.getTopTypes().isEmpty());
    }

    @Test
    void testGenerateStats_ErrorHandling() throws Exception {
        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("ApexClass");

        DescribeMetadataObject obj2 = mock(DescribeMetadataObject.class);
        when(obj2.getXmlName()).thenReturn("BadType");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj1, obj2));
        when(describeResult.getOrganizationNamespace()).thenReturn("");

        when(port.describeMetadata(60.0)).thenReturn(describeResult);
        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(createMockFilePropertiesList(5))
            .thenThrow(new RuntimeException("Type not found"));

        OrgStats stats = MetadataStatistics.generateStats(port, "60.0");

        assertNotNull(stats);
        assertEquals(5, stats.getTotalComponents()); // Only successful type
        assertEquals(2, stats.getTotalTypes());
        assertEquals(5, stats.getTypeCounts().get("ApexClass"));
        assertEquals(0, stats.getTypeCounts().get("BadType")); // Error results in 0
    }

    @Test
    void testGenerateStatsForTypes_SpecificTypes() throws Exception {
        when(describeResult.getOrganizationNamespace()).thenReturn("myorg");

        when(port.describeMetadata(60.0)).thenReturn(describeResult);
        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(createMockFilePropertiesList(10))
            .thenReturn(createMockFilePropertiesList(20));

        List<String> types = Arrays.asList("ApexClass", "CustomObject");
        OrgStats stats = MetadataStatistics.generateStatsForTypes(port, types, "60.0");

        assertNotNull(stats);
        assertEquals("60.0", stats.getApiVersion());
        assertEquals("myorg", stats.getOrganizationNamespace());
        assertEquals(30, stats.getTotalComponents());
        assertEquals(2, stats.getTotalTypes());
        assertEquals(10, stats.getTypeCounts().get("ApexClass"));
        assertEquals(20, stats.getTypeCounts().get("CustomObject"));
    }

    @Test
    void testGenerateStatsForTypes_EmptyList() throws Exception {
        when(describeResult.getOrganizationNamespace()).thenReturn("");
        when(port.describeMetadata(60.0)).thenReturn(describeResult);

        List<String> types = new ArrayList<>();
        OrgStats stats = MetadataStatistics.generateStatsForTypes(port, types, "60.0");

        assertNotNull(stats);
        assertEquals(0, stats.getTotalComponents());
        assertEquals(0, stats.getTotalTypes());
        assertTrue(stats.getTopTypes().isEmpty());
    }

    @Test
    void testGenerateStatsForTypes_WithErrors() throws Exception {
        when(describeResult.getOrganizationNamespace()).thenReturn("");
        when(port.describeMetadata(60.0)).thenReturn(describeResult);
        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(createMockFilePropertiesList(5))
            .thenThrow(new RuntimeException("Type not found"));

        List<String> types = Arrays.asList("ApexClass", "BadType");
        OrgStats stats = MetadataStatistics.generateStatsForTypes(port, types, "60.0");

        assertNotNull(stats);
        assertEquals(5, stats.getTotalComponents());
        assertEquals(2, stats.getTotalTypes());
        assertEquals(5, stats.getTypeCounts().get("ApexClass"));
        assertEquals(0, stats.getTypeCounts().get("BadType"));
    }

    @Test
    void testDefaultTopCount() {
        assertEquals(10, MetadataStatistics.DEFAULT_TOP_COUNT);
    }

    /**
     * Helper to create a list of mock FileProperties.
     */
    private List<FileProperties> createMockFilePropertiesList(int count) {
        List<FileProperties> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(mock(FileProperties.class));
        }
        return list;
    }
}
