@hatch-sdist
Feature: Auto-add Hatch sdist exclude configuration for UV projects

  Scenario: Add hatch sdist exclude to UV project without existing hatch config
    Given a UV project without any hatch sdist configuration
    When the hatch sdist exclude migration executes
    Then the pyproject.toml should have target directory in hatch sdist exclude list
    And the exclude list should have 1 entries

  Scenario: Merge target directory into existing hatch sdist exclude list
    Given a UV project with hatch sdist exclude but without target directory
    When the hatch sdist exclude migration executes
    Then the pyproject.toml should have target directory in hatch sdist exclude list
    And the existing exclude entries should be preserved
    And the exclude list should have 3 entries

  Scenario: Merge target directory into existing multi-line hatch sdist exclude list
    Given a UV project with multi-line hatch sdist exclude without target directory
    When the hatch sdist exclude migration executes
    Then the pyproject.toml should have target directory in hatch sdist exclude list
    And the existing exclude entries should be preserved
    And the exclude list should have 3 entries

  Scenario: Skip migration when target directory already excluded
    Given a UV project with hatch sdist exclude already containing target directory
    When the hatch sdist exclude migration executes
    Then the migration should not execute

  Scenario: Skip migration for Poetry projects
    Given a Poetry project
    When the hatch sdist exclude migration executes
    Then the migration should not execute
