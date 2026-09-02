package com.sucharu.sucharupro.domain.model.vendorportal

/**
 * Primary vendor portal account root (Module 13 Step 01).
 *
 * Represents the portal entity for a canonical Vendor in Module 12.
 */
data class VendorPortalAccount(
    val portalAccountId: String,
    val vendorId: String,
    val tenantId: String = "TENANT-001",
    val projectId: String,
    val status: VendorPortalAccountStatus = VendorPortalAccountStatus.INVITED,
    val portalCode: String,
    val primaryContactEmail: String? = null,
    val primaryContactPhone: String? = null,
    val activatedAt: Long? = null,
    val activatedBy: String? = null,
    val suspendedAt: Long? = null,
    val suspendedBy: String? = null,
    val suspensionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Membership association linking a user identity to a vendor portal account.
 */
data class VendorPortalMembership(
    val membershipId: String,
    val portalAccountId: String,
    val vendorId: String,
    val userId: String,
    val tenantId: String = "TENANT-001",
    val projectScope: String = "*",
    val role: VendorPortalRole = VendorPortalRole.VENDOR_OPERATOR,
    val status: VendorPortalMembershipStatus = VendorPortalMembershipStatus.PENDING_ACTIVATION,
    val invitationToken: String? = null,
    val invitationExpiresAt: Long? = null,
    val activatedAt: Long? = null,
    val lastAccessAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Security and feature access policy for vendor portal operations.
 */
data class VendorPortalAccessPolicy(
    val policyId: String,
    val tenantId: String = "TENANT-001",
    val projectId: String,
    val vendorId: String? = null, // null indicates tenant default
    val allowRfqSubmission: Boolean = true,
    val allowPoAcknowledgement: Boolean = true,
    val allowInvoiceSubmission: Boolean = true,
    val allowQualityDispute: Boolean = true,
    val requireTwoFactorAuth: Boolean = false,
    val ipWhitelist: String? = null,
    val sessionInactivityTimeoutMinutes: Int = 30,
    val maxActiveSessionsPerUser: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Active or historical vendor portal access session.
 */
data class VendorPortalSession(
    val sessionId: String,
    val membershipId: String,
    val userId: String,
    val vendorId: String,
    val tenantId: String = "TENANT-001",
    val projectId: String,
    val sessionTokenHash: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val status: VendorPortalSessionStatus = VendorPortalSessionStatus.ACTIVE,
    val expiresAt: Long,
    val lastActivityAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Immutable security audit record for vendor portal operations.
 */
data class VendorPortalAuditEvent(
    val eventId: String,
    val tenantId: String = "TENANT-001",
    val projectId: String,
    val vendorId: String,
    val membershipId: String? = null,
    val actorUserId: String,
    val eventType: VendorPortalAuditEventType,
    val action: String,
    val targetId: String? = null,
    val result: String = "SUCCESS",
    val details: String = "",
    val ipAddress: String? = null,
    val correlationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * High-performance composite read model representing the active authenticated vendor context.
 */
data class VendorPortalAccessContext(
    val userId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val membershipId: String,
    val role: VendorPortalRole,
    val tenantId: String,
    val projectScope: String,
    val accountStatus: VendorPortalAccountStatus,
    val membershipStatus: VendorPortalMembershipStatus,
    val policy: VendorPortalAccessPolicy,
    val allowedFeatures: List<String> = emptyList()
)
