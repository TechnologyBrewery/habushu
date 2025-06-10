@pytestTest
Feature: testPackage configuration is used when there is no test package is installed and testPackage is set to pytest

  Scenario: Test package configuration is used
    Given a Habushu configuration with testPackage is set to pytest
    And no test package is installed
    When the pytest-test goal detects the test package to use
    Then pytest is used for test