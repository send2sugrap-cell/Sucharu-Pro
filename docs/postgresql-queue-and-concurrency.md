# PostgreSQL Queue & Concurrency Model

## 1. Concurrency Strategy
The background queue leverages PostgreSQL's `SELECT ... FOR UPDATE SKIP LOCKED` primitive within an explicit transaction context.

```sql
SELECT job_id FROM background_jobs
WHERE project_id = :projectId
  AND status IN ('QUEUED', 'RETRY_SCHEDULED')
  AND available_at <= NOW()
ORDER BY priority ASC, available_at ASC, created_at ASC
FOR UPDATE SKIP LOCKED
LIMIT :limit;
```

### Benefits:
- **Lock Contention Free**: Multiple worker nodes poll the same table without blocking each other or deadlocking.
- **Strict Priority Ordering**: Higher priority jobs (e.g. `CRITICAL`, `HIGH`) execute before lower priority jobs.
- **Fair Multi-Tenancy**: Workers query per-tenant or interleave across tenant bounds without cross-tenant data leakage.

## 2. Worker Leases and Stale Recovery
When a job is claimed:
1. `status` transitions to `CLAIMED`.
2. `locked_by` is set to the claiming `worker_id`.
3. `locked_until` is set to `NOW() + INTERVAL 'leaseDurationMs milliseconds'`.

If a worker node crashes or hangs:
- `JobLeaseRecoveryService` detects jobs where `status = 'CLAIMED'` and `locked_until < NOW()`.
- The stale lease is released and the job transitions back to `RETRY_SCHEDULED` for immediate reclamation by healthy workers.
