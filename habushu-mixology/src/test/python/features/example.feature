Feature: Example of a Gherkin feature file
  I want to specify requires in an inherently testable format

  @executedTest
  Scenario: System should double the number of units
    Given a precondition specifying 50 items
    When some action doubles the number of items
    Then a postcondition checks that 100 items now exist

  @executedScenarioOutlineTest
  Scenario Outline: System should double the number of units as an outline
    Given a precondition specifying <items> items
    When some action doubles the number of items
    Then a postcondition checks that <expectedItems> items now exist   

    Examples:
      | items | expectedItems |
      | 1     | 2             |
      | 25    | 50            |
      | 1000  | 2000          | 
      | 14    | 28            |
      | 5     | 10            |

  @manual
  Scenario: FIPS-compliant SSL required (this is not something we can have a test for, so it's manual)
    Given any SSL leveraged within the System
    When a cryptography library is added to the System
    Then it must be pre-certified as FIPS 140.2 compliant