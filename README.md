# micb-qa-automation

UI + API test automation for the **public** website [micb.md](https://micb.md/),
built with Java, Selenium WebDriver, Cucumber (BDD), JUnit 5 and REST Assured.

> **Disclaimer.** This is an independent educational / portfolio project. It is **not** an official
> project of BC "Moldindconbank" S.A., is not endorsed by the bank and uses no internal access.
> Only public pages are tested; no login, no financial operations, no form submissions.

## Goals

* Show a maintainable BDD framework on a real, dynamic banking website.
* Test business logic, not just page loads: currency converter math, credit schedule consistency,
  exchange-rate invariants, language routing, API contracts.
* Be honest about what can and cannot be tested from outside the bank.

## Tech stack

| Area | Tool | Why |
|---|---|---|
| Language / build | Java 21, Maven 3.9 | LTS JDK; standard build tool |
| UI | Selenium WebDriver 4.52 | Selenium Manager resolves the browser driver automatically |
| BDD | Cucumber 7.34 (Gherkin), cucumber-picocontainer | Readable scenarios; DI shares per-scenario state between step classes |
| Runner | JUnit 5 Platform Suite | One entry point, tag filtering, JUnit XML for CI |
| API | REST Assured 5.5 | Fluent HTTP + JSON assertions |
| HTML parsing | jsoup 1.24 | `load_exchange` returns server-rendered HTML, not JSON |
| Assertions | AssertJ 3.27 | Clear messages, soft assertions |
| Logging | SLF4J + Logback | Console + `target/logs/test-run.log` |
| CI | Jenkins (declarative `Jenkinsfile`) | Parameterised suites, reports, artifacts |
| Test management | Jira/Xray (Cucumber JSON import) | Prepared, see [docs/XRAY-INTEGRATION.md](docs/XRAY-INTEGRATION.md) |
| Learning module | PostgreSQL, RabbitMQ, Testcontainers | Separate, local, synthetic, see [learning-lab](learning-lab/README.md) |

## Project structure

```
micb-qa-automation/
├── pom.xml
├── Jenkinsfile
├── docs/
│   ├── TEST-PLAN.md          # scope, priorities, scenarios and expected results
│   ├── SITE-RESEARCH.md      # observed site behaviour that shaped the framework
│   └── XRAY-INTEGRATION.md
├── scripts/xray-import-cloud.sh
├── src/test/java/md/micb/qa/
│   ├── config/      Config            – config.properties + -D overrides
│   ├── driver/      DriverFactory, DriverManager (ThreadLocal)
│   ├── pages/       BasePage, HomePage, ContentPage, BankNetworkPage
│   │   └── components/  Header, CookieBanner, ExchangeRates, CurrencyConverter,
│   │                    CreditCalculator, PaymentScheduleModal
│   ├── api/         ApiClient, LoggingFilter, ExchangeHtmlParser
│   ├── steps/       Navigation/Contact/Exchange/CreditCalculator/Api steps
│   ├── hooks/       UiHooks (browser per scenario, screenshots), ApiHooks
│   ├── support/     Money (BigDecimal parsing), ScenarioContext
│   └── runners/     RunCucumberTest
├── src/test/resources/
│   ├── features/ui/*.feature, features/api/*.feature
│   ├── config.properties, junit-platform.properties, logback-test.xml
└── learning-lab/   # independent Maven project (PostgreSQL + RabbitMQ), NOT part of micb.md tests
```

## Requirements

* JDK 21 (e.g. Eclipse Temurin). Maven is optional: the bundled wrapper `./mvnw` downloads Maven 3.9.9
* Google Chrome (or Firefox with `-Dbrowser=firefox`)
* Internet access to `micb.md` (and to Maven Central / the driver download on the first run)
* Docker — only for `learning-lab` integration tests

## Running

Use `./mvnw` (macOS/Linux) or `mvnw.cmd` (Windows) instead of `mvn` if Maven is not installed.

```bash
mvn test                                          # everything (tag expression "not @wip")
mvn test -Dcucumber.filter.tags="@smoke"          # smoke suite
mvn test -Dcucumber.filter.tags="@regression"
mvn test -Dcucumber.filter.tags="@api"            # API only, no browser
mvn test -Dcucumber.filter.tags="@UI-12"          # a single test-plan item
mvn test -Dheadless=false                         # watch the browser
mvn test -Dbrowser=firefox
```

Configuration lives in `src/test/resources/config.properties`; any key can be overridden with `-Dkey=value`.

Tags: `@smoke` (critical, every build), `@regression`, `@ui`, `@api`, and test-plan IDs (`@UI-01` … `@API-09`).

## Reports and diagnostics

| Output | Path |
|---|---|
| Cucumber HTML report (screenshots of failures embedded) | `target/cucumber-reports/cucumber.html` |
| Cucumber JSON (for Xray / other tools) | `target/cucumber-reports/cucumber.json` |
| JUnit XML (Jenkins) | `target/surefire-reports/*.xml`, `target/cucumber-reports/cucumber-junit.xml` |
| Failure screenshots | `target/screenshots/*.png` |
| Run log (each step, every HTTP call with status and time) | `target/logs/test-run.log` |
| Failed scenario list | `target/cucumber-reports/rerun.txt` |

On a UI failure the report gets the current URL, a screenshot and the page source; on an API failure,
the full last request/response. There are no automatic re-runs: a flaky test must be fixed, not hidden.

## Design decisions

* **Page Object + components.** Header, cookie banner, calculator etc. are components reused by pages.
* **Only explicit waits**, implicit wait = 0, no `Thread.sleep` in tests. Waits target the condition the
  user would see (dropdown has `active`, slider shows the committed value, table element went stale after AJAX).
* **Site readiness.** Cookiebot's auto-blocking runs the site's scripts ≈1 s after `load`, so
  `BasePage` additionally waits until the theme's click handler is attached (details in
  [docs/SITE-RESEARCH.md](docs/SITE-RESEARCH.md)).
* **Independent scenarios.** New browser per UI scenario, `ScenarioContext` per scenario, no order dependency.
* **Low load.** Sequential execution, a full run is ≈35 scenarios / ≈2–3 minutes. The API client sends a
  descriptive `User-Agent`. Jenkins has `disableConcurrentBuilds()`.

### Dynamic data and rounding

* Exchange rates are **never** hard-coded. Tests read the rate from the page/API at run time and check relations:
  buy ≤ sell, values > 0, two decimals, converter result = amount × buy rate.
* Money is parsed into `BigDecimal` (no `double` errors). Expected converter value is rounded HALF_UP to
  2 decimals and compared with **±0.01** tolerance, because the site computes in JavaScript floating point.
* The credit calculator is checked through invariants of its own schedule: rows = term,
  Σ principal = amount, interest + principal + fee = payment for every row, balance chain, Σ payments = total cost
  (±0.01 per row for rounding). Business monotonicity: bigger amount → bigger payment, longer term → smaller payment.
* Historical rates (past dates via API) must be identical in two consecutive requests.

## CI (Jenkins)

`Jenkinsfile` stages: **Checkout → Build (`test-compile`) → Test (selected tags) → optional Xray import**,
then always: publish JUnit results, Cucumber HTML report (HTML Publisher plugin) and archive
reports/screenshots/logs. Failing tests make the build *UNSTABLE*; compilation errors make it *FAILED*.
Parameters: `SUITE`, `BROWSER`, `HEADLESS`, `XRAY_IMPORT`, `XRAY_PROJECT_KEY`. Nightly smoke on weekdays.

## Limitations

* Only the anonymous public site is in scope. Internet banking, payments and application forms are excluded by design.
* The bank's PostgreSQL, RabbitMQ and Jira are not accessible from outside; `learning-lab` demonstrates
  those skills on local containers with synthetic data and is **not** a test of micb.md.
* The Google Map on the branch page is third-party; only filters are tested.
* Content (menu labels, product names) may change; such failures are content changes, not necessarily defects.
* Locale-dependent labels (calculator result names) are read on the Romanian site, the default language.
* The Jenkinsfile was written for a standard Jenkins setup and has not been executed on a Jenkins server
  as part of this project; Xray import has not been executed (no instance/credentials).

## License

Educational use. Trademarks and website content belong to their respective owners.
