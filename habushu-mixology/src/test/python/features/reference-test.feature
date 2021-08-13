Feature: Example of referencing a test resource in test
  I want to be able to test python code using test resources

  Scenario: Test files can reference test resources and build successfully
    When I reference a test resource in my test file
    Then the build can successfully execute the tests