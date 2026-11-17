@ui
Feature: Product navigation and contacts
  As a potential client
  I want to reach product pages and contact information from the menu
  So that I can learn about products and get in touch with the bank

  # Product pages do not always have an <h1> (e.g. "Carduri de debit"), so the stable signal is
  # the breadcrumb of the current page plus the document title. Titles are SEO texts
  # ("Credit de nevoi personale până la ..."), hence the case-insensitive comparison.
  @regression @UI-05
  Scenario Outline: Product page opens from the mega-menu
    Given the visitor opens the home page
    When the visitor opens "<link>" from the "<menu>" menu
    Then the URL path is "<link>"
    And the current breadcrumb is "<breadcrumb>"
    And the page title mentions "<breadcrumb>" ignoring case

    Examples:
      | menu     | link                      | breadcrumb       |
      | Credite  | /credits/nevoi-personale/ | Nevoi personale  |
      | Carduri  | /carduri-de-debit/        | Carduri de debit |
      | Depozite | /deposits/maestro/        | Maestro          |

  @regression @UI-05
  Scenario: Credit calculator page opens from the mega-menu
    Given the visitor opens the home page
    When the visitor opens "/calculator-de-credit/" from the "Credite" menu
    Then the URL path is "/calculator-de-credit/"
    And the credit calculator is displayed

  @regression @UI-14
  Scenario: Contact centre shows 24/7 phone numbers
    Given the visitor opens the home page
    When the visitor opens the contact centre menu
    Then the contact centre text contains "24/7"
    And the contact centre shows at least 1 phone link with "tel:" scheme

  @regression @UI-13
  Scenario: Bank network page switches between branch and ATM filters
    Given the visitor opens the bank network page
    Then branches are selected by default
    And the branch filters "Weekend" and "Situații excepționale" are visible
    And the city filter contains:
      | Chișinău |
      | Bălți    |
    When the visitor chooses ATMs
    Then the ATM filters are visible:
      | Cash-In |
      | EUR     |
      | USD     |
      | NFC     |
    And no branch filters are visible
