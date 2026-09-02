# ==============================================================================
# SUCHARU PRO ERP -- POSTGRESQL RESTORE SCRIPT (PowerShell)
# ==============================================================================
# Restores a PostgreSQL database dump created by backup-db.ps1.
# Security: Password must be supplied via PGPASSWORD env variable.

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    [string]$Host = "localhost",
    [int]$Port = 5432,
    [string]$Database = "sucharu_pro_db",
    [string]$User = "sucharu_app",
    [switch]$Force = $false
)

$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " SUCHARU PRO ERP -- DATABASE RESTORE PROCEDURE" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

if (-not (Test-Path $BackupFile)) {
    throw "Backup file not found at: $BackupFile"
}

Write-Host "Target Database: $Database on ${Host}:${Port}"
Write-Host "Source Backup:   $BackupFile"
Write-Host "WARNING: Restoring will overwrite existing data in $Database." -ForegroundColor Red

if (-not $Force) {
    $confirm = Read-Host "Are you sure you want to proceed with restore? (type 'YES' to confirm)"
    if ($confirm -ne "YES") {
        Write-Host "Restore operation aborted by user." -ForegroundColor Yellow
        exit 0
    }
}

try {
    $pgRestore = Get-Command pg_restore -ErrorAction SilentlyContinue
    if ($null -eq $pgRestore) {
        Write-Warning "pg_restore not found in system PATH. Attempting Docker-based restore..."
        $docker = Get-Command docker -ErrorAction SilentlyContinue
        if ($null -ne $docker) {
            Get-Content $BackupFile -Raw | docker exec -i sucharu_postgres_prod pg_restore -U $User -d $Database --clean --if-exists --no-owner --no-privileges
        } else {
            throw "Neither pg_restore nor docker is available."
        }
    } else {
        & pg_restore -h $Host -p $Port -U $User -d $Database --clean --if-exists --no-owner --no-privileges $BackupFile
    }

    Write-Host "[SUCCESS] Database restore completed successfully." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Database restore failed: $_" -ForegroundColor Red
    exit 1
}
