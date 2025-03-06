@dependencyManagementPoetry
Feature: Test Poetry dependency management capabilities to help align package versions across pyproject.toml files

  Scenario: Poetry Dependency management is not enabled if no dependency management entries are specified
    Given a Habushu configuration with no poetry dependency management entries
    When Habushu executes with poetry
    Then the poetry pyproject.toml file has no updates

  Scenario: Poetry Dependency management makes no changes are made if managed dependencies are found but disabled
    Given a Habushu configuration with poetry dependency management entries
    And update managed dependencies when found is disabled for poetry
    When Habushu executes with poetry
    Then the poetry pyproject.toml file has no updates

  Scenario: The build stops if configured to fail when poetry dependency management changes are needed
    Given a Habushu configuration with poetry dependency management entries
    And fail on poetry managed dependency mismatches is enabled
    When Habushu executes with poetry
    Then the build process with poetry is halted

  Scenario Outline: [tool.poetry.dependencies] dependency is changed when managed and not matching current value
    Given a Habushu configuration with a poetry managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with poetry
    Then the poetry pyproject.toml file has updates

    Examples:
      | package      | operatorAndVersion |
      | krausening   | 100                |
      | krausening   | 15                 |
      | cryptography | ^40.0.0            |

  Scenario Outline: [tool.poetry.group.dev.dependencies] dependency is changed when managed and not matching current value
    Given a Habushu configuration with a poetry managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with poetry
    Then the poetry pyproject.toml file has updates

    Examples:
      | package | operatorAndVersion                                               |
      | uvicorn | ^0.18.0                                                          |
      | uvicorn | {version = \u0022^0.18.0\u0022, extras = [\u0022standard\u0022]} |
      | ruff    | ^0.9.8                                                           |
      | ruff    | >=0.9.7                                                           |
      | behave  | ^1.2.7                                                           |

  Scenario Outline: [tool.poetry.group.<group>] dependency is changed when managed and not matching current value
    Given a Habushu configuration with a poetry managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with poetry
    Then the poetry pyproject.toml file has updates

    Examples:
      | package        | operatorAndVersion |
      | packageFoo     | ^1.1.0             |
      | packageBar     | >=2.2.0            |
      | packageFooTest | ^1.2.7             |
      | packageBarTest | ^0.9.0             |

  Scenario Outline: Inactive managed dependencies are skipped
    Given a Habushu configuration with a poetry inactive managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with poetry
    Then the poetry pyproject.toml file has no updates

    Examples:
      | package    | operatorAndVersion |
      | krausening | 15                 |
      | ruff       | >=0.9.9            |
      | packageFoo | ^1.1.0             |

  Scenario Outline: SNAPSHOT poetry managed dependencies get corrected to dev dependencies by default (overridePackageVersion is true)
    Given a Habushu configuration with a poetry managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with poetry
    Then the poetry pyproject.toml file is updated to contain "<package>" and "<updatedOperatorAndVersion>"

    Examples:
      | package   | operatorAndVersion | updatedOperatorAndVersion |
      | package-a | 1.1.0-SNAPSHOT     | 1.1.0.*                   |
      | package-b | 2-SNAPSHOT         | 2.*                       |

  Scenario Outline: SNAPSHOT poetry managed dependencies do NOT get corrected to dev dependencies when overridePackageVersion is false
    Given a Habushu configuration with a poetry managed dependency of "<package>" and "<operatorAndVersion>"
    And poetry replace development version is disabled
    When Habushu executes with poetry
    Then the poetry pyproject.toml file is updated to contain "<package>" and "<operatorAndVersion>"

    Examples:
      | package   | operatorAndVersion |
      | package-a | 1.1.0-SNAPSHOT     |
      | package-b | 2-SNAPSHOT         |

  Scenario Outline: Skip altering local development versions when processing SNAPSHOT managed dependencies for poetry
    Given a Habushu configuration with a poetry managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with poetry
    Then the poetry pyproject.toml file has no updates

    Examples:
      | package                     | operatorAndVersion |
      | local-dev-package-example-a | 10-SNAPSHOT        |
      | local-dev-package-example-b | 2.14.5-SNAPSHOT    |