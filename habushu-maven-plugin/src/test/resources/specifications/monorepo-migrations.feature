Feature: Test automatic migration of monorepo dependencies into the dependencies pyproject.toml group

  Scenario: Remove monorepo dependencies when existing monorepo group DOES exists
    Given an existing pyproject.toml file with two monorepo dependencies each in the tool.poetry.dependencies and tool.poetry.group.monorepo.dependencies groups
    When Habushu migrations execute
    Then 0 pyproject.toml monorepo dependencies exist in the tool.poetry.group.monorepo.dependencies group
    And 4 pyproject.toml monorepo dependencies exist in the tool.poetry.dependencies group

  Scenario: Remove monorepo dependencies does not change a file with non applicable dependencies
    Given an existing pyproject.toml without any monorepo dependencies
    When Habushu migrations execute
    Then no migration was performed