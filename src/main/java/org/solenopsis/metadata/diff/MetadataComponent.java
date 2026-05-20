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

/**
 * Represents a Salesforce metadata component identified from a file path.
 *
 * @author Scot P. Floess
 */
public class MetadataComponent {

    private final String type;
    private final String name;
    private final String filePath;
    private final boolean deleted;

    /**
     * Constructor for a metadata component.
     *
     * @param type the metadata type (e.g., "ApexClass", "CustomObject")
     * @param name the component name (e.g., "MyClass", "Account.MyField__c")
     * @param filePath the original file path
     * @param deleted true if this component was deleted
     */
    public MetadataComponent(String type, String name, String filePath, boolean deleted) {
        this.type = type;
        this.name = name;
        this.filePath = filePath;
        this.deleted = deleted;
    }

    /**
     * Constructor for a non-deleted component.
     *
     * @param type the metadata type
     * @param name the component name
     * @param filePath the original file path
     */
    public MetadataComponent(String type, String name, String filePath) {
        this(type, name, filePath, false);
    }

    /**
     * Get the metadata type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the component name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the original file path.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Check if this component was deleted.
     *
     * @return true if deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MetadataComponent that = (MetadataComponent) obj;
        return deleted == that.deleted &&
               type.equals(that.type) &&
               name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (deleted ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MetadataComponent{" +
               "type='" + type + '\'' +
               ", name='" + name + '\'' +
               ", deleted=" + deleted +
               ", filePath='" + filePath + '\'' +
               '}';
    }
}
