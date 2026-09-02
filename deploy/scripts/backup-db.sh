#!/usr/bin/env bash
# ==============================================================================
# SUCHARU PRO ERP — POSTGRESQL BACKUP SCRIPT (Bash)
# ==============================================================================
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-sucharu_pro_db}"
DB_USER="${DB_USER:-sucharu_app}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

echo "================================================================="
echo " SUCHARU PRO ERP — DATABASE BACKUP PROCEDURE"
echo "================================================================="

mkdir -p "${BACKUP_DIR}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.dump"

echo "Target Database: ${DB_NAME} on ${DB_HOST}:${DB_PORT}"
echo "Output File:     ${BACKUP_FILE}"

if command -v pg_dump &>/dev/null; then
    pg_dump -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -Fc -f "${BACKUP_FILE}"
elif command -v docker &>/dev/null && docker ps --format '{{.Names}}' | grep -q "sucharu_postgres"; then
    echo "Running pg_dump inside postgres container..."
    docker exec sucharu_postgres_prod pg_dump -U "${DB_USER}" -d "${DB_NAME}" -Fc > "${BACKUP_FILE}"
else
    echo "ERROR: Neither pg_dump nor postgres container found." >&2
    exit 1
fi

if [[ -f "${BACKUP_FILE}" ]]; then
    FILE_SIZE=$(ls -lh "${BACKUP_FILE}" | awk '{print $5}')
    echo "[SUCCESS] Backup created successfully: ${BACKUP_FILE} (${FILE_SIZE})"
fi

# Prune old backups
echo "Pruning backups older than ${RETENTION_DAYS} days in ${BACKUP_DIR}..."
find "${BACKUP_DIR}" -name "*.dump" -type f -mtime +"${RETENTION_DAYS}" -delete || true
echo "Backup retention policy verified."
