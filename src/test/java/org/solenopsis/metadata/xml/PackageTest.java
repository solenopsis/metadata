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
package org.solenopsis.metadata.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.solenopsis.soap.metadata.DescribeMetadataObject;
import org.solenopsis.soap.metadata.DescribeMetadataResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Package.
 *
 * @author Scot P. Floess
 */
@ExtendWith(MockitoExtension.class)
class PackageTest {

    @Mock
    private DescribeMetadataResult mockMetadataResult;

    @Mock
    private DescribeMetadataObject mockMetadataObject;

    @Test
    void testDefaultConstructor() {
        Package pkg = new Package();
        assertNotNull(pkg.getTypes());
        assertTrue(pkg.getTypes().isEmpty());
        assertNull(pkg.getVersion());
    }

    @Test
    void testConstructorWithMetadataListAndVersion() {
        List<DescribeMetadataObject> metadataList = new ArrayList<>();

        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("ApexClass");
        when(obj1.getChildXmlNames()).thenReturn(new ArrayList<>());

        metadataList.add(obj1);

        Package pkg = new Package(metadataList, "60.0");

        assertEquals("60.0", pkg.getVersion());
        assertEquals(1, pkg.getTypes().size());
        assertEquals("ApexClass", pkg.getTypes().get(0).getName());
    }

    @Test
    void testConstructorWithDescribeMetadataResult() {
        List<DescribeMetadataObject> metadataList = new ArrayList<>();

        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("CustomObject");
        when(obj1.getChildXmlNames()).thenReturn(new ArrayList<>());

        metadataList.add(obj1);

        when(mockMetadataResult.getMetadataObjects()).thenReturn(metadataList);

        Package pkg = new Package(mockMetadataResult, "59.0");

        assertEquals("59.0", pkg.getVersion());
        assertEquals(1, pkg.getTypes().size());
        assertEquals("CustomObject", pkg.getTypes().get(0).getName());
    }

    @Test
    void testSettersAndGetters() {
        Package pkg = new Package();

        pkg.setVersion("58.0");
        assertEquals("58.0", pkg.getVersion());

        List<Types> typesList = new ArrayList<>();
        Types types = new Types();
        types.setName("ApexTrigger");
        typesList.add(types);

        pkg.setTypes(typesList);
        assertEquals(1, pkg.getTypes().size());
        assertEquals("ApexTrigger", pkg.getTypes().get(0).getName());
    }

    @Test
    void testWithMultipleMetadataTypes() {
        List<DescribeMetadataObject> metadataList = new ArrayList<>();

        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("ApexClass");
        when(obj1.getChildXmlNames()).thenReturn(new ArrayList<>());

        DescribeMetadataObject obj2 = mock(DescribeMetadataObject.class);
        when(obj2.getXmlName()).thenReturn("ApexPage");
        when(obj2.getChildXmlNames()).thenReturn(new ArrayList<>());

        DescribeMetadataObject obj3 = mock(DescribeMetadataObject.class);
        when(obj3.getXmlName()).thenReturn("CustomObject");
        when(obj3.getChildXmlNames()).thenReturn(new ArrayList<>());

        metadataList.add(obj1);
        metadataList.add(obj2);
        metadataList.add(obj3);

        Package pkg = new Package(metadataList, "60.0");

        assertEquals("60.0", pkg.getVersion());
        assertEquals(3, pkg.getTypes().size());
    }

    @Test
    void testWithEmptyMetadataList() {
        List<DescribeMetadataObject> metadataList = new ArrayList<>();
        Package pkg = new Package(metadataList, "60.0");

        assertEquals("60.0", pkg.getVersion());
        assertNotNull(pkg.getTypes());
        assertTrue(pkg.getTypes().isEmpty());
    }
}
