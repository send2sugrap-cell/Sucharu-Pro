#!/usr/bin/env bash
# ==============================================================================
# SUCHARU PRO ERP — POSTGRESQL RESTORE SCRIPT (Bash)
# ==============================================================================
set -euo pipefail

BACKUP_FILE="${1:?Usage: ./restore-db.sh <path_to_backup_file>}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-sucharu_pro_db}"
DB_USER="${DB_USER:-sucharu_app}"
FORCE="${FORCE:-false}"

echo "================================================================="
echo " SUCHARU PRO ERP — DATABASE RESTORE PROCEDURE"
echo "================================================================="

if [[ ! -f "${BACKUP_FILE}" ]]; then
    echo "ERROR: Backup file not found at: ${BACKUP_FILE}" >&2
    exit 1
fi

echo "Target Database: ${DB_NAME} on ${DB_HOST}:${DB_PORT}"
echo "Source Backup:   ${BACKUP_FILE}"
echo "WARNING: Restoring will overwrite existing data in ${DB_NAME}."

if [[ "${FORCE}" != "true" ]]; then
    read -rp "Are you sure you want to proceed with restore? (type 'YES' to confirm): " CONFIRM
    if [[ "${CONFIRM}" != "YES" ]]; then
        echo "Restore operation aborted."
        exit 0
    fi
fi

if command -v pg_restore &>/dev/null; then
    pg_restore -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists --no-owner --no-privileges "${BACKUP_FILE}"
elif command -v docker &>/dev/null && docker ps --format '{{.Names}}' | grep -q "sucharu_postgres"; then
    echo "Running pg_restore inside postgres container..."
    cat "${BACKUP_FILE}" | docker exec -i sucharu_postgres_prod pg_restore -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists --no-owner --no-privileges
else
    echo "ERROR: Neither pg_restore nor postgres container found." >&2
    exit 1
fi

echo "[SUCCESS] Database restore completed successfully."
