# Event Infrastructure Observability

## Metrics & Health
- **Outbox Backlog Depth**: Monitored via `EventInfrastructureHealthEvaluator`.
- **Latency**: Tracks publication latency and consumer execution latency.
- **Dead-Letter Monitoring**: Flags status as `DEGRADED` (>0) or `CRITICAL` (>50).
