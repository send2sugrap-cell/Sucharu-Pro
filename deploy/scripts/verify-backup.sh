#!/usr/bin/env bash
# ==============================================================================
# SUCHARU PRO ERP — BACKUP INTEGRITY VERIFICATION SCRIPT (Bash)
# ==============================================================================
set -euo pipefail

BACKUP_FILE="${1:?Usage: ./verify-backup.sh <path_to_backup_file>}"

echo "================================================================="
echo " SUCHARU PRO ERP — BACKUP INTEGRITY VERIFICATION"
echo " Target File: ${BACKUP_FILE}"
echo "================================================================="

if [[ ! -f "${BACKUP_FILE}" ]]; then
    echo "ERROR: Backup file not found at: ${BACKUP_FILE}" >&2
    exit 1
fi

if command -v pg_restore &>/dev/null; then
    pg_restore --list "${BACKUP_FILE}" > /dev/null
    TABLE_COUNT=$(pg_restore --list "${BACKUP_FILE}" | grep -c "TABLE DATA" || true)
    echo "[PASS] Backup TOC inspected successfully. Found ${TABLE_COUNT} table data entries."
else
    FILE_SIZE=$(stat -c%s "${BACKUP_FILE}" 2>/dev/null || stat -f%z "${BACKUP_FILE}" 2>/dev/null || echo "0")
    if [[ "${FILE_SIZE}" -lt 1000 ]]; then
        echo "ERROR: Backup file size is suspiciously small: ${FILE_SIZE} bytes" >&2
        exit 1
    fi
    echo "[PASS] Backup file exists and passes minimum size checks (${FILE_SIZE} bytes)."
fi
