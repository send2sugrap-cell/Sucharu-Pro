#!/usr/bin/env bash
# ==============================================================================
# SUCHARU PRO ERP — DEPLOYMENT SMOKE TEST SCRIPT (Bash)
# ==============================================================================
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"

echo "================================================================="
echo " SUCHARU PRO ERP — LIVE DEPLOYMENT SMOKE TEST"
echo " Target URL: ${BASE_URL}"
echo "================================================================="

PASSED=0
FAILED=0

test_endpoint() {
    local name="$1"
    local path="$2"
    local expected_status="$3"
    local auth_header="${4:-}"

    local url="${BASE_URL}${path}"
    local status_code
    if [[ -n "${auth_header}" ]]; then
        status_code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: ${auth_header}" "${url}" || echo "000")
    else
        status_code=$(curl -s -o /dev/null -w "%{http_code}" "${url}" || echo "000")
    fi

    if [[ "${status_code}" == "${expected_status}" ]]; then
        echo -e " [PASS] ${name} -> HTTP ${status_code}"
        ((PASSED++))
    else
        echo -e " [FAIL] ${name} -> Expected HTTP ${expected_status}, got ${status_code}"
        ((FAILED++))
    fi
}

test_endpoint "Liveness Probe (/health)" "/health" "200"
test_endpoint "Readiness Probe (/ready)" "/ready" "200"
test_endpoint "Prometheus Metrics (/metrics)" "/metrics" "200"
test_endpoint "Application Root Metadata (/)" "/" "200"
test_endpoint "Public API (/api/v1/public/company)" "/api/v1/public/company" "200"
test_endpoint "Unauthenticated Guard (/api/v1/customer/orders)" "/api/v1/customer/orders" "401"
test_endpoint "Invalid Token Guard (/api/v1/customer/orders)" "/api/v1/customer/orders" "401" "Bearer invalid_token"
test_endpoint "Admin Summary Guard (/api/v1/admin/operations/summary)" "/api/v1/admin/operations/summary" "401"

echo "================================================================="
echo " SMOKE TEST SUMMARY: ${PASSED} Passed, ${FAILED} Failed"
echo "================================================================="

if [[ "${FAILED}" -gt 0 ]]; then
    exit 1
fi
