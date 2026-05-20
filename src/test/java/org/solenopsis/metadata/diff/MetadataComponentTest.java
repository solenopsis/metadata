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
 * Tests for MetadataComponent.
 *
 * @author Scot P. Floess
 */
class MetadataComponentTest {

    @Test
    void testConstructor_Full() {
        MetadataComponent component = new MetadataComponent("ApexClass", "MyClass", "src/classes/MyClass.cls", true);

        assertEquals("ApexClass", component.getType());
        assertEquals("MyClass", component.getName());
        assertEquals("src/classes/MyClass.cls", component.getFilePath());
        assertTrue(component.isDeleted());
    }

    @Test
    void testConstructor_WithoutDeleted() {
        MetadataComponent component = new MetadataComponent("ApexTrigger", "MyTrigger", "src/triggers/MyTrigger.trigger");

        assertEquals("ApexTrigger", component.getType());
        assertEquals("MyTrigger", component.getName());
        assertEquals("src/triggers/MyTrigger.trigger", component.getFilePath());
        assertFalse(component.isDeleted());
    }

    @Test
    void testEquals_SameObject() {
        MetadataComponent component = new MetadataComponent("ApexClass", "MyClass", "path");

        assertEquals(component, component);
    }

    @Test
    void testEquals_EqualComponents() {
        MetadataComponent comp1 = new MetadataComponent("ApexClass", "MyClass", "path1");
        MetadataComponent comp2 = new MetadataComponent("ApexClass", "MyClass", "path2");

        assertEquals(comp1, comp2);
    }

    @Test
    void testEquals_DifferentType() {
        MetadataComponent comp1 = new MetadataComponent("ApexClass", "MyClass", "path");
        MetadataComponent comp2 = new MetadataComponent("ApexTrigger", "MyClass", "path");

        assertNotEquals(comp1, comp2);
    }

    @Test
    void testEquals_DifferentName() {
        MetadataComponent comp1 = new MetadataComponent("ApexClass", "Class1", "path");
        MetadataComponent comp2 = new MetadataComponent("ApexClass", "Class2", "path");

        assertNotEquals(comp1, comp2);
    }

    @Test
    void testEquals_DifferentDeleted() {
        MetadataComponent comp1 = new MetadataComponent("ApexClass", "MyClass", "path", false);
        MetadataComponent comp2 = new MetadataComponent("ApexClass", "MyClass", "path", true);

        assertNotEquals(comp1, comp2);
    }

    @Test
    void testEquals_Null() {
        MetadataComponent component = new MetadataComponent("ApexClass", "MyClass", "path");

        assertNotEquals(component, null);
    }

    @Test
    void testEquals_DifferentClass() {
        MetadataComponent component = new MetadataComponent("ApexClass", "MyClass", "path");

        assertNotEquals(component, "Not a MetadataComponent");
    }

    @Test
    void testHashCode_EqualComponents() {
        MetadataComponent comp1 = new MetadataComponent("ApexClass", "MyClass", "path1");
        MetadataComponent comp2 = new MetadataComponent("ApexClass", "MyClass", "path2");

        assertEquals(comp1.hashCode(), comp2.hashCode());
    }

    @Test
    void testToString() {
        MetadataComponent component = new MetadataComponent("ApexClass", "MyClass", "src/classes/MyClass.cls", true);

        String str = component.toString();

        assertTrue(str.contains("ApexClass"));
        assertTrue(str.contains("MyClass"));
        assertTrue(str.contains("deleted=true"));
        assertTrue(str.contains("src/classes/MyClass.cls"));
    }
}
