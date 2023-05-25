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
      | uvicorn | ^0.18.0                                      |
      | uvicorn | {version = \u0022^0.18.0\u0022, extras = [\u0022standard\u0022]} |
      | black   | ^23.3.0                                      |
      | black   | >=17.0.0                                     |
      | behave  | ^1.2.7                                       |

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
    Given a Habushu configuration with an inactive managed dependency of "<package>" and "<operatorAndVersion>"
    When Habushu executes
    Then the pyproject.toml file has no updates

    Examples:
      | package    | operatorAndVersion |
      | krausening | 15                 |
      | black      | ^23.3.0            |
      | packageFoo | ^1.1.0             |