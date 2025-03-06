Feature: Example of referencing a script in src
  I want to be able to test a python script was ran successfully

  Scenario: Python script generating files can be referenced
    When I reference a generated src file in my test file
    Then the build can successfully resolve the imports
