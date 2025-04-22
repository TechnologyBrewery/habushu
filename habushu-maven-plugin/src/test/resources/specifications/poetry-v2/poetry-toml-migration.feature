@poetry-v2-migration
Feature: Test automatic migrations of deprecated fields applied to poetry.toml file based on given Poetry version

  Scenario Outline: Poetry version is at least 2.0.0 and the poetry-toml migration removes deprecated virtualenvs and experimental fields from poetry.toml file
    Given the Poetry version is at least "2.0.0"
    And an existing poetry.toml file with deprecated configurations in [virtualenvs] and [experimental]
    When the Habushu poetry-toml migration executes
    Then <expectedVirtualEnvEntries> entries exist in the virtualenvs group
    Then <expectedExperimentalEntries> entries exist in the experimental group

    Examples:
      | expectedVirtualEnvEntries | expectedExperimentalEntries|
      | 2                         | 1                          |

  Scenario Outline: Poetry version is at least 2.0.0 and the poetry-toml migration removes deprecated virtualenvs fields from poetry.toml file
    Given the Poetry version is at least "2.0.0"
    And an existing poetry.toml file with deprecated configurations in [virtualenvs]
    When the Habushu poetry-toml migration executes
    Then <expectedVirtualEnvEntries> entries exist in the virtualenvs group

    Examples:
      | expectedVirtualEnvEntries |
      | 2                         |

  Scenario Outline: Poetry version is at least 2.0.0 and the poetry-toml migration removes one deprecated experimental field from poetry.toml file
    Given the Poetry version is at least "2.0.0"
    And an existing poetry.toml file with one deprecated configuration in [experimental] out of multiple configurations
    When the Habushu poetry-toml migration executes
    Then <expectedExperimentalEntries> entries exist in the experimental group

    Examples:
      | expectedExperimentalEntries |
      | 1                           |

  Scenario: Poetry version is at least 2.0.0 and the poetry-toml migration removes the only deprecated experimental field from poetry.toml file
    Given the Poetry version is at least "2.0.0"
    And an existing poetry.toml file with one deprecated configuration in [experimental]
    When the Habushu poetry-toml migration executes
    Then no entry exists in the experimental group

  Scenario: Poetry version is at least 2.0.0 but the poetry.toml file has no deprecated configs in [virtualenvs] so the poetry-toml migration does not run
    Given the Poetry version is at least "2.0.0"
    And an existing poetry.toml file with no deprecated configurations in [virtualenvs]
    When the Habushu poetry-toml migration executes
    Then the poetry-toml migration did not execute

  Scenario: Poetry version is at least 2.0.0 but the poetry.toml file has no deprecated configs in [experimental] so the poetry-toml migration does not run
    Given the Poetry version is at least "2.0.0"
    And an existing poetry.toml file with no deprecated configurations in [experimental]
    When the Habushu poetry-toml migration executes
    Then the poetry-toml migration did not execute

  Scenario: Poetry version is less than 2.0.0 and the poetry-toml migration does not execute
    Given the Poetry version is less than "2.0.0"
    And an existing poetry.toml file with deprecated configurations in [virtualenvs] and [experimental]
    When the Habushu poetry-toml migration executes
    Then the poetry-toml migration did not execute
