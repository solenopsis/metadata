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
package org.solenopsis.metadata.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holds statistics about metadata in a Salesforce org.
 *
 * @author Scot P. Floess
 */
public class OrgStats {

    private String apiVersion;
    private String organizationNamespace;
    private int totalComponents;
    private int totalTypes;
    private Map<String, Integer> typeCounts;
    private List<TypeCount> topTypes;

    /**
     * Default constructor.
     */
    public OrgStats() {
        this.topTypes = new ArrayList<>();
    }

    /**
     * Get the API version used for this statistics snapshot.
     *
     * @return the API version
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * Set the API version.
     *
     * @param apiVersion the API version
     */
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * Get the organization namespace.
     *
     * @return the organization namespace, or null if none
     */
    public String getOrganizationNamespace() {
        return organizationNamespace;
    }

    /**
     * Set the organization namespace.
     *
     * @param organizationNamespace the organization namespace
     */
    public void setOrganizationNamespace(String organizationNamespace) {
        this.organizationNamespace = organizationNamespace;
    }

    /**
     * Get the total number of metadata components across all types.
     *
     * @return total component count
     */
    public int getTotalComponents() {
        return totalComponents;
    }

    /**
     * Set the total number of metadata components.
     *
     * @param totalComponents total component count
     */
    public void setTotalComponents(int totalComponents) {
        this.totalComponents = totalComponents;
    }

    /**
     * Get the total number of metadata types.
     *
     * @return total type count
     */
    public int getTotalTypes() {
        return totalTypes;
    }

    /**
     * Set the total number of metadata types.
     *
     * @param totalTypes total type count
     */
    public void setTotalTypes(int totalTypes) {
        this.totalTypes = totalTypes;
    }

    /**
     * Get the component counts by metadata type.
     *
     * @return map of type name to component count
     */
    public Map<String, Integer> getTypeCounts() {
        return typeCounts;
    }

    /**
     * Set the component counts by metadata type.
     *
     * @param typeCounts map of type name to component count
     */
    public void setTypeCounts(Map<String, Integer> typeCounts) {
        this.typeCounts = typeCounts;
    }

    /**
     * Get the top metadata types by component count.
     *
     * @return list of top types, sorted by count descending
     */
    public List<TypeCount> getTopTypes() {
        return topTypes;
    }

    /**
     * Set the top metadata types.
     *
     * @param topTypes list of top types
     */
    public void setTopTypes(List<TypeCount> topTypes) {
        this.topTypes = topTypes;
    }

    /**
     * Represents a metadata type and its component count.
     */
    public static class TypeCount {
        private String type;
        private int count;

        /**
         * Constructor.
         *
         * @param type the metadata type name
         * @param count the component count
         */
        public TypeCount(String type, int count) {
            this.type = type;
            this.count = count;
        }

        /**
         * Get the metadata type name.
         *
         * @return the type name
         */
        public String getType() {
            return type;
        }

        /**
         * Set the metadata type name.
         *
         * @param type the type name
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Get the component count.
         *
         * @return the count
         */
        public int getCount() {
            return count;
        }

        /**
         * Set the component count.
         *
         * @param count the count
         */
        public void setCount(int count) {
            this.count = count;
        }
    }
}
