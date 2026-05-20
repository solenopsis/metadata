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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.solenopsis.soap.metadata.Package;
import org.solenopsis.soap.metadata.PackageTypeMembers;

/**
 * Tests for DiffPackageGenerator.
 *
 * @author Scot P. Floess
 */
class DiffPackageGeneratorTest {

    @Test
    void testGeneratePackage_SingleType() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "Class1", "path1"),
            new MetadataComponent("ApexClass", "Class2", "path2")
        );

        Package pkg = DiffPackageGenerator.generatePackage(components, "60.0");

        assertEquals("60.0", pkg.getVersion());
        assertEquals(1, pkg.getTypes().size());

        PackageTypeMembers type = pkg.getTypes().get(0);
        assertEquals("ApexClass", type.getName());
        assertEquals(2, type.getMembers().size());
        assertTrue(type.getMembers().contains("Class1"));
        assertTrue(type.getMembers().contains("Class2"));
    }

    @Test
    void testGeneratePackage_MultipleTypes() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "MyClass", "path1"),
            new MetadataComponent("ApexTrigger", "MyTrigger", "path2"),
            new MetadataComponent("ApexPage", "MyPage", "path3")
        );

        Package pkg = DiffPackageGenerator.generatePackage(components, "60.0");

        assertEquals(3, pkg.getTypes().size());
    }

    @Test
    void testGeneratePackage_ExcludeDeleted() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "Class1", "path1", false),
            new MetadataComponent("ApexClass", "Class2", "path2", true),
            new MetadataComponent("ApexClass", "Class3", "path3", false)
        );

        Package pkg = DiffPackageGenerator.generatePackage(components, "60.0", false);

        assertEquals(1, pkg.getTypes().size());
        PackageTypeMembers type = pkg.getTypes().get(0);
        assertEquals(2, type.getMembers().size());
        assertTrue(type.getMembers().contains("Class1"));
        assertTrue(type.getMembers().contains("Class3"));
        assertFalse(type.getMembers().contains("Class2"));
    }

    @Test
    void testGeneratePackage_OnlyDeleted() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "Class1", "path1", false),
            new MetadataComponent("ApexClass", "Class2", "path2", true),
            new MetadataComponent("ApexTrigger", "Trigger1", "path3", true)
        );

        Package pkg = DiffPackageGenerator.generatePackage(components, "60.0", true);

        assertEquals(2, pkg.getTypes().size());

        // Find ApexClass type
        PackageTypeMembers apexClass = pkg.getTypes().stream()
            .filter(t -> "ApexClass".equals(t.getName()))
            .findFirst()
            .orElse(null);

        assertNotNull(apexClass);
        assertEquals(1, apexClass.getMembers().size());
        assertTrue(apexClass.getMembers().contains("Class2"));
    }

    @Test
    void testGenerateDeployPackage() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "Class1", "path1", false),
            new MetadataComponent("ApexClass", "Class2", "path2", true)
        );

        Package pkg = DiffPackageGenerator.generateDeployPackage(components, "60.0");

        assertEquals(1, pkg.getTypes().size());
        assertEquals(1, pkg.getTypes().get(0).getMembers().size());
        assertTrue(pkg.getTypes().get(0).getMembers().contains("Class1"));
    }

    @Test
    void testGenerateDestructiveChanges() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "Class1", "path1", false),
            new MetadataComponent("ApexClass", "Class2", "path2", true)
        );

        Package pkg = DiffPackageGenerator.generateDestructiveChanges(components, "60.0");

        assertEquals(1, pkg.getTypes().size());
        assertEquals(1, pkg.getTypes().get(0).getMembers().size());
        assertTrue(pkg.getTypes().get(0).getMembers().contains("Class2"));
    }

    @Test
    void testGeneratePackage_EmptyComponents() {
        List<MetadataComponent> components = Arrays.asList();

        Package pkg = DiffPackageGenerator.generatePackage(components, "60.0");

        assertEquals("60.0", pkg.getVersion());
        assertEquals(0, pkg.getTypes().size());
    }

    @Test
    void testHasDeletedComponents_True() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "Class1", "path1", false),
            new MetadataComponent("ApexClass", "Class2", "path2", true)
        );

        assertTrue(DiffPackageGenerator.hasDeletedComponents(components));
    }

    @Test
    void testHasDeletedComponents_False() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "Class1", "path1", false),
            new MetadataComponent("ApexClass", "Class2", "path2", false)
        );

        assertFalse(DiffPackageGenerator.hasDeletedComponents(components));
    }

    @Test
    void testHasDeletedComponents_Empty() {
        List<MetadataComponent> components = Arrays.asList();

        assertFalse(DiffPackageGenerator.hasDeletedComponents(components));
    }

    @Test
    void testCountByType() {
        List<MetadataComponent> components = Arrays.asList(
            new MetadataComponent("ApexClass", "Class1", "path1"),
            new MetadataComponent("ApexClass", "Class2", "path2"),
            new MetadataComponent("ApexTrigger", "Trigger1", "path3"),
            new MetadataComponent("ApexPage", "Page1", "path4"),
            new MetadataComponent("ApexPage", "Page2", "path5"),
            new MetadataComponent("ApexPage", "Page3", "path6")
        );

        Map<String, Integer> counts = DiffPackageGenerator.countByType(components);

        assertEquals(3, counts.size());
        assertEquals(2, counts.get("ApexClass"));
        assertEquals(1, counts.get("ApexTrigger"));
        assertEquals(3, counts.get("ApexPage"));
    }

    @Test
    void testCountByType_Empty() {
        List<MetadataComponent> components = Arrays.asList();

        Map<String, Integer> counts = DiffPackageGenerator.countByType(components);

        assertTrue(counts.isEmpty());
    }
}
