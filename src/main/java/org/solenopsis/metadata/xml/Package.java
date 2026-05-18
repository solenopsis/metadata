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

    public Package() {
        this.typesList = new ArrayList<>();
    }

    public Package(final List<DescribeMetadataObject> metadataList, final String version) {
        this.typesList = Types.createTypes(metadataList);
        this.version = version;
    }

    public Package(final DescribeMetadataResult describeMetadataResult, final String version) {
        this(describeMetadataResult.getMetadataObjects(), version);
    }

    @XmlElement(name = "types")
    public void setTypes(final List<Types> typesList) {
        this.typesList = typesList;
    }

    public List<Types> getTypes() {
        return typesList;
    }

    @XmlElement(name = "version")
    public void setVersion(final String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
