@api
Feature: Exchange rates endpoint used by the home page date picker
  POST /wp-admin/admin-ajax.php with action=load_exchange returns the rates table
  (server-rendered HTML) for a date. The site itself calls it when a visitor picks a date.
  Dates are relative to today, rates are never hard-coded.

  @smoke @API-07
  Scenario: Cash rates for a past date are returned and consistent
    When a client requests exchange rates for the date 30 days ago and tab "cash"
    Then the response status is 200
    And the response is HTML
    And the active tab in the response is "cash"
    And the "cash" rates contain currencies "EUR, USD, GBP, RON, UAH, CHF, RUB"
    And EUR and USD have buy and sell rates
    And every quoted buy rate is positive and not greater than the sell rate
    And every BNM rate is positive

  @regression @API-07
  Scenario: Card rates tab is returned as active when requested
    When a client requests exchange rates for the date 30 days ago and tab "card"
    Then the response status is 200
    And the active tab in the response is "card"
    And the "card" rates contain currencies "USD, EUR"
    And every quoted buy rate is positive and not greater than the sell rate

  @regression @API-08
  Scenario: Historical rates are stable between two requests
    When a client requests exchange rates for the date 45 days ago and tab "cash"
    And the client requests the same date again
    Then both responses contain identical rates
