# AI Agent & n8n Background Execution Security Boundary

## 1. Machine Principal (AI_AGENT) Controls
AI Agents operate as `PrincipalType.AI_AGENT` machine principals:
- **Strict Tenant Bound**: Must match the target `projectId` in `TenantContext`.
- **Capability Whitelist**: May only trigger approved asynchronous job types (e.g. `order.analyze_print_specifications`, `inventory.forecast_demand`, `report.generate_summary`).
- **Human-in-the-Loop Confirmation**: High-impact or destructive jobs (e.g. `finance.execute_bulk_payout`, `system.maintenance_purge`) require verified human confirmation metadata (`confirmationId`, `approvedByHumanId`) before enqueuing.
- **System Isolation**: AI Agents cannot execute raw SQL, modify table schemas, or access host operating system commands.

## 2. n8n Integration Webhook Security
External webhooks from n8n automation workflows are processed through `N8nJobTriggerAdapter`:
- **HMAC-SHA256 Signature Verification**: Every incoming payload must have a valid `X-N8N-Signature` calculated with the shared tenant signing secret.
- **Replay Protection**: Timestamps older than 5 minutes (`300000ms`) are rejected.
- **Sanitized Job Enqueueing**: Authenticated webhooks are converted into server-managed background jobs with strict tenant context.
