# Test Plan — micb.md public website

Independent educational project. Not affiliated with or endorsed by BC "Moldindconbank" S.A.

## 1. Scope

**In scope** (public, anonymous part of https://micb.md/):

| Area | What exists on the site (observed 2026-11-17) |
|---|---|
| Navigation | Top menu (`nav.nav-section`), mega-menu `.subnav-box` with dropdowns: Credite, Carduri, Depozite, Premium, Plăți și transferuri, Alte servicii, Tarife și condiții |
| Languages | `.lang-switcher` with `ro` / `ru` / `en`; WPML URLs `/`, `/ru/…`, `/en/…`; `<html lang>` = `ro-RO` / `ru-RU` / `en-US` |
| Product pages | e.g. `/credits/nevoi-personale/`, `/deposits/maestro/`, `/carduri-de-debit/` |
| Contacts | "Contact centru" dropdown (24/7 phone links), "Rețeaua băncii" page (Google Map + filters) |
| Exchange rates | Home page section `.exchange-section`: cash/card tabs, date picker, BNM rate column |
| Currency converter | Same section: "Vreau să vând" → "Am să obțin" |
| Credit calculator | Home page `section.credit-calculator` + "Grafic de rambursare" modal |
| Public API | WordPress REST `GET /wp-json/wp/v2/pages` (public); `POST /wp-admin/admin-ajax.php` `action=load_exchange` (used by the date picker) |

**Out of scope**: Internet/Mobile banking login, any financial operation, submitting forms or real applications ("Aplică online" is never clicked), load/security testing, the bank's internal systems (PostgreSQL, RabbitMQ, Jira).

## 2. Approach and risks

* BDD scenarios (Cucumber/Gherkin) run with Selenium WebDriver (Chrome) and REST Assured.
* Each scenario is independent: new browser per UI scenario, no shared state, any order.
* **Dynamic data is never hard-coded.** Exchange rates are read from the page or API at run time; tests assert relations (buy ≤ sell, result = amount × rate) and formats, not values.
* **Low load**: tests run sequentially, ~25 scenarios ≈ a few dozen page loads per full run; API tests send single requests.
* Risks: Cloudflare bot protection may challenge an automated browser; content (menu labels, products) may change; the Google Map is third-party and not asserted in detail.

## 3. Scenarios

Priority: **P1** = smoke (critical, run on every build), **P2** = regression.

| ID | Pri | Tags | Scenario | Expected result |
|---|---|---|---|---|
| UI-01 | P1 | @smoke | Home page opens | Title contains "MICB", logo and main menu links (persoane-juridice, despre-banca, cariera, reteaua-bancii) are visible |
| UI-02 | P1 | @smoke | Decline optional cookies | Cookiebot banner closes and does not appear again after reload |
| UI-03 | P1 | @smoke | Switch language RO → RU → EN on home page | URL prefix `/ru/`, `/en/`; `<html lang>` `ru-RU`, `en-US`; selected language marked active |
| UI-04 | P2 | @regression | Switch language on an inner page | Same page opens in the other language (`/despre-banca/` → `/ru/despre-banca/`) |
| UI-05 | P2 | @regression | Open product pages from the mega-menu (outline: 3 products) + calculator page | Correct URL, last breadcrumb = product name, `<title>` mentions it (product pages may have no `<h1>`) |
| UI-06 | P1 | @smoke | Exchange rates table (cash) | All currencies listed; every quoted pair has positive buy ≤ sell; numbers have 2 decimals |
| UI-07 | P2 | @regression | Card exchange rates tab | USD and EUR rows with buy ≤ sell |
| UI-08 | P1 | @smoke | Currency converter USD → MDL (outline: 3 amounts) | Result = amount × bank buy rate read from the table, rounded to 2 decimals (±0.01) |
| UI-09 | P2 | @regression | Exchange rates for a past date | Choosing a date in the date picker reloads the table; invariants from UI-06 hold |
| UI-10 | P2 | @regression | Credit calculator reacts to amount and term | Higher amount → higher monthly payment; longer term → lower monthly payment |
| UI-11 | P2 | @regression | Amount above the maximum | Input is limited to the maximum shown under the slider |
| UI-12 | P1 | @smoke | Repayment schedule is consistent | Rows = term; Σ principal = amount; per row interest + principal + fee = payment; next balance = balance − principal; Σ payments = total cost |
| UI-13 | P2 | @regression | Bank network filters | "Bancomate" shows ATM filters (Cash-In, EUR, USD, NFC) and hides branch filters; city list contains Chișinău and Bălți |
| UI-14 | P2 | @regression | Contact centre dropdown | Shows "Suport 24/7" and phone links with `tel:` scheme |
| API-01 | P1 | @smoke | `GET /wp-json/wp/v2/pages?per_page=5` | 200, JSON array of 5, `X-WP-TotalPages = ceil(X-WP-Total / 5)`, each item has id/slug/link on micb.md |
| API-02 | P2 | @regression | Filter by slug (outline) / unknown slug | Exactly one page with that slug and canonical link / empty array with 200 |
| API-03 | P2 | @regression | WPML `lang=ru` | Link of the same slug points to `/ru/` |
| API-04 | P2 | @regression | `per_page=500` | 400, `code = rest_invalid_param` |
| API-05 | P2 | @regression | Unknown page id | 404, `code = rest_post_invalid_id` |
| API-06 | P2 | @regression | Restricted route `/wp-json/wp/v2/types` | 401, `code = rest_cannot_access` (documents access policy) |
| API-07 | P1 | @smoke | `load_exchange` for a past date | 200 HTML fragment; EUR/USD present; quoted buy ≤ sell, values > 0 |
| API-08 | P2 | @regression | `load_exchange` same past date twice | Identical rates (historical data is stable) |
| API-09 | P2 | @regression | Unknown URL | HTTP 404 |

## 4. Entry / exit criteria

* Entry: site reachable (HTTP 200 on `/`), Chrome available.
* Exit: all P1 pass; P2 failures analysed and reported (bug vs. content change vs. environment).

## 5. Not automated / limitations

* Map markers (Google Maps canvas) — third-party rendering.
* Exact credit formula: the bank calculates interest by actual days in a period, so the payment is **not** a textbook annuity. We assert internal consistency of the schedule instead of re-implementing the bank's formula.
* Forms ("Aplică online", feedback) — not submitted by design.
