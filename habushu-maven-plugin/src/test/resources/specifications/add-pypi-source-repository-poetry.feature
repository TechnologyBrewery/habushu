@addPypiSourceRepoPoetry
Feature: Test adding source PyPi repositories to pyproject.toml file for Poetry projects

  Scenario Outline: Add PyPi source repository with simple suffix enabled
    Given the PyPi simple suffix is enabled
    And a custom PyPi repository URL is set to "https://nexus.github.com/habushu/"
    And the simple suffix is set to "<pypiSimpleSuffix>"
    When the install phase executes
    Then the PyPi source URL added to the Poetry pyproject.toml is "<expectedPypiSourceUrl>"
    Examples:
    | pypiSimpleSuffix | expectedPypiSourceUrl                         |
    | simple           | https://nexus.github.com/habushu/simple/      |
    | simple-test      | https://nexus.github.com/habushu/simple-test/ |

  Scenario: Add PyPi source repository with simple suffix disabled
    Given the PyPi simple suffix is disabled
    And a custom PyPi repository URL is set to "https://nexus.github.com/habushu/"
    When the install phase executes
    Then the PyPi source URL added to the Poetry pyproject.toml is "https://nexus.github.com/habushu/"

  Scenario Outline: Add dev PyPi source repository with simple suffix enabled
    Given the PyPi simple suffix is enabled
    And a custom dev PyPi repository URL is set to "https://test.pypi.org/"
    And the simple suffix is set to "<pypiSimpleSuffix>"
    When the install phase executes
    Then the dev PyPi source URL added to the Poetry pyproject.toml is "<expectedPypiSourceUrl>"
    Examples:
      | pypiSimpleSuffix | expectedPypiSourceUrl              |
      | simple           | https://test.pypi.org/simple/      |
      | simple-test      | https://test.pypi.org/simple-test/ |

  Scenario: Add dev PyPi source repository with simple suffix disabled
    Given the PyPi simple suffix is disabled
    And a custom dev PyPi repository URL is set to "https://test.pypi.org/"
    When the install phase executes
    Then the dev PyPi source URL added to the Poetry pyproject.toml is "https://test.pypi.org/"

  Scenario: Supplemental priority line is added for Poetry projects
  Given a custom PyPi repository URL is set to "https://nexus.github.com/habushu/"
  When the install phase executes
  Then the PyPi source added to the Poetry pyproject.toml is noted as a "supplemental" priority
