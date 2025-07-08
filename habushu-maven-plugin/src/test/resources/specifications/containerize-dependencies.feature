@containerizeDependencies
Feature: Test containerizing Python applications with monorepo dependencies

  The containerization feature of Habushu helps project package their Python applications as Docker containers.  It
  should stage all the necessary files and, optionally, update a Dockerfile to read the files and create a virtual
  environment that can execute the given Python application.  The containerization could be within the same Habushu
  module as the project being packaged or a separate, non-Habushu module.

  Scenario Outline: One Habushu-type dependency is specified
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And updateDockerfile set false
    When the containerize-dependencies goal is executed
    Then the wheels of the dependency and transitive monorepo dependencies are staged in the build directory

    Examples:
    | packageManager |
    | Poetry         |
    | uv             |

  Scenario Outline: Dockerfile is updated when it has injection point tag
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile to update
    When the containerize-dependencies goal is executed
    Then the wheels of the dependency and transitive monorepo dependencies are staged in the build directory
    And the Dockerfile is updated to leverage a virtual environment for the dependency

    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile is updated when it has logic marker tag
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile already updated
    When the containerize-dependencies goal is executed
    Then the wheels of the dependency and transitive monorepo dependencies are staged in the build directory
    And the Dockerfile is updated to leverage a virtual environment for the dependency
    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile is automatically updated with containerization logic without any habushu builder/final stage tags
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile without any habushu builder or final stage comment tag
    When the containerize-dependencies goal is executed
    Then the wheels of the dependency and transitive monorepo dependencies are staged in the build directory
    And the Dockerfile is updated to leverage a virtual environment for the dependency

    Examples:
      | packageManager |
      | Poetry         |
      | uv             |