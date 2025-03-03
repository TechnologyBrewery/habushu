@formatter
Feature: Python Formatting

  Scenario Outline: Habushu formats Python files if formatting is incorrect
    Given a "<packageManager>" Habushu configuration with formatting rules
    And a badly formatted .py file
    When habushu executes during the process-classes lifecycle hook
    Then the validation should automatically format the file

    Examples:
      | packageManager |
      | uv             |
      | poetry         |

  Scenario Outline: Formatter is downloaded and installed if not present -- regardless of dependency management or config file
    Given a "<packageManager>" Habushu configuration without a formatter installed
    And a badly formatted .py file
    When habushu executes during the process-classes lifecycle hook
    Then the build should automatically install the formatter

    Examples:
      | packageManager |
      | uv             |
      | poetry         |

  Scenario Outline: Formatter can be disabled from Maven configs
    Given a "<packageManager>" Habushu configuration with formatting rules
    And a badly formatted .py file
    And the formatter is disabled
    When habushu executes during the process-classes lifecycle hook
    Then the formatter should not run

    Examples:
      | packageManager |
      | uv             |
      | poetry         |

  Scenario Outline: Default formatter configs are generated if not found inside pyproject.toml
    Given a "<packageManager>" Habushu configuration without formatting rules
    When habushu executes during the process-classes lifecycle hook
    Then default formatting rules should be automatically added to the configuration if not already present

    Examples:
      | packageManager |
      | uv             |
      | poetry         |
