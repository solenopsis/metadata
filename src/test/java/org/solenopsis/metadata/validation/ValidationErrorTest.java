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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationError.
 *
 * @author Scot P. Floess
 */
class ValidationErrorTest {

    @Test
    void testConstructor_MessageOnly() {
        ValidationError error = new ValidationError("Test error");

        assertEquals(ValidationError.Severity.ERROR, error.getSeverity());
        assertEquals("Test error", error.getMessage());
        assertNull(error.getContext());
        assertTrue(error.isError());
        assertFalse(error.isWarning());
    }

    @Test
    void testConstructor_MessageAndContext() {
        ValidationError error = new ValidationError("Test error", "ApexClass");

        assertEquals(ValidationError.Severity.ERROR, error.getSeverity());
        assertEquals("Test error", error.getMessage());
        assertEquals("ApexClass", error.getContext());
    }

    @Test
    void testConstructor_Full() {
        ValidationError error = new ValidationError(
            ValidationError.Severity.WARNING,
            "Test warning",
            "CustomObject"
        );

        assertEquals(ValidationError.Severity.WARNING, error.getSeverity());
        assertEquals("Test warning", error.getMessage());
        assertEquals("CustomObject", error.getContext());
        assertFalse(error.isError());
        assertTrue(error.isWarning());
    }

    @Test
    void testToString_NoContext() {
        ValidationError error = new ValidationError("Missing version");

        assertEquals("ERROR: Missing version", error.toString());
    }

    @Test
    void testToString_WithContext() {
        ValidationError error = new ValidationError("Duplicate type", "ApexClass");

        assertEquals("ERROR: Duplicate type (ApexClass)", error.toString());
    }

    @Test
    void testToString_Warning() {
        ValidationError error = new ValidationError(
            ValidationError.Severity.WARNING,
            "Component not found",
            "MyClass"
        );

        assertEquals("WARNING: Component not found (MyClass)", error.toString());
    }

    @Test
    void testSeverityEnum() {
        assertEquals(2, ValidationError.Severity.values().length);
        assertEquals(ValidationError.Severity.ERROR, ValidationError.Severity.valueOf("ERROR"));
        assertEquals(ValidationError.Severity.WARNING, ValidationError.Severity.valueOf("WARNING"));
    }
}
