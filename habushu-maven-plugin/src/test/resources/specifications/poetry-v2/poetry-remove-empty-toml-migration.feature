@poetry-v2-migration
Feature: Test automatic migrations to remove empty [tool.poetry] and [tool.poetry.dependencies] from pyproject.toml file based on given Poetry version

  Scenario: Poetry version is at least 2.0.0 and the poetry-remove-empty-toml migration removes empty [tool.poetry] header
    Given the Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with no direct entries under the tool.poetry header
    When the Habushu poetry-remove-empty-toml migration executes
    Then the tool.poetry header is removed

  Scenario: Poetry version is at least 2.0.0 and the poetry-remove-empty-toml migration removes empty [tool.poetry.dependencies] header
    Given the Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with no direct entries under the tool.poetry.dependencies header
    When the Habushu poetry-remove-empty-toml migration executes
    Then the tool.poetry.dependencies header is removed

  Scenario: Poetry version is at least 2.0.0 and the poetry-remove-empty-toml migration removes empty [tool.poetry] and [tool.poetry.dependencies] headers
    Given the Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with no direct entries under the tool.poetry and tool.poetry.dependencies headers
    When the Habushu poetry-remove-empty-toml migration executes
    Then the tool.poetry and tool.poetry.dependencies headers are removed

  Scenario: Poetry version is at least 2.0.0 and the poetry-remove-empty-toml migration does not remove [tool.poetry] with inline dependencies table
    Given the Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with an inline dependencies table under the tool.poetry header
    When the Habushu poetry-remove-empty-toml migration executes
    Then the tool.poetry header is not removed

  Scenario: Poetry version is less than 2.0.0 and the poetry-remove-empty-toml migration does not execute
    Given the Poetry version is at least "2.0.0"
    And an existing pyproject.toml file with no direct entries under the tool.poetry header
    When the Habushu poetry-remove-empty-toml migration executes
    Then the poetry-remove-empty-toml migration did not execute

