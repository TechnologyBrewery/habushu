@poetryCoreVersionMigration
Feature: Poetry-core version migration
  Baton will migrate the poetry-core version within the pyproject.toml file to match the required version needed by the Habushu.


  Scenario: Migrate poetry-core version when existing poetry-core version is less than 1.6.0
    Given an existing pyproject.toml file with poetry-core version less than 1.6.0 in the build-system group
    When Habushu poetry core migration executes
    Then the poetry-core version is updated to 1.6.0 in the build-system group

  Scenario: No migration will be performed when the poetry-core version is equal to 1.6.0
    Given an existing pyproject.toml file with poetry-core version equal to 1.6.0 in the build-system group
    When Habushu poetry core migration executes
    Then no update was performed for poetry-core version

  Scenario: No migration will be performed when the poetry-core version is greater than or equal to 1.6.0
    Given an existing pyproject.toml file with poetry-core version greater than or equal to 1.6.0 in the build-system group
    When Habushu poetry core migration executes
    Then no update was performed for poetry-core version

  Scenario: No migration will be performed when the poetry-core version is greater than or equal to 1.7.0
    Given an existing pyproject.toml file with poetry-core version greater than or equal to 1.7.0 in the build-system group
    When Habushu poetry core migration executes
    Then no update was performed for poetry-core version

  Scenario: No migration will be performed when the poetry-core version of 2.0.0
    Given an existing pyproject.toml file with poetry-core version greater than or equal to 2.0.0 in the build-system group
    When Habushu poetry core migration executes
    Then no update was performed for poetry-core version

  Scenario: Migrate poetry-core version when existing poetry-core version greater than or equal to 1.5
    Given an existing pyproject.toml file with poetry-core version greater than or equal to 1.5 in the build-system group
    When Habushu poetry core migration executes
    Then the poetry-core version is updated to 1.6.0 in the build-system group

  Scenario: Migrate poetry-core version when existing poetry-core version is 1.5.8
    Given an existing pyproject.toml file with poetry-core version equal to 1.5.8 in the build-system group
    When Habushu poetry core migration executes
    Then the poetry-core version is updated to 1.6.0 in the build-system group

  Scenario: Migrate poetry-core version when existing poetry-core version is greater than or equal to 1.5.8
    Given an existing pyproject.toml file with poetry-core version greater than or equal to 1.5.8 in the build-system group
    When Habushu poetry core migration executes
    Then the poetry-core version is updated to 1.6.0 in the build-system group

  Scenario: Migrate poetry-core version when existing poetry-core version is >=1.0.0 and <2.0.0
    Given an existing pyproject.toml file with poetry-core version greater than or eq to 1.0.0 and less than 2.0.0 in the build-system group
    When Habushu poetry core migration executes
    Then the poetry-core version is updated to 1.6.0 in the build-system group

  Scenario: Migrate poetry-core version when existing poetry-core version is >=1.6.0 and <2.0.0
    Given an existing pyproject.toml file with poetry-core version greater than or eq to 1.6.0 and less than 2.0.0 in the build-system group
    When Habushu poetry core migration executes
    Then no update was performed for poetry-core version

  Scenario: Migrate poetry-core version when existing poetry-core version is >=1.0.0 and <1.6.0
    Given an existing pyproject.toml file with poetry-core version greater than or equal to 1.0.0 and less than 1.6.0 in the build-system group
    When Habushu poetry core migration executes
    Then the poetry-core version is updated to 1.6.0 in the build-system group

  Scenario: Migrate poetry-core version when existing poetry-core version is greater than or equal to 1
    Given an existing pyproject.toml file with poetry-core version greater than or equal to 1 in the build-system group
    When Habushu poetry core migration executes
    Then the poetry-core version is updated to 1.6.0 in the build-system group

  Scenario: Migrate poetry-core version when existing poetry-core version is greater than or equal to 2 and less than 3
    Given an existing pyproject.toml file with poetry-core version greater than or equal to 2 and less than 3 in the build-system group
    When Habushu poetry core migration executes
    Then no update was performed for poetry-core version
