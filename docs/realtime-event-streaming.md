# Real-Time Event Streaming Architecture

## Overview
Sucharu Pro streams live domain events to connected web and mobile clients via WebSocket or Server-Sent Events (SSE).

## Security & Tenant Isolation
- **Topic Formatting**: All stream topics strictly follow `tenant.{projectId}.{aggregateType}.{aggregateId}`.
- **Cross-Tenant Denial**: A client session connected under `tenant_alpha` is prevented from subscribing to topics prefixed with any other tenant.
- **Security Event Filtering**: Authentication, password change, and security audit events are strictly excluded from real-time broadcasting.
- **Data Minimization**: Emits `RealTimeEventFrame` with high-level summaries rather than full internal database state.
- **Ephemeral Delivery**: Disconnected clients or network drops do not trigger endless database outbox retries.
