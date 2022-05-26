Feature: Verify that dependencies get unpacked correctly for tests
    I want to be able to test python code that have dependencies unpacked by habushu

  Scenario: Files can reference dependency files and build successfully
    When I reference a dependency in my python code
    Then the build can successfully execute the tests


