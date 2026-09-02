package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalValidator
import java.util.UUID

/**
 * Production-grade domain implementation of VendorPortalService (Module 13 Step 01).
 */
class VendorPortalServiceImpl(
    private val portalRepository: VendorPortalRepository,
    private val vendorRepository: VendorRepository
) : VendorPortalService {

    override suspend fun createOrInviteAccount(
        vendorId: String,
        portalCode: String,
        primaryContactEmail: String?,
        primaryContactPhone: String?,
        tenantId: String,
        projectId: String,
        actorId: String
    ): DomainResult<VendorPortalAccount> {
        return try {
            VendorPortalValidator.validateAccountCreation(vendorId, portalCode, projectId, tenantId)

            // Validate canonical vendor in Module 12
            val vendor = when (val vRes = vendorRepository.findById(projectId, vendorId)) {
                is DomainResult.Success -> vRes.data
                is DomainResult.Error -> return DomainResult.Error(vRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (vendor.status == VendorStatus.SUSPENDED) {
                return DomainResult.Error(IllegalStateException("Cannot create portal account for suspended vendor '$vendorId'"))
            }

            val account = VendorPortalAccount(
                portalAccountId = UUID.randomUUID().toString(),
                vendorId = vendorId,
                tenantId = tenantId,
                projectId = projectId,
                status = VendorPortalAccountStatus.INVITED,
                portalCode = portalCode.trim().uppercase(),
                primaryContactEmail = primaryContactEmail?.trim(),
                primaryContactPhone = primaryContactPhone?.trim(),
                createdAt = System.currentTimeMillis(),
                createdBy = actorId,
                updatedAt = System.currentTimeMillis(),
                updatedBy = actorId
            )

            val created = portalRepository.createAccount(account)
            if (created is DomainResult.Success) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        actorUserId = actorId,
                        eventType = VendorPortalAuditEventType.ACCOUNT_CREATED,
                        action = "CREATE_PORTAL_ACCOUNT",
                        targetId = created.data.portalAccountId,
                        details = "Created portal account for vendor '$vendorId' with code '${account.portalCode}'"
                    )
                )
            }
            created
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun activateAccount(
        portalAccountId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalAccount> {
        return try {
            val current = when (val aRes = portalRepository.getAccountById(portalAccountId, tenantId)) {
                is DomainResult.Success -> aRes.data ?: return DomainResult.Error(NoSuchElementException("Portal account '$portalAccountId' not found"))
                is DomainResult.Error -> return DomainResult.Error(aRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (!VendorPortalValidator.isValidAccountStatusTransition(current.status, VendorPortalAccountStatus.ACTIVE)) {
                return DomainResult.Error(IllegalStateException("Cannot transition portal account from ${current.status} to ACTIVE"))
            }

            val updated = current.copy(
                status = VendorPortalAccountStatus.ACTIVE,
                activatedAt = System.currentTimeMillis(),
                activatedBy = actorId,
                updatedAt = System.currentTimeMillis(),
                updatedBy = actorId,
                version = current.version + 1
            )
            val saved = portalRepository.updateAccount(updated)
            if (saved is DomainResult.Success) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = current.projectId,
                        vendorId = current.vendorId,
                        actorUserId = actorId,
                        eventType = VendorPortalAuditEventType.ACCOUNT_ACTIVATED,
                        action = "ACTIVATE_PORTAL_ACCOUNT",
                        targetId = portalAccountId,
                        details = "Activated portal account '$portalAccountId'"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun suspendAccount(
        portalAccountId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalAccount> {
        return try {
            val current = when (val aRes = portalRepository.getAccountById(portalAccountId, tenantId)) {
                is DomainResult.Success -> aRes.data ?: return DomainResult.Error(NoSuchElementException("Portal account '$portalAccountId' not found"))
                is DomainResult.Error -> return DomainResult.Error(aRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (!VendorPortalValidator.isValidAccountStatusTransition(current.status, VendorPortalAccountStatus.SUSPENDED)) {
                return DomainResult.Error(IllegalStateException("Cannot transition portal account from ${current.status} to SUSPENDED"))
            }

            val updated = current.copy(
                status = VendorPortalAccountStatus.SUSPENDED,
                suspendedAt = System.currentTimeMillis(),
                suspendedBy = actorId,
                suspensionReason = reason,
                updatedAt = System.currentTimeMillis(),
                updatedBy = actorId,
                version = current.version + 1
            )
            val saved = portalRepository.updateAccount(updated)
            if (saved is DomainResult.Success) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = current.projectId,
                        vendorId = current.vendorId,
                        actorUserId = actorId,
                        eventType = VendorPortalAuditEventType.ACCOUNT_SUSPENDED,
                        action = "SUSPEND_PORTAL_ACCOUNT",
                        targetId = portalAccountId,
                        details = "Suspended portal account '$portalAccountId'. Reason: $reason"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun inviteVendorUser(
        portalAccountId: String,
        vendorId: String,
        userId: String,
        role: VendorPortalRole,
        projectScope: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalMembership> {
        return try {
            VendorPortalValidator.validateMembershipInvitation(portalAccountId, vendorId, userId, tenantId, actorId)

            val account = when (val aRes = portalRepository.getAccountById(portalAccountId, tenantId)) {
                is DomainResult.Success -> aRes.data ?: return DomainResult.Error(NoSuchElementException("Portal account '$portalAccountId' not found"))
                is DomainResult.Error -> return DomainResult.Error(aRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (account.vendorId != vendorId) {
                return DomainResult.Error(IllegalArgumentException("Portal account '$portalAccountId' does not match vendor '$vendorId'"))
            }

            val token = "VPT-" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
            val expiresAt = System.currentTimeMillis() + (7L * 86400000L) // 7 days

            val membership = VendorPortalMembership(
                membershipId = UUID.randomUUID().toString(),
                portalAccountId = portalAccountId,
                vendorId = vendorId,
                userId = userId,
                tenantId = tenantId,
                projectScope = projectScope,
                role = role,
                status = VendorPortalMembershipStatus.PENDING_ACTIVATION,
                invitationToken = token,
                invitationExpiresAt = expiresAt,
                createdAt = System.currentTimeMillis(),
                createdBy = actorId,
                updatedAt = System.currentTimeMillis(),
                updatedBy = actorId
            )

            val created = portalRepository.createMembership(membership)
            if (created is DomainResult.Success) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = account.projectId,
                        vendorId = vendorId,
                        membershipId = created.data.membershipId,
                        actorUserId = actorId,
                        eventType = VendorPortalAuditEventType.MEMBERSHIP_INVITED,
                        action = "INVITE_VENDOR_USER",
                        targetId = userId,
                        details = "Invited user '$userId' as ${role.name} for vendor '$vendorId'"
                    )
                )
            }
            created
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun activateMembership(
        invitationToken: String,
        tenantId: String,
        actorId: String,
        isInternalAdmin: Boolean
    ): DomainResult<VendorPortalMembership> {
        return try {
            val membership = when (val mRes = portalRepository.getMembershipByToken(invitationToken, tenantId)) {
                is DomainResult.Success -> mRes.data ?: return DomainResult.Error(NoSuchElementException("Invalid or expired invitation token"))
                is DomainResult.Error -> return DomainResult.Error(mRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (membership.invitationExpiresAt != null && membership.invitationExpiresAt < System.currentTimeMillis()) {
                return DomainResult.Error(IllegalStateException("Invitation token has expired"))
            }

            VendorPortalValidator.enforceSeparationOfDutiesOnActivation(membership.userId, actorId, isInternalAdmin)

            if (!VendorPortalValidator.isValidMembershipStatusTransition(membership.status, VendorPortalMembershipStatus.ACTIVE)) {
                return DomainResult.Error(IllegalStateException("Cannot activate membership in status '${membership.status}'"))
            }

            val updated = membership.copy(
                status = VendorPortalMembershipStatus.ACTIVE,
                activatedAt = System.currentTimeMillis(),
                invitationToken = null, // Invalidate token once activated
                updatedAt = System.currentTimeMillis(),
                updatedBy = actorId,
                version = membership.version + 1
            )

            val saved = portalRepository.updateMembership(updated)
            if (saved is DomainResult.Success) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = tenantId,
                        vendorId = membership.vendorId,
                        membershipId = membership.membershipId,
                        actorUserId = actorId,
                        eventType = VendorPortalAuditEventType.MEMBERSHIP_ACTIVATED,
                        action = "ACTIVATE_MEMBERSHIP",
                        targetId = membership.userId,
                        details = "Activated vendor portal membership for user '${membership.userId}'"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun updateMembershipStatus(
        membershipId: String,
        newStatus: VendorPortalMembershipStatus,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalMembership> {
        return try {
            val current = when (val mRes = portalRepository.getMembershipById(membershipId, tenantId)) {
                is DomainResult.Success -> mRes.data ?: return DomainResult.Error(NoSuchElementException("Membership '$membershipId' not found"))
                is DomainResult.Error -> return DomainResult.Error(mRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (!VendorPortalValidator.isValidMembershipStatusTransition(current.status, newStatus)) {
                return DomainResult.Error(IllegalStateException("Cannot transition membership from ${current.status} to $newStatus"))
            }

            val updated = current.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis(),
                updatedBy = actorId,
                version = current.version + 1
            )

            val saved = portalRepository.updateMembership(updated)
            if (saved is DomainResult.Success) {
                val eventType = when (newStatus) {
                    VendorPortalMembershipStatus.SUSPENDED -> VendorPortalAuditEventType.MEMBERSHIP_SUSPENDED
                    VendorPortalMembershipStatus.REVOKED -> VendorPortalAuditEventType.MEMBERSHIP_REVOKED
                    VendorPortalMembershipStatus.ACTIVE -> VendorPortalAuditEventType.MEMBERSHIP_ACTIVATED
                    else -> VendorPortalAuditEventType.MEMBERSHIP_INVITED
                }
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = tenantId,
                        vendorId = current.vendorId,
                        membershipId = membershipId,
                        actorUserId = actorId,
                        eventType = eventType,
                        action = "UPDATE_MEMBERSHIP_STATUS",
                        targetId = current.userId,
                        details = "Updated membership status of user '${current.userId}' to $newStatus"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getAccessContext(
        userId: String,
        vendorId: String,
        tenantId: String,
        clientIp: String?
    ): DomainResult<VendorPortalAccessContext> {
        return try {
            // 1. Verify Portal Account
            val account = when (val aRes = portalRepository.getAccountByVendorId(vendorId, tenantId)) {
                is DomainResult.Success -> aRes.data ?: return DomainResult.Error(NoSuchElementException("Portal account for vendor '$vendorId' not found"))
                is DomainResult.Error -> return DomainResult.Error(aRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (account.status != VendorPortalAccountStatus.ACTIVE) {
                return DomainResult.Error(IllegalStateException("Vendor portal account is not ACTIVE (current: ${account.status})"))
            }

            // 2. Verify Vendor exists in canonical Module 12
            val vendor = when (val vRes = vendorRepository.findById(account.projectId, vendorId)) {
                is DomainResult.Success -> vRes.data
                is DomainResult.Error -> return DomainResult.Error(vRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            // 3. Verify User Membership
            val membership = when (val mRes = portalRepository.getMembershipByVendorAndUser(vendorId, userId, tenantId)) {
                is DomainResult.Success -> mRes.data ?: return DomainResult.Error(NoSuchElementException("User '$userId' is not a member of vendor '$vendorId'"))
                is DomainResult.Error -> return DomainResult.Error(mRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (membership.status != VendorPortalMembershipStatus.ACTIVE) {
                return DomainResult.Error(IllegalStateException("Vendor portal membership for user '$userId' is not ACTIVE (current: ${membership.status})"))
            }

            // 4. Resolve Active Policy
            val pRes = portalRepository.getPolicyByVendorId(vendorId, tenantId)
            val policy = (pRes as? DomainResult.Success)?.data
                ?: (portalRepository.getDefaultPolicy(vendor.projectId, tenantId) as? DomainResult.Success)?.data
                ?: VendorPortalAccessPolicy(
                    policyId = "DEFAULT",
                    tenantId = tenantId,
                    projectId = vendor.projectId,
                    vendorId = vendorId
                )

            // 5. Evaluate IP Whitelist
            if (!VendorPortalValidator.isIpAllowed(clientIp, policy.ipWhitelist)) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = vendor.projectId,
                        vendorId = vendorId,
                        membershipId = membership.membershipId,
                        actorUserId = userId,
                        eventType = VendorPortalAuditEventType.ACCESS_DENIED,
                        action = "IP_WHITELIST_BLOCKED",
                        result = "BLOCKED",
                        details = "Access denied from client IP '$clientIp' against whitelist",
                        ipAddress = clientIp
                    )
                )
                return DomainResult.Error(SecurityException("Access from IP '$clientIp' is not permitted by vendor policy"))
            }

            // 6. Compute Allowed Features based on Role and Policy
            val allowedFeatures = mutableListOf<String>()
            if (policy.allowRfqSubmission && membership.role != VendorPortalRole.VENDOR_VIEWER) allowedFeatures.add("RFQ")
            if (policy.allowPoAcknowledgement && membership.role in setOf(VendorPortalRole.VENDOR_ADMIN, VendorPortalRole.VENDOR_OPERATOR, VendorPortalRole.VENDOR_LOGISTICS)) allowedFeatures.add("PURCHASE_ORDERS")
            if (policy.allowInvoiceSubmission && membership.role in setOf(VendorPortalRole.VENDOR_ADMIN, VendorPortalRole.VENDOR_FINANCE)) allowedFeatures.add("INVOICES")
            if (policy.allowQualityDispute && membership.role in setOf(VendorPortalRole.VENDOR_ADMIN, VendorPortalRole.VENDOR_QC, VendorPortalRole.VENDOR_OPERATOR)) allowedFeatures.add("QUALITY_DISPUTES")
            allowedFeatures.add("PROFILE_VIEW")
            allowedFeatures.add("SETTLEMENT_VIEW")

            val context = VendorPortalAccessContext(
                userId = userId,
                vendorId = vendor.vendorId,
                vendorCode = vendor.vendorCode,
                vendorName = vendor.vendorName,
                membershipId = membership.membershipId,
                role = membership.role,
                tenantId = tenantId,
                projectScope = membership.projectScope,
                accountStatus = account.status,
                membershipStatus = membership.status,
                policy = policy,
                allowedFeatures = allowedFeatures
            )
            DomainResult.Success(context)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun createSession(
        membershipId: String,
        userId: String,
        vendorId: String,
        tenantId: String,
        projectId: String,
        sessionTokenHash: String,
        ipAddress: String?,
        userAgent: String?,
        validityMinutes: Long
    ): DomainResult<VendorPortalSession> {
        return try {
            val now = System.currentTimeMillis()
            val expiresAt = now + (validityMinutes * 60000L)

            val session = VendorPortalSession(
                sessionId = UUID.randomUUID().toString(),
                membershipId = membershipId,
                userId = userId,
                vendorId = vendorId,
                tenantId = tenantId,
                projectId = projectId,
                sessionTokenHash = sessionTokenHash,
                ipAddress = ipAddress,
                userAgent = userAgent,
                status = VendorPortalSessionStatus.ACTIVE,
                expiresAt = expiresAt,
                lastActivityAt = now,
                createdAt = now
            )

            val saved = portalRepository.createSession(session)
            if (saved is DomainResult.Success) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        membershipId = membershipId,
                        actorUserId = userId,
                        eventType = VendorPortalAuditEventType.LOGIN_SUCCESS,
                        action = "CREATE_PORTAL_SESSION",
                        targetId = saved.data.sessionId,
                        ipAddress = ipAddress,
                        details = "Created portal session for user '$userId'"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun validateSession(
        tokenHash: String,
        tenantId: String,
        clientIp: String?
    ): DomainResult<VendorPortalSession> {
        return try {
            val session = when (val sRes = portalRepository.getActiveSessionByToken(tokenHash, tenantId)) {
                is DomainResult.Success -> sRes.data ?: return DomainResult.Error(SecurityException("Invalid or expired session"))
                is DomainResult.Error -> return DomainResult.Error(sRes.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            if (session.status != VendorPortalSessionStatus.ACTIVE || session.expiresAt <= System.currentTimeMillis()) {
                return DomainResult.Error(SecurityException("Session is expired or inactive"))
            }

            DomainResult.Success(session)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun revokeSession(
        sessionId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<Boolean> {
        return try {
            val res = portalRepository.revokeSession(sessionId, tenantId)
            if (res is DomainResult.Success && res.data) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = tenantId,
                        vendorId = "",
                        actorUserId = actorId,
                        eventType = VendorPortalAuditEventType.SESSION_TERMINATED,
                        action = "REVOKE_SESSION",
                        targetId = sessionId,
                        details = "Revoked session '$sessionId'"
                    )
                )
            }
            res
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun savePolicy(
        policy: VendorPortalAccessPolicy,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalAccessPolicy> {
        return try {
            val saved = portalRepository.savePolicy(policy)
            if (saved is DomainResult.Success) {
                portalRepository.recordAuditEvent(
                    VendorPortalAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = policy.projectId,
                        vendorId = policy.vendorId ?: "*",
                        actorUserId = actorId,
                        eventType = VendorPortalAuditEventType.POLICY_UPDATED,
                        action = "SAVE_PORTAL_POLICY",
                        targetId = policy.policyId,
                        details = "Updated vendor portal access policy"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getPolicy(
        vendorId: String?,
        projectId: String,
        tenantId: String
    ): DomainResult<VendorPortalAccessPolicy> {
        return try {
            if (vendorId != null) {
                val pRes = portalRepository.getPolicyByVendorId(vendorId, tenantId)
                val policy = if (pRes is DomainResult.Success) pRes.data else null
                if (policy != null) return DomainResult.Success(policy)
            }
            val dRes = portalRepository.getDefaultPolicy(projectId, tenantId)
            val defaultPolicy = if (dRes is DomainResult.Success) dRes.data else null
            if (defaultPolicy != null) return DomainResult.Success(defaultPolicy)

            DomainResult.Success(
                VendorPortalAccessPolicy(
                    policyId = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId
                )
            )
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listMemberships(
        vendorId: String?,
        userId: String?,
        status: VendorPortalMembershipStatus?,
        tenantId: String
    ): DomainResult<List<VendorPortalMembership>> {
        return portalRepository.listMemberships(vendorId, userId, status, tenantId)
    }

    override suspend fun listAccounts(
        projectId: String?,
        status: VendorPortalAccountStatus?,
        tenantId: String
    ): DomainResult<List<VendorPortalAccount>> {
        return portalRepository.listAccounts(projectId, status, tenantId)
    }

    override suspend fun listAuditEvents(
        vendorId: String?,
        actorUserId: String?,
        tenantId: String
    ): DomainResult<List<VendorPortalAuditEvent>> {
        return portalRepository.listAuditEvents(vendorId, actorUserId, tenantId)
    }
}
