# AI Agent Event Integration Architecture

## Overview
The Sucharu AI Agent platform consumes ERP domain events strictly through capability-based boundaries, ensuring AI agents act as intelligent assistants without unauthorized access or rogue state modifications.

## Rules & Security Controls
1. **Machine Principal Only**: The consumer verifies `principal.role == UserRole.AI_AGENT` and `principal.principalType == PrincipalType.AI_AGENT`.
2. **Tenant Scoping**: `principal.projectId` must match `envelope.projectId`.
3. **Capability Mapping**: Access is granted only if the AI Agent holds the required capability:
   - `ORDER_CREATED`, `ORDER_UPDATED` $\rightarrow$ `AI_READ_ORDER_CONTEXT`
   - `CUSTOMER_CREATED` $\rightarrow$ `AI_READ_CUSTOMER_CONTEXT`
   - `AFFILIATE_COMMISSION_EARNED` $\rightarrow$ `AI_READ_AFFILIATE_CONTEXT`
   - `INVOICE_CREATED` $\rightarrow$ `AI_READ_INVOICE`
4. **Strictly Read-Only**: Event consumption is read-only. AI Agents cannot directly mutate database records or emit raw domain events.
5. **Sensitive Event Blocking**: Security events, password modifications, authentication failures, and raw payment records are blocked.
6. **Data Minimization**: Secret keys, tokens, and internal operational metadata are stripped from `AiAgentEventFrame`.
7. **Human Confirmation**: Workflows requiring human authorization preserve `HumanConfirmationMetadata` (`requiresConfirmation`, `confirmationId`, `requestedByAgentId`, `approvedByHumanId`).
