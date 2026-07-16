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

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

import org.solenopsis.metadata.WsdlSubUrlEnum;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;
import org.solenopsis.session.Credentials;
import org.solenopsis.session.SessionContext;
import org.solenopsis.session.credentials.CredentialsUtil;
import org.solenopsis.session.login.LoginService;
import org.solenopsis.session.soap.login.LoginServiceEnum;

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

    // ========== Tests for WSDL selection fields ==========

    @Test
    void testContext_DefaultSkipCustom() {
        Context context = new Context();
        assertFalse(context.skipCustom, "Default skipCustom should be false");
    }

    @Test
    void testContext_DefaultIncludeTooling() {
        Context context = new Context();
        assertFalse(context.includeTooling, "Default includeTooling should be false");
    }

    @Test
    void testContext_DefaultWsdls() {
        Context context = new Context();
        assertNull(context.wsdls, "Default wsdls should be null (use defaults)");
    }

    @Test
    void testContext_WsdlMissingValue() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--wsdl"}),
            "--wsdl without a value should throw");
    }

    @Test
    void testContext_WsdlInvalidValue() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--wsdl", "invalid"}),
            "--wsdl with invalid WSDL name should throw");
    }

    @Test
    void testContext_WsdlInvalidValueMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--wsdl", "bogus"}));
        assertTrue(ex.getMessage().contains("Unknown WSDL type: bogus"),
            "Exception message should contain the invalid WSDL name");
        assertTrue(ex.getMessage().contains("Valid types:"),
            "Exception message should list valid types");
    }

    @Test
    void testContext_WsdlPartiallyInvalid() {
        assertThrows(IllegalArgumentException.class,
            () -> new Context(new String[]{"--wsdl", "apex,invalid"}),
            "--wsdl with a mix of valid and invalid names should throw");
    }

    // ========== Tests for getStandardWsdls ==========

    @Test
    void testGetStandardWsdls_DefaultBehavior() {
        Context context = new Context();
        Set<WsdlSubUrlEnum> wsdls = context.getStandardWsdls();

        assertEquals(4, wsdls.size(), "Default should return 4 standard WSDLs");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.APEX), "Defaults should include APEX");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.ENTERPRISE), "Defaults should include ENTERPRISE");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.METADATA), "Defaults should include METADATA");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.PARTNER), "Defaults should include PARTNER");
        assertFalse(wsdls.contains(WsdlSubUrlEnum.TOOLING), "Defaults should NOT include TOOLING");
        assertFalse(wsdls.contains(WsdlSubUrlEnum.CUSTOM), "Defaults should NOT include CUSTOM");
    }

    @Test
    void testGetStandardWsdls_WithIncludeTooling() {
        Context context = new Context();
        context.includeTooling = true;
        Set<WsdlSubUrlEnum> wsdls = context.getStandardWsdls();

        assertEquals(5, wsdls.size(), "Should return 5 WSDLs with tooling included");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.TOOLING), "Should include TOOLING");
    }

    @Test
    void testGetStandardWsdls_WithExplicitWsdls() {
        Context context = new Context();
        context.wsdls = EnumSet.of(WsdlSubUrlEnum.APEX, WsdlSubUrlEnum.METADATA);
        Set<WsdlSubUrlEnum> wsdls = context.getStandardWsdls();

        assertEquals(2, wsdls.size(), "Should return exactly the specified WSDLs");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.APEX), "Should include APEX");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.METADATA), "Should include METADATA");
    }

    @Test
    void testGetStandardWsdls_ExplicitOverridesIncludeTooling() {
        Context context = new Context();
        context.wsdls = EnumSet.of(WsdlSubUrlEnum.APEX);
        context.includeTooling = true;
        Set<WsdlSubUrlEnum> wsdls = context.getStandardWsdls();

        assertEquals(1, wsdls.size(), "Explicit wsdls should override includeTooling");
        assertFalse(wsdls.contains(WsdlSubUrlEnum.TOOLING),
            "TOOLING should NOT be added when explicit wsdls set is used");
    }

    @Test
    void testGetStandardWsdls_ExplicitWithToolingIncluded() {
        Context context = new Context();
        context.wsdls = EnumSet.of(WsdlSubUrlEnum.TOOLING, WsdlSubUrlEnum.APEX);
        assertEquals(2, context.getStandardWsdls().size());
        assertTrue(context.getStandardWsdls().contains(WsdlSubUrlEnum.TOOLING));
    }

    @Test
    void testGetStandardWsdls_SingleWsdl() {
        Context context = new Context();
        context.wsdls = EnumSet.of(WsdlSubUrlEnum.PARTNER);
        assertEquals(1, context.getStandardWsdls().size());
        assertTrue(context.getStandardWsdls().contains(WsdlSubUrlEnum.PARTNER));
    }

    @Test
    void testGetStandardWsdls_AllStandardWsdls() {
        Context context = new Context();
        context.wsdls = EnumSet.of(
            WsdlSubUrlEnum.APEX, WsdlSubUrlEnum.ENTERPRISE,
            WsdlSubUrlEnum.METADATA, WsdlSubUrlEnum.PARTNER,
            WsdlSubUrlEnum.TOOLING
        );
        assertEquals(5, context.getStandardWsdls().size());
    }

    @Test
    void testContext_SkipCustomCanBeSet() {
        Context context = new Context();
        context.skipCustom = true;
        assertTrue(context.skipCustom);
    }

    @Test
    void testContext_IncludeToolingCanBeSet() {
        Context context = new Context();
        context.includeTooling = true;
        assertTrue(context.includeTooling);
    }

    @Test
    void testContext_WsdlFieldsArePackagePrivate() throws NoSuchFieldException {
        Class<?> contextClass = Context.class;
        assertFalse(java.lang.reflect.Modifier.isPrivate(
            contextClass.getDeclaredField("wsdls").getModifiers()));
        assertFalse(java.lang.reflect.Modifier.isPrivate(
            contextClass.getDeclaredField("skipCustom").getModifiers()));
        assertFalse(java.lang.reflect.Modifier.isPrivate(
            contextClass.getDeclaredField("includeTooling").getModifiers()));
    }

    // ========== Mocked credential tests (issue #9) ==========

    private static LoginService swapLoginService(LoginServiceEnum enumConstant, LoginService replacement) throws Exception {
        Field field = LoginServiceEnum.class.getDeclaredField("loginService");
        field.setAccessible(true);
        LoginService original = (LoginService) field.get(enumConstant);
        field.set(enumConstant, replacement);
        return original;
    }

    @Test
    void testSetCredentials_MockedExternalServices() throws Exception {
        Credentials mockCreds = mock(Credentials.class);
        SessionContext mockSessionCtx = mock(SessionContext.class);
        LoginService mockLoginService = mock(LoginService.class);

        when(mockLoginService.login(mockCreds)).thenReturn(mockSessionCtx);
        when(mockSessionCtx.credentials()).thenReturn(mockCreds);
        when(mockSessionCtx.sessionId()).thenReturn("mock-session-id");
        when(mockSessionCtx.serverUrl()).thenReturn("https://test.salesforce.com");

        LoginService originalLoginService = swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, mockLoginService);

        try (MockedStatic<CredentialsUtil> credsMock = mockStatic(CredentialsUtil.class)) {
            credsMock.when(() -> CredentialsUtil.fromFile("/path/to/creds.properties")).thenReturn(mockCreds);

            Context context = new Context();
            context.setCredentials("/path/to/creds.properties");

            credsMock.verify(() -> CredentialsUtil.fromFile("/path/to/creds.properties"));
            assertSame(mockCreds, context.credentials);
            assertSame(mockSessionCtx, context.sessionContext);
            assertNotNull(context.port);
            verify(mockLoginService, times(2)).login(mockCreds);
        } finally {
            swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, originalLoginService);
        }
    }

    @Test
    void testSetSolenopsisCredentials_MockedExternalServices() throws Exception {
        Credentials mockCreds = mock(Credentials.class);
        SessionContext mockSessionCtx = mock(SessionContext.class);
        LoginService mockLoginService = mock(LoginService.class);

        when(mockLoginService.login(mockCreds)).thenReturn(mockSessionCtx);
        when(mockSessionCtx.credentials()).thenReturn(mockCreds);
        when(mockSessionCtx.sessionId()).thenReturn("mock-session-id");
        when(mockSessionCtx.serverUrl()).thenReturn("https://test.salesforce.com");

        String expectedPath = java.nio.file.Paths.get(
            System.getProperty("user.home"), ".solenopsis", "credentials", "test-env.properties"
        ).toString();

        LoginService originalLoginService = swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, mockLoginService);

        try (MockedStatic<CredentialsUtil> credsMock = mockStatic(CredentialsUtil.class)) {
            credsMock.when(() -> CredentialsUtil.fromFile(expectedPath)).thenReturn(mockCreds);

            Context context = new Context();
            context.setSolenopsisCredentials("test-env");

            credsMock.verify(() -> CredentialsUtil.fromFile(expectedPath));
            assertSame(mockCreds, context.credentials);
            assertNotNull(context.port);
        } finally {
            swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, originalLoginService);
        }
    }

    @Test
    void testContext_ConstructorWithCredsFlag() throws Exception {
        Credentials mockCreds = mock(Credentials.class);
        SessionContext mockSessionCtx = mock(SessionContext.class);
        LoginService mockLoginService = mock(LoginService.class);

        when(mockLoginService.login(mockCreds)).thenReturn(mockSessionCtx);
        when(mockSessionCtx.credentials()).thenReturn(mockCreds);
        when(mockSessionCtx.sessionId()).thenReturn("mock-session-id");
        when(mockSessionCtx.serverUrl()).thenReturn("https://test.salesforce.com");

        LoginService originalLoginService = swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, mockLoginService);

        try (MockedStatic<CredentialsUtil> credsMock = mockStatic(CredentialsUtil.class)) {
            credsMock.when(() -> CredentialsUtil.fromFile("/my/creds.properties")).thenReturn(mockCreds);

            Context context = new Context(new String[]{"--creds", "/my/creds.properties"});

            assertSame(mockCreds, context.credentials);
            assertNotNull(context.port);
            assertEquals("", context.prefix);
        } finally {
            swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, originalLoginService);
        }
    }

    @Test
    void testContext_ConstructorWithSolenopsisFlag() throws Exception {
        Credentials mockCreds = mock(Credentials.class);
        SessionContext mockSessionCtx = mock(SessionContext.class);
        LoginService mockLoginService = mock(LoginService.class);

        when(mockLoginService.login(mockCreds)).thenReturn(mockSessionCtx);
        when(mockSessionCtx.credentials()).thenReturn(mockCreds);
        when(mockSessionCtx.sessionId()).thenReturn("mock-session-id");
        when(mockSessionCtx.serverUrl()).thenReturn("https://test.salesforce.com");

        String expectedPath = java.nio.file.Paths.get(
            System.getProperty("user.home"), ".solenopsis", "credentials", "prod.properties"
        ).toString();

        LoginService originalLoginService = swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, mockLoginService);

        try (MockedStatic<CredentialsUtil> credsMock = mockStatic(CredentialsUtil.class)) {
            credsMock.when(() -> CredentialsUtil.fromFile(expectedPath)).thenReturn(mockCreds);

            Context context = new Context(new String[]{"--solenopsis", "prod"});

            assertSame(mockCreds, context.credentials);
            assertNotNull(context.port);
        } finally {
            swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, originalLoginService);
        }
    }

    @Test
    void testContext_ConstructorWithAllFlags(@TempDir Path tempDir) throws Exception {
        Credentials mockCreds = mock(Credentials.class);
        SessionContext mockSessionCtx = mock(SessionContext.class);
        LoginService mockLoginService = mock(LoginService.class);

        when(mockLoginService.login(mockCreds)).thenReturn(mockSessionCtx);
        when(mockSessionCtx.credentials()).thenReturn(mockCreds);
        when(mockSessionCtx.sessionId()).thenReturn("mock-session-id");
        when(mockSessionCtx.serverUrl()).thenReturn("https://test.salesforce.com");

        Path outputPath = tempDir.resolve("wsdl-out");
        LoginService originalLoginService = swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, mockLoginService);

        try (MockedStatic<CredentialsUtil> credsMock = mockStatic(CredentialsUtil.class)) {
            credsMock.when(() -> CredentialsUtil.fromFile("/my/creds.properties")).thenReturn(mockCreds);

            Context context = new Context(new String[]{
                "--creds", "/my/creds.properties",
                "--prefix", "myorg-",
                "--dir", outputPath.toString()
            });

            assertSame(mockCreds, context.credentials);
            assertEquals("myorg-", context.prefix);
            assertEquals(outputPath.toString(), context.outputDir);
            assertTrue(outputPath.toFile().exists());
        } finally {
            swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, originalLoginService);
        }
    }

    @Test
    void testContext_ConstructorCredsFlagOrderIndependent(@TempDir Path tempDir) throws Exception {
        Credentials mockCreds = mock(Credentials.class);
        SessionContext mockSessionCtx = mock(SessionContext.class);
        LoginService mockLoginService = mock(LoginService.class);

        when(mockLoginService.login(mockCreds)).thenReturn(mockSessionCtx);
        when(mockSessionCtx.credentials()).thenReturn(mockCreds);
        when(mockSessionCtx.sessionId()).thenReturn("mock-session-id");
        when(mockSessionCtx.serverUrl()).thenReturn("https://test.salesforce.com");

        Path outputPath = tempDir.resolve("out");
        LoginService originalLoginService = swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, mockLoginService);

        try (MockedStatic<CredentialsUtil> credsMock = mockStatic(CredentialsUtil.class)) {
            credsMock.when(() -> CredentialsUtil.fromFile("/my/creds.properties")).thenReturn(mockCreds);

            Context context = new Context(new String[]{
                "--prefix", "pre-",
                "--dir", outputPath.toString(),
                "--creds", "/my/creds.properties"
            });

            assertEquals("pre-", context.prefix);
            assertEquals(outputPath.toString(), context.outputDir);
            assertSame(mockCreds, context.credentials);
            assertTrue(outputPath.toFile().exists());
        } finally {
            swapLoginService(LoginServiceEnum.DEFAULT_LOGIN_SERVICE, originalLoginService);
        }
    }
}
