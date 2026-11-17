#!/usr/bin/env bash
# Imports a Cucumber JSON report into Xray Cloud as a Test Execution.
# Docs: https://docs.getxray.app/display/XRAYCLOUD/Import+Execution+Results+-+REST+v2
#
# Required environment variables (never commit their values):
#   XRAY_CLIENT_ID, XRAY_CLIENT_SECRET  - Xray Cloud API key (Xray -> Settings -> API Keys)
#   XRAY_PROJECT_KEY                    - Jira project key, e.g. QA
# Usage: scripts/xray-import-cloud.sh target/cucumber-reports/cucumber.json
set -euo pipefail

REPORT="${1:-target/cucumber-reports/cucumber.json}"
XRAY_URL="${XRAY_URL:-https://xray.cloud.getxray.app}"

: "${XRAY_CLIENT_ID:?XRAY_CLIENT_ID is not set}"
: "${XRAY_CLIENT_SECRET:?XRAY_CLIENT_SECRET is not set}"
: "${XRAY_PROJECT_KEY:?XRAY_PROJECT_KEY is not set}"
[ -s "$REPORT" ] || { echo "Report not found or empty: $REPORT" >&2; exit 1; }

# 1) Exchange the API key for a short-lived token (the token is not printed)
TOKEN=$(curl -sf -H "Content-Type: application/json" -X POST \
  --data "{\"client_id\":\"${XRAY_CLIENT_ID}\",\"client_secret\":\"${XRAY_CLIENT_SECRET}\"}" \
  "${XRAY_URL}/api/v2/authenticate" | tr -d '"')

# 2) Multipart import: results + Test Execution fields (project, summary)
INFO=$(mktemp)
trap 'rm -f "$INFO"' EXIT
cat > "$INFO" <<JSON
{
  "fields": {
    "project": { "key": "${XRAY_PROJECT_KEY}" },
    "summary": "micb.md automated run ${BUILD_TAG:-local} $(date -u +%Y-%m-%dT%H:%MZ)",
    "issuetype": { "name": "Test Execution" }
  }
}
JSON

curl -sf -H "Authorization: Bearer ${TOKEN}" -X POST \
  -F "results=@${REPORT};type=application/json" \
  -F "info=@${INFO};type=application/json" \
  "${XRAY_URL}/api/v2/import/execution/cucumber/multipart"
echo
