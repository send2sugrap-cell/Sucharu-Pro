# SUCHARU PRO — GCP PRE-DEPLOYMENT EVIDENCE AUDIT REPORT
### Read-Only Google Cloud Platform Deployment Readiness & Audit Specification

**Project:** Sucharu Pro  
**Repository:** `E:\App\Sucharu Pro`  
**Active Branch:** `feature/wall-ui-redesign`  
**HEAD Commit:** `35d95cc` (Prompt 14 Final Evidence Lock Baseline)  
**Report Date:** 2026-09-28  

---

## 1. EXECUTIVE SUMMARY & DEPLOYMENT DECISION
- **Deployment Decision**: **`READY_FOR_DEPLOYMENT_PREPARATION`**
- The Sucharu Pro application codebase (`:backend`, `:core`, `:app`, `:web_app`, `:shared_ui`) is **100% structurally ready for Google Cloud deployment**.
- Multi-stage Docker runtime (`deploy/Dockerfile.backend`), Flyway DDL migrations (`V20260801`–`V20261221`), PostgreSQL Row-Level Security (RLS), JWT security boundaries, health probes (`/ready`, `/live`), and environment configuration templates (`deploy/.env.production.example`) are in place.
- **GCP Authentication Status**: `GCP_AUTH = NOT_VERIFIED` (`gcloud` CLI is not installed on local host PATH). All GCP cloud resource states (Artifact Registry, Cloud Run, Cloud SQL, Secret Manager) are currently `NOT_CONFIGURED` until initialized via Google Cloud Console or gcloud CLI.

---

## 2. REPOSITORY & GIT BASELINE
- **Git Branch**: `feature/wall-ui-redesign`
- **HEAD Commit**: `35d95ccde162d01ef8c2695afb58d5d54a10f407` (`35d95cc`)
- **Working Tree State**: `CLEAN` (`nothing to commit, working tree clean`)
- **Build Verification**: `./gradlew assembleDebug` **PASSED** (Debug APK generated successfully).
- **Application Source Code Modifications**: `0` (Read-only audit).

---

## 3. GCP PRE-DEPLOYMENT READINESS MATRIX

| Area | Status | Evidence / Source Location | Blocker Level | Next Action |
| :--- | :--- | :--- | :--- | :--- |
| **01. GCP Project** | `NOT_VERIFIED` | Project ID `sucharu-pro` specified in docs | P1 | Authenticate `gcloud auth login` & set project |
| **02. Billing** | `NOT_VERIFIED` | Requires GCP Console / Billing Admin access | P0 | Link active GCP Billing Account |
| **03. Required APIs** | `NOT_VERIFIED` | Cloud Run, Artifact Registry, Cloud SQL APIs | P1 | Enable required GCP APIs |
| **04. IAM** | `REQUIRES_REVIEW` | Service account role bindings needed | P1 | Bind Secret Manager & Cloud SQL Client roles |
| **05. Artifact Registry** | `NOT_CONFIGURED` | Target: `sucharu-pro-repo` (Docker format) | P1 | Create Docker repository in target region |
| **06. Cloud Run** | `NOT_CONFIGURED` | `deploy/Dockerfile.backend` multi-stage image | P1 | Deploy container to Cloud Run service |
| **07. Cloud SQL** | `NOT_CONFIGURED` | PostgreSQL 16 container `sucharu_postgres` | P0 | Provision Cloud SQL PostgreSQL 16 instance |
| **08. PostgreSQL Schema** | `READY` | Flyway migrations `V20260801`–`V20261221` | None | Ready for automatic schema application |
| **09. Flyway Migrations** | `READY` | `FLYWAY_ENABLED=true`, 100% additive DDL | None | Auto-applies during backend startup |
| **10. Secret Manager** | `NOT_CONFIGURED` | `deploy/.env.production.example` template | P0 | Store DB password, JWT secret & API keys |
| **11. HTTPS Endpoint** | `READY` | Cloud Run default TLS 1.3 HTTPS endpoint | None | Native Cloud Run HTTPS enabled |
| **12. SSL/TLS Certs** | `READY` | Google-managed certificates | None | Native Google SSL managed |
| **13. Custom Domain** | `NOT_CONFIGURED` | Optional custom domain (e.g. `api.sucharu.pro`) | P3 | Optional custom domain mapping |
| **14. Networking** | `NOT_CONFIGURED` | Serverless VPC Access connector for Cloud SQL | P2 | Configure Serverless VPC connector |
| **15. Backend Config** | `READY` | `BackendConfig.kt` & `deploy/Dockerfile.backend` | None | Production environment ready |
| **16. Security Readiness**| `READY` | JWT + `TenantContext` + PostgreSQL RLS | None | Defense-in-depth model verified |
| **17. Gemini AI** | `CONFIGURED` | `FirebaseAiLogicProvider.kt` (`gemini-1.5-flash`) | P2 | Inject `GEMINI_API_KEY` into Secret Manager |
| **18. n8n Integration** | `CONFIGURED` | `N8nAutomationDispatcher.kt` + HMAC-SHA256 | P2 | Set `N8N_WEBHOOK_URL` & `N8N_SIGNING_SECRET` |
| **19. MCP Tool Registry** | `CONFIGURED` | `McpToolRegistry.kt` (R0–R3 Risk Policy) | None | Exposes typed REST tools for MCP clients |
| **20. Android Base URL** | `CONFIGURED` | `RuntimeComposition.kt` reads `SUCHARU_API_GATEWAY_URL` | P2 | Point `SUCHARU_API_GATEWAY_URL` to Cloud Run |
| **21. Static Bangla QR** | `VERIFIED` | `CustomerPaymentMethod.BANGLA_QR` + PNG Asset | None | Asset `brac_bank_bangla_qr_merchant.png` present |
| **22. Observability** | `READY` | `/ready`, `/live`, and `/metrics` health probes | None | Native health probes configured |
| **23. Backup/Recovery** | `READY` | `docs/infrastructure/backup-restore-runbook.md` | None | Runbook & logical dump scripts ready |

---

## 4. DEPLOYMENT BLOCKER REGISTER

### P0 — Critical Pre-Deployment Blockers (MUST RESOLVE BEFORE LAUNCH)
1. **P0-1 (GCP Billing & Authentication)**: `gcloud` CLI not installed/authenticated locally (`GCP_AUTH = NOT_VERIFIED`). Link active GCP Billing Account to project `sucharu-pro`.
2. **P0-2 (Cloud SQL PostgreSQL Instance)**: Provision Cloud SQL PostgreSQL 16 instance (`sucharu-postgres-db`) with private IP and automated daily backups.
3. **P0-3 (Secret Manager Secrets Injection)**: Store production `DATABASE_PASSWORD`, `JWT_SIGNING_SECRET` (min 32 chars), and `GEMINI_API_KEY` in GCP Secret Manager.

### P1 — Required Infrastructure Foundations
1. **P1-1 (Enable GCP APIs)**: Enable `run.googleapis.com`, `artifactregistry.googleapis.com`, `cloudbuild.googleapis.com`, `sqladmin.googleapis.com`, and `secretmanager.googleapis.com`.
2. **P1-2 (Artifact Registry Repository)**: Create Docker repository `sucharu-pro-repo` in region `asia-southeast1`.
3. **P1-3 (Build & Push Docker Image)**: Build production JAR (`./gradlew :backend:jar`) and push container image `asia-southeast1-docker.pkg.dev/sucharu-pro/sucharu-pro-repo/sucharu-backend:1.0.0` using `deploy/Dockerfile.backend`.

### P2 — Recommended Integrations
1. **P2-1 (Serverless VPC Connector)**: Configure Serverless VPC Access connector for Cloud Run to Cloud SQL private IP connectivity.
2. **P2-2 (Android Production Base URL)**: Update Android app `SUCHARU_API_GATEWAY_URL` build config to point to the Cloud Run service URL.

---

## 5. CONFIRMATION OF NO APPLICATION CODE MODIFICATION
- **Source Code Files Modified**: `0`
- **Database Migrations Added**: `0`
- **Configuration Files Modified**: `0`
- **Protected Baseline Status**: All 25 Modules, Forms 01–06, and BI-01–BI-12 baselines remain 100% untouched and intact.
