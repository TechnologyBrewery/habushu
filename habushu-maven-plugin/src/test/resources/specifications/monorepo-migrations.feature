Feature: Test automatic migration of monorepo dependencies into their own pyproject.toml group

  Scenario: Migrate monorepo dependencies when existing monorepo group DOES NOT exists
    Given an existing pyproject.toml file with two monorepo dependencies in the tool.poetry.dependencies group
    When Habushu migrations execute
    Then 2 pyproject.toml monorepo dependencies exist in the tool.poetry.group.monorepo.dependencies group
    And 0 pyproject.toml monorepo dependencies exist in the tool.poetry.dependencies group

  Scenario: Migrate monorepo dependencies when existing monorepo group DOES exists
    Given an existing pyproject.toml file with two monorepo dependencies each in the tool.poetry.dependencies and tool.poetry.group.monorepo.dependencies groups
    When Habushu migrations execute
    Then 4 pyproject.toml monorepo dependencies exist in the tool.poetry.group.monorepo.dependencies group
    And 0 pyproject.toml monorepo dependencies exist in the tool.poetry.dependencies group

  Scenario: Migrate monorepo dependencies does not change a file with non applicable dependencies
    Given an existing pyproject.toml without any monorepo dependencies
    When Habushu migrations execute
    Then no migration was performed

  Scenario: Migrate monorepo dependencies does not change a file with all monorepo dependencies already in monorepo group
    Given an existing pyproject.toml file with three monorepo dependencies already in the tool.poetry.group.monorepo.dependencies group
    When Habushu migrations execute
    Then no migration was performed