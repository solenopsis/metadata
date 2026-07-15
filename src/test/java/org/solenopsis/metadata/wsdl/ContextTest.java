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
package org.solenopsis.metadata.wsdl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Context command-line argument parsing.
 *
 * @author Scot P. Floess
 */
class ContextTest {

    @Test
    void testEnsureCredentials_WithNullCredentials() {
        Context context = new Context();
        assertThrows(IllegalArgumentException.class, () -> context.ensureCredentials(),
            "ensureCredentials() should throw when credentials are null");
    }

    @Test
    void testContext_NoArgsConstructor() {
        Context context = new Context();
        assertNotNull(context, "Context should be instantiated");
        assertEquals("", context.prefix, "Default prefix should be empty");
        assertEquals(System.getProperty("user.home"), context.outputDir,
            "Default outputDir should be user home");
        assertNull(context.credentials, "Credentials should be null initially");
        assertNull(context.port, "Port should be null initially");
        assertNull(context.sessionContext, "SessionContext should be null initially");
    }

    @Test
    void testContext_PrefixCanBeSet() {
        Context context = new Context();
        context.prefix = "test-prefix-";
        assertEquals("test-prefix-", context.prefix, "Prefix should be settable");
    }

    @Test
    void testContext_OutputDirCanBeSet(@TempDir Path tempDir) {
        Context context = new Context();
        context.outputDir = tempDir.toString();
        assertEquals(tempDir.toString(), context.outputDir, "Output dir should be settable");
    }

    @Test
    void testContext_FieldsArePackagePrivate() throws NoSuchFieldException {
        Class<?> contextClass = Context.class;

        java.lang.reflect.Field credentialsField = contextClass.getDeclaredField("credentials");
        assertFalse(java.lang.reflect.Modifier.isPrivate(credentialsField.getModifiers()),
            "credentials should be package-private for testing");

        java.lang.reflect.Field prefixField = contextClass.getDeclaredField("prefix");
        assertFalse(java.lang.reflect.Modifier.isPrivate(prefixField.getModifiers()),
            "prefix should be package-private for testing");

        java.lang.reflect.Field outputDirField = contextClass.getDeclaredField("outputDir");
        assertFalse(java.lang.reflect.Modifier.isPrivate(outputDirField.getModifiers()),
            "outputDir should be package-private for testing");
    }

    @Test
    void testContext_SolenopsisMissingValue() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--solenopsis"}),
            "--solenopsis without a value should throw");
    }

    @Test
    void testContext_CredsMissingValue() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--creds"}),
            "--creds without a value should throw");
    }

    @Test
    void testContext_PrefixMissingValue() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--prefix"}),
            "--prefix without a value should throw");
    }

    @Test
    void testContext_DirMissingValue() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--dir"}),
            "--dir without a value should throw");
    }

    @Test
    void testContext_NoCredentials() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--prefix", "test-"}),
            "Constructor without credentials should throw");
    }

    @Test
    void testContext_EmptyArgs() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{}),
            "Empty args should throw due to missing credentials");
    }

    @Test
    void testContext_DirCreatedBeforeCredentialCheck(@TempDir Path tempDir) {
        Path outputPath = tempDir.resolve("wsdl-output");
        assertFalse(outputPath.toFile().exists(), "Directory should not exist yet");

        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--dir", outputPath.toString()}));

        assertTrue(outputPath.toFile().exists(),
            "Directory should be created even when credentials are missing");
    }

    @Test
    void testContext_UnknownFlagsIgnored() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--unknown", "value", "--another"}),
            "Unknown flags should be ignored, then ensureCredentials throws");
    }

    @Test
    void testContext_MultipleFlagsBeforeCredentialCheck(@TempDir Path tempDir) {
        Path outputPath = tempDir.resolve("output");

        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--prefix", "org-", "--dir", outputPath.toString()}));

        assertTrue(outputPath.toFile().exists(),
            "Directory should be created even with multiple flags and no credentials");
    }
}
