# AI Notification Security & Privacy

## Principles
1. **Deny By Default**: AI Agents hold no implicit notification dispatch or replay rights.
2. **Data Minimization**: Raw event envelopes and payload stack traces are never exposed.
3. **Zero Secret Leakage**: Passwords, tokens, JWTs, and API keys are blocked and stripped.
4. **Tenant Isolation**: Cross-tenant requests are denied immediately with fail-closed security.
