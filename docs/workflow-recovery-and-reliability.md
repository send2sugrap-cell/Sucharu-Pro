# Workflow Recovery, Reliability & Dead-Letter Handling

## Reliability Mechanisms

### 1. Step Retries with Exponential Backoff
- Each `WorkflowStepDefinition` can configure a `StepRetryPolicy` defining `maxAttempts`, `initialDelayMs`, `multiplier`, and `maxDelayMs`.
- Transient failures (network timeout, DB lock contention) retry automatically with jitter.

### 2. Saga Compensation on Unrecoverable Failures
- When permanent errors occur or retry limits are exhausted, the workflow automatically transitions to `COMPENSATING` and executes reverse rollbacks.

### 3. Dead-Letter Quarantine & Safe Replay
- When compensation steps encounter errors or when unrecoverable fatal system faults occur, the workflow instance transitions to `DEAD_LETTER`.
- System administrators can inspect diagnostic error stacks, resolve external dependencies, and issue an authorized replay via `DefaultWorkflowOperationsService.replayDeadLetterWorkflow()`.

### 4. Idempotency & Duplicate Suppression
- `PostgresWorkflowIdempotencyStore` guarantees that duplicate triggers or retried webhook calls do not create multiple parallel workflow instances.
