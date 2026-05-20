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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds the results of package.xml validation.
 *
 * @author Scot P. Floess
 */
public class ValidationResult {

    private final List<ValidationError> errors;

    /**
     * Constructor.
     */
    public ValidationResult() {
        this.errors = new ArrayList<>();
    }

    /**
     * Constructor with initial errors.
     *
     * @param errors list of errors
     */
    public ValidationResult(List<ValidationError> errors) {
        this.errors = new ArrayList<>(errors);
    }

    /**
     * Add a validation error.
     *
     * @param error the error to add
     */
    public void addError(ValidationError error) {
        errors.add(error);
    }

    /**
     * Add a validation error with message only.
     *
     * @param message the error message
     */
    public void addError(String message) {
        errors.add(new ValidationError(message));
    }

    /**
     * Add a validation error with message and context.
     *
     * @param message the error message
     * @param context the error context
     */
    public void addError(String message, String context) {
        errors.add(new ValidationError(message, context));
    }

    /**
     * Add a validation warning.
     *
     * @param message the warning message
     * @param context the warning context
     */
    public void addWarning(String message, String context) {
        errors.add(new ValidationError(ValidationError.Severity.WARNING, message, context));
    }

    /**
     * Get all validation errors and warnings.
     *
     * @return list of all errors
     */
    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Get only validation errors (not warnings).
     *
     * @return list of errors
     */
    public List<ValidationError> getErrorsOnly() {
        return errors.stream()
            .filter(ValidationError::isError)
            .collect(Collectors.toList());
    }

    /**
     * Get only validation warnings.
     *
     * @return list of warnings
     */
    public List<ValidationError> getWarnings() {
        return errors.stream()
            .filter(ValidationError::isWarning)
            .collect(Collectors.toList());
    }

    /**
     * Check if validation passed (no errors).
     *
     * @return true if no errors
     */
    public boolean isValid() {
        return getErrorsOnly().isEmpty();
    }

    /**
     * Check if there are any errors or warnings.
     *
     * @return true if errors list is empty
     */
    public boolean hasNoIssues() {
        return errors.isEmpty();
    }

    /**
     * Get the total number of errors.
     *
     * @return error count
     */
    public int getErrorCount() {
        return (int) errors.stream().filter(ValidationError::isError).count();
    }

    /**
     * Get the total number of warnings.
     *
     * @return warning count
     */
    public int getWarningCount() {
        return (int) errors.stream().filter(ValidationError::isWarning).count();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation Result: ");

        if (hasNoIssues()) {
            sb.append("PASSED - No issues found");
        } else {
            sb.append(getErrorCount()).append(" error(s), ");
            sb.append(getWarningCount()).append(" warning(s)\n");

            for (ValidationError error : errors) {
                sb.append("  ").append(error.toString()).append("\n");
            }
        }

        return sb.toString();
    }
}
