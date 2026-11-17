# Jira / Xray integration

Status: **prepared, not executed.** No Jira/Xray instance or credentials were available, so no
results were imported and no issue keys exist. The scenario tags `@UI-01`, `@API-07` etc. are
this project's own test-plan IDs (see [TEST-PLAN.md](TEST-PLAN.md)), **not** Jira keys.

## Chosen approach: Cucumber JSON import

The test run already produces `target/cucumber-reports/cucumber.json`. Xray imports this format
directly and creates a *Test Execution* with one result per scenario.

| Xray edition | Endpoint |
|---|---|
| Xray Cloud | `POST https://xray.cloud.getxray.app/api/v2/import/execution/cucumber/multipart` (token from `/api/v2/authenticate`) |
| Xray Server / Data Center | `POST {JIRA_URL}/rest/raven/2.0/import/execution/cucumber` (Jira PAT / basic auth) |

`scripts/xray-import-cloud.sh` implements the Cloud variant. Credentials come only from
environment variables (in Jenkins: credentials `xray-client-id`, `xray-client-secret`), never from files in Git.

## How scenarios are linked to Xray tests

Xray matches results by **tags that equal Jira issue keys**. Two workflows:

1. **Tests live in Jira (recommended for a team).** Create *Cucumber* Test issues in Jira, export
   them as `.feature` files (Xray → "Export to Cucumber") or add the keys as tags manually:
   ```gherkin
   @smoke @UI-08 @QA-123
   Scenario Outline: Converter sells <amount> USD at the bank buy rate
   ```
2. **Features live in Git.** Import the `.feature` files to Xray
   (`POST /api/v2/import/feature?projectKey=QA`); Xray creates Test issues and returns their keys,
   which are then added as tags.

Requirement coverage: tag a scenario with the story key (e.g. `@REQ-45`) to link the test to that requirement.

## Running it

Local (after exporting the three variables in your shell, not in a file):

```bash
mvn test -Dcucumber.filter.tags="@smoke"
scripts/xray-import-cloud.sh target/cucumber-reports/cucumber.json
```

Jenkins: run the job with `XRAY_IMPORT=true` and `XRAY_PROJECT_KEY=<key>`.

## Alternative formats

* **JUnit XML** (`target/cucumber-reports/cucumber-junit.xml`) → `/api/v2/import/execution/junit` —
  simpler, but loses Gherkin steps.
* **Xray JSON** — full control (custom fields, evidence), needs a converter step.
