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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.solenopsis.soap.metadata.DescribeMetadataObject;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.FileProperties;
import org.solenopsis.soap.metadata.ListMetadataQuery;
import org.solenopsis.soap.metadata.MetadataPortType;

/**
 * Tests for ListMetadata.
 *
 * @author Scot P. Floess
 */
class ListMetadataTest {

    @Test
    void testListType_Success() throws Exception {
        MetadataPortType port = mock(MetadataPortType.class);
        FileProperties prop1 = mock(FileProperties.class);
        FileProperties prop2 = mock(FileProperties.class);

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(Arrays.asList(prop1, prop2));

        List<FileProperties> result = ListMetadata.listType(port, "ApexClass", 60.0);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(port).listMetadata(anyList(), eq(60.0));
    }

    @Test
    void testListType_EmptyResult() throws Exception {
        MetadataPortType port = mock(MetadataPortType.class);

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(new ArrayList<>());

        List<FileProperties> result = ListMetadata.listType(port, "CustomObject", 60.0);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testListType_NullResult() throws Exception {
        MetadataPortType port = mock(MetadataPortType.class);

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(null);

        List<FileProperties> result = ListMetadata.listType(port, "ApexTrigger", 60.0);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testListType_StringApiVersion() throws Exception {
        MetadataPortType port = mock(MetadataPortType.class);
        FileProperties prop = mock(FileProperties.class);

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(Arrays.asList(prop));

        List<FileProperties> result = ListMetadata.listType(port, "Layout", "60.0");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testListTypes_MultipleTypes() throws Exception {
        MetadataPortType port = mock(MetadataPortType.class);
        FileProperties prop1 = mock(FileProperties.class);
        FileProperties prop2 = mock(FileProperties.class);

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(Arrays.asList(prop1))
            .thenReturn(Arrays.asList(prop2));

        List<String> types = Arrays.asList("ApexClass", "ApexTrigger");
        Map<String, List<FileProperties>> results = ListMetadata.listTypes(port, types, 60.0);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey("ApexClass"));
        assertTrue(results.containsKey("ApexTrigger"));
        assertEquals(1, results.get("ApexClass").size());
        assertEquals(1, results.get("ApexTrigger").size());
    }

    @Test
    void testListAll_Success() throws Exception {
        MetadataPortType port = mock(MetadataPortType.class);
        DescribeMetadataResult describeResult = mock(DescribeMetadataResult.class);

        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("ApexClass");

        DescribeMetadataObject obj2 = mock(DescribeMetadataObject.class);
        when(obj2.getXmlName()).thenReturn("CustomObject");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj1, obj2));

        FileProperties prop1 = mock(FileProperties.class);
        FileProperties prop2 = mock(FileProperties.class);

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(Arrays.asList(prop1))
            .thenReturn(Arrays.asList(prop2));

        Map<String, List<FileProperties>> results =
            ListMetadata.listAll(port, describeResult, 60.0);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey("ApexClass"));
        assertTrue(results.containsKey("CustomObject"));
    }

    @Test
    void testListAll_HandlesErrors() throws Exception {
        MetadataPortType port = mock(MetadataPortType.class);
        DescribeMetadataResult describeResult = mock(DescribeMetadataResult.class);

        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("ApexClass");

        DescribeMetadataObject obj2 = mock(DescribeMetadataObject.class);
        when(obj2.getXmlName()).thenReturn("BadType");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj1, obj2));

        FileProperties prop1 = mock(FileProperties.class);

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(Arrays.asList(prop1))
            .thenThrow(new RuntimeException("Type not found"));

        Map<String, List<FileProperties>> results =
            ListMetadata.listAll(port, describeResult, 60.0);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey("ApexClass"));
        assertTrue(results.containsKey("BadType"));
        assertEquals(1, results.get("ApexClass").size());
        assertEquals(0, results.get("BadType").size()); // Error produces empty list
    }

    @Test
    void testListFolder_Success() throws Exception {
        MetadataPortType port = mock(MetadataPortType.class);
        FileProperties prop1 = mock(FileProperties.class);

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(Arrays.asList(prop1));

        List<FileProperties> result = ListMetadata.listFolder(
            port, "Report", "MyFolder", 60.0
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(port).listMetadata(anyList(), eq(60.0));
    }
}
