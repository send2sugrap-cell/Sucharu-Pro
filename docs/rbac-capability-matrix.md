# SUCHARU PRO — INFRA-03 STEP 02: RBAC CAPABILITY MATRIX

## 1. Overview

The Role-Based Access Control (RBAC) matrix defines explicit, non-overlapping capabilities assigned to each server-authoritative `UserRole`. Sucharu Pro rejects permission wildcards (`*`) to ensure zero capability overgranting.

---

## 2. Capabilities by Role

| Capability | GUEST | CUSTOMER | AFFILIATE | STAFF | MANAGER | ADMIN | AI_AGENT |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `PUBLIC_READ_PRODUCTS` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `PUBLIC_READ_PRICING` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `READ_OWN_PROFILE` | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `UPDATE_OWN_PROFILE` | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| `READ_OWN_ORDERS` | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ |
| `CREATE_ORDER` | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |
| `CANCEL_OWN_ORDER` | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |
| `READ_OWN_INVOICES` | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ |
| `READ_OWN_COMMISSIONS` | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ |
| `READ_OWN_AFFILIATE` | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ |
| `STAFF_READ_ORDERS` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| `STAFF_UPDATE_ORDERS` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| `STAFF_READ_CUSTOMERS` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| `STAFF_UPDATE_CUSTOMERS` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| `STAFF_READ_INVENTORY` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| `STAFF_UPDATE_INVENTORY` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| `STAFF_READ_PRODUCTION` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| `STAFF_UPDATE_PRODUCTION` | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ |
| `MANAGER_APPROVE_ORDER` | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ |
| `MANAGER_APPROVE_DISCOUNT` | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ |
| `MANAGER_APPROVE_PAYMENT` | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ |
| `MANAGER_READ_ANALYTICS` | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| `ADMIN_MANAGE_USERS` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| `ADMIN_MANAGE_ROLES` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| `ADMIN_MANAGE_PERMISSIONS` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| `ADMIN_MANAGE_TENANTS` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| `ADMIN_MANAGE_SYSTEM_CONFIGURATION` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| `AI_READ_ORDER_STATUS` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `AI_READ_PRODUCT_CATALOG` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `AI_READ_CUSTOMER_ANALYTICS` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `AI_CREATE_ORDER` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌* |
| `AI_TRIGGER_NOTIFICATION` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

*\* `AI_CREATE_ORDER` requires explicit human confirmation (`isConfirmedByHuman = true`).*
