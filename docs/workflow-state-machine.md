# Workflow State Machine & Lifecycle

## State Definitions
The workflow lifecycle enforces 12 discrete states:
1. `DRAFT`: Workflow created but not yet published or running.
2. `ACTIVE`: Workflow published and ready for execution.
3. `RUNNING`: Workflow actively processing a step.
4. `WAITING`: Paused waiting for external event or timer.
5. `WAITING_APPROVAL`: Paused at a human approval checkpoint.
6. `PAUSED`: Temporarily suspended by operator.
7. `COMPLETED`: Terminal state upon successful completion of all steps.
8. `FAILED`: Workflow encountered an error and stopped.
9. `COMPENSATING`: Executing reverse saga compensation.
10. `CANCELLED`: Terminal state upon manual cancellation.
11. `TIMED_OUT`: Workflow exceeded total timeout duration.
12. `DEAD_LETTER`: Workflow unrecoverable or compensation failed; awaiting admin replay.

## Transition Rules
| Source Status | Permitted Destination Statuses |
|---|---|
| `DRAFT` | `ACTIVE`, `RUNNING`, `CANCELLED` |
| `ACTIVE` | `RUNNING`, `PAUSED`, `CANCELLED` |
| `RUNNING` | `WAITING`, `WAITING_APPROVAL`, `PAUSED`, `COMPLETED`, `FAILED`, `COMPENSATING`, `CANCELLED`, `TIMED_OUT`, `DEAD_LETTER` |
| `WAITING` | `RUNNING`, `PAUSED`, `CANCELLED`, `TIMED_OUT`, `FAILED` |
| `WAITING_APPROVAL` | `RUNNING`, `PAUSED`, `CANCELLED`, `FAILED`, `COMPENSATING`, `TIMED_OUT` |
| `PAUSED` | `RUNNING`, `ACTIVE`, `CANCELLED` |
| `COMPENSATING` | `FAILED`, `DEAD_LETTER` |
| `DEAD_LETTER` | `RUNNING`, `CANCELLED` |
| `FAILED` | `COMPENSATING`, `DEAD_LETTER` |
| `TIMED_OUT` | `COMPENSATING`, `DEAD_LETTER` |
| `COMPLETED` | *(None - Terminal)* |
| `CANCELLED` | *(None - Terminal)* |
