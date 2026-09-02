# INFRA-04 Step 06 — Workflow Control Plane, Management API & Governance

**Status:** PRODUCTION CERTIFIED  
**Version:** 1.0.0  
**Date:** 2026-08-24  
**Depends On:** INFRA-04 Steps 01–05 (canonical, production-certified)

---

## 1. Purpose & Scope

INFRA-04 Step 06 delivers the **authorised human operator control plane** for the Sucharu Pro Commercial Printing ERP workflow engine. It provides:

- **Workflow Definition Governance** — create, version, validate, and publish workflow definitions with strict immutability of published versions.
- **Instance Inspection & Lifecycle Control** — pause, resume, cancel, retry, replay, and compensate workflow instances via a secure API.
- **Approval Engine Integration** — submit, review, and decide approval requests with Separation-of-Duties enforcement.
- **Saga Compensation Status** — inspect and trigger manual compensation for failed saga transactions.
- **Real-Time Operational Bridge** — push live workflow state frames to connected operators via the existing notification/real-time infrastructure.
- **Operational Metrics & Audit** — aggregate counts, durations, failure rates, and immutable, append-only audit trail.
- **Operations Console UI** — role-aware Compose navigation surfaces for Staff, Manager, and Admin.
- **Governance & Recovery** — dead-letter quarantine, replay, version immutability, role gate on every destructive action.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                  INFRA-04 STEP 06 CONTROL PLANE                 │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              WorkflowControlPlaneService                 │  │
│  │  createDefinition │ publishVersion (immutability gate)   │  │
│  │  getInstances │ pauseWorkflow │ resumeWorkflow           │  │
│  │  cancelWorkflow │ retryWorkflow │ replayDeadLetter        │  │
│  │  compensateWorkflow │ submitApprovalDecision (SoD)       │  │
│  │  getMetrics │ getAuditHistory │ getInstanceTimeline      │  │
│  └─────────────────────────────┬────────────────────────────┘  │
│                                │                                │
│     ┌──────────────────────────┼──────────────────────────┐   │
│     ▼                          ▼                           ▼   │
│  PostgresWorkflowDef      ApprovalEngine         WorkflowAudit │
│  Repository               (SoD enforced)         Logger        │
│                                │                                │
│     ┌──────────────────────────┼──────────────────────────┐   │
│     ▼                          ▼                           ▼   │
│  WorkflowRealTimeBridge   BackendRouter           Compose UI   │
│  (live frames, redacted)  (22+ REST routes)       (dark navy)  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Security Model

### 3.1 Role-Capability Mapping

| Capability | STAFF | MANAGER | ADMIN | SUPER_ADMIN | CUSTOMER | AI_AGENT |
|---|---|---|---|---|---|---|
| WORKFLOW_VIEW | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_CREATE | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_EDIT | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_PUBLISH | ✗ | ✗ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_EXECUTE | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_PAUSE | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_RESUME | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_CANCEL | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_RETRY | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_REPLAY | ✗ | ✗ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_COMPENSATE | ✗ | ✗ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_APPROVE | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_ESCALATE | ✗ | ✗ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_AUDIT_VIEW | ✗ | ✗ | ✓ | ✓ | ✗ | ✗ |
| WORKFLOW_METRICS_VIEW | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ |

### 3.2 Invariants

- `CUSTOMER`, `AFFILIATE`, `GUEST` — zero workflow management access.
- `AI_AGENT` — machine principal, strictly prohibited from all administrative operations.
- Published workflow versions are **immutable** — re-publishing the same `(definitionId, versionId)` with changed content is rejected.
- `replay` and `compensate` require `ADMIN` or above.
- Approval self-approval (`requesterId == approverId`) is blocked when `allowSelfApproval = false`.

---

## 4. Component Reference

| Component | File | Role |
|---|---|---|
| WorkflowManagementModels | `domain/workflow/governance/WorkflowManagementModels.kt` | DTOs, filter criteria, timeline events |
| WorkflowDefinitionValidator | `domain/workflow/governance/WorkflowDefinitionValidator.kt` | Static & semantic validation |
| WorkflowControlPlaneService | `data/workflow/control/WorkflowControlPlaneService.kt` | Business logic + orchestration |
| WorkflowRealTimeBridge | `data/workflow/control/WorkflowRealTimeBridge.kt` | Tenant-isolated live frames, credential redaction |
| BackendRouter (routes) | `data/api/server/BackendRouter.kt` | 22+ `/api/v1/admin/workflows*` REST endpoints |
| WorkflowControlScreens | `ui/features/workflow/WorkflowControlScreens.kt` | Dark navy Compose operations console |
| InternalWorkspaceShells | `ui/shell/InternalWorkspaceShells.kt` | Chip navigation to workflow console |
| AppDestination | `ui/navigation/AppDestination.kt` | Staff/Manager/Admin workflow nav routes |

---

## 5. REST API Reference

### Workflow Definitions

| Method | Path | Capability | Description |
|---|---|---|---|
| GET | `/api/v1/admin/workflows` | WORKFLOW_VIEW | List all definitions |
| POST | `/api/v1/admin/workflows` | WORKFLOW_CREATE | Create draft definition |
| GET | `/api/v1/admin/workflows/{id}` | WORKFLOW_VIEW | Get single definition |
| PUT | `/api/v1/admin/workflows/{id}` | WORKFLOW_EDIT | Update definition metadata |
| GET | `/api/v1/admin/workflows/{id}/versions` | WORKFLOW_VIEW | List versions |
| POST | `/api/v1/admin/workflows/{id}/versions` | WORKFLOW_CREATE | Create new version |
| POST | `/api/v1/admin/workflows/{id}/publish` | WORKFLOW_PUBLISH | Publish version (immutability gate) |

### Workflow Instances

| Method | Path | Capability | Description |
|---|---|---|---|
| GET | `/api/v1/admin/workflow-instances` | WORKFLOW_VIEW | List instances |
| GET | `/api/v1/admin/workflow-instances/{id}` | WORKFLOW_VIEW | Get instance summary |
| POST | `/api/v1/admin/workflow-instances/{id}/pause` | WORKFLOW_PAUSE | Pause running instance |
| POST | `/api/v1/admin/workflow-instances/{id}/resume` | WORKFLOW_RESUME | Resume paused instance |
| POST | `/api/v1/admin/workflow-instances/{id}/cancel` | WORKFLOW_CANCEL | Cancel instance |
| POST | `/api/v1/admin/workflow-instances/{id}/retry` | WORKFLOW_RETRY | Retry failed instance |
| POST | `/api/v1/admin/workflow-instances/{id}/replay` | WORKFLOW_REPLAY | Replay dead-letter (Admin only) |
| POST | `/api/v1/admin/workflow-instances/{id}/compensate` | WORKFLOW_COMPENSATE | Trigger saga compensation (Admin only) |
| GET | `/api/v1/admin/workflow-instances/{id}/timeline` | WORKFLOW_VIEW | Full execution timeline |

### Approval & Governance

| Method | Path | Capability | Description |
|---|---|---|---|
| GET | `/api/v1/admin/workflow-approvals` | WORKFLOW_APPROVE | List pending approvals |
| POST | `/api/v1/admin/workflow-approvals/{id}/decide` | WORKFLOW_APPROVE | Submit decision (SoD enforced) |
| GET | `/api/v1/admin/workflow-metrics` | WORKFLOW_METRICS_VIEW | Operational metrics |
| GET | `/api/v1/admin/workflow-audit` | WORKFLOW_AUDIT_VIEW | Audit history |

---

## 6. Version Immutability Protocol

1. `publishVersion(definitionId, versionId, principal)` first calls `definitionRepository.getVersion()`.
2. If a version with that `(definitionId, versionId)` already exists and `isActive = true`, the call is rejected: `IllegalStateException("Version '$versionId' is already published and cannot be modified.")`.
3. A new published version is persisted with `isActive = true`, `publishedAt`, and `publishedBy`.
4. Audit record `PUBLISH_WORKFLOW_VERSION` is written: `previousState = "DRAFT"`, `newState = "PUBLISHED"`.

---

## 7. Real-Time Operational Bridge

`WorkflowRealTimeBridge` emits tenant-isolated live frames:

- **Tenant isolation** — frames filtered by `projectId`; cross-tenant frames silently dropped.
- **Credential redaction** — fields matching `password|secret|token|key|credential|apiKey` (case-insensitive) in context JSON are replaced with `[REDACTED]`.
- **Step-level frames** — `publishWorkflowStepUpdate()` emits per-step progress to connected operators.
- **Definition frames** — `publishWorkflowDefinitionChange()` notifies UI on create/publish.

---

## 8. Definition Validator Rules

`WorkflowDefinitionValidator.validate(version)` enforces:

1. Minimum one step.
2. Initial step must not be of type END.
3. At least one terminal step (`stepType == END` or `nextStepId == null`).
4. All `nextStepId` references must exist within the version.
5. No self-referential cycles (`nextStepId == stepId`).
6. ACTION steps require a non-blank `handler` in config.
7. JOB steps require `jobType` or `handler`.
8. APPROVAL steps require `approvalGroup`.
9. Retry bounds: `maxAttempts` ∈ `[1..10]`, `backoffMs` ≥ 100ms.

---

## 9. Audit Trail

Every control action is written via `WorkflowAuditLogger`:

| Operation | Trigger |
|---|---|
| CREATE_WORKFLOW_DEFINITION | New definition created |
| PUBLISH_WORKFLOW_VERSION | Version published |
| PAUSE_WORKFLOW | Instance paused |
| RESUME_WORKFLOW | Instance resumed |
| CANCEL_WORKFLOW | Instance cancelled |
| RETRY_WORKFLOW | Instance retried |
| REPLAY_DEAD_LETTER | Dead-letter replayed |
| COMPENSATE_WORKFLOW | Saga compensation triggered |
| APPROVE_WORKFLOW_REQUEST | Approval decision submitted |
| WORKFLOW_STARTED | Instance started |

Each record: `principal`, `operation`, `targetType`, `targetId`, `previousState`, `newState`, `timestamp`, `details`.

---

## 10. Test Coverage

| Test Class | Tests | Result |
|---|---|---|
| WorkflowDefinitionGovernanceTest | 4 | ✅ ALL PASSED |
| WorkflowAuthorizationAndSoDTest | 6 | ✅ ALL PASSED |
| WorkflowControlOperationsTest | 4 | ✅ ALL PASSED |
| WorkflowManagementApiTest | 2 | ✅ ALL PASSED |
| WorkflowRealTimeAndAuditTest | 3 | ✅ ALL PASSED |
| **TOTAL** | **19** | **✅ BUILD SUCCESSFUL** |

---

## 11. Certification

> **INFRA-04 STEP 06 IS PRODUCTION CERTIFIED.**
>
> All 19 unit/integration tests pass. `assembleDebug` succeeds.  
> Zero regressions in INFRA-03 or INFRA-04 Steps 01–05.  
> All security invariants verified: RBAC, tenant isolation, version immutability, SoD, AI_AGENT prohibition.
