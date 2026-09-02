package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Pure domain validator for vendor portal accounts, memberships, and policy state machines (Module 13 Step 01).
 */
object VendorPortalValidator {

    /**
     * Validates whether a VendorPortalAccount transition from [currentStatus] to [newStatus] is allowed.
     */
    fun isValidAccountStatusTransition(
        currentStatus: VendorPortalAccountStatus,
        newStatus: VendorPortalAccountStatus
    ): Boolean {
        if (currentStatus == newStatus) return true
        return when (currentStatus) {
            VendorPortalAccountStatus.INVITED -> newStatus in setOf(
                VendorPortalAccountStatus.PENDING_ACTIVATION,
                VendorPortalAccountStatus.ACTIVE,
                VendorPortalAccountStatus.REVOKED
            )
            VendorPortalAccountStatus.PENDING_ACTIVATION -> newStatus in setOf(
                VendorPortalAccountStatus.ACTIVE,
                VendorPortalAccountStatus.SUSPENDED,
                VendorPortalAccountStatus.REVOKED
            )
            VendorPortalAccountStatus.ACTIVE -> newStatus in setOf(
                VendorPortalAccountStatus.SUSPENDED,
                VendorPortalAccountStatus.DISABLED,
                VendorPortalAccountStatus.REVOKED
            )
            VendorPortalAccountStatus.SUSPENDED -> newStatus in setOf(
                VendorPortalAccountStatus.ACTIVE,
                VendorPortalAccountStatus.DISABLED,
                VendorPortalAccountStatus.REVOKED
            )
            VendorPortalAccountStatus.DISABLED -> newStatus in setOf(
                VendorPortalAccountStatus.ACTIVE,
                VendorPortalAccountStatus.REVOKED
            )
            VendorPortalAccountStatus.REVOKED -> false // Terminal state
        }
    }

    /**
     * Validates whether a VendorPortalMembership transition from [currentStatus] to [newStatus] is allowed.
     */
    fun isValidMembershipStatusTransition(
        currentStatus: VendorPortalMembershipStatus,
        newStatus: VendorPortalMembershipStatus
    ): Boolean {
        if (currentStatus == newStatus) return true
        return when (currentStatus) {
            VendorPortalMembershipStatus.INVITED -> newStatus in setOf(
                VendorPortalMembershipStatus.PENDING_ACTIVATION,
                VendorPortalMembershipStatus.ACTIVE,
                VendorPortalMembershipStatus.REVOKED
            )
            VendorPortalMembershipStatus.PENDING_ACTIVATION -> newStatus in setOf(
                VendorPortalMembershipStatus.ACTIVE,
                VendorPortalMembershipStatus.SUSPENDED,
                VendorPortalMembershipStatus.REVOKED
            )
            VendorPortalMembershipStatus.ACTIVE -> newStatus in setOf(
                VendorPortalMembershipStatus.SUSPENDED,
                VendorPortalMembershipStatus.DISABLED,
                VendorPortalMembershipStatus.REVOKED
            )
            VendorPortalMembershipStatus.SUSPENDED -> newStatus in setOf(
                VendorPortalMembershipStatus.ACTIVE,
                VendorPortalMembershipStatus.DISABLED,
                VendorPortalMembershipStatus.REVOKED
            )
            VendorPortalMembershipStatus.DISABLED -> newStatus in setOf(
                VendorPortalMembershipStatus.ACTIVE,
                VendorPortalMembershipStatus.REVOKED
            )
            VendorPortalMembershipStatus.REVOKED -> false // Terminal state
        }
    }

    /**
     * Validates account creation parameters.
     */
    fun validateAccountCreation(
        vendorId: String,
        portalCode: String,
        projectId: String,
        tenantId: String
    ) {
        require(vendorId.isNotBlank()) { "Vendor ID must not be blank" }
        require(portalCode.isNotBlank()) { "Portal code must not be blank" }
        require(portalCode.length >= 3) { "Portal code must be at least 3 characters" }
        require(projectId.isNotBlank()) { "Project ID must not be blank" }
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank" }
    }

    /**
     * Validates membership invitation parameters.
     */
    fun validateMembershipInvitation(
        portalAccountId: String,
        vendorId: String,
        userId: String,
        tenantId: String,
        actorUserId: String
    ) {
        require(portalAccountId.isNotBlank()) { "Portal account ID must not be blank" }
        require(vendorId.isNotBlank()) { "Vendor ID must not be blank" }
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank" }
        require(actorUserId.isNotBlank()) { "Actor user ID must not be blank" }
    }

    /**
     * Enforces Separation of Duties:
     * A user cannot self-activate their own membership unless explicitly approved or acting with internal ADMIN authority.
     */
    fun enforceSeparationOfDutiesOnActivation(
        memberUserId: String,
        actorUserId: String,
        isInternalAdmin: Boolean
    ) {
        if (!isInternalAdmin && memberUserId == actorUserId) {
            throw IllegalStateException("Separation of Duties violation: A user cannot self-activate their own vendor portal membership without administrative approval")
        }
    }

    /**
     * Evaluates whether an IP address matches a comma-separated whitelist.
     */
    fun isIpAllowed(clientIp: String?, ipWhitelist: String?): Boolean {
        if (ipWhitelist.isNullOrBlank()) return true // No whitelist means all IPs permitted
        if (clientIp.isNullOrBlank()) return false
        val allowedIps = ipWhitelist.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return allowedIps.contains(clientIp)
    }
}
