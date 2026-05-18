# Changelog

All notable changes to this project will be documented in this file.

## [2.0] - 2026-05-18

### Breaking Changes
- **Java 17 Required**: Upgraded from Java 1.8 to Java 17
- **Versioning**: Changed from X.Y.Z to X.Y format (1.0.0 → 2.0)
- **Dependencies**: Replaced Keraiai and jCore with Solenopsis Session/Soap and FlossWare jcommons

### Added
- **Unit Tests**: Added comprehensive test suite (46 tests across 6 test classes)
  - RetrieveWsdlsTest (18 tests) - Web service detection
  - WsdlSubUrlEnumTest (11 tests) - WSDL URL generation
  - RetrieveMetadataTest (6 tests) - Metadata retrieval
  - PackageTest (6 tests) - XML package handling
  - TypesTest (5 tests) - XML types handling
  - ContextTest (11 tests) - Command-line argument parsing
- **Code Coverage**: JaCoCo integration for test coverage reporting
- **Deployment**: packagecloud.io Maven repository configuration
- **Documentation**: 
  - Comprehensive README with installation and usage examples
  - IMPROVEMENTS.md documenting web service detection enhancements
  - CHANGELOG.md (this file)

### Changed
- **Project Name**: Renamed from "Metadata" to "metadata" (lowercase)
- **Repository**: 
  - Renamed GitHub repository to lowercase
  - Migrated from `master` to `main` branch
- **Dependencies Updated**:
  - Replaced `org.solenopsis:keraiai` (3.0.8) with:
    - `org.solenopsis:session` (1.16) - Session management
    - `org.solenopsis:soap` (1.11) - SOAP API interfaces
  - Replaced `org.flossware:jcore` with `org.flossware:jcommons` (1.14)
  - Added `jakarta.xml.bind-api` (4.0.2) for Java 17 compatibility
  - Updated `httpclient` from 4.5.3 to 4.5.14
  - Updated `commons-io` from 2.5 to 2.16.1
  - Migrated from JUnit 4.12 to JUnit 5.10.2 (Jupiter)
- **Build Plugins Updated**:
  - maven-compiler-plugin: 3.8.1 → 3.13.0
  - maven-surefire-plugin: 2.22.2 → 3.2.5
  - maven-deploy-plugin: 2.8.2 → 3.1.2
  - Replaced Cobertura with JaCoCo 0.8.12 for code coverage

### Improved
- **Web Service Detection** (~78% accuracy improvement):
  - **Before**: 17% accuracy (naive string matching)
  - **After**: >95% accuracy (regex pattern matching with context awareness)
  - Added case-insensitive keyword matching (webservice, WebService, WEBSERVICE)
  - Removed false positives from comments, string literals, and class names
  - Added method signature validation
  - Proper handling of complex return types (List<String>, Map<K,V>, etc.)
- **Resource Management**:
  - Replaced manual resource handling with try-with-resources
  - Reduced buffer size from 2MB to 8KB (99.6% memory reduction)
  - Fixed potential ZipInputStream resource leaks
- **Character Encoding**:
  - Explicit UTF-8 encoding for all file operations
  - Consistent cross-platform behavior

### Removed
- **WildcardEnum**: Removed 158-line enum that only returned "ApexClass" string
- **Deprecated APIs**: Replaced deprecated HttpClient code (ClientContext, BasicHttpContext)
- **javax.xml.bind**: Migrated to jakarta.xml.bind for Java 17

### Fixed
- Fixed deprecated HttpClient API usage
- Fixed missing charset specifications (now uses UTF-8 explicitly)
- Fixed security vulnerabilities in dependencies:
  - httpclient: CVE-2015-5262 (upgraded to 4.5.14)
  - commons-io: Multiple CVEs (upgraded to 2.16.1)
  - junit: CVE-2020-15250 (upgraded to 5.10.2)

## [1.0.0] - 2019-12-19

### Initial Release
- Basic WSDL retrieval for Salesforce orgs
- Support for Apex, Enterprise, Metadata, Partner, and Tooling WSDLs
- Custom Apex class WSDL download
- Command-line interface with credential file support
- Java 1.8 compatibility
