@containerizeDependencies
Feature: Test staging source files of specified dependencies and transitive path-based dependencies

  Scenario Outline: One Habushu-type dependency is specified
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And updateDockerfile set false
    When the containerize-dependencies goal is executed
    Then all the source files to build the "<packageManager>"-based dependency are staged in the build directory

    Examples:
    | packageManager |
    | Poetry         |
    | uv             |

  Scenario Outline: Dockerfile is updated the first time with containerization logic
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile to update
    When the containerize-dependencies goal is executed
    Then all the source files to build the "<packageManager>"-based dependency are staged in the build directory
    And the Dockerfile is updated to leverage a virtual environment for the dependency

    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile is updated the second time with containerization logic
      Given a single "<packageManager>"-based dependency with packaging type Habushu
       And a dockerfile already updated
      When the containerize-dependencies goal is executed
      Then all the source files to build the "<packageManager>"-based dependency are staged in the build directory
       And the Dockerfile is updated to leverage a virtual environment for the dependency
    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile is automatically updated with containerization logic without any habushu builder/final stage tags
      Given a single "<packageManager>"-based dependency with packaging type Habushu
       And a dockerfile without any habushu builder or final stage comment tag
      When the containerize-dependencies goal is executed
      Then all the source files to build the "<packageManager>"-based dependency are staged in the build directory
       And the Dockerfile is updated to leverage a virtual environment for the dependency

    Examples:
      | packageManager |
      | Poetry         |
      | uv             |