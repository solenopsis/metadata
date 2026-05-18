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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Types.
 *
 * @author Scot P. Floess
 */
@ExtendWith(MockitoExtension.class)
class TypesTest {

    @Mock
    private DescribeMetadataObject mockMetadataObject;

    @Test
    void testDefaultConstructor() {
        Types types = new Types();
        assertNotNull(types.getMembers());
        assertTrue(types.getMembers().isEmpty());
        assertNull(types.getName());
    }

    @Test
    void testConstructorWithDescribeMetadataObject() {
        List<String> childNames = new ArrayList<>();
        childNames.add("Child1");
        childNames.add("Child2");

        when(mockMetadataObject.getXmlName()).thenReturn("ApexClass");
        when(mockMetadataObject.getChildXmlNames()).thenReturn(childNames);

        Types types = new Types(mockMetadataObject);

        assertEquals("ApexClass", types.getName());
        assertEquals(2, types.getMembers().size());
        assertTrue(types.getMembers().contains("Child1"));
        assertTrue(types.getMembers().contains("Child2"));
    }

    @Test
    void testSettersAndGetters() {
        Types types = new Types();

        types.setName("CustomObject");
        assertEquals("CustomObject", types.getName());

        List<String> members = new ArrayList<>();
        members.add("Account");
        members.add("Contact");

        types.setMembers(members);
        assertEquals(2, types.getMembers().size());
        assertTrue(types.getMembers().contains("Account"));
        assertTrue(types.getMembers().contains("Contact"));
    }

    @Test
    void testCreateTypesFromList() {
        List<DescribeMetadataObject> metadataList = new ArrayList<>();

        DescribeMetadataObject obj1 = mock(DescribeMetadataObject.class);
        when(obj1.getXmlName()).thenReturn("ApexClass");
        when(obj1.getChildXmlNames()).thenReturn(new ArrayList<>());

        DescribeMetadataObject obj2 = mock(DescribeMetadataObject.class);
        when(obj2.getXmlName()).thenReturn("ApexPage");
        when(obj2.getChildXmlNames()).thenReturn(new ArrayList<>());

        metadataList.add(obj1);
        metadataList.add(obj2);

        List<Types> typesList = Types.createTypes(metadataList);

        assertEquals(2, typesList.size());
        assertEquals("ApexClass", typesList.get(0).getName());
        assertEquals("ApexPage", typesList.get(1).getName());
    }

    @Test
    void testCreateTypesWithEmptyList() {
        List<DescribeMetadataObject> metadataList = new ArrayList<>();
        List<Types> typesList = Types.createTypes(metadataList);

        assertNotNull(typesList);
        assertTrue(typesList.isEmpty());
    }
}
