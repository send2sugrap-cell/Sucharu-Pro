# Workflow Background Job Integration

## Overview
Asynchronous, long-running, or resource-heavy steps (e.g. RIP processing, laser plate imaging, bulk notification dispatch) are delegated to the PostgreSQL background job platform implemented in INFRA-04 Step 04.

## `WorkflowJobStepAdapter` Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                     Workflow Step Engine                    │
│                 (Step Type: WorkflowStepType.JOB)           │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 WorkflowJobStepAdapter                      │
│   • Generates Idempotency Key:                              │
│     "wf:job:<workflowId>:<stepId>:<executionId>"            │
│   • Propagates Correlation ID, Causation ID, Actor, Tenant  │
│   • Sets JobTriggerType = WORKFLOW                          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   PostgreSQL Job Queue                      │
│            (background_jobs, job_executions)                │
└─────────────────────────────────────────────────────────────┘
```

## Completion Callback & Workflow Resumption
1. When worker workers finish executing the background job successfully (`JobStatus.SUCCEEDED`), a completion event is dispatched.
2. The orchestrator receives the job output metadata, attaches it to the workflow execution context, and advances the workflow to the subsequent step.
3. If the background job exhausts retries and transitions to `JobStatus.DEAD_LETTER`, the orchestrator marks the step as failed and triggers the Saga Compensation Engine.
