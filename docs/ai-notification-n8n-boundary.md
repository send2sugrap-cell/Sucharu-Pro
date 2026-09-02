# AI Notification & n8n Integration Boundary

## Separation of Concerns
1. AI Agent **NEVER** interacts directly with n8n webhooks or credentials.
2. AI Agent requests standard Sucharu domain actions.
3. Sucharu Pro core publishes domain events.
4. The certified n8n integration adapter (INFRA-04 Step 03) listens to domain events and performs HMAC-signed dispatches.
