[[Return to Examples Documentation]](../../README.md)

# Test with pytest

This project demonstrates how to test with pytest

## Project Structure

```
habushu-uv-pytest/
├── pyproject.toml               # Python project configuration
├── pom.xml                      # Maven project configuration
├── src/
│   └── habushu-uv-pytest/       # Main package code
│       ├── __init__.py
│       └── calculator.py        # Example source code
│
└── tests/                       # Test directory
    ├── skip_test/
    │   └── test_skip_mark.py    # Test file can be in the tests/sub directory
    ├── conftest.py              # Fixture (optional)
    ├── test_calculator.py       # Test file can be in the tests/ directory
    └── test_env.py
```
**Note:** the test file must be named with test_*.py or *_test.py for pytest to detect as test files.

## Configure `pytest` as testPackage

### Configure the `pytest` dependency in `pyproject.toml` file
Preferably, set the `pytest` dependency to the `pyproject.toml` file. Habushu will first detect if there is a `pytest` configured in the `pyproject.toml` file for the test package.

```toml
[tool.uv]
dev-dependencies = [
    "pytest>=8.3.3",
]
```

### Alternatively, configure `testPackage` to `pytest` in the `habush-maven-plugin` plugin
If `pytest` already configured in the `pyproject.toml` file, this configuration can be ignored.

```xml
        <plugin>
            <groupId>org.technologybrewery.habushu</groupId>
            <artifactId>habushu-maven-plugin</artifactId>
            <configuration>
                <testPackage>pytest</testPackage>
            </configuration>
        </plugin>
```

For additional pytest configurations, see the [Pytest Configurations](../../../docs/CONFIGURATION_README.md#pytest-configurations) documentation.