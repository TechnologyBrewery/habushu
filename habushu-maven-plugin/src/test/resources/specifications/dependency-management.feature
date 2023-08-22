Feature: Test dependency management capabilities to help align package versions across pyproject.toml files

  Scenario: Dependency management is not enabled if no dependency management entries are specified
    Given a Habushu configuration with no dependency management entries
    When Habushu executes
    Then the pyproject.toml file has no updates

  Scenario: Dependency management makes no changes are made if managed dependencies are found but disabled
    Given a Habushu configuration with dependency management entries
    And update managed dependencies when found is disabled
    When Habushu executes
    Then the pyproject.toml file has no updates

  Scenario: The build stops if configured to fail when dependency management changes are needed
    Given a Habushu configuration with dependency management entries
    And fail on managed dependency mismatches is enabled
    When Habushu executes
    Then the build process is halted

  Scenario Outline: [tool.poetry.dependencies] dependency is changed when managed and not matching current value
    Given a Habushu configuration with a managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes
    Then the pyproject.toml file has updates

    Examples:
      | package      | operatorAndVersion |
      | krausening   | 100                |
      | krausening   | 15                 |
      | cryptography | ^40.0.0            |

  Scenario Outline: [tool.poetry.dev-dependencies] dependency is changed when managed and not matching current value
    Given a Habushu configuration with a managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes
    Then the pyproject.toml file has updates

    Examples:
      | package | operatorAndVersion                                               |
      | uvicorn | ^0.18.0                                                          |
      | uvicorn | {version = \u0022^0.18.0\u0022, extras = [\u0022standard\u0022]} |
      | black   | ^23.3.0                                                          |
      | black   | >=17.0.0                                                         |
      | behave  | ^1.2.7                                                           |

  Scenario Outline: [tool.poetry.group.<group>] dependency is changed when managed and not matching current value
    Given a Habushu configuration with a managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes
    Then the pyproject.toml file has updates

    Examples:
      | package        | operatorAndVersion |
      | packageFoo     | ^1.1.0             |
      | packageBar     | >=2.2.0            |
      | packageFooTest | ^1.2.7             |
      | packageBarTest | ^0.9.0             |

  Scenario: Inactive managed dependencies are skipped
    Given a Habushu configuration with a managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes
    Then the pyproject.toml file has updates

    Examples:
      | package    | operatorAndVersion |
      | krausening | 15                 |
      | black      | ^23.3.0            |
      | packageFoo | ^1.1.0             |

  Scenario: SNAPSHOT managed dependencies get corrected to dev dependencies by default with Poetry version 1.5.0 + (overridePackageVersion is true)
    Given a Habushu configuration with a managed dependency of "<package>" and "<operatorAndVersion>" and "<poetryVersion>"
    When Habushu executes
    Then the pyproject.toml file is updated to contain "<package>" and "<updatedOperatorAndVersion>"

    Examples:
      | package   | operatorAndVersion | updatedOperatorAndVersion | poetryVersion |
      | package-a | 1.1.0-SNAPSHOT     | 1.1.0.*                   | 1.5.0         |
      | package-b | 2-SNAPSHOT         | 2.*                       | 1.6.0         |

  Scenario: SHIM - SNAPSHOT managed dependencies get corrected to ^ dev dependencies with any Poetry version and a ^ in the version (overridePackageVersion is true)
    Given a Habushu configuration with a managed dependency of "<package>" and "<operatorAndVersion>" and "<poetryVersion>"
    When Habushu executes
    Then the pyproject.toml file is updated to contain "<package>" and "<updatedOperatorAndVersion>"

    Examples:
      | package   | operatorAndVersion | updatedOperatorAndVersion | poetryVersion |
      | package-a | ^1.1.0-SNAPSHOT    | ^1.1.0.dev                | 1.5.0         |
      | package-b | ^2-SNAPSHOT        | ^2.dev                    | 1.6.0         |
      | package-a | ^3.3.0-SNAPSHOT    | ^3.3.0.dev                | 1.2.2         |
      | package-b | ^4-SNAPSHOT        | ^4.dev                    | 1.3.0         |

  Scenario: SNAPSHOT managed dependencies do NOT get corrected to dev dependencies when overridePackageVersion is false
    Given a Habushu configuration with a managed dependency of "<package>" and "<operatorAndVersion>"
    And replace development version is disabled
    When Habushu executes
    Then the pyproject.toml file is updated to contain "<package>" and "<operatorAndVersion>"

    Examples:
      | package   | operatorAndVersion |
      | package-a | 1.1.0-SNAPSHOT     |
      | package-b | 2-SNAPSHOT         |

  Scenario: Skip altering local development versions when processing SNAPSHOT managed dependencies
    Given a Habushu configuration with a managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes
    Then the pyproject.toml file has no updates

    Examples:
      | package                     | operatorAndVersion |
      | local-dev-package-example-a | 10-SNAPSHOT        |
      | local-dev-package-example-b | 2.14.5-SNAPSHOT    |