@pypiRetries
Feature: Allow push calls to PyPI to retry on failure

  Scenario: Retry a default number of times before failing
    Given Habushu is using its default configuration
    When 4 failures are encountered
    Then Habushu fails to perform the push using Poetry

  Scenario: Retry a default number of times before failing
    Given Habushu is using its default configuration
    When 4 failures are encountered
    Then Habushu fails to perform the push using UV


  Scenario Outline: Retry a configurable number of times before failing
    Given Habushu is configured to retry <retryCount>
    When <failuresEncountered> failures are encountered
    Then Habushu fails to perform the push using Poetry

    Examples:
      | retryCount | failuresEncountered |
      | 0          | 1                   |
      | 1          | 1                   |
      | 4          | 4                   |
      | 10         | 10                  |

  Scenario Outline: Retry a configurable number of times before failing
    Given Habushu is configured to retry <retryCount>
    When <failuresEncountered> failures are encountered
    Then Habushu fails to perform the push using UV

    Examples:
      | retryCount | failuresEncountered |
      | 0          | 1                   |
      | 1          | 1                   |
      | 4          | 4                   |
      | 10         | 10                  |

  Scenario Outline: Retry a configurable number of times before succeeding on before last try
    Given Habushu is configured to retry <retryCount>
    When <failuresEncountered> failures are encountered
    Then Habushu successfully performs the push using Poetry

    Examples:
      | retryCount | failuresEncountered |
      | 0          | 0                   |
      | 1          | 0                   |
      | 2          | 1                   |
      | 4          | 3                   |
      | 10         | 8                   |

  Scenario Outline: Retry a configurable number of times before succeeding on before last try
    Given Habushu is configured to retry <retryCount>
    When <failuresEncountered> failures are encountered
    Then Habushu successfully performs the push using UV

    Examples:
      | retryCount | failuresEncountered |
      | 0          | 0                   |
      | 1          | 0                   |
      | 2          | 1                   |
      | 4          | 3                   |
      | 10         | 8                   |