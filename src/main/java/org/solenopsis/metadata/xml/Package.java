/*
 * Copyright (C) 2017 Scot P. Floess
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
package org.solenopsis.metadata.xml;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.solenopsis.soap.metadata.DescribeMetadataObject;
import org.solenopsis.soap.metadata.DescribeMetadataResult;

/**
 * The package root element.
 *
 * @author Scot P. Floess
 */
@XmlRootElement(name = "package")
@XmlType(propOrder = {"types, version"})
public class Package {
    private List<Types> typesList;

    private String version;

    /**
     * Default constructor. Initializes an empty types list.
     */
    public Package() {
        this.typesList = new ArrayList<>();
    }

    /**
     * Constructs a package from a list of metadata objects.
     *
     * @param metadataList list of Salesforce metadata objects
     * @param version the Salesforce API version
     */
    public Package(final List<DescribeMetadataObject> metadataList, final String version) {
        this.typesList = Types.createTypes(metadataList);
        this.version = version;
    }

    /**
     * Constructs a package from describe metadata result.
     *
     * @param describeMetadataResult the Salesforce metadata description
     * @param version the Salesforce API version
     */
    public Package(final DescribeMetadataResult describeMetadataResult, final String version) {
        this(describeMetadataResult.getMetadataObjects(), version);
    }

    /**
     * Sets the types list for XML binding.
     *
     * @param typesList list of metadata types
     */
    @XmlElement(name = "types")
    public void setTypes(final List<Types> typesList) {
        this.typesList = typesList;
    }

    /**
     * Returns the types list.
     *
     * @return list of metadata types
     */
    public List<Types> getTypes() {
        return typesList;
    }

    /**
     * Sets the Salesforce API version for XML binding.
     *
     * @param version the API version
     */
    @XmlElement(name = "version")
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Returns the Salesforce API version.
     *
     * @return the API version
     */
    public String getVersion() {
        return version;
    }
}
