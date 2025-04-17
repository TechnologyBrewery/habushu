[[Return to Examples Documentation]](../../README.md)

# A Simple Poetry Package

This project demonstrates how to create a reusable Python package using Habushu with Poetry as the package manager. It provides a template for structuring your Python library with proper packaging and testing
capabilities.

This example is used by [habushu-poetry-package-consumer](../habushu-poetry-package-consumer) as a dependency to highlight how Habushu helps support consistent monorepo
projects with Poetry.

## Features

- **Maven Integration**: Seamlessly integrates Python development with Maven build lifecycle
- **Poetry Package Management**: Uses Poetry for fast, reliable dependency management
- **Reusable Module Structure**: Demonstrates proper Python package organization
- **BDD Testing**: Includes Behave for behavior-driven development testing

## Project Structure

```
habushu-poetry-package/
├── pyproject.toml       # Python project configuration
├── pom.xml              # Maven project configuration
├── src/
│   ├── habushu_poetry_package/  # Main package code
│   │   ├── __init__.py
│   │   ├── helloworld.py    # Example module
│   │   ├── reusable_module/ # Subpackage example
│   │   │   └── worker.py
│   │   └── util/            # Utility functions
│   │       └── useful.py
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

### Integrating Habushu Into a Poetry Project

To add Habushu to your project, create a `pom.xml` file in the top level of the project. Then, follow the steps outline in [Integrating Your Poetry/uv Project with Habushu and Maven](../../../docs/USAGE_README.md#integrating-your-poetryuv-project-with-habushu-and-maven).

### Building the Package

To build the package using Maven:

```bash
mvn clean install
```

This will:
1. Create a Python virtual environment
2. Install dependencies using Poetry
3. Run tests
4. Package the library

### Using the Package in Another Project

After building, you can use this package in other projects by adding it as a dependency in your `pyproject.toml`:
   ```bash
   TODO
   poetry add path/to/habushu-poetry-package.whl
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
