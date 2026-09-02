# Event-Driven Workflow Integration

## Overview
Workflows in Sucharu Pro can be automatically triggered by incoming domain events or paused to wait for specific correlating business events.

## Components
1. **`EventToWorkflowTrigger`**:
   - Subscribes to published `EventEnvelope` streams from the Transactional Outbox.
   - Extracts tenant, actor, causation, correlation, and event payload.
   - Idempotently creates a `WorkflowInstance` and starts execution.

2. **`EVENT_WAIT` Step Barrier**:
   - When a workflow reaches a step of type `EVENT_WAIT`, the workflow state transitions to `WAITING`.
   - When the matching correlating event arrives (e.g. `PaymentCapturedEvent` matching `orderId`), the orchestrator resumes the workflow to `RUNNING` and advances to the next step.

## Idempotency
- Incoming event triggers derive an `idempotency_key` of format: `wf:trig:<eventId>:<definitionId>:<versionId>`.
- Duplicate event deliveries are detected at the database layer and safely suppressed without creating redundant workflow runs.
