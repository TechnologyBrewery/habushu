Feature: Environment variables are configured during behave execution through python-dotenv
  I want to be able use python-dotenv to set an environment variable within the virtual environment during testing

  Scenario: Default .env configuration is active when behave tests are run
  When the Krausening-managed configuration is loaded
  Then the default Krausening profile is loaded

  @integration_test
  Scenario: .env configuration specified by Maven profile is active when behave tests are run
  When the Krausening-managed configuration is loaded
  Then the integration-test Krausening profile is loaded
