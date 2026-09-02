# Observability Architecture

## Principles
1. **Non-Blocking Telemetry**: Metric collection uses atomic counters and lock-free concurrency. Telemetry failures never disrupt business operations.
2. **Zero Credential Leakage**: Logs, metrics, and traces are strictly sanitized before recording.
3. **Tenant-Aware**: Multi-tenant metrics and alerts are isolated using server-authoritative tenant context and PostgreSQL Row Level Security (RLS).
4. **AI Agent Boundary**: AI Agents receive high-level safe operational summaries and are denied raw telemetry or database internals.
