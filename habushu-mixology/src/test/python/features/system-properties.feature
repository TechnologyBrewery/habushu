Feature: System Properties are set during habushu testing
  I want to be able to set a system property within the virtual environment during testing

  Scenario: System property from pom is active when behave tests are run 
  Given a system property is set in the pom
  When the behave tests are run
  Then the system property is available in the virtual environment

  Scenario: System property from profile is active when behave tests are run
  Given a system property is set in a profile
  When the behave tests are run using the profile
  Then the system property from the profile is available in the virtual environment
