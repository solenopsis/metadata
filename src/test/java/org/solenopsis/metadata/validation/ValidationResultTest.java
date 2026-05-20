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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for ValidationResult.
 *
 * @author Scot P. Floess
 */
class ValidationResultTest {

    @Test
    void testDefaultConstructor() {
        ValidationResult result = new ValidationResult();

        assertTrue(result.isValid());
        assertTrue(result.hasNoIssues());
        assertEquals(0, result.getErrorCount());
        assertEquals(0, result.getWarningCount());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testConstructor_WithErrors() {
        List<ValidationError> errors = Arrays.asList(
            new ValidationError("Error 1"),
            new ValidationError("Error 2")
        );

        ValidationResult result = new ValidationResult(errors);

        assertFalse(result.isValid());
        assertEquals(2, result.getErrorCount());
        assertEquals(2, result.getErrors().size());
    }

    @Test
    void testAddError_MessageOnly() {
        ValidationResult result = new ValidationResult();
        result.addError("Test error");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
        assertEquals("Test error", result.getErrors().get(0).getMessage());
    }

    @Test
    void testAddError_MessageAndContext() {
        ValidationResult result = new ValidationResult();
        result.addError("Test error", "ApexClass");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
        assertEquals("ApexClass", result.getErrors().get(0).getContext());
    }

    @Test
    void testAddError_ValidationError() {
        ValidationResult result = new ValidationResult();
        ValidationError error = new ValidationError("Test error");
        result.addError(error);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
    }

    @Test
    void testAddWarning() {
        ValidationResult result = new ValidationResult();
        result.addWarning("Test warning", "CustomObject");

        assertTrue(result.isValid()); // Still valid with only warnings
        assertFalse(result.hasNoIssues()); // But has issues
        assertEquals(0, result.getErrorCount());
        assertEquals(1, result.getWarningCount());
    }

    @Test
    void testGetErrorsOnly() {
        ValidationResult result = new ValidationResult();
        result.addError("Error 1");
        result.addWarning("Warning 1", "Test");
        result.addError("Error 2");

        List<ValidationError> errors = result.getErrorsOnly();

        assertEquals(2, errors.size());
        assertTrue(errors.stream().allMatch(ValidationError::isError));
    }

    @Test
    void testGetWarnings() {
        ValidationResult result = new ValidationResult();
        result.addError("Error 1");
        result.addWarning("Warning 1", "Test");
        result.addWarning("Warning 2", "Test");

        List<ValidationError> warnings = result.getWarnings();

        assertEquals(2, warnings.size());
        assertTrue(warnings.stream().allMatch(ValidationError::isWarning));
    }

    @Test
    void testIsValid_WithErrors() {
        ValidationResult result = new ValidationResult();
        result.addError("Test error");

        assertFalse(result.isValid());
    }

    @Test
    void testIsValid_WithWarningsOnly() {
        ValidationResult result = new ValidationResult();
        result.addWarning("Test warning", "Test");

        assertTrue(result.isValid());
    }

    @Test
    void testHasNoIssues() {
        ValidationResult result = new ValidationResult();

        assertTrue(result.hasNoIssues());

        result.addWarning("Warning", "Test");

        assertFalse(result.hasNoIssues());
    }

    @Test
    void testToString_NoIssues() {
        ValidationResult result = new ValidationResult();
        String output = result.toString();

        assertTrue(output.contains("PASSED"));
        assertTrue(output.contains("No issues found"));
    }

    @Test
    void testToString_WithIssues() {
        ValidationResult result = new ValidationResult();
        result.addError("Error 1");
        result.addWarning("Warning 1", "Test");

        String output = result.toString();

        assertTrue(output.contains("1 error(s)"));
        assertTrue(output.contains("1 warning(s)"));
        assertTrue(output.contains("Error 1"));
        assertTrue(output.contains("Warning 1"));
    }

    @Test
    void testGetErrors_ReturnsDefensiveCopy() {
        ValidationResult result = new ValidationResult();
        result.addError("Error 1");

        List<ValidationError> errors1 = result.getErrors();
        List<ValidationError> errors2 = result.getErrors();

        assertNotSame(errors1, errors2);
        assertEquals(errors1.size(), errors2.size());
    }
}
