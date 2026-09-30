# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## PRODUCTION ROLLBACK & FAILURE-SAFETY POLICY

**Date:** 2026-09-29  
**Repository:** `E:\App\Sucharu Pro`  
**Target Project:** `sucharu-pro`  
**Status:** `VERIFIED & ADOPTED`  

---

## Executive Overview

This document establishes the mandatory **Rollback & Failure-Safety Policy** for Sucharu Pro production deployments on Google Cloud Platform. It governs all Cloud Run revisions, traffic migrations, database compatibility checks, secret/IAM/network failure isolations, and automated rollback workflows.

---

## 1. Immutable Revision Policy

* Every production deployment **MUST** create a new immutable Cloud Run revision.
* Never overwrite or mutate a previously validated production revision.
* The deployed container image **MUST** remain traceable to an immutable Git commit SHA.
* Do not rely on `latest` tag alone for rollback identification.

---

## 2. Pre-Deployment Safety Gate

Before directing production traffic to a new Cloud Run revision, verify:
* Container image exists in Artifact Registry (`asia-southeast1-docker.pkg.dev/sucharu-pro/sucharu-pro-repo/sucharu-backend-server`).
* Cloud Run revision reaches `Ready` / healthy state.
* Required Secret Manager references are valid (`DATABASE_PASSWORD`, `JWT_SIGNING_SECRET`, `GEMINI_API_KEY`, `N8N_SIGNING_SECRET`).
* Cloud SQL private IP connectivity is verified (`10.20.0.3:5432` via `sucharu-vpc`).
* `/health` and `/ready` endpoints respond successfully with HTTP 200.
* Flyway/database compatibility has been verified.
* Runtime service account (`sucharu-backend-sa@sucharu-pro.iam.gserviceaccount.com`) and required IAM bindings are unchanged and valid.
* No unexpected configuration drift is detected.

> [!CAUTION]
> If any critical gate fails, **DO NOT** send production traffic to the new revision.

---

## 3. Zero-Traffic Validation

Where practical:
* Deploy the new revision with `--no-traffic`.
* Validate the revision independently via direct revision URL or tag.
* Use a revision tag for controlled testing when appropriate.
* Only migrate production traffic after validation completes successfully.

---

## 4. Controlled Traffic Migration

Preferred production rollout flow:
1. Existing known-good revision remains active and serving 100% traffic.
2. New revision is deployed with `--no-traffic`.
3. New revision is validated independently.
4. A small controlled percentage of traffic (e.g., 5-10%) MAY be directed to the new revision.
5. Monitor health, error rate, latency, logs, database connection errors, and business-critical endpoints.
6. Increase traffic progressively only when validation remains 100% healthy.
7. Keep the previous known-good revision available until full acceptance is complete.

---

## 5. Automatic Failure Stop

Immediately stop further rollout if any critical condition appears, including:
* Repeated startup or readiness probe failures.
* HTTP 5xx error rate increase.
* Authentication / JWT validation failures.
* Database connection or pool exhaustion failures.
* Flyway schema / migration failures.
* Multi-tenant RLS (Row-Level Security) failure.
* Payment or finance integrity anomalies.
* Unexpected secret or configuration resolution failure.
* Material regression in critical business APIs.

> [!WARNING]
> Do not attempt repeated blind redeployments against an unresolved failure.

---

## 6. Rollback Procedure

If the new revision is unhealthy:
1. Identify the last known-good Cloud Run revision name.
2. Stop further traffic migration immediately.
3. Route 100% traffic back to the last known-good revision:
   ```cmd
   gcloud run services update-traffic sucharu-backend-server --to-revisions KNOWN_GOOD_REVISION=100 --region=asia-southeast1 --project=sucharu-pro
   ```
4. Verify `/health` and `/ready` respond with HTTP 200 on the restored revision.
5. Verify critical business endpoints.
6. Inspect Cloud Run logs and deployment events via Cloud Logging.
7. Record the failed revision, Git commit SHA, container image digest, failure timestamp, and rollback timestamp in audit logs.
8. Keep the failed revision available for post-mortem investigation unless removal is specifically required.

---

## 7. Database Migration Safety

Application rollback **MUST NOT** automatically imply database rollback.

Before production deployment:
* Classify every Flyway migration as backward-compatible or requiring coordinated deployment.
* Never destroy or rewrite historical production data as part of an application rollback.
* Avoid destructive migrations (`DROP COLUMN`, `DROP TABLE`) in the same release as an application change unless separately approved and verified.
* If a database migration is incompatible with the previous application revision, the deployment **MUST** be stopped rather than blindly rolling the application back.
* Database recovery **MUST** use established Cloud SQL backup/restore and point-in-time recovery (PITR) procedures when required.

---

## 8. Secret and Configuration Failure

If deployment fails because of missing or invalid secrets:
* **DO NOT** print secret values to logs or console.
* **DO NOT** copy secrets into source code or Dockerfiles.
* **DO NOT** bake secrets into container images.
* Correct Secret Manager configuration/version/IAM access (`roles/secretmanager.secretAccessor`).
* Redeploy only after secret availability is re-validated.

---

## 9. IAM Failure Safety

If a deployment or runtime failure is caused by IAM:
* **DO NOT** grant `roles/owner`, `roles/editor`, or broad administrative roles as a workaround.
* Identify the exact missing permission.
* Grant only the minimum required least-privilege role.
* Record the IAM change in the deployment audit log.

---

## 10. Network / Cloud SQL Failure Safety

If private networking or Cloud SQL connectivity fails:
* **DO NOT** expose the database publicly (`0.0.0.0/0`) merely to bypass the failure.
* Diagnose Direct VPC egress, private IP (`10.20.0.3`), DNS/routing, firewall rules, IAM, and database configuration.
* Preserve the intended private-network architecture (`sucharu-vpc` / `sucharu-subnet`).
* Stop deployment until private connectivity is re-verified.

---

## 11. Rollback & Deployment Evidence Recording

Every production deployment **MUST** record the following audit evidence:
* Git commit SHA.
* Container image URI and immutable SHA256 digest.
* Cloud Run revision name.
* Deployment timestamp.
* Previous known-good revision name.
* Traffic percentage before/after deployment.
* Health/readiness test results (`/health`, `/ready`).
* Database migration execution result.
* Rollback decision and outcome (if applicable).

---

## 12. Final Failure-Safety Rule

> [!IMPORTANT]
> **No deployment failure may be "fixed" by bypassing security, tenant isolation, database controls, secret protection, or approval requirements.**

If a critical validation fails, the safe workflow is:
```text
STOP → PRESERVE EVIDENCE → KEEP KNOWN-GOOD REVISION → ROLLBACK IF NECESSARY → DIAGNOSE → FIX → RE-VALIDATE → REDEPLOY
```
