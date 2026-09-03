package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository
import java.util.UUID

/**
 * Production implementation of AffiliateService (Module 20 Step 01).
 */
class AffiliateServiceImpl(
    private val affiliateRepository: AffiliateRepository
) : AffiliateService {

    override suspend fun createAffiliate(
        tenantId: String,
        command: CreateAffiliateCommand,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        assertMutationAllowed(actorPrincipal)

        // Only ADMIN, MANAGER, STAFF, or the user themselves (if onboarding as AFFILIATE/CUSTOMER)
        if (!actorPrincipal.isStaff && actorPrincipal.userId != command.userId) {
            throw ForbiddenException("You are not authorized to create an affiliate profile for another user.")
        }

        val existingUserAffiliate = affiliateRepository.findByUserId(tenantId, command.userId)
        if (existingUserAffiliate != null) {
            // Idempotent return if matching
            if (existingUserAffiliate.displayName == command.displayName &&
                existingUserAffiliate.affiliateType == command.affiliateType
            ) {
                return existingUserAffiliate
            }
            throw IllegalStateException("User '${command.userId}' already has an affiliate profile in tenant '$tenantId'.")
        }

        val resolvedCode = if (!command.affiliateCode.isNullOrBlank()) {
            AffiliateValidationEngine.normalizeAndValidateCode(command.affiliateCode)
        } else {
            AffiliateValidationEngine.generateDefaultAffiliateCode(command.displayName, command.userId)
        }

        val existingByCode = affiliateRepository.findByAffiliateCode(tenantId, resolvedCode)
        if (existingByCode != null) {
            throw IllegalStateException("Affiliate code '$resolvedCode' is already in use in tenant '$tenantId'.")
        }

        val now = System.currentTimeMillis()
        val affiliateId = "AFF-${UUID.randomUUID().toString().take(12)}"
        val correlationId = "CORR-AFF-CREATE-${UUID.randomUUID().toString().take(8)}"

        val agreementAcceptedAt = if (!command.agreementReference.isNullOrBlank()) now else null
        val agreementAcceptedBy = if (!command.agreementReference.isNullOrBlank()) actorPrincipal.userId else null

        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = command.userId,
            customerId = command.customerId,
            displayName = command.displayName.trim(),
            affiliateCode = resolvedCode,
            status = AffiliateStatus.PENDING,
            affiliateType = command.affiliateType,
            contactPhone = command.contactPhone?.trim(),
            contactEmail = command.contactEmail?.trim(),
            taxIdOrGst = command.taxIdOrGst?.trim(),
            onboardingState = OnboardingState.SUBMITTED,
            verificationState = VerificationState.UNVERIFIED,
            agreementReference = command.agreementReference,
            agreementVersion = command.agreementVersion ?: "v1.0",
            agreementAcceptedAt = agreementAcceptedAt,
            agreementAcceptedBy = agreementAcceptedBy,
            joinedAt = now,
            createdAt = now,
            updatedAt = now,
            version = 1L,
            metadataJson = command.metadataJson
        )

        val saved = affiliateRepository.saveAffiliate(profile)

        // Evaluate initial eligibility
        val eligibility = AffiliateValidationEngine.evaluateEligibility(saved, actorPrincipal.userId)
        affiliateRepository.saveEligibility(eligibility)

        // Record Audit
        recordAudit(
            tenantId = tenantId,
            affiliateId = saved.affiliateId,
            eventType = AffiliateAuditEventType.AFFILIATE_CREATED,
            previousStatus = null,
            newStatus = saved.status,
            actorPrincipal = actorPrincipal,
            reason = "Affiliate profile created",
            correlationId = correlationId
        )

        // Record Outbox Event
        recordOutbox(
            tenantId = tenantId,
            aggregateId = saved.affiliateId,
            eventType = "AffiliateCreated",
            payloadJson = """{"affiliateId":"${saved.affiliateId}","userId":"${saved.userId}","affiliateCode":"${saved.affiliateCode}","status":"${saved.status}"}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun getAffiliateById(
        tenantId: String,
        affiliateId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        val profile = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")
        assertReadAccess(profile, actorPrincipal)
        return profile
    }

    override suspend fun getAffiliateByCode(
        tenantId: String,
        affiliateCode: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        val normalized = AffiliateValidationEngine.normalizeAndValidateCode(affiliateCode)
        val profile = affiliateRepository.findByAffiliateCode(tenantId, normalized)
            ?: throw NoSuchElementException("Affiliate with code '$normalized' not found in tenant '$tenantId'.")
        assertReadAccess(profile, actorPrincipal)
        return profile
    }

    override suspend fun getMyAffiliateProfile(
        tenantId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        val profile = affiliateRepository.findByUserId(tenantId, actorPrincipal.userId)
            ?: throw NoSuchElementException("No affiliate profile found for current user '${actorPrincipal.userId}'.")
        return profile
    }

    override suspend fun updateAffiliateProfile(
        tenantId: String,
        affiliateId: String,
        command: UpdateAffiliateProfileCommand,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        assertMutationAllowed(actorPrincipal)

        val current = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        if (!actorPrincipal.isStaff && current.userId != actorPrincipal.userId) {
            throw ForbiddenException("You are not authorized to update this affiliate profile.")
        }

        // Only Staff/Admin can change verification state or affiliate type for an existing affiliate
        val targetVerification = if (command.verificationState != null) {
            if (!actorPrincipal.isStaff) throw ForbiddenException("Only staff/admin can update verification state.")
            command.verificationState
        } else {
            current.verificationState
        }

        val targetType = if (command.affiliateType != null) {
            if (!actorPrincipal.isStaff && current.status == AffiliateStatus.ACTIVE) {
                throw ForbiddenException("Affiliates cannot change their affiliate type once active.")
            }
            command.affiliateType
        } else {
            current.affiliateType
        }

        val now = System.currentTimeMillis()
        val correlationId = "CORR-AFF-UPDATE-${UUID.randomUUID().toString().take(8)}"

        val updated = current.copy(
            displayName = command.displayName?.trim() ?: current.displayName,
            contactPhone = command.contactPhone?.trim() ?: current.contactPhone,
            contactEmail = command.contactEmail?.trim() ?: current.contactEmail,
            taxIdOrGst = command.taxIdOrGst?.trim() ?: current.taxIdOrGst,
            affiliateType = targetType,
            verificationState = targetVerification,
            metadataJson = command.metadataJson ?: current.metadataJson,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = affiliateRepository.saveAffiliate(updated)

        // Re-evaluate eligibility
        val eligibility = AffiliateValidationEngine.evaluateEligibility(saved, actorPrincipal.userId)
        affiliateRepository.saveEligibility(eligibility)

        recordAudit(
            tenantId = tenantId,
            affiliateId = saved.affiliateId,
            eventType = AffiliateAuditEventType.AFFILIATE_UPDATED,
            previousStatus = current.status,
            newStatus = saved.status,
            actorPrincipal = actorPrincipal,
            reason = "Affiliate profile details updated",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun activateAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        assertMutationAllowed(actorPrincipal)
        assertGovernanceAuthority(actorPrincipal, "activate affiliate")

        val current = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        // Idempotency: already active
        if (current.status == AffiliateStatus.ACTIVE) {
            return current
        }

        AffiliateValidationEngine.validateStateTransition(current.status, AffiliateStatus.ACTIVE)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-AFF-ACTIVATE-${UUID.randomUUID().toString().take(8)}"

        val activated = current.copy(
            status = AffiliateStatus.ACTIVE,
            onboardingState = OnboardingState.APPROVED,
            activatedAt = current.activatedAt ?: now,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = affiliateRepository.saveAffiliate(activated)

        // Re-evaluate eligibility
        val eligibility = AffiliateValidationEngine.evaluateEligibility(saved, actorPrincipal.userId)
        affiliateRepository.saveEligibility(eligibility)

        recordAudit(
            tenantId = tenantId,
            affiliateId = saved.affiliateId,
            eventType = AffiliateAuditEventType.AFFILIATE_ACTIVATED,
            previousStatus = current.status,
            newStatus = AffiliateStatus.ACTIVE,
            actorPrincipal = actorPrincipal,
            reason = reason.ifBlank { "Affiliate activated by governance authority" },
            correlationId = correlationId
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = saved.affiliateId,
            eventType = "AffiliateActivated",
            payloadJson = """{"affiliateId":"${saved.affiliateId}","userId":"${saved.userId}","status":"ACTIVE","activatedAt":$now}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun suspendAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        assertMutationAllowed(actorPrincipal)
        assertGovernanceAuthority(actorPrincipal, "suspend affiliate")

        val current = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        // Idempotency: already suspended
        if (current.status == AffiliateStatus.SUSPENDED) {
            return current
        }

        AffiliateValidationEngine.validateStateTransition(current.status, AffiliateStatus.SUSPENDED)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-AFF-SUSPEND-${UUID.randomUUID().toString().take(8)}"

        val suspended = current.copy(
            status = AffiliateStatus.SUSPENDED,
            suspendedAt = now,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = affiliateRepository.saveAffiliate(suspended)

        val eligibility = AffiliateValidationEngine.evaluateEligibility(saved, actorPrincipal.userId)
        affiliateRepository.saveEligibility(eligibility)

        recordAudit(
            tenantId = tenantId,
            affiliateId = saved.affiliateId,
            eventType = AffiliateAuditEventType.AFFILIATE_SUSPENDED,
            previousStatus = current.status,
            newStatus = AffiliateStatus.SUSPENDED,
            actorPrincipal = actorPrincipal,
            reason = reason.ifBlank { "Affiliate suspended by governance authority" },
            correlationId = correlationId
        )

        recordOutbox(
            tenantId = tenantId,
            aggregateId = saved.affiliateId,
            eventType = "AffiliateSuspended",
            payloadJson = """{"affiliateId":"${saved.affiliateId}","userId":"${saved.userId}","status":"SUSPENDED","suspendedAt":$now,"reason":"$reason"}""",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun reactivateAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        return activateAffiliate(tenantId, affiliateId, reason.ifBlank { "Affiliate reactivated" }, actorPrincipal)
    }

    override suspend fun rejectAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        assertMutationAllowed(actorPrincipal)
        assertGovernanceAuthority(actorPrincipal, "reject affiliate")

        val current = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        if (current.status == AffiliateStatus.REJECTED) {
            return current
        }

        AffiliateValidationEngine.validateStateTransition(current.status, AffiliateStatus.REJECTED)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-AFF-REJECT-${UUID.randomUUID().toString().take(8)}"

        val rejected = current.copy(
            status = AffiliateStatus.REJECTED,
            onboardingState = OnboardingState.REJECTED,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = affiliateRepository.saveAffiliate(rejected)

        val eligibility = AffiliateValidationEngine.evaluateEligibility(saved, actorPrincipal.userId)
        affiliateRepository.saveEligibility(eligibility)

        recordAudit(
            tenantId = tenantId,
            affiliateId = saved.affiliateId,
            eventType = AffiliateAuditEventType.AFFILIATE_REJECTED,
            previousStatus = current.status,
            newStatus = AffiliateStatus.REJECTED,
            actorPrincipal = actorPrincipal,
            reason = reason.ifBlank { "Affiliate application rejected" },
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun terminateAffiliate(
        tenantId: String,
        affiliateId: String,
        reason: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        assertMutationAllowed(actorPrincipal)
        // Only Admin can terminate
        if (actorPrincipal.role != UserRole.ADMIN) {
            throw ForbiddenException("Only Administrator can terminate an affiliate profile.")
        }

        val current = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        if (current.status == AffiliateStatus.TERMINATED) {
            return current
        }

        AffiliateValidationEngine.validateStateTransition(current.status, AffiliateStatus.TERMINATED)

        val now = System.currentTimeMillis()
        val correlationId = "CORR-AFF-TERMINATE-${UUID.randomUUID().toString().take(8)}"

        val terminated = current.copy(
            status = AffiliateStatus.TERMINATED,
            terminatedAt = now,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = affiliateRepository.saveAffiliate(terminated)

        val eligibility = AffiliateValidationEngine.evaluateEligibility(saved, actorPrincipal.userId)
        affiliateRepository.saveEligibility(eligibility)

        recordAudit(
            tenantId = tenantId,
            affiliateId = saved.affiliateId,
            eventType = AffiliateAuditEventType.AFFILIATE_TERMINATED,
            previousStatus = current.status,
            newStatus = AffiliateStatus.TERMINATED,
            actorPrincipal = actorPrincipal,
            reason = reason.ifBlank { "Affiliate terminated by Admin" },
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun acceptAgreement(
        tenantId: String,
        affiliateId: String,
        agreementReference: String,
        agreementVersion: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateProfile {
        validateTenantAccess(tenantId, actorPrincipal)
        assertMutationAllowed(actorPrincipal)

        val current = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        if (!actorPrincipal.isStaff && current.userId != actorPrincipal.userId) {
            throw ForbiddenException("You cannot accept an agreement on behalf of another affiliate.")
        }

        val now = System.currentTimeMillis()
        val correlationId = "CORR-AFF-AGREEMENT-${UUID.randomUUID().toString().take(8)}"

        val accepted = current.copy(
            agreementReference = agreementReference.trim(),
            agreementVersion = agreementVersion.trim().ifBlank { "v1.0" },
            agreementAcceptedAt = now,
            agreementAcceptedBy = actorPrincipal.userId,
            updatedAt = now,
            version = current.version + 1
        )

        val saved = affiliateRepository.saveAffiliate(accepted)

        val eligibility = AffiliateValidationEngine.evaluateEligibility(saved, actorPrincipal.userId)
        affiliateRepository.saveEligibility(eligibility)

        recordAudit(
            tenantId = tenantId,
            affiliateId = saved.affiliateId,
            eventType = AffiliateAuditEventType.AGREEMENT_ACCEPTED,
            previousStatus = current.status,
            newStatus = saved.status,
            actorPrincipal = actorPrincipal,
            reason = "Affiliate terms agreement accepted (Ref: $agreementReference, Version: $agreementVersion)",
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun evaluateEligibility(
        tenantId: String,
        affiliateId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateEligibility {
        validateTenantAccess(tenantId, actorPrincipal)
        val profile = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")
        assertReadAccess(profile, actorPrincipal)

        val eligibility = AffiliateValidationEngine.evaluateEligibility(profile, actorPrincipal.userId)
        affiliateRepository.saveEligibility(eligibility)
        return eligibility
    }

    override suspend fun listAffiliates(
        tenantId: String,
        status: AffiliateStatus?,
        affiliateType: AffiliateType?,
        actorPrincipal: AuthenticatedPrincipal
    ): List<AffiliateProfile> {
        validateTenantAccess(tenantId, actorPrincipal)
        if (!actorPrincipal.isStaff && !actorPrincipal.isAiAgent) {
            // Affiliates/customers can only list their own
            val own = affiliateRepository.findByUserId(tenantId, actorPrincipal.userId)
            return if (own != null) listOf(own) else emptyList()
        }
        return affiliateRepository.listAffiliates(tenantId, status, affiliateType)
    }

    override suspend fun listAuditRecords(
        tenantId: String,
        affiliateId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): List<AffiliateAuditRecord> {
        validateTenantAccess(tenantId, actorPrincipal)
        val profile = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")
        assertReadAccess(profile, actorPrincipal)
        return affiliateRepository.listAuditRecords(tenantId, affiliateId)
    }

    override suspend fun getGovernanceSummary(
        tenantId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): AffiliateGovernanceSummary {
        validateTenantAccess(tenantId, actorPrincipal)
        if (!actorPrincipal.isStaff && !actorPrincipal.isAiAgent) {
            throw ForbiddenException("Access denied: Only staff, managers, and admins can access governance overview.")
        }
        return affiliateRepository.getGovernanceSummary(tenantId)
    }

    override suspend fun getHandoffContract(
        tenantId: String,
        affiliateId: String,
        actorPrincipal: AuthenticatedPrincipal
    ): Module20Step01AffiliateHandoffContract {
        validateTenantAccess(tenantId, actorPrincipal)
        val profile = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw NoSuchElementException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")
        assertReadAccess(profile, actorPrincipal)

        val eligibility = affiliateRepository.findLatestEligibility(tenantId, affiliateId)
            ?: AffiliateValidationEngine.evaluateEligibility(profile, actorPrincipal.userId)

        return AffiliateValidationEngine.synthesizeHandoffContract(profile, eligibility)
    }

    private fun validateTenantAccess(tenantId: String, principal: AuthenticatedPrincipal) {
        if (principal.projectId != tenantId) {
            throw ForbiddenException("Cross-tenant access forbidden: Context '${principal.projectId}' cannot access '$tenantId'.")
        }
    }

    private fun assertMutationAllowed(principal: AuthenticatedPrincipal) {
        if (principal.isAiAgent) {
            throw ForbiddenException("AI Agent is strictly READ-ONLY and is forbidden from mutating affiliate records.")
        }
        if (principal.role in setOf(UserRole.VENDOR, UserRole.GUEST)) {
            throw ForbiddenException("Role '${principal.role}' is not authorized for affiliate operations.")
        }
    }

    private fun assertGovernanceAuthority(principal: AuthenticatedPrincipal, actionName: String) {
        if (principal.role !in setOf(UserRole.ADMIN, UserRole.MANAGER)) {
            throw ForbiddenException("Role '${principal.role}' does not have governance authority to $actionName.")
        }
    }

    private fun assertReadAccess(profile: AffiliateProfile, principal: AuthenticatedPrincipal) {
        if (principal.isStaff || principal.isAiAgent) return
        if (profile.userId == principal.userId || profile.affiliateId == principal.effectiveAffiliateId) return
        throw ForbiddenException("Access denied: You are not authorized to view this affiliate profile.")
    }

    private suspend fun recordAudit(
        tenantId: String,
        affiliateId: String,
        eventType: AffiliateAuditEventType,
        previousStatus: AffiliateStatus?,
        newStatus: AffiliateStatus,
        actorPrincipal: AuthenticatedPrincipal,
        reason: String,
        correlationId: String
    ) {
        val now = System.currentTimeMillis()
        val actorType = if (actorPrincipal.isAiAgent) AffiliateActorType.AI_AGENT else AffiliateActorType.HUMAN

        val recordHash = AffiliateValidationEngine.computeRecordHash(
            tenantId = tenantId,
            affiliateId = affiliateId,
            eventType = eventType,
            previousStatus = previousStatus,
            newStatus = newStatus,
            actorType = actorType,
            actorId = actorPrincipal.userId,
            actorRole = actorPrincipal.role.name,
            timestamp = now,
            correlationId = correlationId,
            reason = reason
        )

        val latestAudit = affiliateRepository.findLatestAuditRecord(tenantId, affiliateId)
        val chainHash = AffiliateValidationEngine.computeChainHash(latestAudit?.chainHash, recordHash)

        val auditRecord = AffiliateAuditRecord(
            auditId = "AUD-AFF-${UUID.randomUUID().toString().take(12)}",
            tenantId = tenantId,
            affiliateId = affiliateId,
            eventType = eventType,
            previousStatus = previousStatus,
            newStatus = newStatus,
            actorType = actorType,
            actorId = actorPrincipal.userId,
            actorRole = actorPrincipal.role.name,
            reason = reason,
            correlationId = correlationId,
            recordHash = recordHash,
            previousAuditHash = latestAudit?.chainHash,
            chainHash = chainHash,
            timestamp = now
        )

        affiliateRepository.appendAuditRecord(auditRecord)
    }

    private suspend fun recordOutbox(
        tenantId: String,
        aggregateId: String,
        eventType: String,
        payloadJson: String,
        correlationId: String
    ) {
        val outboxEvent = AffiliateOutboxEvent(
            outboxId = "EVT-AFF-${UUID.randomUUID().toString().take(12)}",
            tenantId = tenantId,
            aggregateId = aggregateId,
            eventType = eventType,
            payloadJson = payloadJson,
            status = "PENDING",
            correlationId = correlationId,
            version = 1L,
            createdAt = System.currentTimeMillis()
        )
        affiliateRepository.appendOutboxEvent(outboxEvent)
    }
}
