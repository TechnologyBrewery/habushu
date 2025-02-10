@poetryToProjectMigration
Feature: Test automatic migrations of required fields from [tool.project] to [project] applied to pyproject.toml file based on given Poetry version

  Scenario Outline: Poetry version is at least 2.0.0 and the poetry-to-project migration moves fields from [tool.poetry] to [project]
    Given the Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with entries in the tool.poetry group
    When the Habushu poetry-to-project migration executes
    Then <expectedToolPoetryEntries> entries exist in the tool.poetry group
    And <expectedProjectEntries> entries exist in the project group

    Examples:
      | expectedToolPoetryEntries | expectedProjectEntries |
      | 3                         | 6                      |

  Scenario Outline: Poetry version is at least 2.0.0 and there are overlapping fields between [tool.poetry] and [project]. Then, the poetry-to-project migration only migrates fields from [tool.poetry] that do not already exist in [project]
    Given the Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with overlapping entries between the tool.poetry and project groups
    When the Habushu poetry-to-project migration executes
    Then <expectedToolPoetryEntries> entries exist in the tool.poetry group
    And <expectedProjectEntries> entries exist in the project group

    Examples:
      | expectedToolPoetryEntries | expectedProjectEntries |
      | 3                         | 6                      |

  Scenario: Poetry version is less than 2.0.0 and the poetry-to-project migration does not execute
    Given the Poetry version is less than "2.0.0"
    And an existing pyproject.toml file with no project group
    When the Habushu poetry-to-project migration executes
    Then the poetry to project migration did not execute
