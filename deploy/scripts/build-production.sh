#!/usr/bin/env bash
# ==============================================================================
# SUCHARU PRO ERP — PRODUCTION BUILD SCRIPT (Bash / Linux / macOS)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "================================================================="
echo " SUCHARU PRO ERP — PRODUCTION BUILD PIPELINE"
echo "================================================================="
echo "Project Root: ${PROJECT_ROOT}"

cd "${PROJECT_ROOT}"

echo -e "\n[Step 1/3] Cleaning workspace..."
./gradlew clean

echo -e "\n[Step 2/3] Running complete unit and integration test suites..."
./gradlew :core:test :backend:test

echo -e "\n[Step 3/3] Packaging executable production fat JAR..."
./gradlew :backend:jar

ARTIFACT_PATH="${PROJECT_ROOT}/backend/build/libs/sucharu-server.jar"
if [[ -f "${ARTIFACT_PATH}" ]]; then
    FILE_SIZE=$(ls -lh "${ARTIFACT_PATH}" | awk '{print $5}')
    echo "================================================================="
    echo " PRODUCTION BUILD SUCCESSFUL"
    echo " Artifact: ${ARTIFACT_PATH} (${FILE_SIZE})"
    echo "================================================================="
else
    echo "ERROR: Expected build artifact not found at: ${ARTIFACT_PATH}" >&2
    exit 1
fi
