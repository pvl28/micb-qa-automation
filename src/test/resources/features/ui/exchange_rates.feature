@ui
Feature: Exchange rates and currency converter
  Rates change every day, so no scenario contains a rate value.
  Scenarios read rates from the page at run time and check relations and calculations.

  Background:
    Given the visitor opens the home page
    And the visitor scrolls to the exchange rates

  @smoke @UI-06
  Scenario: Cash exchange rates are complete and consistent
    Then the cash rates table lists currencies:
      | EUR |
      | USD |
      | GBP |
      | RON |
      | UAH |
      | CHF |
      | RUB |
    And every quoted currency has positive buy and sell rates with 2 decimals
    And every quoted currency has a buy rate lower than or equal to the sell rate
    And EUR and USD are quoted

  @regression @UI-07
  Scenario: Card exchange rates are shown for USD and EUR
    When the visitor selects the "card" rates tab
    Then the card rates table lists currencies:
      | USD |
      | EUR |
    And every quoted currency has a buy rate lower than or equal to the sell rate

  @smoke @UI-08
  Scenario Outline: Converter sells <amount> USD at the bank buy rate
    Given the converter sells "USD" for "MDL"
    When the visitor enters <amount> in the converter
    Then the converter result equals <amount> multiplied by the cash buy rate of "USD", rounded to 2 decimals

    Examples:
      | amount |
      | 100    |
      | 123.45 |
      | 7      |

  @regression @UI-09
  Scenario: Rates for a past date are loaded from the date picker
    When the visitor selects day 10 of the previous month in the rates date picker
    Then the rates date field shows the selected date
    And every quoted currency has a buy rate lower than or equal to the sell rate
    And EUR and USD are quoted
