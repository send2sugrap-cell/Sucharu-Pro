# INFRA-04 Step 09 — Production Observability, Delivery Analytics & Operational Readiness

## 1. Executive Summary
INFRA-04 Step 09 concludes the canonical event and notification milestone of Sucharu Pro Commercial Printing ERP.
It establishes a production-grade, decoupled, non-blocking observability, delivery analytics, health evaluation, SLO/SLA monitoring, and operational alerting architecture across the entire system.

## 2. Monitored Subsystems
1. **Domain Events & Transactional Outbox**: Publication latencies, consumer execution, backlog depth, dead letters, queue age.
2. **Notification Delivery Platform**: Channel delivery rates (In-App, Push, Email, SMS), provider latencies, suppressions, rate-limit hits.
3. **External Providers**: Health snapshots, consecutive failures, circuit breaker states (CLOSED/OPEN), availability.
4. **Background Jobs**: Worker throughput, lease recovery, dead-letter monitoring, queue starvation detection.
5. **Saga Workflows**: Workflow duration, compensation rates, stuck approvals, timeout patterns.
6. **AI Agent Boundary**: Request volumes, draft creations, confirmation backlogs, rate-limit blocks, credential leak rejection rates.
7. **n8n Automation**: Webhook dispatch latencies, delivery rates, retry counts, signature verification rejections.

## 3. Cardinality Protection & Auto-Redaction
- **Metric Cardinality**: Metric labels are bounded to strictly allowed dimensions (`project`, `event_type`, `event_category`, `notification_channel`, `provider`, `job_type`, `workflow_type`, `failure_class`, `operation`, `environment`). High-cardinality IDs (`orderId`, `customerId`, `jwt`, `traceId`) are stripped from metric keys.
- **Auto-Redaction**: `StructuredObservabilityLogger` automatically redacts JWTs, API keys, passwords, bearer tokens, HMAC secrets, and sensitive customer metadata.

## 4. Operational Alert Engine & Anti-Noise
- Deduplication key: `$projectId:$subsystem:$alertKey`.
- 5-minute cooldown and aggregation window ensures that spikes (e.g. 100 provider errors in 10s) produce exactly 1 aggregated alert with occurrence counters.
- States: `OPEN` -> `ACKNOWLEDGED` -> `RESOLVED`.
