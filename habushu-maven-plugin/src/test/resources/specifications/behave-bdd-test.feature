@behaveBddTest
Feature: testPackage configuration is used when there is no test package is installed and testPackage is set to behave

  Scenario: Test package configuration is used
    Given a Habushu configuration with testPackage is set to behave
    And no test package is installed
    When the behave-bdd-test goal detects the test package to use
    Then behave is used for test
