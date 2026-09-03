package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateCommandCenterRepository
import java.util.UUID

/**
 * Service Implementation for Affiliate Administrative Command Center & Governance Operations.
 *
 * Guarantees:
 * - Aggregates state across Steps 01-04 into operational overview & work queue
 * - Controlled administrative actions execute through underlying authorities
 * - Every state-changing action writes to append-only SHA-256 chained audit log
 * - Tenant isolation enforced on all queries and mutations
 *
 * Module 20 Step 05.
 */
class AffiliateCommandCenterServiceImpl(
    private val commandCenterRepository: AffiliateCommandCenterRepository,
    private val affiliateService: AffiliateService,
    private val programService: AffiliateProgramService,
    private val profileService: AffiliateProfileService,
    private val communicationService: AffiliateCommunicationService
) : AffiliateCommandCenterService {

    private fun buildPrincipal(tenantId: String, userId: String = "system-cc", role: String = "ADMIN"): com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal {
        return com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal(
            userId = userId,
            username = userId,
            role = com.sucharu.sucharupro.data.api.model.UserRole.valueOf(role.uppercase()),
            projectId = tenantId
        )
    }

    override suspend fun getCommandCenterOverview(tenantId: String): AffiliateCommandCenterOverview {
        val affiliates = affiliateService.listAffiliates(tenantId, actorPrincipal = buildPrincipal(tenantId))
        val workItems = getOrSynthesizeWorkItems(tenantId, affiliates)

        val total = affiliates.size.toLong()
        val draft = affiliates.count { it.status == AffiliateStatus.PENDING && it.onboardingState == OnboardingState.DRAFT }.toLong()
        val pendingReview = affiliates.count { it.status == AffiliateStatus.PENDING }.toLong()
        val active = affiliates.count { it.status == AffiliateStatus.ACTIVE }.toLong()
        val suspended = affiliates.count { it.status == AffiliateStatus.SUSPENDED }.toLong()
        val rejected = affiliates.count { it.status == AffiliateStatus.REJECTED }.toLong()
        val terminated = affiliates.count { it.status == AffiliateStatus.TERMINATED }.toLong()

        val verifPending = affiliates.count { it.verificationState == VerificationState.PENDING_DOCUMENTS || it.verificationState == VerificationState.UNVERIFIED }.toLong()
        val agreementPending = affiliates.count { it.agreementReference.isNullOrBlank() }.toLong()

        val openItems = workItems.filter { !it.isResolved }
        val openCount = openItems.size.toLong()
        val urgentCount = openItems.count { it.priority == AffiliateGovernanceWorkItemPriority.URGENT }.toLong()

        val profileSummary = runCatching { profileService.getGovernanceSummary(tenantId) }.getOrNull()
        val commSummary = runCatching { communicationService.getGovernanceSummary(tenantId) }.getOrNull()

        val profileIncompleteCount = profileSummary?.incompleteProfiles ?: 0L
        val govAttentionCount = (suspended + openItems.count { it.priority == AffiliateGovernanceWorkItemPriority.HIGH || it.priority == AffiliateGovernanceWorkItemPriority.URGENT }).toLong()
        val commAttentionCount = commSummary?.failedCount ?: 0L

        return AffiliateCommandCenterOverview(
            tenantId = tenantId,
            totalAffiliates = total,
            draftCount = draft,
            pendingReviewCount = pendingReview,
            activeCount = active,
            suspendedCount = suspended,
            rejectedCount = rejected,
            terminatedCount = terminated,
            verificationPendingCount = verifPending,
            profileIncompleteCount = profileIncompleteCount,
            agreementPendingCount = agreementPending,
            governanceAttentionRequiredCount = govAttentionCount,
            communicationAttentionRequiredCount = commAttentionCount,
            openWorkItemsCount = openCount,
            urgentWorkItemsCount = urgentCount
        )
    }

    override suspend fun listWorkItems(
        tenantId: String,
        priority: AffiliateGovernanceWorkItemPriority?,
        status: AffiliateGovernanceWorkItemStatus?,
        itemType: AffiliateGovernanceWorkItemType?,
        affiliateId: String?
    ): List<AffiliateGovernanceWorkItem> {
        val affiliates = affiliateService.listAffiliates(tenantId, actorPrincipal = buildPrincipal(tenantId))
        val allItems = getOrSynthesizeWorkItems(tenantId, affiliates)

        return allItems.filter { item ->
            val matchesPriority = priority == null || item.priority == priority
            val matchesStatus = status == null || item.status == status
            val matchesType = itemType == null || item.itemType == itemType
            val matchesAffiliate = affiliateId == null || item.affiliateId == affiliateId
            matchesPriority && matchesStatus && matchesType && matchesAffiliate
        }
    }

    override suspend fun findWorkItemById(tenantId: String, workItemId: String): AffiliateGovernanceWorkItem? {
        val stored = commandCenterRepository.findWorkItemById(tenantId, workItemId)
        if (stored != null) return stored

        val affiliates = affiliateService.listAffiliates(tenantId, actorPrincipal = buildPrincipal(tenantId))
        val synthesized = getOrSynthesizeWorkItems(tenantId, affiliates)
        return synthesized.firstOrNull { it.workItemId == workItemId }
    }

    override suspend fun resolveWorkItem(
        tenantId: String,
        workItemId: String,
        resolutionNotes: String,
        status: AffiliateGovernanceWorkItemStatus,
        actorUserId: String,
        actorRole: String,
        correlationId: String
    ): AffiliateGovernanceWorkItem {
        val existing = findWorkItemById(tenantId, workItemId)
            ?: throw NoSuchElementException("Governance Work Item '$workItemId' not found in tenant '$tenantId'.")

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = status,
            resolutionNotes = resolutionNotes,
            resolvedByUserId = actorUserId,
            resolvedAt = now,
            updatedAt = now,
            version = existing.version + 1
        )

        val saved = commandCenterRepository.saveWorkItem(updated)

        recordAudit(
            tenantId = tenantId,
            affiliateId = existing.affiliateId,
            workItemId = workItemId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = AffiliateActorType.HUMAN,
            action = "WORK_ITEM_RESOLVED",
            previousState = existing.status.name,
            newState = status.name,
            reason = resolutionNotes,
            correlationId = correlationId
        )

        return saved
    }

    override suspend fun getAdministrativeDetailView(
        tenantId: String,
        affiliateId: String,
        userId: String
    ): AffiliateAdministrativeDetailView {
        val p = buildPrincipal(tenantId, userId)
        val identity = affiliateService.getAffiliateById(tenantId, affiliateId, p)
        val eligibility = affiliateService.evaluateEligibility(tenantId, affiliateId, p)
        val opProfile = runCatching { profileService.getProfileByAffiliateId(tenantId, affiliateId) }.getOrNull()
        val verifs = runCatching { profileService.listVerifications(tenantId, affiliateId) }.getOrDefault(emptyList<AffiliateVerificationRecord>())
        val docs = runCatching { profileService.listDocuments(tenantId, affiliateId) }.getOrDefault(emptyList<AffiliateDocumentReference>())

        val enrollments = runCatching { programService.findEnrollmentsByAffiliate(tenantId, affiliateId) }.getOrDefault(emptyList<AffiliateEnrollment>())
        val comms = runCatching { communicationService.listCommunications(tenantId, affiliateId) }.getOrDefault(emptyList())
        val workItems = listWorkItems(tenantId = tenantId, affiliateId = affiliateId)
        val audits = listAuditRecords(tenantId = tenantId, affiliateId = affiliateId)
        val handoff = getHandoffContract(tenantId, userId)

        return AffiliateAdministrativeDetailView(
            tenantId = tenantId,
            affiliateId = affiliateId,
            identityProfile = identity,
            eligibility = eligibility,
            operationalProfile = opProfile,
            verifications = verifs,
            documentReferences = docs,
            programRelationships = enrollments,
            recentCommunications = comms,
            openWorkItems = workItems.filter { !it.isResolved },
            auditTrail = audits,
            handoffContract = handoff
        )
    }

    override suspend fun executeAdminAction(
        tenantId: String,
        affiliateId: String,
        action: String,
        reason: String,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        correlationId: String
    ): AffiliateProfile {
        val p = buildPrincipal(tenantId, actorUserId, actorRole)
        val previousAff = affiliateService.getAffiliateById(tenantId, affiliateId, p)
        val previousState = previousAff.status.name

        val updatedAff = when (action.uppercase()) {
            "APPROVE", "ACTIVATE" -> {
                affiliateService.activateAffiliate(tenantId, affiliateId, reason, p)
            }
            "SUSPEND" -> {
                affiliateService.suspendAffiliate(tenantId, affiliateId, reason, p)
            }
            "REACTIVATE" -> {
                affiliateService.reactivateAffiliate(tenantId, affiliateId, reason, p)
            }
            "REJECT" -> {
                affiliateService.rejectAffiliate(tenantId, affiliateId, reason, p)
            }
            "TERMINATE" -> {
                affiliateService.terminateAffiliate(tenantId, affiliateId, reason, p)
            }
            else -> throw IllegalArgumentException("Unsupported administrative action '$action'.")
        }

        // Record audit
        recordAudit(
            tenantId = tenantId,
            affiliateId = affiliateId,
            workItemId = null,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = "ADMIN_ACTION_${action.uppercase()}",
            previousState = previousState,
            newState = updatedAff.status.name,
            reason = reason,
            correlationId = correlationId
        )

        // Dispatch operational notification via Step 04
        runCatching {
            val commType = when (action.uppercase()) {
                "SUSPEND", "TERMINATE" -> AffiliateCommunicationType.SECURITY
                "APPROVE", "ACTIVATE", "REACTIVATE" -> AffiliateCommunicationType.APPLICATION
                else -> AffiliateCommunicationType.GOVERNANCE
            }
            communicationService.createCommunication(
                tenantId = tenantId,
                affiliateId = affiliateId,
                recipientUserId = updatedAff.userId,
                communicationType = commType,
                title = "Administrative Lifecycle Action: $action",
                message = "Your affiliate account status has been updated to ${updatedAff.status.name}. Reason: $reason",
                actorUserId = actorUserId,
                actorRole = actorRole,
                actorType = actorType,
                correlationId = correlationId
            )
        }

        return updatedAff
    }

    override suspend fun listAuditRecords(
        tenantId: String,
        affiliateId: String?
    ): List<AffiliateGovernanceWorkItemAuditRecord> {
        return commandCenterRepository.listAuditRecords(tenantId, affiliateId)
    }

    override suspend fun getHandoffContract(
        tenantId: String,
        userId: String
    ): Module20Step05AffiliateCommandCenterHandoffContract {
        val affiliates = affiliateService.listAffiliates(tenantId, actorPrincipal = buildPrincipal(tenantId))
        val workItems = getOrSynthesizeWorkItems(tenantId, affiliates)
        return AffiliateCommandCenterPolicyEngine.synthesizeHandoffContract(
            tenantId = tenantId,
            userId = userId,
            affiliates = affiliates,
            workItems = workItems
        )
    }

    private suspend fun getOrSynthesizeWorkItems(
        tenantId: String,
        affiliates: List<AffiliateProfile>
    ): List<AffiliateGovernanceWorkItem> {
        val storedItems = commandCenterRepository.listWorkItems(tenantId)
        val storedMap = storedItems.associateBy { it.workItemId }

        val opProfiles = mutableMapOf<String, AffiliateOperationalProfile?>()
        val verifs = mutableMapOf<String, List<AffiliateVerificationRecord>>()
        val enrolls = mutableMapOf<String, List<AffiliateEnrollment>>()
        val comms = mutableMapOf<String, List<AffiliateCommunicationRecord>>()

        for (aff in affiliates) {
            val affId = aff.affiliateId
            opProfiles[affId] = runCatching { profileService.getProfileByAffiliateId(tenantId, affId) }.getOrNull()
            verifs[affId] = runCatching { profileService.listVerifications(tenantId, affId) }.getOrDefault(emptyList<AffiliateVerificationRecord>())
            enrolls[affId] = runCatching { programService.findEnrollmentsByAffiliate(tenantId, affId) }.getOrDefault(emptyList<AffiliateEnrollment>())
            comms[affId] = runCatching { communicationService.listCommunications(tenantId, affId) }.getOrDefault(emptyList())
        }

        val synthesized = AffiliateCommandCenterPolicyEngine.synthesizeWorkItems(
            tenantId = tenantId,
            affiliates = affiliates,
            operationalProfiles = opProfiles,
            verifications = verifs,
            enrollments = enrolls,
            communications = comms
        )

        // Merge stored state (e.g. if resolved/dismissed)
        return synthesized.map { item ->
            storedMap[item.workItemId] ?: item
        }
    }

    private suspend fun recordAudit(
        tenantId: String,
        affiliateId: String?,
        workItemId: String?,
        actorUserId: String,
        actorRole: String,
        actorType: AffiliateActorType,
        action: String,
        previousState: String?,
        newState: String,
        reason: String?,
        correlationId: String
    ): AffiliateGovernanceWorkItemAuditRecord {
        val auditId = "AUD-CC-${UUID.randomUUID().toString().take(12)}"
        val timestamp = System.currentTimeMillis()
        val previousHash = commandCenterRepository.getLastAuditChainHash(tenantId)

        val recordHash = AffiliateCommandCenterPolicyEngine.computeAuditRecordHash(
            tenantId = tenantId,
            auditId = auditId,
            affiliateId = affiliateId,
            workItemId = workItemId,
            actorUserId = actorUserId,
            action = action,
            previousState = previousState,
            newState = newState,
            correlationId = correlationId,
            timestamp = timestamp
        )

        val chainHash = AffiliateCommandCenterPolicyEngine.computeAuditChainHash(previousHash, recordHash)

        val record = AffiliateGovernanceWorkItemAuditRecord(
            tenantId = tenantId,
            auditId = auditId,
            affiliateId = affiliateId,
            workItemId = workItemId,
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorType = actorType,
            action = action,
            previousState = previousState,
            newState = newState,
            reason = reason,
            correlationId = correlationId,
            idempotencyKey = null,
            recordHash = recordHash,
            previousAuditHash = previousHash,
            chainHash = chainHash,
            timestamp = timestamp
        )

        return commandCenterRepository.saveAuditRecord(record)
    }
}
