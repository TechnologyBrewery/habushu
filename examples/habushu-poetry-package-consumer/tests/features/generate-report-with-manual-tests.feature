@manual
Feature: Given a Python module with at least one automated test and one or more manual tests, I should be able to view
  an HTML report of the Behave tests

  Scenario: Generate reports with manual tests present
    Given a Python module with at least one automated test and at least one manual test
    When I build the python module
    Then my build should succeed
    And I should be able to see a HTML report of the tests in the target directory