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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.solenopsis.soap.metadata.AsyncResult;
import org.solenopsis.soap.metadata.MetadataPortType;
import org.solenopsis.soap.metadata.Package;
import org.solenopsis.soap.metadata.RetrieveRequest;
import org.solenopsis.soap.metadata.RetrieveResult;
import org.solenopsis.soap.metadata.StatusCode;
import org.solenopsis.metadata.WsdlSubUrlEnum;
import org.solenopsis.session.SessionContext;

/**
 * Tests for RetrieveWsdls web service detection.
 *
 * @author Scot P. Floess
 */
class RetrieveWsdlsTest {

    @Test
    void testIsWebServiceClass_WithWebServiceKeywordLowercase() {
        String content = "public class MyService {\n" +
                        "    webservice static String getData() {\n" +
                        "        return 'test';\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect 'webservice' keyword (lowercase)");
    }

    @Test
    void testIsWebServiceClass_WithWebServiceKeywordMixedCase() {
        String content = "public class MyService {\n" +
                        "    WebService static void processData() {\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect 'WebService' keyword (mixed case)");
    }

    @Test
    void testIsWebServiceClass_WithWebServiceKeywordUppercase() {
        String content = "public class MyService {\n" +
                        "    WEBSERVICE static Integer calculate() {\n" +
                        "        return 42;\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect 'WEBSERVICE' keyword (uppercase)");
    }

    @Test
    void testIsWebServiceClass_WithStaticModifier() {
        String content = "public class MyService {\n" +
                        "    webservice static String getData(String param) {\n" +
                        "        return param;\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect webservice with static modifier");
    }

    @Test
    void testIsWebServiceClass_WithoutStaticModifier() {
        String content = "public class MyService {\n" +
                        "    webservice void process() {\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect webservice without static modifier");
    }

    @Test
    void testIsWebServiceClass_FalsePositiveComment() {
        String content = "public class NotAService {\n" +
                        "    // This is a WebService example\n" +
                        "    public void doSomething() {\n" +
                        "    }\n" +
                        "}";

        assertFalse(RetrieveWsdls.isWebServiceClass(content),
            "Should NOT detect 'WebService' in comments");
    }

    @Test
    void testIsWebServiceClass_FalsePositiveStringLiteral() {
        String content = "public class NotAService {\n" +
                        "    String description = 'This is a WebService implementation';\n" +
                        "    public void doSomething() {\n" +
                        "    }\n" +
                        "}";

        assertFalse(RetrieveWsdls.isWebServiceClass(content),
            "Should NOT detect 'WebService' in string literals");
    }

    @Test
    void testIsWebServiceClass_FalsePositiveClassName() {
        String content = "public class MyWebServiceHelper {\n" +
                        "    public void doSomething() {\n" +
                        "    }\n" +
                        "}";

        assertFalse(RetrieveWsdls.isWebServiceClass(content),
            "Should NOT detect 'WebService' in class names");
    }

    @Test
    void testIsWebServiceClass_FalsePositiveVariableName() {
        String content = "public class NotAService {\n" +
                        "    String webServiceUrl = 'https://api.example.com';\n" +
                        "    public void doSomething() {\n" +
                        "    }\n" +
                        "}";

        assertFalse(RetrieveWsdls.isWebServiceClass(content),
            "Should NOT detect 'webService' in variable names");
    }

    @Test
    void testIsWebServiceClass_WithMultipleWebServiceMethods() {
        String content = "public class MyService {\n" +
                        "    webservice static String getData() {\n" +
                        "        return 'data';\n" +
                        "    }\n" +
                        "    webservice static void processData(String data) {\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect class with multiple webservice methods");
    }

    @Test
    void testIsWebServiceClass_WithComplexReturnType() {
        String content = "public class MyService {\n" +
                        "    webservice static List<String> getDataList() {\n" +
                        "        return new List<String>();\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect webservice with complex return type");
    }

    @Test
    void testIsWebServiceClass_WithParameters() {
        String content = "public class MyService {\n" +
                        "    webservice static String processData(String input, Integer count) {\n" +
                        "        return input;\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect webservice with multiple parameters");
    }

    @Test
    void testIsWebServiceClass_EmptyContent() {
        assertFalse(RetrieveWsdls.isWebServiceClass(""),
            "Should return false for empty content");
    }

    @Test
    void testIsWebServiceClass_NullContent() {
        assertFalse(RetrieveWsdls.isWebServiceClass(null),
            "Should return false for null content");
    }

    @Test
    void testIsWebServiceClass_NoWebService() {
        String content = "public class RegularClass {\n" +
                        "    public void regularMethod() {\n" +
                        "    }\n" +
                        "}";

        assertFalse(RetrieveWsdls.isWebServiceClass(content),
            "Should return false for class without webservice methods");
    }

    @Test
    void testIsWebServiceClass_WithAnnotation() {
        String content = "public class MyService {\n" +
                        "    @Deprecated\n" +
                        "    webservice static String legacyMethod() {\n" +
                        "        return 'legacy';\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect webservice with annotations");
    }

    @Test
    void testIsWebServiceClass_WithGlobalModifier() {
        String content = "global class MyService {\n" +
                        "    global webservice static String getData() {\n" +
                        "        return 'data';\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect global webservice method");
    }

    @Test
    void testIsWebServiceClass_RealWorldExample() {
        String content = "/**\n" +
                        " * Service class for external integrations\n" +
                        " * Exposes WebService methods for SOAP API\n" +
                        " */\n" +
                        "global class AccountWebService {\n" +
                        "    \n" +
                        "    // Get account by ID\n" +
                        "    webservice static Account getAccount(String accountId) {\n" +
                        "        return [SELECT Id, Name FROM Account WHERE Id = :accountId];\n" +
                        "    }\n" +
                        "    \n" +
                        "    // Update account\n" +
                        "    WebService static Boolean updateAccount(Account acc) {\n" +
                        "        try {\n" +
                        "            update acc;\n" +
                        "            return true;\n" +
                        "        } catch (Exception e) {\n" +
                        "            return false;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        assertTrue(RetrieveWsdls.isWebServiceClass(content),
            "Should detect real-world webservice class with comments and multiple methods");
    }

    // ========== Tests for createRetrieveRequest ==========

    @Test
    void testCreateRetrieveRequest_ValidApiVersion() throws Exception {
        String apiVersion = "60.0";

        RetrieveRequest request = RetrieveWsdls.createRetrieveRequest(apiVersion);

        assertNotNull(request, "Request should not be null");
        assertEquals(60.0, request.getApiVersion(), "API version should match");
        assertNotNull(request.getUnpackaged(), "Unpackaged should not be null");

        Package pkg = request.getUnpackaged();
        assertEquals("60.0", pkg.getVersion(), "Package version should match");
        assertNotNull(pkg.getTypes(), "Types list should not be null");
        assertEquals(1, pkg.getTypes().size(), "Should have exactly one type");
        assertEquals("ApexClass", pkg.getTypes().get(0).getName(), "Type should be ApexClass");
        assertEquals("*", pkg.getTypes().get(0).getMembers().get(0), "Members should include wildcard");
    }

    @Test
    void testCreateRetrieveRequest_DifferentVersions() throws Exception {
        String[] versions = {"55.0", "58.0", "61.0"};

        for (String version : versions) {
            RetrieveRequest request = RetrieveWsdls.createRetrieveRequest(version);
            assertEquals(Double.parseDouble(version), request.getApiVersion(),
                "API version should match for version " + version);
            assertEquals(version, request.getUnpackaged().getVersion(),
                "Package version should match for version " + version);
        }
    }

    @Test
    void testCreateRetrieveRequest_InvalidApiVersion() {
        assertThrows(NumberFormatException.class, () -> {
            RetrieveWsdls.createRetrieveRequest("invalid");
        }, "Should throw NumberFormatException for invalid API version");
    }

    @Test
    void testCreateRetrieveRequest_NullApiVersion() {
        assertThrows(NullPointerException.class, () -> {
            RetrieveWsdls.createRetrieveRequest(null);
        }, "Should throw NullPointerException for null API version");
    }

    // ========== Tests for ensureRetrieveSuccess ==========

    @Test
    void testEnsureRetrieveSuccess_WithSuccessfulResult() {
        RetrieveResult result = mock(RetrieveResult.class);
        when(result.isSuccess()).thenReturn(true);

        RetrieveResult returnedResult = RetrieveWsdls.ensureRetrieveSuccess(result);

        assertSame(result, returnedResult, "Should return the same result object on success");
        verify(result).isSuccess();
    }

    @Test
    void testEnsureRetrieveSuccess_WithFailedResult() {
        RetrieveResult result = mock(RetrieveResult.class);
        StatusCode statusCode = mock(StatusCode.class);
        when(result.isSuccess()).thenReturn(false);
        when(result.getErrorMessage()).thenReturn("Test error message");
        when(result.getErrorStatusCode()).thenReturn(statusCode);
        when(statusCode.toString()).thenReturn("ERROR_CODE_123");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            RetrieveWsdls.ensureRetrieveSuccess(result);
        }, "Should throw RuntimeException for failed result");

        assertTrue(exception.getMessage().contains("Test error message"),
            "Exception message should contain error message");
        verify(result).isSuccess();
        verify(result).getErrorMessage();
        verify(result).getErrorStatusCode();
    }

    @Test
    void testEnsureRetrieveSuccess_WithNullErrorInfo() {
        RetrieveResult result = mock(RetrieveResult.class);
        when(result.isSuccess()).thenReturn(false);
        when(result.getErrorMessage()).thenReturn(null);
        when(result.getErrorStatusCode()).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            RetrieveWsdls.ensureRetrieveSuccess(result);
        }, "Should throw RuntimeException even with null error info");

        assertTrue(exception.getMessage().contains("null"),
            "Exception message should handle null error info");
    }

    // ========== Tests for retrieveApexClasses ==========

    @Test
    void testRetrieveApexClasses_Success() throws Exception {
        MetadataPortType metadataPort = mock(MetadataPortType.class);
        AsyncResult asyncResult = mock(AsyncResult.class);
        RetrieveResult result = mock(RetrieveResult.class);
        byte[] expectedZipData = "test zip data".getBytes(StandardCharsets.UTF_8);

        when(metadataPort.retrieve(any(RetrieveRequest.class))).thenReturn(asyncResult);
        when(asyncResult.getId()).thenReturn("test-async-id");
        when(metadataPort.checkRetrieveStatus("test-async-id", true)).thenReturn(result);
        when(result.isDone()).thenReturn(true);
        when(result.isSuccess()).thenReturn(true);
        when(result.getZipFile()).thenReturn(expectedZipData);

        byte[] zipData = RetrieveWsdls.retrieveApexClasses(metadataPort, "60.0");

        assertArrayEquals(expectedZipData, zipData, "Should return the zip file data");
        verify(metadataPort).retrieve(any(RetrieveRequest.class));
        verify(metadataPort).checkRetrieveStatus("test-async-id", true);
        verify(result).isDone();
        verify(result).isSuccess();
        verify(result).getZipFile();
    }

    @Test
    void testRetrieveApexClasses_PollingUntilDone() throws Exception {
        MetadataPortType metadataPort = mock(MetadataPortType.class);
        AsyncResult asyncResult = mock(AsyncResult.class);
        RetrieveResult resultNotDone = mock(RetrieveResult.class);
        RetrieveResult resultDone = mock(RetrieveResult.class);
        byte[] zipData = "test".getBytes(StandardCharsets.UTF_8);

        when(metadataPort.retrieve(any(RetrieveRequest.class))).thenReturn(asyncResult);
        when(asyncResult.getId()).thenReturn("test-id");
        when(metadataPort.checkRetrieveStatus("test-id", true))
            .thenReturn(resultNotDone)
            .thenReturn(resultNotDone)
            .thenReturn(resultDone);
        when(resultNotDone.isDone()).thenReturn(false);
        when(resultDone.isDone()).thenReturn(true);
        when(resultDone.isSuccess()).thenReturn(true);
        when(resultDone.getZipFile()).thenReturn(zipData);

        byte[] result = RetrieveWsdls.retrieveApexClasses(metadataPort, "60.0");

        assertArrayEquals(zipData, result);
        verify(metadataPort, times(3)).checkRetrieveStatus("test-id", true);
    }

    @Test
    void testRetrieveApexClasses_Failure() throws Exception {
        MetadataPortType metadataPort = mock(MetadataPortType.class);
        AsyncResult asyncResult = mock(AsyncResult.class);
        RetrieveResult result = mock(RetrieveResult.class);
        StatusCode statusCode = mock(StatusCode.class);

        when(metadataPort.retrieve(any(RetrieveRequest.class))).thenReturn(asyncResult);
        when(asyncResult.getId()).thenReturn("test-id");
        when(metadataPort.checkRetrieveStatus("test-id", true)).thenReturn(result);
        when(result.isDone()).thenReturn(true);
        when(result.isSuccess()).thenReturn(false);
        when(result.getErrorMessage()).thenReturn("Retrieval failed");
        when(result.getErrorStatusCode()).thenReturn(statusCode);

        assertThrows(RuntimeException.class, () -> {
            RetrieveWsdls.retrieveApexClasses(metadataPort, "60.0");
        }, "Should throw RuntimeException on retrieval failure");
    }

    // ========== Tests for findCustomWsdls ==========

    @Test
    void testFindCustomWsdls_WithWebServiceClasses() throws Exception {
        // Create a mock zip file with Apex classes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add a web service class
            String webServiceContent = "public class MyService {\n" +
                                      "    webservice static String getData() {\n" +
                                      "        return 'data';\n" +
                                      "    }\n" +
                                      "}";
            ZipEntry entry1 = new ZipEntry("classes/MyService.cls");
            zos.putNextEntry(entry1);
            zos.write(webServiceContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Add a regular class (not a web service)
            String regularContent = "public class RegularClass {\n" +
                                   "    public void method() {}\n" +
                                   "}";
            ZipEntry entry2 = new ZipEntry("classes/RegularClass.cls");
            zos.putNextEntry(entry2);
            zos.write(regularContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Add another web service class
            String anotherServiceContent = "global class AnotherService {\n" +
                                          "    WebService static void process() {}\n" +
                                          "}";
            ZipEntry entry3 = new ZipEntry("classes/AnotherService.cls");
            zos.putNextEntry(entry3);
            zos.write(anotherServiceContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Add a non-.cls file (should be skipped)
            ZipEntry entry4 = new ZipEntry("classes/MyService.cls-meta.xml");
            zos.putNextEntry(entry4);
            zos.write("<xml></xml>".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Add a directory (should be skipped)
            ZipEntry dirEntry = new ZipEntry("classes/");
            zos.putNextEntry(dirEntry);
            zos.closeEntry();
        }

        byte[] zipData = baos.toByteArray();

        // Mock the dependencies
        Context context = mock(Context.class);
        MetadataPortType metadataPort = mock(MetadataPortType.class);
        AsyncResult asyncResult = mock(AsyncResult.class);
        RetrieveResult result = mock(RetrieveResult.class);

        context.port = metadataPort;
        context.credentials = mock(org.solenopsis.session.Credentials.class);
        when(context.credentials.version()).thenReturn("60.0");

        when(metadataPort.retrieve(any(RetrieveRequest.class))).thenReturn(asyncResult);
        when(asyncResult.getId()).thenReturn("test-id");
        when(metadataPort.checkRetrieveStatus("test-id", true)).thenReturn(result);
        when(result.isDone()).thenReturn(true);
        when(result.isSuccess()).thenReturn(true);
        when(result.getZipFile()).thenReturn(zipData);

        // Test
        List<String> webServiceClasses = RetrieveWsdls.findCustomWsdls(context);

        // Verify
        assertNotNull(webServiceClasses, "Result should not be null");
        assertEquals(2, webServiceClasses.size(), "Should find exactly 2 web service classes");
        assertTrue(webServiceClasses.contains("MyService"), "Should contain MyService");
        assertTrue(webServiceClasses.contains("AnotherService"), "Should contain AnotherService");
        assertFalse(webServiceClasses.contains("RegularClass"), "Should not contain RegularClass");
    }

    @Test
    void testFindCustomWsdls_NoWebServiceClasses() throws Exception {
        // Create a mock zip file with no web service classes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            String regularContent = "public class RegularClass {\n" +
                                   "    public void method() {}\n" +
                                   "}";
            ZipEntry entry = new ZipEntry("classes/RegularClass.cls");
            zos.putNextEntry(entry);
            zos.write(regularContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        byte[] zipData = baos.toByteArray();

        Context context = mock(Context.class);
        MetadataPortType metadataPort = mock(MetadataPortType.class);
        AsyncResult asyncResult = mock(AsyncResult.class);
        RetrieveResult result = mock(RetrieveResult.class);

        context.port = metadataPort;
        context.credentials = mock(org.solenopsis.session.Credentials.class);
        when(context.credentials.version()).thenReturn("60.0");

        when(metadataPort.retrieve(any(RetrieveRequest.class))).thenReturn(asyncResult);
        when(asyncResult.getId()).thenReturn("test-id");
        when(metadataPort.checkRetrieveStatus("test-id", true)).thenReturn(result);
        when(result.isDone()).thenReturn(true);
        when(result.isSuccess()).thenReturn(true);
        when(result.getZipFile()).thenReturn(zipData);

        List<String> webServiceClasses = RetrieveWsdls.findCustomWsdls(context);

        assertNotNull(webServiceClasses);
        assertTrue(webServiceClasses.isEmpty(), "Should return empty list when no web services found");
    }

    @Test
    void testFindCustomWsdls_EmptyZip() throws Exception {
        // Create an empty zip file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Empty zip
        }

        byte[] zipData = baos.toByteArray();

        Context context = mock(Context.class);
        MetadataPortType metadataPort = mock(MetadataPortType.class);
        AsyncResult asyncResult = mock(AsyncResult.class);
        RetrieveResult result = mock(RetrieveResult.class);

        context.port = metadataPort;
        context.credentials = mock(org.solenopsis.session.Credentials.class);
        when(context.credentials.version()).thenReturn("60.0");

        when(metadataPort.retrieve(any(RetrieveRequest.class))).thenReturn(asyncResult);
        when(asyncResult.getId()).thenReturn("test-id");
        when(metadataPort.checkRetrieveStatus("test-id", true)).thenReturn(result);
        when(result.isDone()).thenReturn(true);
        when(result.isSuccess()).thenReturn(true);
        when(result.getZipFile()).thenReturn(zipData);

        List<String> webServiceClasses = RetrieveWsdls.findCustomWsdls(context);

        assertNotNull(webServiceClasses);
        assertTrue(webServiceClasses.isEmpty(), "Should return empty list for empty zip");
    }

    // ========== Tests for retrieveWsdl with MockedStatic HttpClients ==========

    @Test
    void testRetrieveWsdl_Success(@TempDir Path tempDir) throws Exception {
        Context context = new Context();
        context.outputDir = tempDir.toString();
        context.prefix = "test-";
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.sessionId()).thenReturn("test-session-id");

        String wsdlContent = "<?xml version=\"1.0\"?><definitions>Test WSDL</definitions>";
        ByteArrayInputStream wsdlStream = new ByteArrayInputStream(wsdlContent.getBytes(StandardCharsets.UTF_8));

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);

        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(wsdlStream);

        HttpGet httpGet = new HttpGet("https://test.salesforce.com/services/wsdl/apex");

        try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
            httpClientsMock.when(HttpClients::createDefault).thenReturn(mockHttpClient);

            RetrieveWsdls.retrieveWsdl(context, httpGet, "apex.wsdl");

            httpClientsMock.verify(HttpClients::createDefault);
            verify(mockHttpClient).execute(any(HttpGet.class), any(HttpClientContext.class));
        }

        File outputFile = new File(tempDir.toString(), "test-apex.wsdl");
        assertTrue(outputFile.exists(), "WSDL file should be created");
        String fileContent = Files.readString(outputFile.toPath());
        assertTrue(fileContent.contains("Test WSDL"), "File should contain WSDL content");
    }

    @Test
    void testRetrieveWsdl_CreatesOutputFile(@TempDir Path tempDir) throws Exception {
        Context context = new Context();
        context.outputDir = tempDir.toString();
        context.prefix = "myorg-";
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.sessionId()).thenReturn("session-123");

        String wsdlContent = "<definitions>Enterprise WSDL</definitions>";
        ByteArrayInputStream wsdlStream = new ByteArrayInputStream(wsdlContent.getBytes(StandardCharsets.UTF_8));

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);

        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(wsdlStream);

        try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
            httpClientsMock.when(HttpClients::createDefault).thenReturn(mockHttpClient);

            RetrieveWsdls.retrieveWsdl(context, new HttpGet("https://example.com/wsdl"), "enterprise.wsdl");
        }

        File outputFile = new File(tempDir.toString(), "myorg-enterprise.wsdl");
        assertTrue(outputFile.exists(), "Output file with prefix should be created");
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("Enterprise WSDL"), "File should contain expected content");
    }

    @Test
    void testRetrieveWsdl_WithEmptyPrefix(@TempDir Path tempDir) throws Exception {
        Context context = new Context();
        context.outputDir = tempDir.toString();
        context.prefix = "";
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.sessionId()).thenReturn("session-456");

        String wsdlContent = "<definitions>Partner WSDL</definitions>";
        ByteArrayInputStream wsdlStream = new ByteArrayInputStream(wsdlContent.getBytes(StandardCharsets.UTF_8));

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);

        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(wsdlStream);

        try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
            httpClientsMock.when(HttpClients::createDefault).thenReturn(mockHttpClient);

            RetrieveWsdls.retrieveWsdl(context, new HttpGet("https://example.com/wsdl"), "partner.wsdl");
        }

        File outputFile = new File(tempDir.toString(), "partner.wsdl");
        assertTrue(outputFile.exists(), "Output file without prefix should be created");
    }

    @Test
    void testRetrieveWsdl_SetsCookieWithSessionId(@TempDir Path tempDir) throws Exception {
        Context context = new Context();
        context.outputDir = tempDir.toString();
        context.prefix = "";
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.sessionId()).thenReturn("special-session-id");

        ByteArrayInputStream emptyStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);

        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(emptyStream);

        try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
            httpClientsMock.when(HttpClients::createDefault).thenReturn(mockHttpClient);

            RetrieveWsdls.retrieveWsdl(context, new HttpGet("https://example.com"), "test.wsdl");

            verify(context.sessionContext).sessionId();
        }
    }

    // ========== Tests for retrieveWsdls with MockedStatic HttpClients ==========

    @Test
    void testRetrieveWsdls_WithEmptyCustomList(@TempDir Path tempDir) throws Exception {
        Context context = new Context();
        context.outputDir = tempDir.toString();
        context.prefix = "";
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.sessionId()).thenReturn("test-session");
        when(context.sessionContext.serverUrl()).thenReturn("https://test.salesforce.com");

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);

        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent())
            .thenReturn(new ByteArrayInputStream("<wsdl>apex</wsdl>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl>enterprise</wsdl>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl>metadata</wsdl>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl>partner</wsdl>".getBytes(StandardCharsets.UTF_8)));

        try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
            httpClientsMock.when(HttpClients::createDefault).thenReturn(mockHttpClient);

            RetrieveWsdls.retrieveWsdls(context, new ArrayList<>());

            verify(mockHttpClient, times(4)).execute(any(HttpGet.class), any(HttpClientContext.class));
        }

        assertTrue(new File(tempDir.toString(), "apex.wsdl").exists(), "apex.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "enterprise.wsdl").exists(), "enterprise.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "metadata.wsdl").exists(), "metadata.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "partner.wsdl").exists(), "partner.wsdl should exist");
    }

    @Test
    void testRetrieveWsdls_WithCustomWsdls(@TempDir Path tempDir) throws Exception {
        Context context = new Context();
        context.outputDir = tempDir.toString();
        context.prefix = "";
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.sessionId()).thenReturn("test-session");
        when(context.sessionContext.serverUrl()).thenReturn("https://test.salesforce.com");

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);

        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent())
            .thenReturn(new ByteArrayInputStream("<wsdl>1</wsdl>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl>2</wsdl>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl>3</wsdl>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl>4</wsdl>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl>custom1</wsdl>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl>custom2</wsdl>".getBytes(StandardCharsets.UTF_8)));

        List<String> customWsdls = Arrays.asList("MyService", "AnotherService");

        try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
            httpClientsMock.when(HttpClients::createDefault).thenReturn(mockHttpClient);

            RetrieveWsdls.retrieveWsdls(context, customWsdls);

            verify(mockHttpClient, times(6)).execute(any(HttpGet.class), any(HttpClientContext.class));
        }

        assertTrue(new File(tempDir.toString(), "apex.wsdl").exists(), "apex.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "enterprise.wsdl").exists(), "enterprise.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "metadata.wsdl").exists(), "metadata.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "partner.wsdl").exists(), "partner.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "MyService.wsdl").exists(), "MyService.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "AnotherService.wsdl").exists(), "AnotherService.wsdl should exist");
    }

    @Test
    void testRetrieveWsdls_WithPrefix(@TempDir Path tempDir) throws Exception {
        Context context = new Context();
        context.outputDir = tempDir.toString();
        context.prefix = "org-";
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.sessionId()).thenReturn("test-session");
        when(context.sessionContext.serverUrl()).thenReturn("https://test.salesforce.com");

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);

        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent())
            .thenReturn(new ByteArrayInputStream("<wsdl/>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl/>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl/>".getBytes(StandardCharsets.UTF_8)))
            .thenReturn(new ByteArrayInputStream("<wsdl/>".getBytes(StandardCharsets.UTF_8)));

        try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
            httpClientsMock.when(HttpClients::createDefault).thenReturn(mockHttpClient);

            RetrieveWsdls.retrieveWsdls(context, new ArrayList<>());
        }

        assertTrue(new File(tempDir.toString(), "org-apex.wsdl").exists(), "Prefixed apex.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "org-enterprise.wsdl").exists(), "Prefixed enterprise.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "org-metadata.wsdl").exists(), "Prefixed metadata.wsdl should exist");
        assertTrue(new File(tempDir.toString(), "org-partner.wsdl").exists(), "Prefixed partner.wsdl should exist");
    }

    @Test
    void testRetrieveWsdls_CustomUrlConstructionPerClass() {
        String serverUrl = "https://test.salesforce.com";
        List<String> customWsdls = Arrays.asList("MyService", "AnotherService");

        for (String wsdl : customWsdls) {
            String url = WsdlSubUrlEnum.CUSTOM.computeUrl(serverUrl) + "/" + wsdl;
            assertTrue(url.contains(wsdl),
                "URL should contain class name: " + wsdl);
            assertTrue(url.contains("/services/wsdl/class/"),
                "URL should contain custom WSDL path");
        }
    }

    @Test
    void testRetrieveWsdls_StandardWsdlEnumsExist() {
        assertNotNull(WsdlSubUrlEnum.APEX, "APEX enum should exist");
        assertNotNull(WsdlSubUrlEnum.ENTERPRISE, "ENTERPRISE enum should exist");
        assertNotNull(WsdlSubUrlEnum.METADATA, "METADATA enum should exist");
        assertNotNull(WsdlSubUrlEnum.PARTNER, "PARTNER enum should exist");

        assertFalse(WsdlSubUrlEnum.APEX.getSubUrl().isEmpty(), "APEX sub-URL should not be empty");
        assertFalse(WsdlSubUrlEnum.ENTERPRISE.getSubUrl().isEmpty(), "ENTERPRISE sub-URL should not be empty");
        assertFalse(WsdlSubUrlEnum.METADATA.getSubUrl().isEmpty(), "METADATA sub-URL should not be empty");
        assertFalse(WsdlSubUrlEnum.PARTNER.getSubUrl().isEmpty(), "PARTNER sub-URL should not be empty");
    }

    @Test
    void testRetrieveWsdls_CustomWsdlUrlConstruction() {
        String serverUrl = "https://test.salesforce.com";
        String customClassName = "MyService";

        String customUrl = WsdlSubUrlEnum.CUSTOM.computeUrl(serverUrl) + "/" + customClassName;

        assertTrue(customUrl.contains("/services/wsdl/class/"),
            "Custom WSDL URL should contain correct path");
        assertTrue(customUrl.endsWith("/" + customClassName),
            "Custom WSDL URL should end with class name");
        assertTrue(customUrl.startsWith(serverUrl),
            "Custom WSDL URL should start with server URL");
    }

    // ========== Tests for main method ==========

    @Test
    void testMain_MethodExists() {
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method mainMethod = RetrieveWsdls.class.getDeclaredMethod(
                "main", String[].class);
            assertNotNull(mainMethod, "Main method should exist");
            assertEquals(void.class, mainMethod.getReturnType(), "Main should return void");
            assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()),
                "Main should be public");
            assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()),
                "Main should be static");
        });
    }

    @Test
    void testMain_WithNoCredentialsThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> RetrieveWsdls.main(new String[]{"--prefix", "test-"}),
            "main() with no credentials should throw IllegalArgumentException from Context");
    }

    @Test
    void testMain_WithEmptyArgsThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> RetrieveWsdls.main(new String[]{}),
            "main() with empty args should throw IllegalArgumentException");
    }

    // ========== Additional helper/utility tests ==========

    @Test
    void testContext_CanBeInstantiatedForTesting() {
        Context context = new Context();
        assertNotNull(context, "Should be able to create Context object");
        assertEquals("", context.prefix, "Default prefix should be empty");
        assertEquals(System.getProperty("user.home"), context.outputDir,
            "Default outputDir should be user home");
    }

    @Test
    void testContext_SessionIdUsedInCookie() {
        Context context = new Context();
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.sessionId()).thenReturn("mock-session-id-12345");

        String sessionId = context.sessionContext.sessionId();
        assertEquals("mock-session-id-12345", sessionId,
            "Session ID should be retrieved from SessionContext");

        assertTrue(sessionId.length() > 0, "Session ID should not be empty");
    }

    @Test
    void testContext_ServerUrlUsedInHttpGet() {
        Context context = new Context();
        context.sessionContext = mock(SessionContext.class);
        when(context.sessionContext.serverUrl()).thenReturn("https://na1.salesforce.com");

        String serverUrl = context.sessionContext.serverUrl();
        assertEquals("https://na1.salesforce.com", serverUrl,
            "Server URL should be retrieved from SessionContext");

        assertTrue(serverUrl.startsWith("https://"), "Server URL should use HTTPS");
    }

    // ========== Tests for WSDL selection (--wsdl, --skip-custom, --include-tooling) ==========

    @Test
    void testRetrieveWsdls_DefaultStandardWsdlSet() {
        Context context = new Context();
        Set<WsdlSubUrlEnum> wsdls = context.getStandardWsdls();

        assertEquals(4, wsdls.size(), "Default should include 4 standard WSDLs");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.APEX));
        assertTrue(wsdls.contains(WsdlSubUrlEnum.ENTERPRISE));
        assertTrue(wsdls.contains(WsdlSubUrlEnum.METADATA));
        assertTrue(wsdls.contains(WsdlSubUrlEnum.PARTNER));
        assertFalse(wsdls.contains(WsdlSubUrlEnum.TOOLING));
    }

    @Test
    void testRetrieveWsdls_WithToolingIncluded() {
        Context context = new Context();
        context.includeTooling = true;
        Set<WsdlSubUrlEnum> wsdls = context.getStandardWsdls();

        assertTrue(wsdls.contains(WsdlSubUrlEnum.TOOLING),
            "Tooling WSDL should be included when includeTooling is true");
    }

    @Test
    void testRetrieveWsdls_SkipCustomSkipsDiscovery() {
        Context context = new Context();
        context.skipCustom = true;

        List<String> customWsdls = context.skipCustom ? Collections.emptyList() : Arrays.asList("MyService");
        assertTrue(customWsdls.isEmpty(),
            "Custom WSDLs list should be empty when skipCustom is true");
    }

    @Test
    void testRetrieveWsdls_CustomWsdlsPassedWhenNotSkipped() {
        Context context = new Context();
        context.skipCustom = false;

        List<String> customWsdls = context.skipCustom ? Collections.emptyList() : Arrays.asList("MyService", "AnotherService");
        assertEquals(2, customWsdls.size(),
            "Custom WSDLs should be passed when skipCustom is false");
    }

    @Test
    void testRetrieveWsdls_ExplicitWsdlSelection() {
        Context context = new Context();
        context.wsdls = EnumSet.of(WsdlSubUrlEnum.APEX, WsdlSubUrlEnum.METADATA);
        Set<WsdlSubUrlEnum> wsdls = context.getStandardWsdls();

        assertEquals(2, wsdls.size(), "Should return only selected WSDLs");
        assertTrue(wsdls.contains(WsdlSubUrlEnum.APEX));
        assertTrue(wsdls.contains(WsdlSubUrlEnum.METADATA));
        assertFalse(wsdls.contains(WsdlSubUrlEnum.ENTERPRISE));
        assertFalse(wsdls.contains(WsdlSubUrlEnum.PARTNER));
    }

    @Test
    void testRetrieveWsdls_ToolingUrlConstruction() {
        String serverUrl = "https://test.salesforce.com";
        String toolingUrl = WsdlSubUrlEnum.TOOLING.computeUrl(serverUrl);

        assertTrue(toolingUrl.contains("/services/wsdl/tooling"),
            "Tooling URL should contain correct path");
        assertTrue(toolingUrl.startsWith(serverUrl),
            "Tooling URL should start with server URL");
    }

    @Test
    void testRetrieveWsdls_ToolingEnumExists() {
        assertNotNull(WsdlSubUrlEnum.TOOLING, "TOOLING enum should exist");
        assertFalse(WsdlSubUrlEnum.TOOLING.getSubUrl().isEmpty(),
            "TOOLING sub-URL should not be empty");
        assertEquals("/services/wsdl/tooling", WsdlSubUrlEnum.TOOLING.getSubUrl(),
            "TOOLING sub-URL should be correct");
    }

    @Test
    void testRetrieveWsdls_WsdlFileNaming() {
        assertEquals("apex", WsdlSubUrlEnum.APEX.name().toLowerCase());
        assertEquals("enterprise", WsdlSubUrlEnum.ENTERPRISE.name().toLowerCase());
        assertEquals("metadata", WsdlSubUrlEnum.METADATA.name().toLowerCase());
        assertEquals("partner", WsdlSubUrlEnum.PARTNER.name().toLowerCase());
        assertEquals("tooling", WsdlSubUrlEnum.TOOLING.name().toLowerCase());
    }

    @Test
    void testRetrieveWsdls_MethodStillAcceptsList() {
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = RetrieveWsdls.class.getDeclaredMethod(
                "retrieveWsdls", Context.class, List.class);
            assertNotNull(method);
            assertEquals(void.class, method.getReturnType());
        });
    }

    @Test
    void testMain_WithSkipCustomFlagStillNeedsCredentials() {
        assertThrows(IllegalArgumentException.class,
            () -> RetrieveWsdls.main(new String[]{"--skip-custom"}),
            "main() with --skip-custom but no credentials should still throw");
    }

    @Test
    void testMain_WithIncludeToolingFlagStillNeedsCredentials() {
        assertThrows(IllegalArgumentException.class,
            () -> RetrieveWsdls.main(new String[]{"--include-tooling"}),
            "main() with --include-tooling but no credentials should still throw");
    }

    @Test
    void testMain_WithWsdlFlagStillNeedsCredentials() {
        assertThrows(IllegalArgumentException.class,
            () -> RetrieveWsdls.main(new String[]{"--wsdl", "apex,metadata"}),
            "main() with --wsdl but no credentials should still throw");
    }

    @Test
    void testMain_WithAllNewFlagsStillNeedsCredentials() {
        assertThrows(IllegalArgumentException.class,
            () -> RetrieveWsdls.main(new String[]{
                "--wsdl", "apex,partner",
                "--skip-custom",
                "--include-tooling"
            }),
            "main() with all new flags but no credentials should still throw");
    }
}
