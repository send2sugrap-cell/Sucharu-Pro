# ==============================================================================
# SUCHARU PRO ERP -- PRODUCTION BUILD SCRIPT (PowerShell)
# ==============================================================================
# Performs a clean, deterministic production build of core, backend, and fat JAR.
# Verifies test suites and release artifact presence.

[CmdletBinding()]
param(
    [switch]$SkipTests = $false
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path "$ScriptDir\..\..").Path

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " SUCHARU PRO ERP -- PRODUCTION BUILD PIPELINE" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "Project Root: $ProjectRoot"

Set-Location $ProjectRoot

try {
    Write-Host "[Step 1/3] Cleaning workspace..." -ForegroundColor Yellow
    & .\gradlew.bat clean

    if (-not $SkipTests) {
        Write-Host "[Step 2/3] Running complete unit and integration test suites..." -ForegroundColor Yellow
        & .\gradlew.bat :core:test :backend:test
        if ($LASTEXITCODE -ne 0) {
            throw "Test suite execution failed with exit code $LASTEXITCODE"
        }
    } else {
        Write-Host "[Step 2/3] Skipping test suites (-SkipTests specified)..." -ForegroundColor Magenta
    }

    Write-Host "[Step 3/3] Packaging executable production fat JAR..." -ForegroundColor Yellow
    & .\gradlew.bat :backend:jar
    if ($LASTEXITCODE -ne 0) {
        throw "JAR packaging failed with exit code $LASTEXITCODE"
    }

    $ArtifactPath = "$ProjectRoot\backend\build\libs\sucharu-server.jar"
    if (Test-Path $ArtifactPath) {
        $FileSize = (Get-Item $ArtifactPath).Length
        $FileSizeMB = [Math]::Round($FileSize / 1048576, 2)
        Write-Host "=================================================================" -ForegroundColor Green
        Write-Host " PRODUCTION BUILD SUCCESSFUL" -ForegroundColor Green
        Write-Host " Artifact: $ArtifactPath ($FileSizeMB MB)" -ForegroundColor Green
        Write-Host "=================================================================" -ForegroundColor Green
    } else {
        throw "Expected build artifact not found at: $ArtifactPath"
    }
} catch {
    Write-Host "=================================================================" -ForegroundColor Red
    Write-Host " PRODUCTION BUILD FAILED: $_" -ForegroundColor Red
    Write-Host "=================================================================" -ForegroundColor Red
    exit 1
}
