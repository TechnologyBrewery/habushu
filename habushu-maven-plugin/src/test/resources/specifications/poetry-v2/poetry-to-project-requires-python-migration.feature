@poetryToProjectRequiresPythonMigration
Feature: Test automatic migrations of python dependency from [tool.project] to [project] applied to pyproject.toml file based on given Poetry version

  Scenario: Poetry version is at least 2.0.0 and the poetry-to-project-requires-python migration adds a new requires-python entry to [project] group and removes the python dependency from [tool.poetry.dependencies] group
    Given Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with no requires-python entry in the project group
    When the Habushu poetry-to-project-requires-python migration executes
    Then the requires-python entry is added to the project group and set to "^3.11"
    And the python entry no longer exists in the tool.poetry.dependencies group

  Scenario: Poetry version is at least 2.0.0 and project group already has a requires-python entry so the migration does not execute
    Given Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with a requires-python entry in the project group
    When the Habushu poetry-to-project-requires-python migration executes
    Then the poetry to project requires python migration did not execute

  Scenario: Poetry version is less than 2.0.0 so the migration does not execute
    Given Poetry version is less than "2.0.0"
    And an existing pyproject.toml file with no requires-python
    When the Habushu poetry-to-project-requires-python migration executes
    Then the poetry to project requires python migration did not execute