Feature: Example of referencing a script in src
  I want to be able to test python code in the src directory

  Scenario: Test files can reference src files and build successfully
    When I reference a src file in my test file
    Then the build can successfully execute the tests
