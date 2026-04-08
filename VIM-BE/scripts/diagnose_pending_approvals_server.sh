#!/usr/bin/env bash
# Run on the Linux staging host (or WSL) after deploy. Helps separate DB vs app vs auth issues.
#
# Usage:
#   export BASE="https://your-staging-host/velocity"   # context path if any
#   export TOKEN="Bearer eyJ..."                       # JWT from browser devtools after login
#   ./diagnose_pending_approvals_server.sh
#
# Or without token (expect 401) to confirm routing:
#   BASE="https://host/velocity" ./diagnose_pending_approvals_server.sh

set -euo pipefail
BASE="${BASE:-http://localhost:8080/velocity}"
TOKEN="${TOKEN:-}"

echo "=== 1) Health: OPTIONS / root (adjust if your server blocks) ==="
curl -sS -o /dev/null -w "%{http_code}\n" "${BASE}/" || true

echo ""
echo "=== 2) Pending APIs (need valid JWT for 200) ==="
HDR=()
if [[ -n "${TOKEN}" ]]; then
  HDR=(-H "Authorization: ${TOKEN}")
fi

echo "GET getAllApplicationsPendingApproval"
curl -sS -w "\nHTTP %{http_code}\n" "${HDR[@]}" "${BASE}/getAllApplicationsPendingApproval" | tail -20

echo ""
echo "GET getApplicationsPendingApproval?departmentHeadUserId=1"
curl -sS -w "\nHTTP %{http_code}\n" "${HDR[@]}" "${BASE}/getApplicationsPendingApproval?departmentHeadUserId=1" | tail -20

echo ""
echo "=== 3) Recent errors in Spring log (path may differ) ==="
for f in /var/log/spring-app.log ./logs/spring-app.log ~/logs/spring-app.log; do
  if [[ -f "$f" ]]; then
    echo "--- tail $f (grep pending / Error) ---"
    grep -iE 'pending|getApplicationsPending|getAllApplications|Error fetching' "$f" | tail -40 || true
    break
  fi
done

echo ""
echo "If HTTP 500: read full stack trace in the log line after 'Error fetching applications pending approval'."
