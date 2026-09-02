# ==============================================================================
# SUCHARU PRO ERP -- POSTGRESQL BACKUP SCRIPT (PowerShell)
# ==============================================================================
# Performs consistent, non-blocking logical backup of PostgreSQL using pg_dump.
# Security: Password must be supplied via PGPASSWORD env variable.

[CmdletBinding()]
param(
    [string]$Host = "localhost",
    [int]$Port = 5432,
    [string]$Database = "sucharu_pro_db",
    [string]$User = "sucharu_app",
    [string]$BackupDir = "$PSScriptRoot\..\..\backups",
    [int]$RetentionDays = 30
)

$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " SUCHARU PRO ERP -- DATABASE BACKUP PROCEDURE" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupFile = "$BackupDir\${Database}_${Timestamp}.dump"

Write-Host "Target Database: $Database on ${Host}:${Port}"
Write-Host "Output File:     $BackupFile"

try {
    $pgDump = Get-Command pg_dump -ErrorAction SilentlyContinue
    if ($null -eq $pgDump) {
        Write-Warning "pg_dump not found in system PATH. Attempting Docker-based backup..."
        $docker = Get-Command docker -ErrorAction SilentlyContinue
        if ($null -ne $docker) {
            docker exec sucharu_postgres_prod pg_dump -U $User -d $Database -Fc > $BackupFile
        } else {
            throw "Neither pg_dump nor docker is available in system PATH."
        }
    } else {
        & pg_dump -h $Host -p $Port -U $User -d $Database -Fc -f $BackupFile
    }

    if (Test-Path $BackupFile) {
        $Size = (Get-Item $BackupFile).Length
        $SizeMB = [Math]::Round($Size / 1048576, 2)
        Write-Host "[SUCCESS] Backup completed successfully ($SizeMB MB)" -ForegroundColor Green
    } else {
        throw "Backup file was not generated."
    }

    # Prune old backups
    Write-Host "Pruning backups older than $RetentionDays days in $BackupDir..." -ForegroundColor Yellow
    Get-ChildItem -Path $BackupDir -Filter "*.dump" | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } | ForEach-Object {
        Write-Host " Removing old backup: $($_.Name)" -ForegroundColor DarkGray
        Remove-Item $_.FullName -Force
    }

    Write-Host "Backup retention policy verified." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Backup failed: $_" -ForegroundColor Red
    exit 1
}
