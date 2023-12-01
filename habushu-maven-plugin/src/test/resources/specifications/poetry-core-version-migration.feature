Feature: Poetry-core version migration
  Baton will migrate the poetry-core version within the pyproject.toml file to match the required version needed by the Habushu.


Scenario: Migrate poetry-core version when existing poetry-core version is less than 1.6.0
Given an existing pyproject.toml file with poetry-core version less than 1.6.0 in the build-system group
When Habushu poetry core migration executes
Then the poetry-core version is updated to 1.6.0 in the build-system group

Scenario: No migration will be performed when the poetry-core version is equal to 1.6.0
Given an existing pyproject.toml file with poetry-core version of 1.6.0 in the build-system group
When Habushu poetry core migration executes
Then no update was performed for poetry-core version

Scenario: No migration will be performed when the poetry-core version of 1.7.0
Given an existing pyproject.toml file with poetry-core version of 1.7.0 in the build-system group
When Habushu poetry core migration executes
Then no update was performed for poetry-core version

Scenario: No migration will be performed when the poetry-core version of 2.0.0
Given an existing pyproject.toml file with poetry-core version of 2.0.0 in the build-system group
When Habushu poetry core migration executes
Then no update was performed for poetry-core version