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
package org.solenopsis.metadata.diff;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FilePathMapper.
 *
 * @author Scot P. Floess
 */
class FilePathMapperTest {

    @Test
    void testParseFilePath_ApexClass() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/classes/MyClass.cls");

        assertNotNull(component);
        assertEquals("ApexClass", component.getType());
        assertEquals("MyClass", component.getName());
        assertFalse(component.isDeleted());
    }

    @Test
    void testParseFilePath_ApexClassWithMeta() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/classes/MyClass.cls-meta.xml");

        assertNotNull(component);
        assertEquals("ApexClass", component.getType());
        assertEquals("MyClass", component.getName());
    }

    @Test
    void testParseFilePath_ApexTrigger() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/triggers/AccountTrigger.trigger");

        assertNotNull(component);
        assertEquals("ApexTrigger", component.getType());
        assertEquals("AccountTrigger", component.getName());
    }

    @Test
    void testParseFilePath_ApexPage() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/pages/MyPage.page");

        assertNotNull(component);
        assertEquals("ApexPage", component.getType());
        assertEquals("MyPage", component.getName());
    }

    @Test
    void testParseFilePath_LWC() {
        MetadataComponent component = FilePathMapper.parseFilePath("force-app/main/default/lwc/myComponent/myComponent.js");

        assertNotNull(component);
        assertEquals("LightningComponentBundle", component.getType());
        assertEquals("myComponent", component.getName());
    }

    @Test
    void testParseFilePath_CustomField() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/objects/Account/fields/MyField__c.field-meta.xml");

        assertNotNull(component);
        assertEquals("CustomField", component.getType());
        assertEquals("Account.MyField__c", component.getName());
    }

    @Test
    void testParseFilePath_ValidationRule() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/objects/Account/validationRules/MyRule.validationRule-meta.xml");

        assertNotNull(component);
        assertEquals("ValidationRule", component.getType());
        assertEquals("Account.MyRule", component.getName());
    }

    @Test
    void testParseFilePath_RecordType() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/objects/Opportunity/recordTypes/Enterprise.recordType-meta.xml");

        assertNotNull(component);
        assertEquals("RecordType", component.getType());
        assertEquals("Opportunity.Enterprise", component.getName());
    }

    @Test
    void testParseFilePath_Layout() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/layouts/Account-Account%20Layout.layout-meta.xml");

        assertNotNull(component);
        assertEquals("Layout", component.getType());
        assertEquals("Account-Account%20Layout", component.getName());
    }

    @Test
    void testParseFilePath_LayoutSimple() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/layouts/MyLayout.layout");

        assertNotNull(component);
        assertEquals("Layout", component.getType());
        assertEquals("MyLayout", component.getName());
    }

    @Test
    void testParseFilePath_StaticResource() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/staticresources/MyResource.resource");

        assertNotNull(component);
        assertEquals("StaticResource", component.getType());
        assertEquals("MyResource", component.getName());
    }

    @Test
    void testParseFilePath_PermissionSet() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/permissionsets/MyPermSet.permissionset");

        assertNotNull(component);
        assertEquals("PermissionSet", component.getType());
        assertEquals("MyPermSet", component.getName());
    }

    @Test
    void testParseFilePath_Profile() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/profiles/Admin.profile");

        assertNotNull(component);
        assertEquals("Profile", component.getType());
        assertEquals("Admin", component.getName());
    }

    @Test
    void testParseFilePath_Flow() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/flows/MyFlow.flow");

        assertNotNull(component);
        assertEquals("Flow", component.getType());
        assertEquals("MyFlow", component.getName());
    }

    @Test
    void testParseFilePath_Null() {
        MetadataComponent component = FilePathMapper.parseFilePath(null);

        assertNull(component);
    }

    @Test
    void testParseFilePath_Empty() {
        MetadataComponent component = FilePathMapper.parseFilePath("");

        assertNull(component);
    }

    @Test
    void testParseFilePath_NonMetadata() {
        MetadataComponent component = FilePathMapper.parseFilePath("README.md");

        assertNull(component);
    }

    @Test
    void testParseFilePath_UnknownFolder() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/unknown/Something.txt");

        assertNull(component);
    }

    @Test
    void testParseFilePath_WithDeleted() {
        MetadataComponent component = FilePathMapper.parseFilePath("src/classes/MyClass.cls", true);

        assertNotNull(component);
        assertEquals("ApexClass", component.getType());
        assertEquals("MyClass", component.getName());
        assertTrue(component.isDeleted());
    }

    @Test
    void testParseFilePath_WindowsPaths() {
        MetadataComponent component = FilePathMapper.parseFilePath("src\\classes\\MyClass.cls");

        assertNotNull(component);
        assertEquals("ApexClass", component.getType());
        assertEquals("MyClass", component.getName());
    }

    @Test
    void testIsMetadataPath_True() {
        assertTrue(FilePathMapper.isMetadataPath("src/classes/MyClass.cls"));
        assertTrue(FilePathMapper.isMetadataPath("src/objects/Account/fields/MyField__c.field-meta.xml"));
        assertTrue(FilePathMapper.isMetadataPath("src/layouts/MyLayout.layout"));
    }

    @Test
    void testIsMetadataPath_False() {
        assertFalse(FilePathMapper.isMetadataPath("README.md"));
        assertFalse(FilePathMapper.isMetadataPath("pom.xml"));
        assertFalse(FilePathMapper.isMetadataPath(null));
    }
}
