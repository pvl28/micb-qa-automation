@ui
Feature: Home page, cookie consent and language switching
  As a visitor of micb.md
  I want the home page to open in my language
  So that I can find bank products and information

  @smoke @UI-01
  Scenario: Home page opens with logo and main menu
    Given the visitor opens the home page
    Then the page title contains "MICB"
    And the bank logo is visible
    And the main menu contains links to:
      | /persoane-juridice/ |
      | /despre-banca/      |
      | /cariera/           |
      | /reteaua-bancii/    |

  @smoke @UI-02
  Scenario: Declined cookie consent is remembered after reload
    Given the visitor opens the home page without handling cookies
    When the visitor declines optional cookies
    And the visitor reloads the page
    Then the cookie banner is not shown again

  @smoke @UI-03
  Scenario: Visitor switches the home page language RO -> RU -> EN
    Given the visitor opens the home page
    Then the page language is "ro-RO"
    When the visitor switches the language to "ru"
    Then the URL path starts with "/ru/"
    And the page language is "ru-RU"
    And the active language is "ru"
    When the visitor switches the language to "en"
    Then the URL path starts with "/en/"
    And the page language is "en-US"
    And the active language is "en"

  @regression @UI-04
  Scenario Outline: Language switch keeps the visitor on the same page
    # WPML may translate slugs: the EN page of "Despre bancă" has its own address.
    Given the visitor opens the page "<page>"
    When the visitor switches the language to "<lang>"
    Then the URL path is "<translated>"
    And the page language is "<htmlLang>"
    And the page has a heading

    Examples:
      | page           | lang | translated                  | htmlLang |
      | /despre-banca/ | ru   | /ru/despre-banca/           | ru-RU    |
      | /despre-banca/ | en   | /en/about-moldinconbank/    | en-US    |
