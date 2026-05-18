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
import static org.junit.jupiter.api.Assertions.*;

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
}
