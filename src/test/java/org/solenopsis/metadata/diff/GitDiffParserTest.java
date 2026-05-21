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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GitDiffParser.
 *
 * Note: These tests require a git repository and valid git references.
 * They are enabled only when running in the actual project repository.
 *
 * @author Scot P. Floess
 */
class GitDiffParserTest {

    /**
     * Check if we're in a git repository.
     */
    static boolean isGitRepository() {
        return new File(".git").exists();
    }

    @Test
    @EnabledIf("isGitRepository")
    void testGetChangedFiles_HeadVsHead() throws Exception {
        // HEAD vs HEAD should return no changes
        List<String> files = GitDiffParser.getChangedFiles("HEAD", "HEAD");

        assertNotNull(files);
        assertEquals(0, files.size(), "HEAD vs HEAD should have no changes");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testGetDeletedFiles_HeadVsHead() throws Exception {
        // HEAD vs HEAD should return no deletions
        List<String> files = GitDiffParser.getDeletedFiles("HEAD", "HEAD");

        assertNotNull(files);
        assertEquals(0, files.size(), "HEAD vs HEAD should have no deletions");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testGetAddedOrModifiedFiles_HeadVsHead() throws Exception {
        // HEAD vs HEAD should return no additions/modifications
        List<String> files = GitDiffParser.getAddedOrModifiedFiles("HEAD", "HEAD");

        assertNotNull(files);
        assertEquals(0, files.size(), "HEAD vs HEAD should have no additions/modifications");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testGetChangedFiles_WithWorkingDir() throws Exception {
        File workingDir = new File(".");
        List<String> files = GitDiffParser.getChangedFiles("HEAD", "HEAD", workingDir);

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    @EnabledIf("isGitRepository")
    void testGetDeletedFiles_WithWorkingDir() throws Exception {
        File workingDir = new File(".");
        List<String> files = GitDiffParser.getDeletedFiles("HEAD", "HEAD", workingDir);

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    @EnabledIf("isGitRepository")
    void testGetAddedOrModifiedFiles_WithWorkingDir() throws Exception {
        File workingDir = new File(".");
        List<String> files = GitDiffParser.getAddedOrModifiedFiles("HEAD", "HEAD", workingDir);

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    @EnabledIf("isGitRepository")
    void testParseChangedComponents_HeadVsHead() throws Exception {
        List<MetadataComponent> components = GitDiffParser.parseChangedComponents("HEAD", "HEAD");

        assertNotNull(components);
        assertEquals(0, components.size(), "HEAD vs HEAD should have no changed components");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testParseChangedComponents_WithWorkingDir() throws Exception {
        File workingDir = new File(".");
        List<MetadataComponent> components = GitDiffParser.parseChangedComponents("HEAD", "HEAD", workingDir);

        assertNotNull(components);
        assertEquals(0, components.size());
    }

    @Test
    void testGetChangedFiles_InvalidRef() {
        // Test with invalid git reference
        Exception exception = assertThrows(Exception.class, () -> {
            GitDiffParser.getChangedFiles("invalid-ref-12345", "invalid-ref-67890");
        });

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Git command failed") ||
                   exception.getMessage().contains("Cannot run program"),
                   "Should fail with git error or process error");
    }

    @Test
    void testGetDeletedFiles_InvalidRef() {
        Exception exception = assertThrows(Exception.class, () -> {
            GitDiffParser.getDeletedFiles("invalid-ref-12345", "invalid-ref-67890");
        });

        assertNotNull(exception);
    }

    @Test
    void testGetAddedOrModifiedFiles_InvalidRef() {
        Exception exception = assertThrows(Exception.class, () -> {
            GitDiffParser.getAddedOrModifiedFiles("invalid-ref-12345", "invalid-ref-67890");
        });

        assertNotNull(exception);
    }

    @Test
    void testParseChangedComponents_InvalidRef() {
        Exception exception = assertThrows(Exception.class, () -> {
            GitDiffParser.parseChangedComponents("invalid-ref-12345", "invalid-ref-67890");
        });

        assertNotNull(exception);
    }

    @Test
    @EnabledIf("isGitRepository")
    void testGetChangedFiles_ReturnsListNotNull() throws Exception {
        List<String> files = GitDiffParser.getChangedFiles("HEAD~1", "HEAD");

        // Even if there are no changes, should return empty list, not null
        assertNotNull(files, "Should return empty list, not null");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testParseChangedComponents_FiltersNonMetadata() throws Exception {
        // Parse changes and verify that non-metadata files are filtered out
        List<MetadataComponent> components = GitDiffParser.parseChangedComponents("HEAD~1", "HEAD");

        assertNotNull(components);

        // All components should have valid types (no nulls should get through)
        for (MetadataComponent component : components) {
            assertNotNull(component.getType(), "Component type should not be null");
            assertNotNull(component.getName(), "Component name should not be null");
            assertFalse(component.getType().isEmpty(), "Component type should not be empty");
            assertFalse(component.getName().isEmpty(), "Component name should not be empty");
        }
    }
}
