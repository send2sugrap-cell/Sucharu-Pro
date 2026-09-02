# Capability-Aware Navigation

## Overview

Sucharu Pro's navigation UI is **capability-aware**: navigation items are dynamically filtered based on the `AuthorizationCapability` set held by the current `AuthenticatedPrincipal`. Items that the user lacks the capability for are hidden from menus entirely — they are not merely greyed out or disabled.

---

## Implementation

`CapabilityAwareNavigation` is a stateless Kotlin object that provides:

```kotlin
fun filterNavigationItems(
    items: List<NavigationItem>,
    principal: AuthenticatedPrincipal
): List<NavigationItem>
```

It delegates to `RoleCapabilityMatrix.hasCapability(principal.role, item.requiredCapability)` for each item. Items returning `false` are excluded from the resulting list before it is passed to any composable.

---

## Navigation Item Structure

Each navigation item carries:
- `destination: AppDestination` — the canonical typed destination
- `requiredCapability: AuthorizationCapability?` — the capability gate (`null` = public item, always visible)
- `label: String` — display text
- `icon` — visual icon reference

---

## Capability-to-Route Mapping

| Navigation Item | Required Capability |
|---|---|
| My Orders | `READ_OWN_ORDERS` |
| My Invoices | `READ_OWN_INVOICES` |
| My Payments | `READ_OWN_PAYMENTS` |
| Delivery Tracking | `READ_OWN_DELIVERIES` |
| Returns | `READ_OWN_RETURNS` |
| Session Security | `READ_OWN_SESSIONS` |
| Referral Links | `READ_OWN_REFERRALS` |
| Commission | `READ_OWN_COMMISSIONS` |
| Payouts | `READ_OWN_COMMISSIONS` |
| Pending Approvals | `MANAGER_APPROVE_ORDER` |
| Financial Summary | `MANAGER_VIEW_FINANCIAL_SUMMARY` |
| Operational Reports | `MANAGER_VIEW_OPERATIONAL_ANALYTICS` |
| User Management | `ADMIN_MANAGE_USERS` |
| Role & Capability Matrix | `ADMIN_MANAGE_ROLES` |
| Security Audit Logs | `ADMIN_VIEW_AUDIT` |
| System Configuration | `ADMIN_MANAGE_SYSTEM_CONFIGURATION` |

---

## Security Note

Capability-aware UI filtering is a **UX defense layer**, not the primary authorization boundary. Even if a client circumvents UI filtering (e.g., via a crafted deep link), the `DeepLinkAuthorizer` will independently verify capability before allowing navigation. The backend API enforces authorization a third time before returning any data.

This three-layer defense is intentional and follows defense-in-depth principles.
