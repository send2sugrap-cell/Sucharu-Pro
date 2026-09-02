# Retry Backoff & Dead-Letter Queue

## 1. Retry Engine
`JobRetryEngine` implements exponential backoff with full jitter to avoid synchronous retry spikes against external services and internal databases.

```
delay = min(maxBackoffMs, initialBackoffMs * (multiplier ^ attempt))
jitteredDelay = delay * (1 - jitterFactor + random() * jitterFactor)
```

## 2. Failure Classification
- `TRANSIENT`: Network timeouts, rate limits, lock contentions -> Retry scheduled with exponential backoff.
- `NON_RETRYABLE`: Schema violations, validation failures, invalid arguments -> Immediately moved to Dead-Letter Queue.

## 3. Dead-Letter Queue & Administrative Operations
Jobs that exceed `max_attempts` or encounter `NON_RETRYABLE` errors are quarantined into `job_dead_letters`:
- **Quarantine Reason**: Preserves exact stack trace, error classification, and failing payload.
- **Admin Replay**: Admins can inspect failed jobs and trigger `operationsService.retryDeadLetterJob()`, resetting the job for re-execution under full audit logging.
- **Admin Cancellation**: Unwanted jobs can be resolved without replay.
