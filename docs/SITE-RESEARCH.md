# Site research notes (2026-11-17)

Facts observed on https://micb.md/ before and while writing tests. Locators and API calls in the
project are based only on these observations.

## Platform
* WordPress + custom theme `micb`, WPML (languages), Yoast SEO (breadcrumbs), Contact Form 7.
* Cloudflare in front of the site. Headless Chrome was **not** challenged during development.
* Cookie consent: Cookiebot, loaded with `data-blockingmode="auto"`.

## Behaviour that affected test design
| Observation | Consequence in the framework |
|---|---|
| Cookiebot "auto blocking" delays the site's own scripts (jQuery, theme, calculator) until consent status is resolved, ≈1 s **after** the `load` event. Clicks in that window are lost although `document.readyState === "complete"`. Proven with a probe that traced `Event.stopPropagation` from the theme's handler and timed when `jQuery._data(button,'events').click` appears. | `BasePage.waitForDocumentReady()` also waits for the theme's click handler on the mega-menu button. |
| Mega-menu (`.subnav-box .dropdown`) opens on **click** on desktop (> 1024 px), toggles class `active`, fades in (opacity transition 0.2 s). | `Header.expand()` clicks once and waits for `active` + visibility. |
| Google Analytics cross-domain linker appends `?_gl=…` to some navigations. | URL assertions compare the **path** only. |
| WPML translates slugs: `/despre-banca/` → `/ru/despre-banca/` but `/en/about-moldinconbank/`. | Expected URLs in Examples tables, not built by string concatenation. |
| Product pages often have no `<h1>` (e.g. `/carduri-de-debit/`, `/deposits/maestro/`). | Product checks use `.breadcrumb_last` + `<title>`. *(Possible accessibility/SEO finding for the site owner.)* |
| SEO titles differ from page names ("Credit de nevoi personale până la 400.000 MDL"). | Title check is case-insensitive "mentions". |
| Bank network filters are shown by script after init; Weekend/Emergency for branches, Cash-In/EUR/USD/NFC for ATMs. | Explicit waits for visibility / invisibility. |
| Date picker field can be covered by the sticky header. | Element is scrolled to the centre before clicking. |

## Exchange rates and converter
* Home page `.exchange-section`: tabs `cash` (7 currencies, with BNM column) and `card` (USD, EUR).
* Cells `.buy_XXX` / `.sell_XXX`; "–" when a currency is not traded (UAH, RUB at the time of research).
* Converter "Vreau să vând USD → Am să obțin MDL": result = amount × **bank buy rate** (bank buys USD), 2 decimals.
  Verified manually: 100 → 1 818.00, 123.45 → 2 244.32, 7 → 127.26 at buy rate 18.18.

## Credit calculator (plugin `micb-credit-calculator`)
* Inputs are text fields bound to noUiSlider; value committed on Enter / blur / change; clamped to the
  purpose's range (e.g. 5 000 – 400 000 MDL for "Nevoi personale"), rounded to step 5 000.
* Interest is calculated by actual days per period (first-row interest differs), so the payment is **not**
  a textbook annuity → tests check schedule invariants instead of re-implementing the formula.
* "Aplică online" opens an application flow — never clicked by tests.

## Public interfaces
* `GET /wp-json/wp/v2/pages` — public (200). `X-WP-Total`, `X-WP-TotalPages` headers; `_fields`, `slug`, `lang` supported.
* `GET /wp-json/`, `/wp-json/wp/v2/types`, custom post types (credits, deposits, news) — **401** `rest_cannot_access`.
* `POST /wp-admin/admin-ajax.php action=load_exchange date_value=dd-mm-yyyy active_tab=cash|card` —
  returns HTML with both tabs, the requested one marked `active`.
