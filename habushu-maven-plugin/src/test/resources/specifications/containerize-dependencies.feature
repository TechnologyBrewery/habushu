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
    Then the dependency wheel and requirements file are staged in the build directory

    Examples:
    | packageManager |
    | Poetry         |
    | uv             |

  Scenario Outline: Multiple wheels match the search pattern (e.g. during dev publishing)
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile to update
    And a second wheel for the dependency "extensions_python_dep_X-1.0.0.dev1757432284715-py3-none-any.whl" created later
    When the containerize-dependencies goal is executed
    Then the "extensions_python_dep_X-1.0.0.dev1757432284715-py3-none-any.whl" wheel is staged
    And the dockerfile installs the "extensions_python_dep_X-1.0.0.dev1757432284715-py3-none-any.whl" wheel
    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Multiple wheels match when using Poetry 2.3 dev0 timestamp format
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile to update
    And a second wheel for the dependency "extensions_python_dep_X-1.0.0.dev01757432284715-py3-none-any.whl" created later
    When the containerize-dependencies goal is executed
    Then the "extensions_python_dep_X-1.0.0.dev01757432284715-py3-none-any.whl" wheel is staged
    And the dockerfile installs the "extensions_python_dep_X-1.0.0.dev01757432284715-py3-none-any.whl" wheel
    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile is updated when it has injection point tag
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile to update
    When the containerize-dependencies goal is executed
    Then the dependency wheel and requirements file are staged in the build directory
    And the Dockerfile installs the requirements file and the dependency wheel
    And the Dockerfile is updated to leverage a virtual environment for the dependency
    And the original logic in the Dockerfile is preserved

    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile is updated when it has logic marker tag
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile already updated
    When the containerize-dependencies goal is executed
    Then the dependency wheel and requirements file are staged in the build directory
    And the Dockerfile installs the requirements file and the dependency wheel
    And the Dockerfile is updated to leverage a virtual environment for the dependency
    And the original logic in the Dockerfile is preserved
    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile is automatically updated with containerization logic without any habushu builder/final stage tags
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile without any habushu builder or final stage comment tag
    When the containerize-dependencies goal is executed
    Then the dependency wheel and requirements file are staged in the build directory
    And the Dockerfile installs the requirements file and the dependency wheel
    And the Dockerfile is updated to leverage a virtual environment for the dependency
    And the original logic in the Dockerfile is preserved

    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile uses custom repository URL
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile to update
    And the pypiRepoUrl is set to a custom repository
    When the containerize-dependencies goal is executed
    Then the dependency wheel and requirements file are staged in the build directory
    And the Dockerfile uses the custom index to install wheels
    And the original logic in the Dockerfile is preserved

    Examples:
      | packageManager |
      | Poetry         |
      | uv             |

  Scenario Outline: Dockerfile uses custom repository URL
    Given a single "<packageManager>"-based dependency with packaging type Habushu
    And a dockerfile to update
    And habushu is configured to use a dev repository
    And the dev repository url is set to a custom repository
    When the containerize-dependencies goal is executed
    Then the dependency wheel and requirements file are staged in the build directory
    And the Dockerfile adds the custom dev index during wheel installation
    And the original logic in the Dockerfile is preserved

    Examples:
      | packageManager |
      | Poetry         |
      | uv             |
