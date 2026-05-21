# Testing Documentation

## Test Coverage Overview

**Overall Project Coverage:** 85% instruction, 84% branch (216 tests)

### Package-Level Coverage

| Package | Instruction Coverage | Branch Coverage | Test Count |
|---------|---------------------|-----------------|------------|
| org.solenopsis.metadata.xml | 100% | 100% | 11 tests |
| org.solenopsis.metadata.stats | 99% | 100% | 19 tests |
| org.solenopsis.metadata | 98% | n/a | 17 tests |
| org.solenopsis.metadata.list | 96% | 87% | 8 tests |
| org.solenopsis.metadata.validation | 96% | 88% | 38 tests |
| **org.solenopsis.metadata.diff** | **90%** | **89%** | **59 tests** |
| **org.solenopsis.metadata.wsdl** | **46%** | **62%** | **64 tests** |

### org.solenopsis.metadata.wsdl Package

#### RetrieveWsdls Class (60% coverage, 90% branch)

**Well-Tested Methods:**
- `isWebServiceClass()` - 18 comprehensive tests covering all edge cases
- `createRetrieveRequest()` - 4 tests for various API versions
- `ensureRetrieveSuccess()` - 3 tests for success/failure scenarios
- `retrieveApexClasses()` - 3 tests including polling logic
- `findCustomWsdls()` - 3 tests with mock ZIP files

**Methods Difficult to Unit Test:**

1. **`retrieveWsdl()`** - Makes actual HTTP calls to Salesforce
   - Requires mocking Apache HttpClient's `HttpClients.createDefault().execute()`
   - Involves file I/O (FileWriter)
   - Tested via: reflection tests, path construction tests
   - **Recommended:** Integration tests with mock HTTP server

2. **`retrieveWsdls()`** - Orchestrates multiple `retrieveWsdl()` calls
   - Depends on `retrieveWsdl()` which requires HTTP mocking
   - Tested via: reflection tests, URL construction logic tests
   - **Recommended:** Integration tests

3. **`main()`** - Application entry point
   - Creates Context which attempts Salesforce login
   - Tested via: reflection tests to verify signature
   - **Recommended:** End-to-end integration tests

#### Context Class (7% coverage, 0% branch)

**Why Coverage is Low:**

The Context class is designed for command-line execution and integrates with external systems that are difficult to mock in unit tests:

**Methods Requiring Integration Testing:**

1. **`setCredentials(String fileName)`**
   - Calls `CredentialsUtil.fromFile()` - requires actual credential file
   - Calls `LoginServiceEnum.DEFAULT_LOGIN_SERVICE.getLoginService().login()` - **attempts actual Salesforce login**
   - Creates `MetadataPortType` - requires active Salesforce connection
   - **Cannot be unit tested** without mocking the entire Salesforce SDK

2. **`setSolenopsisCredentials(String env)`**
   - Reads from `~/.solenopsis/credentials/` directory
   - Delegates to `setCredentials()` (see above)
   - **Cannot be unit tested** without actual credential files

3. **`ensureCredentials()`**
   - Calls `System.exit(1)` on failure
   - **Cannot be tested** without a SecurityManager or JVM fork
   - Recommended approach: Test in integration tests or exclude from coverage

4. **`Context(String[] args)`**
   - Parses command-line arguments
   - Calls methods above based on arguments
   - Creates directories with `File.mkdirs()`
   - **Partially tested** via argument parsing logic tests

**What IS Tested:**

- No-args constructor (for test mocking)
- Field visibility and accessibility
- Default values
- Argument parsing logic (not full constructor)
- Path construction logic
- Directory creation behavior

### org.solenopsis.metadata.diff Package (90% coverage, 89% branch)

**Well-Tested Classes:**

1. **MetadataComponent** (98% coverage) - 11 tests
   - Constructor validation
   - Equals and hashCode contracts
   - Deleted flag behavior
   - toString formatting

2. **FilePathMapper** (97% coverage, 95% branch) - 22 tests
   - ApexClass, ApexTrigger, ApexPage parsing
   - LWC and Aura bundle parsing
   - CustomField, ValidationRule, RecordType parsing
   - Layout parsing (simple and complex)
   - Edge cases: null, empty, non-metadata paths
   - Windows path handling (backslash normalization)

3. **DiffPackageGenerator** (82% coverage, 100% branch) - 12 tests
   - Single and multiple type package generation
   - Deploy vs destructive changes separation
   - Component filtering (deleted vs non-deleted)
   - Utility methods: hasDeletedComponents(), countByType()

4. **GitDiffParser** (85% coverage, 68% branch) - 14 tests
   - Changed files detection (HEAD vs HEAD, invalid refs)
   - Deleted files detection
   - Added/modified files detection
   - Working directory parameter handling
   - Component parsing with metadata filtering
   - Error handling for invalid git references
   - Integration-style tests using actual git repository

**Testing Approach:**

- GitDiffParser tests run conditionally when a git repository is present (`@EnabledIf("isGitRepository")`)
- Tests use HEAD vs HEAD comparisons (no changes expected) to verify correct behavior
- Error cases test invalid git references to ensure proper exception handling
- Non-metadata files are filtered out during component parsing

## Testing Strategy

### Unit Tests (Current)
- Pure logic functions (pattern matching, parsing, validation)
- Mock-based tests for methods that accept injectable dependencies
- Reflection-based tests to verify method signatures and behavior

### Integration Tests (Recommended for Future)
- HTTP-based methods: Use WireMock or similar to mock Salesforce endpoints
- File I/O: Use temporary directories and mock credential files
- Login flow: Mock Salesforce authentication responses
- System.exit: Use JUnit's `@Fork` or similar

### End-to-End Tests (Recommended for Future)
- Full workflow: Create Context → Find WSDLs → Download WSDLs
- Requires: Salesforce sandbox org with test credentials
- Should verify: Complete WSDL retrieval process

## Running Tests

```bash
# Run all tests
mvn clean test

# Run tests with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html

# Run specific test class
mvn test -Dtest=RetrieveWsdlsTest
```

## Coverage Gaps Analysis

### Why 100% Coverage Isn't Achieved

The remaining 15% uncovered instructions are primarily in:

1. **Integration points** (12%): HTTP calls, Salesforce login, file I/O
2. **Application entry points** (2%): `main()` methods
3. **System interaction** (1%): `System.exit()`, directory creation

These methods are designed for integration with external systems and are more appropriate for integration or end-to-end testing rather than unit testing.

### Improving Coverage Further

To reach higher coverage would require:

1. **Dependency Injection**: Refactor to inject HttpClient, CredentialsUtil, etc.
2. **Integration Test Framework**: Set up WireMock, embedded Salesforce mock
3. **Test Harness**: Custom SecurityManager to intercept System.exit calls
4. **Sandbox Environment**: Salesforce developer org for real integration tests

**Trade-off:** The current 85% coverage provides excellent confidence in core business logic while keeping tests maintainable and fast. All business logic packages have >85% coverage.

## Test Quality Metrics

- **Total Tests:** 216
- **Test Execution Time:** ~30 seconds
- **Test Failures:** 0
- **Test Categories:**
  - Logic/Algorithm: 120 tests
  - Validation: 60 tests
  - Edge Cases: 30 tests
  - Integration Simulation: 6 tests

## Continuous Improvement

Future test additions should focus on:
1. Integration tests for HTTP/file I/O methods
2. Contract tests for Salesforce API interactions
3. Performance tests for large WSDL downloads
4. Error handling tests with various failure modes
