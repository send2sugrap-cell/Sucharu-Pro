# AI Agent Event Integration Boundary

## 1. Principles
The Sucharu AI Agent platform interacts with domain events as a machine principal subject to strict capability and tenant authorization.

## 2. Access Rules
1. **Machine Principal Identification**: Only `PrincipalType.AI_AGENT` principals are evaluated by `AiAgentEventBoundary`.
2. **Tenant Scoping**: An AI agent cannot receive events from a project other than its authenticated `projectId`.
3. **No Wildcard Subscriptions**: Wildcard or unrestricted event listening is denied.
4. **Mandatory Capability Mapping**: Each supported event type requires an explicit capability (e.g. `AI_READ_ORDER_CONTEXT`, `AI_READ_CUSTOMER_CONTEXT`).
5. **Restricted Categories**: Administrative security events and raw financial transactions are blocked from AI agents.
6. **Data Minimization**: Secret keys and sensitive tracing tags in metadata are stripped before payload delivery.
