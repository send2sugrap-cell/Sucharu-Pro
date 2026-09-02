# AI Notification Action Gateway

## Purpose
Acts as the authoritative mediator between AI action requests and notification infrastructure.

## Key Features
- **Deterministic Idempotency**: Deduplicates repeated AI calls based on `(projectId, agentId, actionType, idempotencyKey)`.
- **Draft vs Execution Routing**: Prevents accidental dispatch during draft generation.
- **Unified Delivery Integration**: Dispatches confirmed notifications directly through `NotificationDispatchService`.
