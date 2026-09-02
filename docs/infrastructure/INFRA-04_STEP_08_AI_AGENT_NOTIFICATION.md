# INFRA-04 Step 08 — AI Agent Event Integration & Conversational Notification Boundary

## 1. Executive Summary
INFRA-04 Step 08 establishes the production-grade, fail-closed security and operational boundary between the Sucharu AI Agent platform and the Sucharu Pro notification delivery architecture.

The AI Agent is treated strictly as a **machine principal** (`PrincipalType.AI_AGENT`, `UserRole.AI_AGENT`) that operates under a **deny-by-default** security posture. It cannot autonomously dispatch high-impact notifications, access raw event stores or secrets, bypass tenant boundaries, or disable mandatory security alerts.

## 2. Core Architectural Components

### 2.1 AI Agent Notification Security Boundary
The [`AiAgentNotificationSecurityBoundary`](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/data/notification/ai/AiAgentNotificationSecurityBoundary.kt) mediates every incoming AI Agent request. It deterministically validates:
1. Machine principal authenticity (`isAiAgent == true`).
2. Server-authoritative tenant isolation (`principal.projectId == serverProjectId`).
3. Explicit capability check (e.g. `AI_CREATE_NOTIFICATION_DRAFT`, `AI_REQUEST_NOTIFICATION_SEND`).
4. Sensitive event category blocking (auth failures, passwords, session management, payment details).
5. Payload sanitization & credential leak detection.
6. Rate limiting (tenant-isolated per agent and per project).
7. Suppression status.
8. Immutable security notification guarantees.

### 2.2 Data-Minimized Event View
AI Agents never receive raw `DomainEvent` or `EventEnvelope` instances. The [`AiAgentNotificationEventConsumer`](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/data/notification/ai/AiAgentNotificationEventConsumer.kt) strips internal metadata, passwords, and sensitive fields into [`AiNotificationEventView`](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/domain/notification/ai/AiNotificationModels.kt).

### 2.3 Action Gateway & Draft vs. Execution Separation
The [`AiNotificationActionGateway`](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/data/notification/ai/AiNotificationActionGateway.kt) enforces strict separation between:
- `CREATE_DRAFT`: Generating, explaining, or proposing notifications (read/draft only, zero delivery side-effects).
- `REQUEST_SEND`, `REQUEST_REPLAY`, `REQUEST_SUPPRESSION`, `REQUEST_PREFERENCE_UPDATE`: High-impact actions requiring explicit human confirmation.

### 2.4 Human-In-The-Loop Confirmation Lifecycle
The [`AiNotificationConfirmationService`](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/data/notification/ai/AiNotificationConfirmationService.kt) manages human approvals:
- Requires a human approver holding `MANAGER` or `ADMIN` role.
- Prevents self-approval (requester != approver).
- Rejects expired, replayed, or cross-tenant confirmations.
- Associates verified confirmation IDs directly with action execution.

### 2.5 Idempotency & Rate Limiting
- Unique key: `(projectId, agentId, actionType, idempotencyKey)` stored in `ai_notification_action_records`.
- Replayed identical requests return cached execution summaries without triggering duplicate deliveries.
- Tenant-isolated sliding window rate limits protect against automated conversational loops.

### 2.6 Append-Only Audit Logging
Every interaction (context read, draft creation, confirmation request, approval, denial, execution, replay) is recorded in `ai_notification_audit` with zero credential leakage.

## 3. Database Schema
Migration [`V20260911__ai_agent_notification_boundary.sql`](file:///e:/App/Sucharu%20Pro/app/src/main/resources/db/migration/V20260911__ai_agent_notification_boundary.sql):
- `ai_notification_action_records`: Idempotency tracking with PostgreSQL RLS.
- `ai_notification_confirmations`: Human confirmation records with expiration and RLS.
- `ai_notification_audit`: Immutable audit log table with RLS and `UPDATE`/`DELETE` denied.
