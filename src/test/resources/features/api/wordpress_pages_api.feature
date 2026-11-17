@api
Feature: Public WordPress REST API of micb.md
  The site exposes GET /wp-json/wp/v2/pages without authentication (it is linked from every
  page header). Other routes are closed with 401. These scenarios document that contract.

  @smoke @API-01
  Scenario: Pages list is paginated and well-formed
    When a client requests pages with parameters:
      | per_page | 5                  |
      | _fields  | id,slug,link,title |
    Then the response status is 200
    And the response is JSON
    And the response contains 5 items
    And the pagination headers are consistent for page size 5
    And every page item has a positive id, a slug, a title and a link on the site

  @regression @API-02
  Scenario Outline: Filtering by slug returns exactly that page
    When a client requests pages with parameters:
      | slug    | <slug>             |
      | _fields | id,slug,link,title |
    Then the response status is 200
    And the response contains 1 items
    And the only page has slug "<slug>" and link path "<path>"

    Examples:
      | slug                 | path                  |
      | despre-banca         | /despre-banca/        |
      | calculator-de-credit | /calculator-de-credit/ |

  @regression @API-03
  Scenario: Language parameter returns the Russian translation of a page
    When a client requests pages with parameters:
      | slug    | despre-banca  |
      | lang    | ru            |
      | _fields | id,slug,link  |
    Then the response status is 200
    And the response contains 1 items
    And the only page has slug "despre-banca" and link path "/ru/despre-banca/"

  @regression @API-02
  Scenario: Unknown slug returns an empty list, not an error
    When a client requests pages with parameters:
      | slug | qa-page-that-does-not-exist-7f3a |
    Then the response status is 200
    And the response contains 0 items

  @regression @API-04
  Scenario: Page size above the limit is rejected with a validation error
    When a client requests pages with parameters:
      | per_page | 500 |
    Then the response status is 400
    And the error code is "rest_invalid_param"
    And the error details mention parameter "per_page"

  @regression @API-05
  Scenario: Unknown page id returns 404 with an error code
    When a client requests the page with id 999999999
    Then the response status is 404
    And the error code is "rest_post_invalid_id"

  @regression @API-06
  Scenario: Restricted REST routes require authorisation
    When a client sends GET "/wp-json/wp/v2/types"
    Then the response status is 401
    And the error code is "rest_cannot_access"

  @regression @API-09
  Scenario: Unknown page URL returns 404
    When a client sends GET "/qa-page-that-does-not-exist-7f3a/"
    Then the response status is 404
