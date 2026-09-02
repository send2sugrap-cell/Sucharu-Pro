# SUCHARU PRO — INFRA-03 STEP 02: AI AGENT AUTHORIZATION BOUNDARY

## 1. Executive Summary

Sucharu Pro provides first-class support for AI Agent tool calls (`PrincipalType.AI_AGENT`, `UserRole.AI_AGENT`) while maintaining strict security boundaries to prevent prompt injection, autonomous overreaching, or unauthorized data mutation.

---

## 2. AI Agent Access Rules

1. **Read-Only Default**: AI Agents are primarily granted read capabilities (`AI_READ_ORDER_STATUS`, `AI_READ_PRODUCT_CATALOG`, `AI_READ_CUSTOMER_ANALYTICS`).
2. **Explicit Capability Registration**: AI Agent tool calls cannot invoke operations outside the capabilities granted in `RoleCapabilityMatrix.kt`.
3. **Critical Operation Protection**: Any action classified as `ActionSensitivity.CRITICAL` or `isApprovalAction = true` (e.g. order placement, payment approval, user deletion) is **automatically denied** unless explicitly flagged with human confirmation (`isConfirmedByHuman = true`).

---

## 3. Tool Authorization Flow

```
AI Agent Request (tool: "create_order", params: {...})
                      │
                      ▼
        BackendAuthorizationService
                      │
        ┌─────────────┴─────────────┐
        │ Is Principal AI Agent?    │
        └─────────────┬─────────────┘
                      │ YES
        ┌─────────────▼─────────────┐
        │ Action CRITICAL / APPROVE?│
        └─────────────┬─────────────┘
               │             │
              YES            NO
               │             │
        ┌──────▼──────┐  ┌───▼───────────┐
        │ Human       │  │ Check Role    │
        │ Confirmed?  │  │ Capability    │
        └──────┬──────┘  │ Matrix        │
          │         │    └───┬───────────┘
         YES       NO        │
          │         │      ALLOW / DENY
        ALLOW      DENY
```

---

## 4. Audit & Accountability

Every AI Agent authorization request records `isAiAgent = true` and `agentId` in the `AuthAuditEvent` logs. This provides complete forensic visibility into autonomous assistant activities across the ERP system.
