@ui
Feature: Credit calculator
  The calculator is informational: interest is calculated by actual days in each period,
  so the monthly payment is not a textbook annuity. Instead of copying the bank's formula,
  scenarios check business invariants and the internal consistency of the repayment schedule.

  Background:
    Given the visitor opens the home page
    And the visitor scrolls to the credit calculator

  @regression @UI-10
  Scenario: Monthly payment grows with the amount and falls with a longer term
    When the visitor sets the credit amount to 50000 MDL and the term to 24 months
    And the visitor remembers the monthly payment as "base"
    And the visitor sets the credit amount to 100000 MDL and the term to 24 months
    Then the monthly payment is greater than "base"
    When the visitor sets the credit amount to 50000 MDL and the term to 48 months
    Then the monthly payment is less than "base"

  @regression @UI-11
  Scenario: Amount above the allowed maximum is limited to the maximum
    When the visitor sets the credit amount to 99999999 MDL and the term to 24 months
    Then the credit amount equals the maximum shown under the slider

  @regression @UI-10
  Scenario: Calculator results are consistent
    When the visitor sets the credit amount to 60000 MDL and the term to 36 months
    Then the total cost of the credit is greater than the credit amount
    And the effective annual rate is greater than or equal to the interest rate

  @smoke @UI-12
  Scenario Outline: Repayment schedule is internally consistent (<amount> MDL, <term> months)
    When the visitor sets the credit amount to <amount> MDL and the term to <term> months
    And the visitor opens the repayment schedule
    Then the schedule has <term> rows
    And the principal parts add up to <amount> MDL
    And in every row interest + principal + fee equals the payment
    And every next opening balance equals the previous balance minus principal
    And the schedule payments add up to the total cost
    And the schedule monthly payment matches the calculator

    Examples:
      | amount | term |
      | 100000 | 32   |
      | 25000  | 12   |
