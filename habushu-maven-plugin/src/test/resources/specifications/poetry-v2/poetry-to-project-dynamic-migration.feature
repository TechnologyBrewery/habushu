@poetryToProjectDynamicMigration
Feature: Test automatic addition of dynamic field to [project] section of pyproject.toml file for projects using Poetry version of at least 2.0.0

  Scenario: Poetry version is at least 2.0.0 and [project] section does not have dynamic entry and [tool.poetry] does not have a readme so only dynamic = ["version", "dependencies"] is injected
    Given Poetry version at least "2.0.0"
    And an existing pyproject.toml file with no dynamic in project and no readme in tool.poetry
    When the Habushu poetry-to-project-dynamic migration executes
    Then the dynamic entry is added to the project group and contains
      | version      |
      | dependencies |

  Scenario: Poetry version is at least 2.0.0 and [project] section does not have dynamic entry and [tool.poetry] has a single readme so dynamic = ["version", "dependencies"] is injected and readme is migrated from [tool.poetry] to [project]
    Given Poetry version at least "2.0.0"
    And an existing pyproject.toml file with no dynamic in project and with a single readme in tool.poetry
    When the Habushu poetry-to-project-dynamic migration executes
    Then the dynamic entry is added to the project group and contains
      | version      |
      | dependencies |
    And the readme entry is added to the project group as "README.md"
    And the readme entry is removed from the tool.poetry group

  Scenario: Poetry version is at least 2.0.0 and [project] section does not have dynamic entry and [tool.poetry] has multiple readmes so dynamic = ["version", "dependencies", "readme"] is injected and the readmes remains in tool.poetry
    Given Poetry version at least "2.0.0"
    And an existing pyproject.toml file with no dynamic in project and with multiple readmes in tool.poetry
    When the Habushu poetry-to-project-dynamic migration executes
    Then the dynamic entry is added to the project group and contains
      | version       |
      | dependencies  |
      | readme        |
    And the readme entry remains in the tool.poetry group and contains
      | README.md                |
      | src/example_1/README.md  |

  Scenario: Poetry version is at least 2.0.0 and [project] section has a dynamic entry but it is missing one of the required fields so the missing field is appended to the existing dynamic list. For example dynamic = ["version"] becomes dynamic = ["version", "dependencies"].
    Given Poetry version at least "2.0.0"
    And an existing pyproject.toml file with a dynamic entry but missing required field in project
      |version       |
    When the Habushu poetry-to-project-dynamic migration executes
    Then the missing field is appended to the dynamic entry and now contains
      | version      |
      | dependencies |

  Scenario: Poetry version is less than 2.0.0 and the poetry-to-project-dynamic migration does not execute
    Given Poetry version less than "2.0.0"
    And an existing pyproject.toml file with no dynamic in project
    When the Habushu poetry-to-project migration executes
    Then the poetry to project dynamic migration did not execute
