package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.data.auth.model.AccountStatus

/**
 * Supported user roles in the Sucharu Pro Unified Ecosystem (INFRA-02 Step 04).
 */
enum class UserRole {
    GUEST,
    CUSTOMER,
    AFFILIATE,
    STAFF,
    MANAGER,
    ADMIN,
    VENDOR,
    AI_AGENT
}

/**
 * Principal classifications for human vs automated system actors.
 */
enum class PrincipalType {
    HUMAN,
    AI_AGENT,
    SYSTEM
}

/**
 * Granular user capabilities.
 */
enum class UserPermission {
    READ_PUBLIC,
    READ_OWN_PROFILE,
    UPDATE_OWN_PROFILE,
    READ_OWN_ORDERS,
    CREATE_ORDER,
    READ_OWN_INVOICES,
    READ_OWN_PAYMENTS,
    READ_OWN_DELIVERY,
    READ_OWN_AFFILIATE,
    MANAGE_CUSTOMERS,
    MANAGE_ORDERS,
    MANAGE_FINANCE,
    MANAGE_QC,
    MANAGE_INVENTORY,
    READ_VENDORS,
    MANAGE_VENDORS,
    READ_VENDOR_RATES,
    MANAGE_VENDOR_RATES,
    READ_VENDOR_WORK_ORDERS,
    MANAGE_VENDOR_WORK_ORDERS,
    RELEASE_VENDOR_WORK_ORDERS,
    READ_VENDOR_PURCHASE_ORDERS,
    MANAGE_VENDOR_PURCHASE_ORDERS,
    APPROVE_VENDOR_PURCHASE_ORDERS,
    ISSUE_VENDOR_PURCHASE_ORDERS,
    READ_VENDOR_RECEIPTS,
    MANAGE_VENDOR_RECEIPTS,
    INSPECT_VENDOR_RECEIPTS,
    READ_VENDOR_INVOICES,
    MANAGE_VENDOR_INVOICES,
    MATCH_VENDOR_INVOICES,
    APPROVE_VENDOR_INVOICES,
    RESOLVE_VENDOR_INVOICE_EXCEPTIONS,
    READ_VENDOR_QUALITY,
    MANAGE_VENDOR_QUALITY,
    CREATE_VENDOR_REJECTION,
    MANAGE_VENDOR_REJECTION,
    CREATE_VENDOR_DISPUTE,
    MANAGE_VENDOR_DISPUTE,
    RESOLVE_VENDOR_DISPUTE,
    READ_VENDOR_PERFORMANCE,
    MANAGE_VENDOR_PERFORMANCE,
    EVALUATE_VENDOR,
    APPROVE_VENDOR_EVALUATION,
    READ_VENDOR_COMPLIANCE,
    MANAGE_VENDOR_COMPLIANCE,
    MANAGE_VENDOR_CORRECTIVE_ACTION,
    READ_VENDOR_SETTLEMENT,
    MANAGE_VENDOR_SETTLEMENT,
    APPROVE_VENDOR_SETTLEMENT,
    RECONCILE_VENDOR_SETTLEMENT,
    READ_VENDOR_ANALYTICS,
    READ_VENDOR_FINANCIAL_ANALYTICS,
    READ_VENDOR_360,
    READ_VENDOR_PORTAL,
    MANAGE_VENDOR_PORTAL_ACCESS,
    MANAGE_VENDOR_PORTAL_USERS,
    READ_VENDOR_PORTAL_AUDIT,
    READ_VENDOR_RFQ,
    CREATE_VENDOR_RFQ,
    MANAGE_VENDOR_RFQ,
    INVITE_VENDOR_TO_RFQ,
    READ_VENDOR_QUOTATION,
    CREATE_VENDOR_QUOTATION,
    SUBMIT_VENDOR_QUOTATION,
    REVISE_VENDOR_QUOTATION,
    WITHDRAW_VENDOR_QUOTATION,
    EVALUATE_VENDOR_BID,
    AWARD_VENDOR_RFQ,
    READ_VENDOR_RFQ_AUDIT,
    MANAGE_RFQ_CLARIFICATION,
    READ_VENDOR_PURCHASE_ORDER,
    ACKNOWLEDGE_VENDOR_PURCHASE_ORDER,
    READ_VENDOR_WORK_ORDER,
    ACKNOWLEDGE_VENDOR_WORK_ORDER,
    SUBMIT_VENDOR_PROGRESS,
    MANAGE_VENDOR_PROGRESS,
    CREATE_VENDOR_BLOCKER,
    MANAGE_VENDOR_BLOCKER,
    CREATE_VENDOR_CLARIFICATION,
    RESPOND_VENDOR_CLARIFICATION,
    CREATE_VENDOR_COLLABORATION,
    READ_VENDOR_COLLABORATION,
    SUBMIT_VENDOR_COMPLETION_REQUEST,
    READ_VENDOR_COMPLETION_REQUEST,
    MANAGE_VENDOR_COMPLETION_REQUEST,
    READ_VENDOR_EVIDENCE,
    MANAGE_VENDOR_EVIDENCE,
    READ_VENDOR_COLLABORATION_AUDIT,
    ADMIN_ALL
}

/**
 * Cryptographically trusted authenticated identity context (INFRA-02 Step 04, INFRA-03 Step 02).
 */
data class AuthenticatedPrincipal(
    val userId: String,
    val projectId: String,
    val username: String,
    val role: UserRole,
    val permissions: Set<UserPermission> = emptySet(),
    val email: String? = null,
    val tokenExpiresAt: Long = System.currentTimeMillis() + 86400000L,
    val principalType: PrincipalType = PrincipalType.HUMAN,
    val agentId: String? = null,
    val customerId: String? = null,
    val affiliateId: String? = null,
    val staffId: String? = null,
    val vendorId: String? = null,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE
) {
    fun hasPermission(permission: UserPermission): Boolean {
        if (role == UserRole.ADMIN || permissions.contains(UserPermission.ADMIN_ALL)) return true
        return permissions.contains(permission)
    }

    val isCustomer: Boolean get() = role == UserRole.CUSTOMER
    val isAffiliate: Boolean get() = role == UserRole.AFFILIATE
    val isVendor: Boolean get() = role == UserRole.VENDOR
    val isStaff: Boolean get() = role in setOf(UserRole.STAFF, UserRole.MANAGER, UserRole.ADMIN)
    val isAiAgent: Boolean get() = role == UserRole.AI_AGENT || principalType == PrincipalType.AI_AGENT
    val effectiveCustomerId: String get() = customerId ?: userId
    val effectiveAffiliateId: String get() = affiliateId ?: userId
    val effectiveStaffId: String get() = staffId ?: userId
    val effectiveVendorId: String get() = vendorId ?: userId
}

/**
 * Login authentication request.
 */
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String,
    val requestedProjectId: String? = null
)

/**
 * Authentication token response.
 */
data class AuthTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 86400L,
    val principal: AuthenticatedPrincipal
)
