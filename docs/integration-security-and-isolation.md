# Integration Security, Tenant Isolation & Idempotency

## 1. Multi-Tenant Row-Level Security (RLS)
- All integration delivery records are persisted in PostgreSQL table `integration_delivery_records` protected by RLS policy:
  ```sql
  ALTER TABLE integration_delivery_records ENABLE ROW LEVEL SECURITY;
  CREATE POLICY integration_delivery_tenant_isolation ON integration_delivery_records
      FOR ALL
      USING (project_id = CURRENT_SETTING('app.current_project_id', true));
  ```
- Any cross-tenant read or write query is denied at both the application level (`PostgresIntegrationDeliveryRepository`) and the PostgreSQL database engine level.

## 2. Idempotency Guarantees
- **Internal Consumers**: Managed by `PostgresEventIdempotencyStore` keyed on `(project_id, consumer_id, event_id)`.
- **Notification Deliveries**: Keyed on `(projectId, eventId, recipientId, channel)`.
- **Integration Deliveries**: Unique database constraint on `(project_id, consumer_id, event_id)`.

## 3. Machine Principal Isolation
- `AI_AGENT` is strictly a machine principal, never an interactive user.
- Machine principals cannot spoof `projectId`, `userId`, or `actorId`.
- Machine principals are prevented from performing arbitrary business mutations.

## 4. Secret & Credential Sanitization
- Passwords, JWT tokens, private keys, gateway credentials, and API secrets are stripped across:
  - Event payload serialization
  - Notification intent resolution
  - Real-time event framing
  - n8n webhook payloads
  - AI Agent event frames
  - Integration audit logs
