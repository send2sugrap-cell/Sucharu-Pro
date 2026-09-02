# SUCHARU PRO — INFRA-04 STEP 05: WORKFLOW ORCHESTRATION, SAGA & HUMAN-IN-THE-LOOP

## 1. Executive Summary
INFRA-04 Step 05 establishes a production-grade, multi-tenant, server-authoritative **Workflow Orchestration, Saga Compensation, Approval Engine & Human-in-the-Loop Foundation** for Sucharu Pro (Commercial Printing ERP).

This infrastructure layer coordinates complex, multi-step printing business processes across:
`Domain Events` → `Transactional Outbox` → `Event Consumers` → `Background Jobs` → `Notifications` → `n8n Webhooks` → `AI Agent Integration`.

---

## 2. Core Capabilities Implemented

### 2.1 Workflow State Machine (12 Strict States)
```
DRAFT ──> ACTIVE ──> RUNNING ──┬──> WAITING
                               ├──> WAITING_APPROVAL
                               ├──> PAUSED
                               ├──> COMPLETED (Terminal)
                               ├──> FAILED ──┬──> COMPENSATING ──> DEAD_LETTER (Admin Replay)
                               ├──> TIMED_OUT│
                               └──> CANCELLED (Terminal)
```
- **State Transition Matrix**: Guaranteed atomic forward and backward transitions in `workflow_transitions` with strict validation.
- **Terminal States**: `COMPLETED`, `CANCELLED` are immutable and terminal.
- **Dead-Letter State**: Supports replay by system administrators with authenticated audit logging.

### 2.2 Reusable Declarative Step Engine (`WorkflowStepEngine`)
Supports 10 step execution types without coupling to specific business domain modules:
1. `ACTION`: In-process deterministic execution.
2. `EVENT_WAIT`: Event correlation pause/resume barrier.
3. `JOB`: Offloads long-running asynchronous tasks (PDF rendering, laser plate creation, inventory reservation) to the PostgreSQL job queue.
4. `CONDITION`: Context-driven branching logic (`true`/`false` paths).
5. `DELAY`: Timestamp/duration-based pausing.
6. `APPROVAL`: Human approval checkpoint with threshold evaluation.
7. `NOTIFICATION`: Multi-channel customer/staff notification dispatch.
8. `WEBHOOK`: HMAC-signed outbound HTTP callbacks (n8n/external services).
9. `COMPENSATION`: Explicit reverse compensation step.
10. `END`: Clean termination step.

### 2.3 Saga & Reverse Compensation Engine (`SagaCompensationEngine`)
- **Topological Reversal**: Completed steps in `StepExecutionStatus.SUCCEEDED` are rolled back in reverse completion order (`completedAt DESC`).
- **Failure Classification**: Records root causes (`WorkflowFailureRecord`) and marks uncompensable steps for administrator dead-letter review.

### 2.4 Human-in-the-Loop & Approval Engine (`ApprovalEngine`)
- **Separation of Duties (SoD)**: Requesters cannot approve their own requests when `allowSelfApproval == false`.
- **Machine Principal Security Fence**: `PrincipalType.AI_AGENT` and `UserRole.AI_AGENT` are strictly denied from approving workflow requests.
- **Role Hierarchy & Escalation**: Automatic timeout routing or manual escalation to supervisory roles (e.g. `STAFF` -> `MANAGER` -> `ADMIN`).
- **Multi-Approver Thresholds**: Supports M-of-N quorum policies.

### 2.5 AI Agent Machine Principal Boundary (`AiAgentWorkflowSecurityBoundary`)
- **Capability Whitelisting**: Limits automated agent workflow initiation to registered printing capabilities (e.g. `order.analyze_print_specs`, `estimate.calculate_imposition`).
- **Human Confirmation Gate**: High-impact actions (bulk refunds, financial disbursements) strictly require verified human approval metadata (`confirmationId`, `approvedByHumanId`).
- **Cross-Tenant Prevention**: Machine principal cannot execute workflows across tenant boundaries.

### 2.6 PostgreSQL 16 Schema with Row-Level Security (RLS)
Created `V20260908__create_workflow_orchestration_and_approval_tables.sql` defining 12 tables with mandatory `tenant_isolation_policy` on `project_id = CURRENT_SETTING('app.current_project_id')`:
1. `workflow_definitions`
2. `workflow_versions`
3. `workflow_instances`
4. `workflow_steps`
5. `workflow_step_executions`
6. `workflow_transitions`
7. `workflow_variables`
8. `workflow_compensations`
9. `workflow_approval_policies`
10. `workflow_approval_requests`
11. `workflow_approval_decisions`
12. `workflow_escalations`

---

## 3. Verification & Production Certification
- **Unit & Integration Tests**: 24/24 workflow orchestration tests passing.
- **Full Regression Suite**: 97/97 INFRA-04 tests passing with zero failures.
- **Android Compilation**: `./gradlew.bat assembleDebug` -> **100% BUILD SUCCESSFUL**.
