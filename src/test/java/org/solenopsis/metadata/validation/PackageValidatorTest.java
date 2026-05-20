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
package org.solenopsis.metadata.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.solenopsis.soap.metadata.DescribeMetadataObject;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.FileProperties;
import org.solenopsis.soap.metadata.MetadataPortType;
import org.solenopsis.soap.metadata.Package;
import org.solenopsis.soap.metadata.PackageTypeMembers;

/**
 * Tests for PackageValidator.
 *
 * @author Scot P. Floess
 */
class PackageValidatorTest {

    private Package pkg;
    private MetadataPortType port;
    private DescribeMetadataResult describeResult;

    @BeforeEach
    void setUp() {
        pkg = new Package();
        port = mock(MetadataPortType.class);
        describeResult = mock(DescribeMetadataResult.class);
    }

    @Test
    void testValidateStructure_ValidPackage() {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("MyClass");

        pkg.getTypes().add(type);

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testValidateStructure_MissingVersion() {
        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("MyClass");

        pkg.getTypes().add(type);

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Missing or empty version")));
    }

    @Test
    void testValidateStructure_InvalidVersion() {
        pkg.setVersion("invalid");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("MyClass");

        pkg.getTypes().add(type);

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Invalid version format")));
    }

    @Test
    void testValidateStructure_NoTypes() {
        pkg.setVersion("60.0");

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("No types defined")));
    }

    @Test
    void testValidateStructure_TypeMissingName() {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.getMembers().add("MyClass");

        pkg.getTypes().add(type);

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Type missing name")));
    }

    @Test
    void testValidateStructure_DuplicateType() {
        pkg.setVersion("60.0");

        PackageTypeMembers type1 = new PackageTypeMembers();
        type1.setName("ApexClass");
        type1.getMembers().add("Class1");

        PackageTypeMembers type2 = new PackageTypeMembers();
        type2.setName("ApexClass");
        type2.getMembers().add("Class2");

        pkg.getTypes().add(type1);
        pkg.getTypes().add(type2);

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Duplicate type definition")));
    }

    @Test
    void testValidateStructure_TypeWithNoMembers() {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");

        pkg.getTypes().add(type);

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertTrue(result.isValid()); // Valid but has warnings
        assertEquals(1, result.getWarningCount());
        assertTrue(result.getWarnings().stream()
            .anyMatch(e -> e.getMessage().contains("Type has no members")));
    }

    @Test
    void testValidateStructure_EmptyMemberName() {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("");

        pkg.getTypes().add(type);

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Empty member name")));
    }

    @Test
    void testValidateStructure_DuplicateMember() {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("MyClass");
        type.getMembers().add("MyClass");

        pkg.getTypes().add(type);

        ValidationResult result = PackageValidator.validateStructure(pkg);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Duplicate member: MyClass")));
    }

    @Test
    void testValidateAgainstOrg_ValidPackage() throws Exception {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("MyClass");

        pkg.getTypes().add(type);

        // Setup org metadata
        DescribeMetadataObject obj = mock(DescribeMetadataObject.class);
        when(obj.getXmlName()).thenReturn("ApexClass");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj));

        when(port.describeMetadata(60.0)).thenReturn(describeResult);

        FileProperties prop = mock(FileProperties.class);
        when(prop.getFullName()).thenReturn("MyClass");

        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(Arrays.asList(prop));

        ValidationResult result = PackageValidator.validateAgainstOrg(pkg, port, "60.0");

        assertTrue(result.isValid());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testValidateAgainstOrg_TypeNotFound() throws Exception {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("UnknownType");
        type.getMembers().add("Component");

        pkg.getTypes().add(type);

        when(describeResult.getMetadataObjects())
            .thenReturn(new ArrayList<>());

        when(port.describeMetadata(60.0)).thenReturn(describeResult);

        ValidationResult result = PackageValidator.validateAgainstOrg(pkg, port, "60.0");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Type not found in org")));
    }

    @Test
    void testValidateAgainstOrg_MemberNotFound() throws Exception {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("MissingClass");

        pkg.getTypes().add(type);

        DescribeMetadataObject obj = mock(DescribeMetadataObject.class);
        when(obj.getXmlName()).thenReturn("ApexClass");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj));

        when(port.describeMetadata(60.0)).thenReturn(describeResult);
        when(port.listMetadata(anyList(), eq(60.0)))
            .thenReturn(new ArrayList<>()); // No components

        ValidationResult result = PackageValidator.validateAgainstOrg(pkg, port, "60.0");

        assertTrue(result.isValid()); // Valid but has warnings
        assertEquals(1, result.getWarningCount());
        assertTrue(result.getWarnings().stream()
            .anyMatch(e -> e.getMessage().contains("Member not found in org")));
    }

    @Test
    void testValidateAgainstOrg_WildcardMember() throws Exception {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("*");

        pkg.getTypes().add(type);

        DescribeMetadataObject obj = mock(DescribeMetadataObject.class);
        when(obj.getXmlName()).thenReturn("ApexClass");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj));

        when(port.describeMetadata(60.0)).thenReturn(describeResult);

        ValidationResult result = PackageValidator.validateAgainstOrg(pkg, port, "60.0");

        assertTrue(result.isValid());
        assertEquals(0, result.getWarningCount()); // Wildcard is valid
    }

    @Test
    void testValidateAgainstOrg_WildcardWithSpecificMembers() throws Exception {
        pkg.setVersion("60.0");

        PackageTypeMembers type = new PackageTypeMembers();
        type.setName("ApexClass");
        type.getMembers().add("*");
        type.getMembers().add("MyClass");

        pkg.getTypes().add(type);

        DescribeMetadataObject obj = mock(DescribeMetadataObject.class);
        when(obj.getXmlName()).thenReturn("ApexClass");

        when(describeResult.getMetadataObjects())
            .thenReturn(Arrays.asList(obj));

        when(port.describeMetadata(60.0)).thenReturn(describeResult);

        ValidationResult result = PackageValidator.validateAgainstOrg(pkg, port, "60.0");

        assertTrue(result.isValid());
        assertEquals(1, result.getWarningCount());
        assertTrue(result.getWarnings().stream()
            .anyMatch(e -> e.getMessage().contains("Wildcard mixed with specific members")));
    }

    @Test
    void testValidateVersion_Compatible() {
        pkg.setVersion("60.0");

        ValidationResult result = PackageValidator.validateVersion(pkg, "60.0");

        assertTrue(result.isValid());
        assertEquals(0, result.getWarningCount());
    }

    @Test
    void testValidateVersion_PackageNewer() {
        pkg.setVersion("61.0");

        ValidationResult result = PackageValidator.validateVersion(pkg, "60.0");

        assertTrue(result.isValid());
        assertEquals(1, result.getWarningCount());
        assertTrue(result.getWarnings().stream()
            .anyMatch(e -> e.getMessage().contains("higher than org version")));
    }

    @Test
    void testValidateVersion_InvalidFormat() {
        pkg.setVersion("invalid");

        ValidationResult result = PackageValidator.validateVersion(pkg, "60.0");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Invalid version format")));
    }
}
