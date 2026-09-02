# n8n Automation Integration Architecture

## Overview
Sucharu Pro integrates with n8n workflow automations via secure, HMAC-SHA256-signed webhooks.

## Architecture
1. **Sanitized Payload**: `N8nPayloadBuilder` generates `N8nWebhookPayload` using `N8nIntegrationBoundary`, stripping tokens, passwords, and secret metadata.
2. **HMAC Signing**: Computes an HMAC-SHA256 signature using the server-authoritative `signingSecret`. Transmitted via `X-Sucharu-Signature` header.
3. **Trace Headers**: Outgoing requests include `X-Sucharu-Event-Id`, `X-Sucharu-Project-Id`, `X-Sucharu-Correlation-Id`, and `X-Sucharu-Timestamp`.
4. **Failure Classification**:
   - HTTP 2xx: `EventConsumerResult.Success`.
   - HTTP 401/403: `EventConsumerResult.Failure` (`SECURITY`, non-retryable).
   - HTTP 4xx: `EventConsumerResult.Failure` (`VALIDATION`, non-retryable).
   - HTTP 5xx / Network Timeout: `EventConsumerResult.Failure` (`TRANSIENT`, retryable).
5. **Security Events**: Internal authentication and security events are rejected at the boundary and never dispatched to n8n.
