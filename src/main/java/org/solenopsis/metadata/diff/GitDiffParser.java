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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses git diff output to identify changed Salesforce metadata files.
 *
 * @author Scot P. Floess
 */
public class GitDiffParser {

    private static final Logger logger = LoggerFactory.getLogger(GitDiffParser.class);

    /**
     * Get changed files between two git references.
     *
     * @param fromRef the starting reference (commit, branch, tag)
     * @param toRef the ending reference
     * @return list of changed file paths
     * @throws Exception if git command fails
     */
    public static List<String> getChangedFiles(String fromRef, String toRef) throws Exception {
        return getChangedFiles(fromRef, toRef, null);
    }

    /**
     * Get changed files between two git references.
     *
     * @param fromRef the starting reference
     * @param toRef the ending reference
     * @param workingDir the git repository directory, or null for current directory
     * @return list of changed file paths
     * @throws Exception if git command fails
     */
    public static List<String> getChangedFiles(String fromRef, String toRef, File workingDir) throws Exception {
        String command = "git diff --name-only " + fromRef + " " + toRef;
        logger.info("Executing: {}", command);

        return executeGitCommand(command, workingDir);
    }

    /**
     * Get deleted files between two git references.
     *
     * @param fromRef the starting reference
     * @param toRef the ending reference
     * @return list of deleted file paths
     * @throws Exception if git command fails
     */
    public static List<String> getDeletedFiles(String fromRef, String toRef) throws Exception {
        return getDeletedFiles(fromRef, toRef, null);
    }

    /**
     * Get deleted files between two git references.
     *
     * @param fromRef the starting reference
     * @param toRef the ending reference
     * @param workingDir the git repository directory
     * @return list of deleted file paths
     * @throws Exception if git command fails
     */
    public static List<String> getDeletedFiles(String fromRef, String toRef, File workingDir) throws Exception {
        String command = "git diff --name-only --diff-filter=D " + fromRef + " " + toRef;
        logger.info("Executing: {}", command);

        return executeGitCommand(command, workingDir);
    }

    /**
     * Get added or modified files between two git references.
     *
     * @param fromRef the starting reference
     * @param toRef the ending reference
     * @return list of added/modified file paths
     * @throws Exception if git command fails
     */
    public static List<String> getAddedOrModifiedFiles(String fromRef, String toRef) throws Exception {
        return getAddedOrModifiedFiles(fromRef, toRef, null);
    }

    /**
     * Get added or modified files between two git references.
     *
     * @param fromRef the starting reference
     * @param toRef the ending reference
     * @param workingDir the git repository directory
     * @return list of added/modified file paths
     * @throws Exception if git command fails
     */
    public static List<String> getAddedOrModifiedFiles(String fromRef, String toRef, File workingDir) throws Exception {
        String command = "git diff --name-only --diff-filter=AM " + fromRef + " " + toRef;
        logger.info("Executing: {}", command);

        return executeGitCommand(command, workingDir);
    }

    /**
     * Execute a git command and return the output lines.
     *
     * @param command the git command to execute
     * @param workingDir the working directory, or null for current directory
     * @return list of output lines
     * @throws Exception if command fails
     */
    private static List<String> executeGitCommand(String command, File workingDir) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command.split("\\s+"));

        if (workingDir != null) {
            builder.directory(workingDir);
        }

        builder.redirectErrorStream(true);
        Process process = builder.start();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git command failed with exit code " + exitCode + ": " + command);
        }

        logger.debug("Found {} files", lines.size());
        return lines;
    }

    /**
     * Parse changed files into metadata components.
     *
     * @param fromRef the starting reference
     * @param toRef the ending reference
     * @return list of metadata components
     * @throws Exception if parsing fails
     */
    public static List<MetadataComponent> parseChangedComponents(String fromRef, String toRef) throws Exception {
        return parseChangedComponents(fromRef, toRef, null);
    }

    /**
     * Parse changed files into metadata components.
     *
     * @param fromRef the starting reference
     * @param toRef the ending reference
     * @param workingDir the git repository directory
     * @return list of metadata components
     * @throws Exception if parsing fails
     */
    public static List<MetadataComponent> parseChangedComponents(String fromRef, String toRef, File workingDir) throws Exception {
        List<MetadataComponent> components = new ArrayList<>();

        // Get added/modified files
        List<String> addedOrModified = getAddedOrModifiedFiles(fromRef, toRef, workingDir);
        for (String filePath : addedOrModified) {
            MetadataComponent component = FilePathMapper.parseFilePath(filePath, false);
            if (component != null) {
                components.add(component);
                logger.debug("Added/Modified: {}", component);
            }
        }

        // Get deleted files
        List<String> deleted = getDeletedFiles(fromRef, toRef, workingDir);
        for (String filePath : deleted) {
            MetadataComponent component = FilePathMapper.parseFilePath(filePath, true);
            if (component != null) {
                components.add(component);
                logger.debug("Deleted: {}", component);
            }
        }

        logger.info("Parsed {} metadata components from git diff", components.size());
        return components;
    }
}
