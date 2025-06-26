@poetryCommandHelper
Feature: Test creating Poetry Helper commands based on the given Poetry version

  Scenario Outline: Check if Poetry version is at least 2.0.0
    Given the Poetry version is "<poetryVersion>"
    When the version is checked against "2.0.0"
    Then the result should be <expectedResult>

    Examples:
      | poetryVersion | expectedResult |
      | 1.6.1         | false          |
      | 2.0.0         | true           |
      | 2.0.1         | true           |

  Scenario Outline: Create lock command with skipLockUpdate
    Given the Poetry version is "<poetryVersion>"
    And skipLockUpdate is <skipLockUpdate>
    When the lock command is created
    Then the returned arguments should be:
      | <expectedArg1> |
      | <expectedArg2> |

    Examples:
      | poetryVersion | skipLockUpdate | expectedArg1 | expectedArg2       |
      | 2.0.0         | true                 | lock         |                    |
      | 2.0.0         | false                | lock         | --regenerate       |
      | 1.6.1         | true                 | lock         | --no-update        |
      | 1.6.1         | false                | lock         |                    |

  Scenario Outline: Create install command with forceSync
    Given the Poetry version is "<poetryVersion>"
    And forceSync is <forceSync>
    When the install command is created
    Then the returned arguments should be:
      | <expectedArg1> |
      | <expectedArg2> |

    Examples:
      | poetryVersion | forceSync | expectedArg1 | expectedArg2 |
      | 2.0.0         | true      | sync         |              |
      | 2.0.0         | false     | install      |              |
      | 1.6.1         | true      | install      | --sync       |
      | 1.6.1         | false     | install      |              |

  Scenario Outline: Create use pyenv command
    Given the Poetry version is "<poetryVersion>"
    When the use pyenv command is created
    Then the returned arguments should be:
      | config         |
      | --local        |
      | <expectedArg1> |
      | <expectedArg2> |

    Examples:
      | poetryVersion | expectedArg1                     | expectedArg2 |
      | 2.0.0         | virtualenvs.use-poetry-python    | false        |
      | 1.6.1         | virtualenvs.prefer-active-python | true         |
