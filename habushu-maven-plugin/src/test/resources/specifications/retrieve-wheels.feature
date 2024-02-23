Feature: Test wheel dependencies capabilities

  Scenario: Wheel artifacts are not copied if no wheel dependencies entries are specified
    Given a Habushu configuration with no wheel dependencies entries
    When Habushu executes retrieve wheel dependencies 
    Then no wheel artifacts are copied

  Scenario: Wheel dependencies are copied when specified
    Given a Habushu configuration with a wheel dependency
    When Habushu executes retrieve wheel dependencies
    Then the wheel artifact is copied