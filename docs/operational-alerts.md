# Operational Alerts

## Lifecycle & Deduplication
- Alerts are deduplicated by `(projectId, subsystem, alertKey)`.
- Re-occurring failures increment occurrence counters on the existing `OPEN` alert.
- Resolving a condition transitions status to `RESOLVED`.
