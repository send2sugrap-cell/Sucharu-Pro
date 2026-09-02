# n8n Automation Event Integration Boundary

## 1. Principles
The Sucharu Pro domain layer maintains zero direct coupling to n8n workflow engines.

## 2. Decoupled Payload Adaptation
- Domain events are converted to `N8nWebhookPayload` via `N8nIntegrationBoundary`.
- Zero credentials, passwords, or authentication tokens are included in outbound webhook payloads.
- Security events (passwords, sessions, auth failures) are blocked from being routed to external automation webhooks.
- Webhook endpoints are configured externally via environment infrastructure, not hardcoded in domain source code.
