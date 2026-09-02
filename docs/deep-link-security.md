# Deep Link Security

## Overview

Sucharu Pro supports deep links that route users to specific screens (e.g., from email notifications, push notifications, or external marketing links). Deep link handling is a high-risk attack surface that requires careful security design.

---

## Threat Model

| Threat | Mitigation |
|---|---|
| Unauthorized resource access via crafted deep link | `DeepLinkAuthorizer` evaluates all deep links against `AuthenticatedPrincipal` before navigation |
| IDOR (Insecure Direct Object Reference) | Ownership verification is applied to parameterized routes (orders, invoices, referrals) |
| Role escalation via crafted route prefix | Route prefix checks are followed by explicit `principal.role` verification |
| Client-side role spoofing | `clientSuppliedRole` parameter is silently discarded; only server JWT role is used |
| AI_AGENT machine principal accessing human UI | Hard-blocked unconditionally before any route evaluation |
| Unauthenticated access to protected routes | `null` principal is redirected to Login before any protected route is evaluated |
| Post-logout back navigation to protected screen | Back-stack is cleared atomically on every logout/session-expiry event |

---

## Deep Link Authorization Algorithm

```
authorizeDeepLink(route, principal, ?, ?, ?)
                                  ↑
                  clientSuppliedUserId / projectId / role
                  ──── DISCARDED ────
```

1. **Normalize**: Strip leading `/`, trim whitespace
2. **Public check**: If route matches any `Public.*` pattern → allow without authentication
3. **Authentication gate**: If `principal == null` → redirect to Login
4. **AI_AGENT gate**: If `principal.role == AI_AGENT` → Forbidden (hard-block)
5. **Anti-spoofing firewall**: Discard all client-supplied hints
6. **Role namespace check**: Verify `principal.role` has access to the route namespace
7. **Capability check**: Verify `RoleCapabilityMatrix` for the specific capability
8. **Ownership check**: For parameterized routes, verify the resource belongs to the principal
9. **Return**: Authorized `AppDestination` or `Security.Forbidden` / `Security.NotFound`

---

## Ownership Checks

Parameterized deep links that reference specific resources perform ownership enforcement:

| Route Pattern | Check |
|---|---|
| `customer/orders/{id}` | `orderId` must not indicate a different customer's resource |
| `customer/invoices/{id}` | `invoiceId` must not indicate a different customer's resource |
| `affiliate/referrals/{id}` | `refId` must not indicate a different affiliate's resource |
| `affiliate/commission/{id}` | `commId` must not indicate a different affiliate's resource |
| `admin/users/{id}` | Accessible only to ADMIN role — no further ownership check needed |

> [!NOTE]
> These checks serve as a client-side defense-in-depth layer. Definitive ownership enforcement is always performed server-side in the backend API before any data is returned.

---

## Deep Link Entry Points

Deep links may arrive from:
- **Push notifications** → FCM payload contains target route
- **Email links** → marketing/transactional emails with app deep link URIs
- **Share links** → in-app content shared externally
- **n8n automation** → AI agent platform may dispatch notification-triggered deep links to human users

In all cases, the same `authorizeDeepLink()` path is followed. There is no bypass or shortcut for any entry point.
