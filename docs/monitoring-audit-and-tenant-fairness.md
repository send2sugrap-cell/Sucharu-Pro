# Monitoring, Audit Logging & Tenant Fairness

## 1. Multi-Tenant Fairness Throttler
To prevent high-volume tenants from monopolizing background worker capacity:
- `TenantFairnessThrottler` tracks active in-flight executions per `projectId`.
- If a tenant exceeds their concurrency limit (`maxConcurrentJobsPerTenant`), excess jobs remain in `QUEUED` status and are deferred to subsequent worker poll cycles.

## 2. Observability & Metrics
`JobMetrics` provides thread-safe observability metrics:
- `jobsEnqueued`, `jobsSucceeded`, `jobsFailed`, `jobsRetried`, `deadLetterCount`.
- Execution durations recorded by job type.
- Active in-flight worker count.

## 3. Audit Logging
`JobAuditLogger` produces structured audit logs for administrative interventions:
- Logged operations: `ENQUEUE`, `CLAIM`, `EXECUTE_START`, `EXECUTE_SUCCESS`, `RETRY_SCHEDULED`, `DEAD_LETTER`, `CANCEL`, `MANUAL_REPLAY`.
- **Sensitive Credential Scrubbing**: Automatically scrubs passwords, bearer tokens, API keys, and authorization headers from audit payloads and logs.
