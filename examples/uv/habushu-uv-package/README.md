# Habushu UV Package

A general-purpose approach to developing a reusable Python package/library using Habushu with the UV package manager.

## Overview

This project demonstrates how to create a reusable Python package using Habushu with UV as the package manager. It 
provides a template for structuring your Python library with proper packaging, dependency management, and testing 
capabilities.

## Features

- **Maven Integration**: Seamlessly integrates Python development with Maven build lifecycle
- **UV Package Management**: Uses UV for fast, reliable dependency management
- **Reusable Module Structure**: Demonstrates proper Python package organization
- **BDD Testing**: Includes Behave for behavior-driven development testing
- **Resource Management**: Shows how to include and access non-Python resources
- **Protocol Buffers Support**: Example of integrating protobuf files in your package

## Project Structure

```
habushu-uv-package/
├── pyproject.toml       # Python project configuration
├── pom.xml              # Maven project configuration
├── src/
│   ├── habushu_uv_package/  # Main package code
│   │   ├── __init__.py
│   │   ├── helloworld.py    # Example module
│   │   ├── reusable_module/ # Subpackage example
│   │   │   └── worker.py
│   │   └── util/            # Utility functions
│   │       └── useful.py
│   ├── person.proto         # Example protobuf definition
│   └── resources/           # Non-Python resources
│       └── example-resources.txt
└── tests/                   # Test suite
    ├── features/            # Behave feature files
    │   ├── reference-src.feature
    │   └── steps/           # Step definitions
    │       └── reference_src.py
    └── resources/           # Test resources
        └── config/
            └── test.properties
```

## Getting Started

### Building the Package

To build the package using Maven:

```bash
mvn clean install
```

This will:
1. Create a Python virtual environment
2. Install dependencies using UV
3. Run tests
4. Package the library

### Using the Package in Another Project

After building, you can use this package in other projects by adding it as a dependency in your `pyproject.toml`:
   ```toml
   [project]
   dependencies = [
       "habushu_uv_package>=3.0.0.dev"
   ]
   ```

## Development

### Running Tests

Tests are written using Behave for BDD-style testing:

```bash
mvn test
```

Or to run specific tests:

```bash
mvn test -Dbehave.options="--tags=integration_test"
```

### Integration Test Profile

The project includes an integration test profile:

```bash
mvn test -Pintegration-test
```

This demonstrates how to inject different environment variables for different test scenarios.

## Habushu Integration

This project demonstrates several Habushu features:

1. **Managed Dependencies**: Control Python dependencies through Maven
   ```xml
   <managedDependencies>
       <packageDefinition>
           <packageName>krausening</packageName>
           <operatorAndVersion>>=20</operatorAndVersion>
       </packageDefinition>
   </managedDependencies>
   ```

2. **Virtual Environment Management**: Automatic creation and cleanup
   ```xml
   <deleteVirtualEnv>true</deleteVirtualEnv>
   ```

3. **Test Configuration**: Customizable test execution
   ```xml
   <behaveOptions>-D environment=integration_test --tags integration_test</behaveOptions>
   ```

4. **PyPI Repository Configuration**: Automatically configured in pyproject.toml
