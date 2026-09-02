package com.sucharu.sucharupro.data.auth.authorization

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal

/**
 * Granular canonical capabilities across public, customer, affiliate, staff, manager, admin, and AI agent domains.
 */
enum class AuthorizationCapability {
    // Public capabilities
    PUBLIC_READ_COMPANY,
    PUBLIC_READ_SERVICES,
    PUBLIC_READ_PRODUCTS,
    PUBLIC_READ_OFFERS,
    PUBLIC_READ_PORTFOLIO,
    PUBLIC_READ_FAQ,
    PUBLIC_READ_ANNOUNCEMENTS,
    PUBLIC_USE_AI_ASSISTANT,

    // Identity & Profile capabilities (INFRA-03 Step 03)
    READ_OWN_IDENTITY,
    READ_OWN_PROFILE,
    UPDATE_OWN_PROFILE,
    CHANGE_OWN_PASSWORD,
    READ_OWN_SESSIONS,
    REVOKE_OWN_SESSION,
    REVOKE_ALL_SESSIONS,
    VERIFY_OWN_CONTACT,

    // Customer capabilities
    READ_OWN_ORDERS,
    CREATE_ORDER,
    UPDATE_OWN_ORDER,
    CANCEL_OWN_ORDER,
    READ_OWN_INVOICES,
    READ_OWN_PAYMENTS,
    READ_OWN_RECEIPTS,
    CREATE_RETURN_REQUEST,
    READ_OWN_RETURNS,
    READ_OWN_DELIVERIES,

    // Affiliate capabilities
    READ_OWN_AFFILIATE_PROFILE,
    READ_OWN_REFERRALS,
    READ_OWN_COMMISSIONS,

    // Staff capabilities
    STAFF_READ_CUSTOMERS,
    STAFF_READ_ORDERS,
    STAFF_UPDATE_ORDERS,
    STAFF_READ_INVENTORY,
    STAFF_UPDATE_INVENTORY,
    STAFF_READ_DELIVERY,
    STAFF_UPDATE_DELIVERY,
    STAFF_READ_QC,
    STAFF_UPDATE_QC,
    STAFF_READ_RETURNS,
    STAFF_UPDATE_RETURNS,

    // Manager capabilities
    MANAGER_APPROVE_ORDER,
    MANAGER_APPROVE_RETURN,
    MANAGER_APPROVE_PAYMENT,
    MANAGER_VIEW_FINANCIAL_SUMMARY,
    MANAGER_VIEW_OPERATIONAL_ANALYTICS,

    // Admin capabilities
    ADMIN_MANAGE_USERS,
    ADMIN_MANAGE_ROLES,
    ADMIN_MANAGE_PERMISSIONS,
    ADMIN_MANAGE_PROJECT,
    ADMIN_VIEW_AUDIT,
    ADMIN_MANAGE_SYSTEM_CONFIGURATION,
    ADMIN_SUSPEND_ACCOUNT,
    ADMIN_REACTIVATE_ACCOUNT,
    ADMIN_DEACTIVATE_ACCOUNT,
    ADMIN_REVOKE_USER_SESSIONS,
    ADMIN_ALL,

    // Notification Security Capabilities (INFRA-04 Step 07)
    NOTIFICATION_VIEW,
    NOTIFICATION_SEND,
    NOTIFICATION_AUDIT_VIEW,
    NOTIFICATION_SECURITY_ADMIN,
    NOTIFICATION_REPLAY,
    NOTIFICATION_SUPPRESSION_MANAGE,

    // Workflow Control Plane Capabilities (INFRA-04 Step 06)
    WORKFLOW_VIEW,
    WORKFLOW_CREATE,
    WORKFLOW_EDIT,
    WORKFLOW_PUBLISH,
    WORKFLOW_EXECUTE,
    WORKFLOW_PAUSE,
    WORKFLOW_RESUME,
    WORKFLOW_CANCEL,
    WORKFLOW_RETRY,
    WORKFLOW_REPLAY,
    WORKFLOW_COMPENSATE,
    WORKFLOW_APPROVE,
    WORKFLOW_ESCALATE,
    WORKFLOW_AUDIT_VIEW,
    WORKFLOW_METRICS_VIEW,

    // AI Agent capabilities
    AI_READ_CUSTOMER_CONTEXT,
    AI_READ_ORDER_CONTEXT,
    AI_CREATE_ORDER,
    AI_READ_ORDER_STATUS,
    AI_CREATE_RETURN_REQUEST,
    AI_READ_INVOICE,
    AI_READ_AFFILIATE_CONTEXT,

    // AI Agent Notification Capabilities (INFRA-04 Step 08)
    AI_READ_NOTIFICATION_CONTEXT,
    AI_READ_NOTIFICATION_STATUS,
    AI_READ_NOTIFICATION_HISTORY,
    AI_CREATE_NOTIFICATION_DRAFT,
    AI_REQUEST_NOTIFICATION_SEND,
    AI_REQUEST_NOTIFICATION_REPLAY,
    AI_REQUEST_NOTIFICATION_SUPPRESSION,
    AI_REQUEST_NOTIFICATION_PREFERENCE_UPDATE,

    // Production Observability & Operational Readiness Capabilities (INFRA-04 Step 09)
    OBSERVABILITY_VIEW,
    OBSERVABILITY_TENANT_VIEW,
    OBSERVABILITY_ALERT_VIEW,
    OBSERVABILITY_ADMIN,
    OBSERVABILITY_AUDIT_VIEW
}

/**
 * Standard operation action type.
 */
enum class AuthorizationAction {
    READ,
    CREATE,
    UPDATE,
    DELETE,
    EXECUTE,
    APPROVE,
    CANCEL
}

/**
 * Security sensitivity rating for actions/endpoints.
 */
enum class ActionSensitivity {
    PUBLIC,
    LOW,
    NORMAL,
    SENSITIVE,
    CRITICAL
}

/**
 * Resource ownership scope classification.
 */
enum class ResourceScope {
    PUBLIC,
    CUSTOMER_OWNED,
    AFFILIATE_OWNED,
    TENANT_OWNED,
    STAFF_SCOPED,
    MANAGER_SCOPED,
    ADMIN_SCOPED
}

/**
 * Machine-readable denial reason codes for security audits and sanitized responses.
 */
enum class DenialReasonCode {
    UNAUTHENTICATED,
    UNKNOWN_ROLE,
    MISSING_CAPABILITY,
    TENANT_MISMATCH,
    CUSTOMER_OWNERSHIP_VIOLATION,
    AFFILIATE_OWNERSHIP_VIOLATION,
    ROLE_NOT_AUTHORIZED,
    UNAUTHORIZED_AI_TOOL,
    APPROVAL_REQUIRED,
    SESSION_REVOKED,
    INVALID_CONTEXT
}

/**
 * Result of an authorization evaluation.
 */
sealed class AuthorizationDecision {
    object Allow : AuthorizationDecision()
    data class Deny(val reasonCode: DenialReasonCode, val message: String) : AuthorizationDecision()

    val isAllowed: Boolean get() = this is Allow
    val isDenied: Boolean get() = this is Deny
}

/**
 * Evaluation context for ABAC / contextual authorization decisions.
 */
data class AuthorizationContext(
    val principal: AuthenticatedPrincipal?,
    val requiredCapability: AuthorizationCapability,
    val action: AuthorizationAction = AuthorizationAction.READ,
    val targetResourceType: String? = null,
    val targetResourceId: String? = null,
    val targetCustomerId: String? = null,
    val targetAffiliateId: String? = null,
    val targetProjectId: String? = null,
    val sensitivity: ActionSensitivity = ActionSensitivity.NORMAL,
    val isApprovalAction: Boolean = false,
    val toolId: String? = null
)
