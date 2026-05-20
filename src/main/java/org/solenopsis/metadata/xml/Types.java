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

/**
 * Represents metadata types in a Salesforce package.xml file.
 *
 * @author Scot P. Floess
 */
@XmlRootElement(name = "types")
@XmlType(propOrder = {"members, name"})
public class Types {
    private String name;

    private List<String> membersList;

    /**
     * Creates a list of Types from Salesforce metadata objects.
     *
     * @param metadataList list of Salesforce metadata objects
     * @return list of Types objects
     */
    static List<Types> createTypes(final List<DescribeMetadataObject> metadataList) {
        final List<Types> retVal = new ArrayList<>(metadataList.size());

        for (final DescribeMetadataObject describeMetadataObject : metadataList) {
            retVal.add(new Types(describeMetadataObject));
        }

        return retVal;
    }

    /**
     * Default constructor. Initializes an empty members list.
     */
    public Types() {
        this.membersList = new ArrayList<>();
    }

    /**
     * Constructs a Types object from a Salesforce metadata object.
     *
     * @param describeMetadataObject the metadata object to convert
     */
    public Types(final DescribeMetadataObject describeMetadataObject) {
        this.name = describeMetadataObject.getXmlName();
        this.membersList = new ArrayList<>(describeMetadataObject.getChildXmlNames().size());
        this.membersList.addAll(describeMetadataObject.getChildXmlNames());
    }

    /**
     * Sets the metadata type name for XML binding.
     *
     * @param name the metadata type name
     */
    @XmlElement(name = "name")
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the metadata type name.
     *
     * @return the metadata type name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the members list for XML binding.
     *
     * @param membersList list of member names
     */
    @XmlElement(name = "members")
    public void setMembers(final List<String> membersList) {
        this.membersList = membersList;
    }

    /**
     * Returns the members list.
     *
     * @return list of member names
     */
    public List<String> getMembers() {
        return membersList;
    }
}
