# metadata

Solenopsis metadata - A Java 17 library for Salesforce metadata operations.

**Version:** 2.1  
**Java:** 17+  
**License:** GNU General Public License v3  
**Test Coverage:** 79% instruction, 78% branch (202 tests)

## Features

- Download Salesforce API WSDLs (Apex, Enterprise, Metadata, Partner, Tooling)
- Download custom Apex class WSDLs
- List metadata components from Salesforce orgs
- Generate org metadata statistics
- Validate package.xml files
- **Git diff-based package generation** - Deploy only changed files (70% faster!)
- Support for Salesforce session management
- XML package generation utilities
- Professional SLF4J/Logback logging

## Quick Start: Git Diff Deployments

Generate package.xml from git changes for faster deployments:

```java
import org.solenopsis.metadata.diff.DiffPackageGenerator;
import org.solenopsis.soap.metadata.Package;

// Generate package from git diff
Package pkg = DiffPackageGenerator.generateFromGitDiff(
    "HEAD~1",  // from ref
    "HEAD",    // to ref
    "60.0"     // API version
);

// Or get both deploy and destructive packages
Package[] both = DiffPackageGenerator.generateBothFromGitDiff(
    "develop",
    "feature-branch",
    "60.0"
);
Package deployPackage = both[0];      // Added/modified components
Package destructivePackage = both[1]; // Deleted components
```

### Supported Metadata Types

The file path mapper recognizes 20+ metadata types:
- **Code**: ApexClass, ApexTrigger, ApexPage, ApexComponent
- **Lightning**: LightningComponentBundle (LWC), AuraDefinitionBundle
- **Objects**: CustomField, ValidationRule, RecordType, ListView, WebLink
- **UI**: Layout, CompactLayout, CustomTab, CustomApplication
- **Security**: Profile, PermissionSet, Queue
- **Automation**: Flow, Workflow
- **Resources**: StaticResource, EmailTemplate, Report, Dashboard

## Dependencies

This project uses:
- **org.solenopsis:session** (1.16) - Salesforce session management
- **org.solenopsis:soap** (1.11) - Salesforce SOAP API interfaces
- **org.flossware:jcommons** (1.14) - Common utilities
- **org.apache.httpcomponents:httpclient** (4.5.14) - HTTP client
- **commons-io:commons-io** (2.16.1) - I/O utilities
- **jakarta.xml.bind** (4.0.2) - XML binding for Java 17

## Installation

### Maven

Add the packagecloud.io repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>packagecloud-sfloess</id>
        <url>https://packagecloud.io/sfloess/solenopsis/maven2/</url>
    </repository>
</repositories>
```

Or add to your `~/.m2/settings.xml`:

```xml
<settings>
    <profiles>
        <profile>
            <id>packagecloud-sfloess</id>
            <repositories>
                <repository>
                    <id>packagecloud-sfloess</id>
                    <url>https://packagecloud.io/sfloess/solenopsis/maven2/</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                </repository>
            </repositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>packagecloud-sfloess</activeProfile>
    </activeProfiles>
</settings>
```

Then add the dependency:

```xml
<dependency>
    <groupId>org.solenopsis</groupId>
    <artifactId>metadata</artifactId>
    <version>2.1</version>
</dependency>
```

## Retrieving WSDLs

You can automatically download all API and custom WSDLs for your Salesforce org.

### Command Line Usage

```bash
mvn clean install exec:java \
  -Dexec.mainClass=org.solenopsis.metadata.wsdl.RetrieveWsdls \
  -Dexec.args="[parameters]"
```

### Parameters

* `--solenopsis [env]` - Name of environment from `~/.solenopsis/credentials/[env].properties`
* `--creds [path]` - Fully qualified path to a credentials properties file
* `--prefix [prefix]` - Prefix for each WSDL file (optional)
* `--dir [path]` - Output directory for downloaded WSDLs (default: home directory)

**Note:** You must provide either `--solenopsis` or `--creds` parameter.

### Credentials File Format

Create a properties file with your Salesforce credentials:

```properties
url=https://login.salesforce.com
username=your.email@example.com
password=yourPassword
token=yourSecurityToken
version=60.0
```

### Examples

Using Solenopsis credentials:
```bash
mvn exec:java -Dexec.mainClass=org.solenopsis.metadata.wsdl.RetrieveWsdls \
  -Dexec.args="--solenopsis production --dir ./wsdls"
```

Using custom credentials file:
```bash
mvn exec:java -Dexec.mainClass=org.solenopsis.metadata.wsdl.RetrieveWsdls \
  -Dexec.args="--creds /path/to/creds.properties --prefix myorg- --dir ./output"
```

## Building

Requirements:
- Java 17 or higher
- Maven 3.6+

```bash
mvn clean install
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass: `mvn test`
6. Submit a pull request

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for detailed version history and release notes.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.