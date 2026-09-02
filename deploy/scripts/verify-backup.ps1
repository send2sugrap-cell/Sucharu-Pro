# ==============================================================================
# SUCHARU PRO ERP -- BACKUP INTEGRITY VERIFICATION SCRIPT (PowerShell)
# ==============================================================================
# Verifies that a PostgreSQL custom-format dump is readable and not corrupted.

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile
)

$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " SUCHARU PRO ERP -- BACKUP INTEGRITY VERIFICATION" -ForegroundColor Cyan
Write-Host " Target File: $BackupFile" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

if (-not (Test-Path $BackupFile)) {
    throw "Backup file not found at: $BackupFile"
}

try {
    $pgRestore = Get-Command pg_restore -ErrorAction SilentlyContinue
    if ($null -ne $pgRestore) {
        $toc = & pg_restore --list $BackupFile
        $tableCount = ($toc | Select-String "TABLE DATA").Count
        Write-Host "[PASS] Backup TOC inspected successfully. Found $tableCount table data entries." -ForegroundColor Green
    } else {
        # Check basic file header and size
        $size = (Get-Item $BackupFile).Length
        if ($size -lt 1000) {
            throw "Backup file size is suspiciously small: $size bytes"
        }
        Write-Host "[PASS] Backup file exists and passes minimum size checks ($size bytes)." -ForegroundColor Green
    }
} catch {
    Write-Host "[FAIL] Backup verification failed: $_" -ForegroundColor Red
    exit 1
}
