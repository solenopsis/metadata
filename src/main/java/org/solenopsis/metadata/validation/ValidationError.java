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

/**
 * Represents a validation error found in a package.xml file.
 *
 * @author Scot P. Floess
 */
public class ValidationError {

    /**
     * Severity levels for validation errors.
     */
    public enum Severity {
        ERROR,
        WARNING
    }

    private final Severity severity;
    private final String message;
    private final String context;

    /**
     * Constructor for an error without context.
     *
     * @param message the error message
     */
    public ValidationError(String message) {
        this(Severity.ERROR, message, null);
    }

    /**
     * Constructor for an error with context.
     *
     * @param message the error message
     * @param context additional context (e.g., type name, line number)
     */
    public ValidationError(String message, String context) {
        this(Severity.ERROR, message, context);
    }

    /**
     * Constructor with severity, message, and context.
     *
     * @param severity the error severity
     * @param message the error message
     * @param context additional context
     */
    public ValidationError(Severity severity, String message, String context) {
        this.severity = severity;
        this.message = message;
        this.context = context;
    }

    /**
     * Get the error severity.
     *
     * @return the severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Get the error message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the error context.
     *
     * @return the context, or null if none
     */
    public String getContext() {
        return context;
    }

    /**
     * Check if this is an error (vs warning).
     *
     * @return true if severity is ERROR
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

    /**
     * Check if this is a warning (vs error).
     *
     * @return true if severity is WARNING
     */
    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(severity).append(": ").append(message);
        if (context != null && !context.isEmpty()) {
            sb.append(" (").append(context).append(")");
        }
        return sb.toString();
    }
}
