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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for OrgStats.
 *
 * @author Scot P. Floess
 */
class OrgStatsTest {

    @Test
    void testDefaultConstructor() {
        OrgStats stats = new OrgStats();

        assertNotNull(stats);
        assertNotNull(stats.getTopTypes());
        assertTrue(stats.getTopTypes().isEmpty());
        assertNull(stats.getApiVersion());
        assertNull(stats.getOrganizationNamespace());
        assertEquals(0, stats.getTotalComponents());
        assertEquals(0, stats.getTotalTypes());
    }

    @Test
    void testApiVersion() {
        OrgStats stats = new OrgStats();
        stats.setApiVersion("60.0");

        assertEquals("60.0", stats.getApiVersion());
    }

    @Test
    void testOrganizationNamespace() {
        OrgStats stats = new OrgStats();
        stats.setOrganizationNamespace("myorg");

        assertEquals("myorg", stats.getOrganizationNamespace());
    }

    @Test
    void testTotalComponents() {
        OrgStats stats = new OrgStats();
        stats.setTotalComponents(12345);

        assertEquals(12345, stats.getTotalComponents());
    }

    @Test
    void testTotalTypes() {
        OrgStats stats = new OrgStats();
        stats.setTotalTypes(87);

        assertEquals(87, stats.getTotalTypes());
    }

    @Test
    void testTypeCounts() {
        OrgStats stats = new OrgStats();
        Map<String, Integer> typeCounts = new HashMap<>();
        typeCounts.put("ApexClass", 100);
        typeCounts.put("CustomObject", 50);

        stats.setTypeCounts(typeCounts);

        assertNotNull(stats.getTypeCounts());
        assertEquals(2, stats.getTypeCounts().size());
        assertEquals(100, stats.getTypeCounts().get("ApexClass"));
        assertEquals(50, stats.getTypeCounts().get("CustomObject"));
    }

    @Test
    void testTopTypes() {
        OrgStats stats = new OrgStats();
        List<OrgStats.TypeCount> topTypes = new ArrayList<>();
        topTypes.add(new OrgStats.TypeCount("ApexClass", 100));
        topTypes.add(new OrgStats.TypeCount("Layout", 75));

        stats.setTopTypes(topTypes);

        assertNotNull(stats.getTopTypes());
        assertEquals(2, stats.getTopTypes().size());
        assertEquals("ApexClass", stats.getTopTypes().get(0).getType());
        assertEquals(100, stats.getTopTypes().get(0).getCount());
    }

    @Test
    void testTypeCount_Constructor() {
        OrgStats.TypeCount typeCount = new OrgStats.TypeCount("ApexClass", 100);

        assertEquals("ApexClass", typeCount.getType());
        assertEquals(100, typeCount.getCount());
    }

    @Test
    void testTypeCount_Setters() {
        OrgStats.TypeCount typeCount = new OrgStats.TypeCount("ApexClass", 100);

        typeCount.setType("CustomObject");
        typeCount.setCount(50);

        assertEquals("CustomObject", typeCount.getType());
        assertEquals(50, typeCount.getCount());
    }

    @Test
    void testCompleteStats() {
        OrgStats stats = new OrgStats();

        stats.setApiVersion("60.0");
        stats.setOrganizationNamespace("myorg");
        stats.setTotalComponents(250);
        stats.setTotalTypes(3);

        Map<String, Integer> typeCounts = new HashMap<>();
        typeCounts.put("ApexClass", 100);
        typeCounts.put("CustomObject", 75);
        typeCounts.put("Layout", 75);
        stats.setTypeCounts(typeCounts);

        List<OrgStats.TypeCount> topTypes = new ArrayList<>();
        topTypes.add(new OrgStats.TypeCount("ApexClass", 100));
        topTypes.add(new OrgStats.TypeCount("Layout", 75));
        topTypes.add(new OrgStats.TypeCount("CustomObject", 75));
        stats.setTopTypes(topTypes);

        assertEquals("60.0", stats.getApiVersion());
        assertEquals("myorg", stats.getOrganizationNamespace());
        assertEquals(250, stats.getTotalComponents());
        assertEquals(3, stats.getTotalTypes());
        assertEquals(3, stats.getTypeCounts().size());
        assertEquals(3, stats.getTopTypes().size());
    }
}
