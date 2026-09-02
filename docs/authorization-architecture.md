# SUCHARU PRO — INFRA-03 STEP 02: AUTHORIZATION ARCHITECTURE

## 1. Executive Summary

Sucharu Pro implements a production-grade, multi-tenant authorization engine supporting **Role-Based Access Control (RBAC)**, **Attribute-Based Access Control (ABAC)**, **Customer/Affiliate Data Isolation**, and **AI Agent Boundary Control**.

Authorization is strictly server-authoritative, deterministic, deny-by-default, and isolated per tenant PostgreSQL session context (`TenantContext`).

---

## 2. Core Architectural Components

```
Client / Mobile App / AI Agent
         │
         │ (HTTP / JSON API Request + Authorization: Bearer <JWT>)
         ▼
 ┌────────────────────────────────────────────────────────┐
 │ BackendSecurityContext (Server-Authoritative Context) │
 └───────────────────────────┬────────────────────────────┘
                             │ Validated AuthenticatedPrincipal
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ BackendAuthorizationService                            │
 ├────────────────────────────────────────────────────────┤
 │ 1. System/Admin Bypass Check                            │
 │ 2. Multi-Tenant Isolation Check (target == principal)  │
 │ 3. AI Agent Capability & Confirmation Boundary Check   │
 │ 4. Role Capability Matrix (RBAC Check)                 │
 │ 5. Ownership Policy Check (ABAC Customer / Affiliate)  │
 │ 6. Structured Security Audit Logging                   │
 └───────────────────────────┬────────────────────────────┘
                             │ Decision: ALLOW or DENY(ReasonCode)
                             ▼
  Action Execution / Standardized Sanitized Exception (403 Forbidden)
```

---

## 3. Key Infrastructure Classes

1. **`AuthorizationModels.kt`**
   - Defines fine-grained capabilities (`AuthorizationCapability`), actions (`AuthorizationAction`), sensitivity levels (`ActionSensitivity`), denial reasons (`DenialReasonCode`), context (`AuthorizationContext`), and decisions (`AuthorizationDecision`).

2. **`RoleCapabilityMatrix.kt`**
   - Server-authoritative mapping between `UserRole` (`GUEST`, `CUSTOMER`, `AFFILIATE`, `STAFF`, `MANAGER`, `ADMIN`, `AI_AGENT`) and explicit `AuthorizationCapability` sets. No wildcard wildcard permissions (`*`) are used.

3. **`BackendAuthorizationService.kt`**
   - Enforces RBAC + ABAC context evaluation.
   - Evaluates multi-tenant boundaries (`targetProjectId == principal.projectId`).
   - Evaluates horizontal ownership (`effectiveCustomerId`, `effectiveAffiliateId`).
   - Enforces AI Agent tool safety boundaries (denies critical operations without confirmation token).
   - Records structured security audit events to `AuthAuditDataSource`.

4. **`BackendAuthorizationPolicy.kt`**
   - API layer gateway for route handlers. Delegates to `BackendAuthorizationService` and converts deny decisions into sanitized `ForbiddenException` or `UnauthenticatedException`.

---

## 4. Evaluation Control Flow

1. **Deny-by-Default**: If `context.principal == null`, immediately return `DENY(UNAUTHENTICATED)`.
2. **Tenant Isolation**: If `context.targetProjectId` is provided and does not match `principal.projectId`, return `DENY(TENANT_MISMATCH)`.
3. **AI Agent Tool Boundary**:
   - If `principal.isAiAgent` and requested capability is not mapped in `RoleCapabilityMatrix`, return `DENY(UNAUTHORIZED_AI_TOOL)`.
   - If action is `CRITICAL` or `isApprovalAction` and `isConfirmedByHuman` is false, return `DENY(UNAUTHORIZED_AI_TOOL)`.
4. **RBAC Capability Matrix**: If `RoleCapabilityMatrix` does not grant `requiredCapability` to `principal.role`, return `DENY(MISSING_CAPABILITY)`.
5. **ABAC Ownership Validation**:
   - If `targetCustomerId` is specified and `principal` is not Staff/Admin, `principal.effectiveCustomerId` must equal `targetCustomerId`. Otherwise return `DENY(CUSTOMER_OWNERSHIP_VIOLATION)`.
   - If `targetAffiliateId` is specified and `principal` is not Staff/Admin, `principal.effectiveAffiliateId` must equal `targetAffiliateId`. Otherwise return `DENY(AFFILIATE_OWNERSHIP_VIOLATION)`.
6. **Decision**: Return `ALLOW`.

---

## 5. Pure Domain Model Preservation

The business domain entity `com.sucharu.sucharupro.domain.model.user.UserRole` remains strictly pure containing only verified domain user roles (`ADMIN`, `MANAGER`, `STAFF`, `DESIGNER`, `QC_INSPECTOR`, `ACCOUNTS`, `WAREHOUSE`, `VENDOR`, `CUSTOMER`, `AFFILIATE`). All system security roles (`GUEST`, `AI_AGENT`) are managed strictly within the API / Data Authorization Layer (`com.sucharu.sucharupro.data.api.model.UserRole`).
