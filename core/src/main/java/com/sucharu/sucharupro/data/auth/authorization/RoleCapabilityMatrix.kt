package com.sucharu.sucharupro.data.auth.authorization

import com.sucharu.sucharupro.data.api.model.UserRole

/**
 * Deterministic Role-to-Capability Registry and Authorization Matrix (INFRA-03 Step 02 & Step 03).
 *
 * Mappings are explicit without wildcard '*' defaults. Deny-by-default logic applies.
 */
object RoleCapabilityMatrix {

    private val publicCapabilities: Set<AuthorizationCapability> = setOf(
        AuthorizationCapability.PUBLIC_READ_COMPANY,
        AuthorizationCapability.PUBLIC_READ_SERVICES,
        AuthorizationCapability.PUBLIC_READ_PRODUCTS,
        AuthorizationCapability.PUBLIC_READ_OFFERS,
        AuthorizationCapability.PUBLIC_READ_PORTFOLIO,
        AuthorizationCapability.PUBLIC_READ_FAQ,
        AuthorizationCapability.PUBLIC_READ_ANNOUNCEMENTS,
        AuthorizationCapability.PUBLIC_USE_AI_ASSISTANT
    )

    private val authenticatedIdentityCapabilities: Set<AuthorizationCapability> = setOf(
        AuthorizationCapability.READ_OWN_IDENTITY,
        AuthorizationCapability.READ_OWN_PROFILE,
        AuthorizationCapability.UPDATE_OWN_PROFILE,
        AuthorizationCapability.CHANGE_OWN_PASSWORD,
        AuthorizationCapability.READ_OWN_SESSIONS,
        AuthorizationCapability.REVOKE_OWN_SESSION,
        AuthorizationCapability.REVOKE_ALL_SESSIONS,
        AuthorizationCapability.VERIFY_OWN_CONTACT
    )

    private val customerCapabilities: Set<AuthorizationCapability> = publicCapabilities + authenticatedIdentityCapabilities + setOf(
        AuthorizationCapability.READ_OWN_ORDERS,
        AuthorizationCapability.CREATE_ORDER,
        AuthorizationCapability.UPDATE_OWN_ORDER,
        AuthorizationCapability.CANCEL_OWN_ORDER,
        AuthorizationCapability.READ_OWN_INVOICES,
        AuthorizationCapability.READ_OWN_PAYMENTS,
        AuthorizationCapability.READ_OWN_RECEIPTS,
        AuthorizationCapability.CREATE_RETURN_REQUEST,
        AuthorizationCapability.READ_OWN_RETURNS,
        AuthorizationCapability.READ_OWN_DELIVERIES,
        AuthorizationCapability.NOTIFICATION_VIEW
    )

    private val affiliateCapabilities: Set<AuthorizationCapability> = publicCapabilities + authenticatedIdentityCapabilities + setOf(
        AuthorizationCapability.READ_OWN_AFFILIATE_PROFILE,
        AuthorizationCapability.READ_OWN_REFERRALS,
        AuthorizationCapability.READ_OWN_COMMISSIONS
    )

    private val staffCapabilities: Set<AuthorizationCapability> = publicCapabilities + authenticatedIdentityCapabilities + setOf(
        AuthorizationCapability.READ_OWN_ORDERS,
        AuthorizationCapability.STAFF_READ_CUSTOMERS,
        AuthorizationCapability.STAFF_READ_ORDERS,
        AuthorizationCapability.STAFF_UPDATE_ORDERS,
        AuthorizationCapability.STAFF_READ_INVENTORY,
        AuthorizationCapability.STAFF_UPDATE_INVENTORY,
        AuthorizationCapability.STAFF_READ_DELIVERY,
        AuthorizationCapability.STAFF_UPDATE_DELIVERY,
        AuthorizationCapability.STAFF_READ_QC,
        AuthorizationCapability.STAFF_UPDATE_QC,
        AuthorizationCapability.STAFF_READ_RETURNS,
        AuthorizationCapability.STAFF_UPDATE_RETURNS,
        AuthorizationCapability.WORKFLOW_VIEW,
        AuthorizationCapability.WORKFLOW_EXECUTE,
        AuthorizationCapability.WORKFLOW_ESCALATE,
        AuthorizationCapability.NOTIFICATION_VIEW,
        AuthorizationCapability.NOTIFICATION_SEND,
        AuthorizationCapability.OBSERVABILITY_TENANT_VIEW
    )

    private val managerCapabilities: Set<AuthorizationCapability> = staffCapabilities + setOf(
        AuthorizationCapability.MANAGER_APPROVE_ORDER,
        AuthorizationCapability.MANAGER_APPROVE_RETURN,
        AuthorizationCapability.MANAGER_APPROVE_PAYMENT,
        AuthorizationCapability.MANAGER_VIEW_FINANCIAL_SUMMARY,
        AuthorizationCapability.MANAGER_VIEW_OPERATIONAL_ANALYTICS,
        AuthorizationCapability.WORKFLOW_CREATE,
        AuthorizationCapability.WORKFLOW_EDIT,
        AuthorizationCapability.WORKFLOW_PAUSE,
        AuthorizationCapability.WORKFLOW_RESUME,
        AuthorizationCapability.WORKFLOW_CANCEL,
        AuthorizationCapability.WORKFLOW_RETRY,
        AuthorizationCapability.WORKFLOW_APPROVE,
        AuthorizationCapability.WORKFLOW_METRICS_VIEW,
        AuthorizationCapability.NOTIFICATION_REPLAY,
        AuthorizationCapability.NOTIFICATION_SUPPRESSION_MANAGE,
        AuthorizationCapability.OBSERVABILITY_VIEW,
        AuthorizationCapability.OBSERVABILITY_ALERT_VIEW
    )

    private val adminCapabilities: Set<AuthorizationCapability> = AuthorizationCapability.entries.toSet()

    private val aiAgentCapabilities: Set<AuthorizationCapability> = setOf(
        AuthorizationCapability.PUBLIC_READ_COMPANY,
        AuthorizationCapability.PUBLIC_READ_SERVICES,
        AuthorizationCapability.PUBLIC_READ_PRODUCTS,
        AuthorizationCapability.PUBLIC_READ_OFFERS,
        AuthorizationCapability.PUBLIC_READ_FAQ,
        AuthorizationCapability.PUBLIC_USE_AI_ASSISTANT,
        AuthorizationCapability.READ_OWN_IDENTITY,
        AuthorizationCapability.READ_OWN_PROFILE,
        AuthorizationCapability.AI_READ_CUSTOMER_CONTEXT,
        AuthorizationCapability.AI_READ_ORDER_CONTEXT,
        AuthorizationCapability.AI_CREATE_ORDER,
        AuthorizationCapability.AI_READ_ORDER_STATUS,
        AuthorizationCapability.AI_CREATE_RETURN_REQUEST,
        AuthorizationCapability.AI_READ_INVOICE,
        AuthorizationCapability.AI_READ_AFFILIATE_CONTEXT,
        AuthorizationCapability.WORKFLOW_EXECUTE
    )

    private val matrix: Map<UserRole, Set<AuthorizationCapability>> = mapOf(
        UserRole.GUEST to publicCapabilities,
        UserRole.CUSTOMER to customerCapabilities,
        UserRole.AFFILIATE to affiliateCapabilities,
        UserRole.STAFF to staffCapabilities,
        UserRole.MANAGER to managerCapabilities,
        UserRole.ADMIN to adminCapabilities,
        UserRole.AI_AGENT to aiAgentCapabilities
    )

    /**
     * Resolves capabilities granted to a role.
     */
    fun getCapabilities(role: UserRole): Set<AuthorizationCapability> {
        return matrix[role] ?: emptySet()
    }

    /**
     * Checks if a role has explicit access to a capability.
     */
    fun hasCapability(role: UserRole, capability: AuthorizationCapability): Boolean {
        if (role == UserRole.ADMIN) return true
        val capabilities = getCapabilities(role)
        return capabilities.contains(capability) || capabilities.contains(AuthorizationCapability.ADMIN_ALL)
    }
}
