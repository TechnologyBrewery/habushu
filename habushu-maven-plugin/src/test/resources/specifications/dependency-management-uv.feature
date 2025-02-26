@dependencyManagementUv
Feature: Test dependency management capabilities to help align package versions across UV pyproject.toml files

  Scenario: UV Dependency management is not enabled if no dependency management entries are specified
    Given a Habushu configuration with no UV dependency management entries
    When Habushu executes with uv
    Then the UV pyproject.toml file has no updates

  Scenario: Dependency management makes no changes are made if managed dependencies are found but disabled
    Given a Habushu configuration with UV dependency management entries
    And update UV managed dependencies when found is disabled
    When Habushu executes with uv
    Then the UV pyproject.toml file has no updates

  Scenario: The UV build stops if configured to fail when dependency management changes are needed
    Given a Habushu configuration with UV dependency management entries
    And fail on UV managed dependency mismatches is enabled
    When Habushu executes with uv
    Then the UV build process is halted

  Scenario Outline: dependencies is changed when managed and not matching current value
    Given a Habushu configuration with a UV managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with uv
    Then the UV pyproject.toml file has updates

    Examples:
      | package      | operatorAndVersion |
      | krausening   | 100                |
      | krausening   | 15                 |
      | cryptography | ^40.0.0            |

  Scenario Outline: dev dependency is changed when managed and not matching current value
    Given a Habushu configuration with a UV managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with uv
    Then the UV pyproject.toml file has updates

    Examples:
      | package | operatorAndVersion                                               |
      | uvicorn | ^0.18.0                                                          |
      | uvicorn | {version = \u0022^0.18.0\u0022, extras = [\u0022standard\u0022]} |
      | black   | ^23.3.0                                                          |
      | black   | >=17.0.0                                                         |
      | behave  | ^1.2.7                                                           |

  Scenario Outline: dependency is changed when managed and not matching current value
    Given a Habushu configuration with a UV managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with uv
    Then the UV pyproject.toml file has updates

    Examples:
      | package        | operatorAndVersion |
      | packageFoo     | ^1.1.0             |
      | packageBar     | >=2.2.0            |
      | packageFooTest | ^1.2.7             |
      | packageBarTest | ^0.9.0             |

  Scenario Outline: Inactive managed dependencies are skipped
    Given a Habushu configuration with a UV managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with uv
    Then the UV pyproject.toml file has updates

    Examples:
      | package    | operatorAndVersion |
      | krausening | 15                 |
      | black      | ^23.3.0            |
      | packageFoo | ^1.1.0             |

  Scenario Outline: SNAPSHOT managed dependencies get corrected to dev dependencies by default (overridePackageVersion is true)
    Given a Habushu configuration with a UV managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with uv
    Then the UV pyproject.toml file is updated to contain "<package>" and "<updatedOperatorAndVersion>"

    Examples:
      | package   | operatorAndVersion | updatedOperatorAndVersion |
      | package-a | 1.1.0-SNAPSHOT     | 1.1.0.*                   |
      | package-b | 2-SNAPSHOT         | 2.*                       |

  Scenario Outline: SNAPSHOT managed dependencies do NOT get corrected to dev dependencies when overridePackageVersion is false
    Given a Habushu configuration with a UV managed dependency of "<package>" and "<operatorAndVersion>"
    And replace development version is disabled
    When Habushu executes with uv
    Then the UV pyproject.toml file is updated to contain "<package>" and "<operatorAndVersion>"

    Examples:
      | package   | operatorAndVersion |
      | package-a | 1.1.0-SNAPSHOT     |
      | package-b | 2-SNAPSHOT         |

  Scenario Outline: Skip altering local development versions when processing SNAPSHOT managed dependencies
    Given a Habushu configuration with a UV managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes with uv
    Then the UV pyproject.toml file has no updates

    Examples:
      | package                     | operatorAndVersion |
      | local-dev-package-example-a | 10-SNAPSHOT        |
      | local-dev-package-example-b | 2.14.5-SNAPSHOT    |