# ==============================================================================
# SUCHARU PRO ERP -- DEPLOYMENT SMOKE TEST SCRIPT (PowerShell)
# ==============================================================================
# Executes non-destructive HTTP smoke tests against a running Sucharu Pro instance.

[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " SUCHARU PRO ERP -- LIVE DEPLOYMENT SMOKE TEST" -ForegroundColor Cyan
Write-Host " Target URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$passed = 0
$failed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Path,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [int]$ExpectedStatus = 200,
        [string]$ExpectedSubstr = ""
    )

    $url = "$BaseUrl$Path"
    try {
        $response = Invoke-WebRequest -Uri $url -Method $Method -Headers $Headers -UseBasicParsing -SkipHttpErrorCheck
        $statusCode = $response.StatusCode
        $content = $response.Content

        if ($statusCode -eq $ExpectedStatus -and ($ExpectedSubstr -eq "" -or $content.Contains($ExpectedSubstr))) {
            Write-Host " [PASS] $Name -> HTTP $statusCode" -ForegroundColor Green
            $global:passed++
        } else {
            Write-Host " [FAIL] $Name -> Expected HTTP $ExpectedStatus, got $statusCode (Response: $content)" -ForegroundColor Red
            $global:failed++
        }
    } catch {
        Write-Host " [FAIL] $Name -> Exception: $_" -ForegroundColor Red
        $global:failed++
    }
}

# 1. Liveness Probe
Test-Endpoint -Name "Liveness Probe (/health)" -Path "/health" -ExpectedStatus 200 -ExpectedSubstr "UP"

# 2. Readiness Probe
Test-Endpoint -Name "Readiness Probe (/ready)" -Path "/ready" -ExpectedStatus 200

# 3. Prometheus Metrics Export
Test-Endpoint -Name "Prometheus Metrics (/metrics)" -Path "/metrics" -ExpectedStatus 200 -ExpectedSubstr "# HELP"

# 4. Release Root Identity
Test-Endpoint -Name "Application Root Metadata (/)" -Path "/" -ExpectedStatus 200 -ExpectedSubstr "sucharu"

# 5. Public Company Info
Test-Endpoint -Name "Public API (/api/v1/public/company)" -Path "/api/v1/public/company" -ExpectedStatus 200

# 6. Protected Route Unauthenticated Guard
Test-Endpoint -Name "Unauthenticated Guard (/api/v1/customer/orders)" -Path "/api/v1/customer/orders" -ExpectedStatus 401

# 7. Invalid Token Guard
Test-Endpoint -Name "Invalid Token Guard (/api/v1/customer/orders)" -Path "/api/v1/customer/orders" -Headers @{"Authorization"="Bearer invalid_forged_token"} -ExpectedStatus 401

# 8. Operational Summary Security Guard
Test-Endpoint -Name "Admin Summary Guard (/api/v1/admin/operations/summary)" -Path "/api/v1/admin/operations/summary" -ExpectedStatus 401

Write-Host "=================================================================" -ForegroundColor Cyan
if ($failed -eq 0) {
    Write-Host " SMOKE TEST SUMMARY: $passed Passed, $failed Failed" -ForegroundColor Green
} else {
    Write-Host " SMOKE TEST SUMMARY: $passed Passed, $failed Failed" -ForegroundColor Red
}
Write-Host "=================================================================" -ForegroundColor Cyan

if ($failed -gt 0) {
    exit 1
}
