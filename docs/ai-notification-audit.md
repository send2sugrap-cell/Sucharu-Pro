# AI Notification Audit & Traceability

## Logging Requirements
- Append-only records stored in `ai_notification_audit` table.
- PostgreSQL RLS enforces tenant isolation.
- `UPDATE` and `DELETE` queries are strictly prohibited.
- Log payloads include only sanitized summaries, correlation IDs, agent IDs, and decision codes.
