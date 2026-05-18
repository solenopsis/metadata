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
package org.solenopsis.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.solenopsis.soap.metadata.DescribeMetadataResult;
import org.solenopsis.soap.metadata.MetadataPortType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RetrieveMetadata.
 *
 * @author Scot P. Floess
 */
@ExtendWith(MockitoExtension.class)
class RetrieveMetadataTest {

    @Mock
    private MetadataPortType mockPort;

    @Mock
    private DescribeMetadataResult mockResult;

    @Test
    void testGetMetadataCompleteWithDoubleVersion() throws Exception {
        Double apiVersion = 60.0;
        when(mockPort.describeMetadata(apiVersion)).thenReturn(mockResult);

        DescribeMetadataResult result = RetrieveMetadata.getMetadataComplete(mockPort, "60.0");

        assertNotNull(result);
        assertEquals(mockResult, result);
        verify(mockPort).describeMetadata(60.0);
    }

    @Test
    void testGetMetadataCompleteWithStringVersion() throws Exception {
        when(mockPort.describeMetadata(59.0)).thenReturn(mockResult);

        DescribeMetadataResult result = RetrieveMetadata.getMetadataComplete(mockPort, "59.0");

        assertNotNull(result);
        assertEquals(mockResult, result);
        verify(mockPort).describeMetadata(59.0);
    }

    @Test
    void testGetMetadataCondensedWithStringVersion() throws Exception {
        when(mockPort.describeMetadata(58.0)).thenReturn(mockResult);

        DescribeMetadataResult result = RetrieveMetadata.getMetadataCondensed(mockPort, "58.0");

        assertNotNull(result);
        assertEquals(mockResult, result);
        verify(mockPort).describeMetadata(58.0);
    }

    @Test
    void testGetMetadataWithInvalidVersion() {
        assertThrows(NumberFormatException.class, () -> {
            RetrieveMetadata.getMetadataComplete(mockPort, "invalid");
        });
    }

    @Test
    void testGetMetadataWithNullPort() {
        assertThrows(NullPointerException.class, () -> {
            RetrieveMetadata.getMetadataComplete(null, "60.0");
        });
    }

    @Test
    void testGetMetadataWithException() throws Exception {
        when(mockPort.describeMetadata(anyDouble()))
            .thenThrow(new RuntimeException("API Error"));

        assertThrows(RuntimeException.class, () -> {
            RetrieveMetadata.getMetadataComplete(mockPort, "60.0");
        });
    }
}
