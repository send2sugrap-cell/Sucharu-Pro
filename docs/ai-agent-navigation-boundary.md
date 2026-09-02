# AI Agent Navigation Boundary

## Overview

Sucharu Pro's future AI Agent Platform is orchestrated through **n8n**. AI agents operate as machine principals using `UserRole.AI_AGENT`. This role is fundamentally different from all human roles and carries strict isolation guarantees.

---

## AI_AGENT Principal Characteristics

| Property | Value |
|---|---|
| `UserRole` | `AI_AGENT` |
| Principal type | Machine / non-human |
| Authentication | Server-issued JWT (separate issuance flow from human login) |
| Interactive navigation | **STRICTLY PROHIBITED** |
| API access | Permitted via dedicated backend API endpoints |
| n8n integration | Yes — automated workflow triggers |

---

## Hard Navigation Block

`DeepLinkAuthorizer` enforces an unconditional block on all `AI_AGENT` navigation requests:

```kotlin
if (principal.role == UserRole.AI_AGENT) {
    return AppDestination.Security.Forbidden
}
```

This check executes **before** any route evaluation, capability check, or ownership verification. There is no code path by which an `AI_AGENT` principal can be routed to any human-interactive workspace destination.

---

## Rationale

1. **Separation of concerns** — AI agents operate on structured API data, not interactive UIs
2. **Session isolation** — Human sessions and AI agent sessions must be strictly separated
3. **Audit clarity** — AI agent API calls are separately audited; mixing them with human navigation events would corrupt audit trails
4. **Security perimeter** — Preventing AI agents from navigating the UI eliminates an entire class of potential privilege escalation via automated UI manipulation
5. **n8n architecture** — n8n workflows communicate with Sucharu Pro exclusively through dedicated webhook/API endpoints, never through the Android navigation stack

---

## AI Agent–Initiated User Notifications

When an AI agent workflow produces output relevant to a human user (e.g., a quotation ready notification), it:

1. Calls the backend notification API with `UserRole.AI_AGENT` credentials
2. Backend creates a notification record for the target human user
3. The human user's Android client receives the push notification
4. The human user's client evaluates the deep link via `DeepLinkAuthorizer` using the **human's** `AuthenticatedPrincipal`
5. Navigation proceeds normally under the human's role and capabilities

The AI agent never navigates anything directly. It only triggers human-side navigation indirectly via notifications.

---

## Future n8n Integration Points

The following backend API endpoints are designated for AI agent consumption (not navigation):

- `POST /api/v1/agent/quotation-generate` — automated quotation drafting
- `POST /api/v1/agent/order-status-update` — automated status push
- `POST /api/v1/agent/notification-dispatch` — send notification to human user
- `GET /api/v1/agent/production-queue` — read production queue for scheduling

These endpoints require `UserRole.AI_AGENT` in the JWT and are inaccessible to human roles.
