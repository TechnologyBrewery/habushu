@containerizeDependencies
Feature: Test staging source files of specified dependencies and transitive path-based dependencies

  Scenario: One Habushu-type dependency is specified
    Given a single dependency with packaging type habushu
      And updateDockerfile set false
    When the containerize-dependencies goal is executed
    Then the source files of the dependency and transitive Habushu-type dependencies are staged for containerization

  Scenario: Dockerfile is updated the first time with containerization logic
    Given a single dependency with packaging type habushu
     And a dockerfile to update
    When the containerize-dependencies goal is executed
    Then all the source files to build the dependency are staged in the build directory
     And the Dockerfile is updated to leverage a virtual environment for the dependency

  Scenario: Dockerfile is updated the second time with containerization logic
      Given a single dependency with packaging type habushu
       And a dockerfile already updated
      When the containerize-dependencies goal is executed
      Then all the source files to build the dependency are staged in the build directory
       And the Dockerfile is updated to leverage a virtual environment for the dependency

  Scenario: Dockerfile is automatically updated with containerization logic without any habushu builder/final stage tags
      Given a single dependency with packaging type habushu
       And a dockerfile without any habushu builder or final stage comment tag
      When the containerize-dependencies goal is executed
      Then all the source files to build the dependency are staged in the build directory
       And the Dockerfile is updated to leverage a virtual environment for the dependency