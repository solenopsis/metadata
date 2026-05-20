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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps Salesforce metadata file paths to metadata types and component names.
 *
 * @author Scot P. Floess
 */
public class FilePathMapper {

    private static final Logger logger = LoggerFactory.getLogger(FilePathMapper.class);

    /**
     * Pattern for simple metadata types (classes, triggers, pages, components).
     * Example: src/classes/MyClass.cls -> ApexClass:MyClass
     */
    private static final Pattern SIMPLE_PATTERN = Pattern.compile(
        "(?:force-app/main/default|src)/(\\w+)/([^/]+?)/.*\\.(\\w+)$|(?:force-app/main/default|src)/(\\w+)/([^/]+?)\\.(\\w+)(?:-meta\\.xml)?$"
    );

    /**
     * Pattern for object-related metadata.
     * Example: src/objects/Account/fields/MyField__c.field-meta.xml -> CustomField:Account.MyField__c
     */
    private static final Pattern OBJECT_PATTERN = Pattern.compile(
        "(?:force-app/main/default|src)/objects/([^/]+)/(\\w+)/([^/]+?)\\.(\\w+)-meta\\.xml$"
    );

    /**
     * Pattern for layouts.
     * Example: src/layouts/Account-Layout.layout-meta.xml -> Layout:Account-Layout
     */
    private static final Pattern LAYOUT_PATTERN = Pattern.compile(
        "(?:force-app/main/default|src)/layouts/([^/]+?)\\.layout(?:-meta\\.xml)?$"
    );

    /**
     * Metadata type mappings from folder/extension to Salesforce type.
     */
    private static final Map<String, String> TYPE_MAPPINGS = new HashMap<>();

    static {
        // Simple types
        TYPE_MAPPINGS.put("classes", "ApexClass");
        TYPE_MAPPINGS.put("triggers", "ApexTrigger");
        TYPE_MAPPINGS.put("pages", "ApexPage");
        TYPE_MAPPINGS.put("components", "ApexComponent");
        TYPE_MAPPINGS.put("staticresources", "StaticResource");
        TYPE_MAPPINGS.put("aura", "AuraDefinitionBundle");
        TYPE_MAPPINGS.put("lwc", "LightningComponentBundle");
        TYPE_MAPPINGS.put("workflows", "Workflow");
        TYPE_MAPPINGS.put("flows", "Flow");
        TYPE_MAPPINGS.put("tabs", "CustomTab");
        TYPE_MAPPINGS.put("applications", "CustomApplication");
        TYPE_MAPPINGS.put("permissionsets", "PermissionSet");
        TYPE_MAPPINGS.put("profiles", "Profile");
        TYPE_MAPPINGS.put("queues", "Queue");
        TYPE_MAPPINGS.put("email", "EmailTemplate");
        TYPE_MAPPINGS.put("reports", "Report");
        TYPE_MAPPINGS.put("dashboards", "Dashboard");

        // Object-related types
        TYPE_MAPPINGS.put("fields", "CustomField");
        TYPE_MAPPINGS.put("validationRules", "ValidationRule");
        TYPE_MAPPINGS.put("recordTypes", "RecordType");
        TYPE_MAPPINGS.put("listViews", "ListView");
        TYPE_MAPPINGS.put("webLinks", "WebLink");
        TYPE_MAPPINGS.put("compactLayouts", "CompactLayout");
        TYPE_MAPPINGS.put("fieldSets", "FieldSet");
        TYPE_MAPPINGS.put("businessProcesses", "BusinessProcess");
    }

    /**
     * Parse a file path and return the corresponding metadata component.
     *
     * @param filePath the file path to parse
     * @return the metadata component, or null if path cannot be parsed
     */
    public static MetadataComponent parseFilePath(String filePath) {
        return parseFilePath(filePath, false);
    }

    /**
     * Parse a file path and return the corresponding metadata component.
     *
     * @param filePath the file path to parse
     * @param deleted true if this file was deleted
     * @return the metadata component, or null if path cannot be parsed
     */
    public static MetadataComponent parseFilePath(String filePath, boolean deleted) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // Normalize path separators
        filePath = filePath.replace('\\', '/');

        // Try object-related pattern first
        MetadataComponent component = parseObjectPath(filePath, deleted);
        if (component != null) {
            return component;
        }

        // Try layout pattern
        component = parseLayoutPath(filePath, deleted);
        if (component != null) {
            return component;
        }

        // Try simple pattern
        component = parseSimplePath(filePath, deleted);
        if (component != null) {
            return component;
        }

        logger.debug("Could not parse file path: {}", filePath);
        return null;
    }

    /**
     * Parse a simple metadata path (classes, triggers, pages, etc.).
     */
    private static MetadataComponent parseSimplePath(String filePath, boolean deleted) {
        Matcher matcher = SIMPLE_PATTERN.matcher(filePath);
        if (!matcher.find()) {
            return null;
        }

        String folder;
        String name;

        // Check if it's the bundle pattern (lwc/aura with subdirectory)
        if (matcher.group(1) != null) {
            folder = matcher.group(1);
            name = matcher.group(2);
        } else {
            // Standard pattern
            folder = matcher.group(4);
            name = matcher.group(5);
        }

        String type = TYPE_MAPPINGS.get(folder);
        if (type == null) {
            logger.debug("Unknown folder type: {}", folder);
            return null;
        }

        return new MetadataComponent(type, name, filePath, deleted);
    }

    /**
     * Parse an object-related metadata path (fields, validation rules, etc.).
     */
    private static MetadataComponent parseObjectPath(String filePath, boolean deleted) {
        Matcher matcher = OBJECT_PATTERN.matcher(filePath);
        if (!matcher.find()) {
            return null;
        }

        String objectName = matcher.group(1);
        String folder = matcher.group(2);
        String itemName = matcher.group(3);

        String type = TYPE_MAPPINGS.get(folder);
        if (type == null) {
            logger.debug("Unknown object folder type: {}", folder);
            return null;
        }

        // For object-related metadata, name format is "Object.Component"
        String fullName = objectName + "." + itemName;

        return new MetadataComponent(type, fullName, filePath, deleted);
    }

    /**
     * Parse a layout path.
     */
    private static MetadataComponent parseLayoutPath(String filePath, boolean deleted) {
        Matcher matcher = LAYOUT_PATTERN.matcher(filePath);
        if (!matcher.find()) {
            return null;
        }

        String name = matcher.group(1);
        return new MetadataComponent("Layout", name, filePath, deleted);
    }

    /**
     * Check if a file path represents Salesforce metadata.
     *
     * @param filePath the file path
     * @return true if this looks like Salesforce metadata
     */
    public static boolean isMetadataPath(String filePath) {
        return parseFilePath(filePath) != null;
    }
}
