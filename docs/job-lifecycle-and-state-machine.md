# Job Lifecycle & State Machine

## Job Status State Machine
```
       +--------------+
       |   PENDING    | (Waiting for DAG dependencies or future scheduled time)
       +-------+------+
               | (Dependencies satisfied / available_at reached)
               v
       +-------+------+
       |    QUEUED    |<----------------------------+
       +-------+------+                             |
               | (Claimed via SKIP LOCKED)          |
               v                                    |
       +-------+------+                             |
       |   CLAIMED    |                             |
       +-------+------+                             |
               | (Execution started)                |
               v                                    |
       +-------+------+                             |
       |   RUNNING    |                             |
       +-------+------+                             |
        /      |     \                              |
       /       |      \                             |
(Success)  (Transient) (Exhausted/Non-retryable)    |
     /         |        \                           |
    v          v         v                          |
+---------+ +---------+ +-------------+             |
|SUCCEEDED| |  RETRY  | | DEAD_LETTER |             |
+---------+ |_SCHEDULE| +------+------+             |
            +----+----+        |                    |
                 |             | (Admin Replay)     |
                 +-------------+--------------------+
```

## State Definitions
- `PENDING`: Initial state when blocked by upstream DAG steps.
- `QUEUED`: Ready for immediate claiming by background workers.
- `CLAIMED`: Leased by an active worker node.
- `RUNNING`: Actively executing in the handler.
- `SUCCEEDED`: Terminal success state with execution history recorded.
- `RETRY_SCHEDULED`: Failed transiently; waiting for exponential backoff delay before next claim.
- `DEAD_LETTER`: Quarantined after exhausting maximum retries or upon non-retryable fatal failure.
- `CANCELLED`: Administratively cancelled before execution.
