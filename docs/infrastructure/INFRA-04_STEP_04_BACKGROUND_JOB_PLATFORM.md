# INFRA-04 STEP 04 — Production-Grade Event-Driven Workflow Automation, Scheduling & Background Processing

## Executive Summary
This document certifies the architectural specification and implementation of **INFRA-04 STEP 04: Production-Grade Event-Driven Workflow Automation, Scheduling, Async Job Execution & Reliable Background Processing** for **Sucharu Pro — Commercial Printing ERP**.

The background processing engine operates server-side on PostgreSQL, completely decoupled from Android client lifecycles, and provides multi-tenant isolated, horizontally scalable, idempotent, DAG workflow-capable, retry-resilient background computation.

---

## 1. Architectural Baseline & Principles
- **PostgreSQL-Backed Queue**: Uses `SELECT ... FOR UPDATE SKIP LOCKED` for lock contention-free worker claiming.
- **Server Authoritative & Multi-Tenant**: Tenant boundary enforcement via PostgreSQL Row-Level Security (`project_id`).
- **Idempotent by Design**: Deterministic idempotency keys prevent duplicate executions.
- **Exponential Backoff with Full Jitter**: Automated retry engine with configurable policies per job type.
- **DAG Workflow Dependencies**: Directed Acyclic Graph engine with DFS cycle detection.
- **Recurring Schedules & Cron**: Timezone-aware server-authoritative schedule calculator.
- **Strict Machine Principal Isolation**: AI Agents cannot bypass tenant bounds or execute high-impact actions without human confirmation.

---

## 2. Table Schemas & Row-Level Security
Five core tables created in Flyway migration `V20260907__create_background_job_execution_tables.sql`:
1. `background_jobs`: Job definitions, priority, status, lease metadata, attempts, idempotency keys.
2. `job_executions`: Append-only execution history records with worker ID, duration, and error details.
3. `job_schedules`: Cron expressions, fixed interval schedules, timezone, and next run calculation.
4. `job_dependencies`: Upstream/downstream job dependencies for DAG workflow step execution.
5. `job_dead_letters`: Quarantined failed jobs for manual review and administrative replay.

---

## 3. Subsystem Overview

| Component | Responsibility |
|---|---|
| `PostgresJobRepository` | Primary persistence, `FOR UPDATE SKIP LOCKED` job claiming, state transitions |
| `BackgroundJobWorker` | Bounded concurrent polling loop executing claimed jobs |
| `JobExecutionEngine` | Context setup, handler invocation, retry scheduling, DAG satisfaction |
| `JobScheduler` | Evaluates due recurring schedules and enqueues workflow jobs |
| `JobRetryEngine` | Exponential backoff calculation with full jitter |
| `JobDependencyManager` | Dependency satisfaction checks & DFS cycle detection |
| `AiAgentJobSecurityBoundary` | Evaluates AI Agent capabilities and human approval gates |
| `N8nJobTriggerAdapter` | Verifies HMAC-SHA256 signatures for n8n webhooks |
| `TenantFairnessThrottler` | Prevents single-tenant queue starvation |
| `JobAuditLogger` | Audit trail logging with automatic credential scrubbing |

---

## 4. Certification & Verification
- **Automated Tests**: 30 dedicated background job tests + 123 domain event / outbox tests = **153 passed unit tests**.
- **Android Assembly**: `assembleDebug` completed successfully with zero build errors.
