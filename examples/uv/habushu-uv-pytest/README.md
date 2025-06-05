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

## Configure `testPackage` to `pytest` in the `habush-maven-plugin` plugin

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