@manual
Feature: Test that generated projects output test results in Cucumber reports format

  Scenario: Generate a cucumber report through Behave
    Given a Habushu configuration with no dependency management entries
    When the Habushu project is built
    Then a Cucumber report file "target/cucumber-reports/cucumber.json" exists
