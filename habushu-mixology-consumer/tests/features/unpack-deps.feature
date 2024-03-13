Feature: Verify that dependencies get unpacked correctly for tests
    I want to be able to test python code that have dependencies unpacked by habushu

  Scenario: Files can reference dependency files and build successfully
    When I reference a dependency in my python code
    Then the build can successfully execute the tests

Scenario: Files can reference transitive dependencies and build successfully
    When I reference krausening transitively from habushu-mixology
    Then I can access it in habushu-mixology-consumer without further intervention
