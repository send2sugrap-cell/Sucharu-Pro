# Role Workspace Routing

## Overview

Sucharu Pro routes authenticated users to role-specific workspaces using only the server-authoritative `AuthenticatedPrincipal`. Client-supplied role hints are always discarded.

---

## Default Entry Destinations by Role

| Role | Entry Destination | Route |
|---|---|---|
| GUEST (no auth) | Public Home | `public/home` |
| CUSTOMER | Customer Dashboard | `customer/home` |
| AFFILIATE | Affiliate Dashboard | `affiliate/home` |
| STAFF | Assigned Work | `staff/assigned-work` |
| MANAGER | Operations Overview | `manager/operations` |
| ADMIN | System Control Center | `admin/dashboard` |
| AI_AGENT | **FORBIDDEN** — hard-blocked | N/A |

---

## Routing Decision Authority

```
AppNavigationManager
└── reads: AuthenticatedPrincipal.role (from verified JWT via AuthenticationSessionManager)
└── never reads: client-local SharedPreferences role hint
└── delegates to: DeepLinkAuthorizer for route-level authorization
```

---

## Role Escalation / Cross-Workspace Access

Only `ADMIN` may access cross-role routes for management purposes (`customer/*`, `affiliate/*`).

All other role cross-workspace attempts resolve to `AppDestination.Security.Forbidden`.

---

## Workspace Navigation Items

### Customer Workspace
| Item | Destination | Required Capability |
|---|---|---|
| Dashboard | `customer/home` | `READ_OWN_IDENTITY` |
| My Orders | `customer/orders` | `READ_OWN_ORDERS` |
| Invoices | `customer/invoices` | `READ_OWN_INVOICES` |
| Payments | `customer/payments` | `READ_OWN_PAYMENTS` |
| Delivery Tracking | `customer/delivery` | `READ_OWN_DELIVERIES` |
| Returns | `customer/returns` | `READ_OWN_RETURNS` |
| AI Assistant | `customer/ai-assistant` | `PUBLIC_USE_AI_ASSISTANT` |
| Session Security | `customer/sessions` | `READ_OWN_SESSIONS` |

### Affiliate Workspace
| Item | Destination | Required Capability |
|---|---|---|
| Dashboard | `affiliate/home` | `READ_OWN_AFFILIATE_PROFILE` |
| Referral Links | `affiliate/referral-links` | `READ_OWN_REFERRALS` |
| Commission | `affiliate/commission` | `READ_OWN_COMMISSIONS` |
| Payouts | `affiliate/payouts` | `READ_OWN_COMMISSIONS` |
| Performance | `affiliate/performance` | `READ_OWN_AFFILIATE_PROFILE` |

### Staff Workspace
| Item | Destination | Required Capability |
|---|---|---|
| Assigned Work | `staff/assigned-work` | `STAFF_READ_ORDERS` |
| Production | `staff/production` | `STAFF_READ_ORDERS` |
| QC | `staff/qc` | `STAFF_READ_QC` |
| Inventory | `staff/inventory` | `STAFF_READ_INVENTORY` |
| Delivery | `staff/delivery` | `STAFF_READ_DELIVERY` |

### Manager Workspace
| Item | Destination | Required Capability |
|---|---|---|
| Operations Overview | `manager/operations` | `MANAGER_VIEW_OPERATIONAL_ANALYTICS` |
| Pending Approvals | `manager/approvals` | `MANAGER_APPROVE_ORDER` |
| Financial Summary | `manager/finance` | `MANAGER_VIEW_FINANCIAL_SUMMARY` |
| Reports | `manager/reports` | `MANAGER_VIEW_OPERATIONAL_ANALYTICS` |

### Admin Workspace
| Item | Destination | Required Capability |
|---|---|---|
| System Control | `admin/dashboard` | `ADMIN_ALL` |
| User Management | `admin/users` | `ADMIN_MANAGE_USERS` |
| Roles & Capabilities | `admin/roles` | `ADMIN_MANAGE_ROLES` |
| Security Audit | `admin/security` | `ADMIN_VIEW_AUDIT` |
| Configuration | `admin/configuration` | `ADMIN_MANAGE_SYSTEM_CONFIGURATION` |
| Infrastructure Health | `admin/monitoring` | `ADMIN_ALL` |
