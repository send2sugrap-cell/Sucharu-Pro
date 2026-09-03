package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import java.security.MessageDigest

/**
 * Deterministic Governance & Operational Policy Engine for Affiliate Command Center (Step 05).
 *
 * Responsibilities:
 * 1. Dynamically synthesizes Work Queue items from live affiliate, profile, verification, enrollment, and communication states.
 * 2. Computes SHA-256 audit record and chain hashes.
 * 3. Synthesizes immutable, integrity-sealed AI Governance Handoff Contracts.
 */
object AffiliateCommandCenterPolicyEngine {

    const val GENESIS_AFFILIATE_COMMAND_CENTER_AUDIT_BLOCK =
        "0000000000000000000000000000000000000000000000000000000000000000"

    /**
     * Synthesizes dynamic operational work items from live affiliate data across Steps 01-04.
     */
    fun synthesizeWorkItems(
        tenantId: String,
        affiliates: List<AffiliateProfile>,
        operationalProfiles: Map<String, AffiliateOperationalProfile?>,
        verifications: Map<String, List<AffiliateVerificationRecord>>,
        enrollments: Map<String, List<AffiliateEnrollment>>,
        communications: Map<String, List<AffiliateCommunicationRecord>>
    ): List<AffiliateGovernanceWorkItem> {
        val items = mutableListOf<AffiliateGovernanceWorkItem>()

        for (aff in affiliates) {
            val affId = aff.affiliateId

            // 1. Pending affiliate review
            if (aff.status == AffiliateStatus.PENDING) {
                items.add(
                    AffiliateGovernanceWorkItem(
                        tenantId = tenantId,
                        workItemId = "WI-REV-$affId",
                        affiliateId = affId,
                        itemType = AffiliateGovernanceWorkItemType.PENDING_REVIEW,
                        priority = AffiliateGovernanceWorkItemPriority.HIGH,
                        status = AffiliateGovernanceWorkItemStatus.OPEN,
                        title = "Pending Application Review: ${aff.displayName}",
                        description = "Affiliate '${aff.displayName}' (${aff.affiliateCode}) is in PENDING state and requires administrative review.",
                        requiredAction = "REVIEW_AND_APPROVE_OR_REJECT",
                        assignedRole = "ADMIN",
                        createdAt = aff.createdAt
                    )
                )
            }

            // 2. Suspended review
            if (aff.status == AffiliateStatus.SUSPENDED) {
                items.add(
                    AffiliateGovernanceWorkItem(
                        tenantId = tenantId,
                        workItemId = "WI-SUSP-$affId",
                        affiliateId = affId,
                        itemType = AffiliateGovernanceWorkItemType.SUSPENDED_REVIEW,
                        priority = AffiliateGovernanceWorkItemPriority.URGENT,
                        status = AffiliateGovernanceWorkItemStatus.OPEN,
                        title = "Suspended Affiliate Review: ${aff.displayName}",
                        description = "Affiliate '${aff.displayName}' is currently SUSPENDED. Review compliance before clearance.",
                        requiredAction = "REVIEW_SUSPENSION_AND_REACTIVATE",
                        assignedRole = "ADMIN",
                        createdAt = aff.updatedAt
                    )
                )
            }

            // 3. Agreement acceptance pending
            if (aff.agreementReference.isNull_or_blank()) {
                items.add(
                    AffiliateGovernanceWorkItem(
                        tenantId = tenantId,
                        workItemId = "WI-AGR-$affId",
                        affiliateId = affId,
                        itemType = AffiliateGovernanceWorkItemType.AGREEMENT_ACCEPTANCE,
                        priority = AffiliateGovernanceWorkItemPriority.MEDIUM,
                        status = AffiliateGovernanceWorkItemStatus.OPEN,
                        title = "Terms Agreement Pending: ${aff.displayName}",
                        description = "Affiliate '${aff.displayName}' has not accepted current partner terms and conditions.",
                        requiredAction = "REQUEST_AGREEMENT_ACCEPTANCE",
                        assignedRole = "STAFF",
                        createdAt = aff.createdAt
                    )
                )
            }

            // 4. Incomplete profile
            val opProf = operationalProfiles[affId]
            if (opProf == null || opProf.profileStatus == AffiliateProfileStatus.INCOMPLETE || opProf.completenessScore < 60) {
                items.add(
                    AffiliateGovernanceWorkItem(
                        tenantId = tenantId,
                        workItemId = "WI-PROF-$affId",
                        affiliateId = affId,
                        itemType = AffiliateGovernanceWorkItemType.INCOMPLETE_PROFILE,
                        priority = AffiliateGovernanceWorkItemPriority.MEDIUM,
                        status = AffiliateGovernanceWorkItemStatus.OPEN,
                        title = "Profile Incomplete: ${aff.displayName}",
                        description = "Affiliate profile completeness score is ${opProf?.completenessScore ?: 0}%. Essential details missing.",
                        requiredAction = "REQUEST_PROFILE_COMPLETION",
                        assignedRole = "STAFF",
                        createdAt = opProf?.updatedAt ?: aff.createdAt
                    )
                )
            }

            // 5. Verification records pending review
            val affVerifs = verifications[affId] ?: emptyList()
            for (ver in affVerifs) {
                if (ver.status == AffiliateVerificationStatus.SUBMITTED || ver.status == AffiliateVerificationStatus.UNDER_REVIEW) {
                    val p = if (ver.verificationType == com.sucharu.sucharupro.domain.model.affiliate.AffiliateVerificationType.IDENTITY || ver.verificationType == com.sucharu.sucharupro.domain.model.affiliate.AffiliateVerificationType.TAX) AffiliateGovernanceWorkItemPriority.HIGH else AffiliateGovernanceWorkItemPriority.MEDIUM
                    items.add(
                        AffiliateGovernanceWorkItem(
                            tenantId = tenantId,
                            workItemId = "WI-VERIF-${ver.verificationId}",
                            affiliateId = affId,
                            itemType = if (ver.verificationType == com.sucharu.sucharupro.domain.model.affiliate.AffiliateVerificationType.BUSINESS) AffiliateGovernanceWorkItemType.BUSINESS_VERIFICATION else AffiliateGovernanceWorkItemType.IDENTITY_VERIFICATION,
                            priority = p,
                            status = AffiliateGovernanceWorkItemStatus.OPEN,
                            title = "${ver.verificationType} Verification Submitted: ${aff.displayName}",
                            description = "Verification record '${ver.verificationId}' (${ver.verificationType}) is pending review.",
                            requiredAction = "REVIEW_VERIFICATION_DOCUMENT",
                            assignedRole = "ADMIN",
                            createdAt = ver.createdAt
                        )
                    )
                }
            }

            // 6. Program Enrollments requiring action
            val affEnrolls = enrollments[affId] ?: emptyList()
            for (enr in affEnrolls) {
                if (enr.enrollmentStatus == AffiliateEnrollmentStatus.PENDING) {
                    items.add(
                        AffiliateGovernanceWorkItem(
                            tenantId = tenantId,
                            workItemId = "WI-ENR-${enr.enrollmentId}",
                            affiliateId = affId,
                            programId = enr.programId,
                            itemType = AffiliateGovernanceWorkItemType.ENROLLMENT_ACTION,
                            priority = AffiliateGovernanceWorkItemPriority.HIGH,
                            status = AffiliateGovernanceWorkItemStatus.OPEN,
                            title = "Program Enrollment Pending: ${aff.displayName}",
                            description = "Enrollment request '${enr.enrollmentId}' for program '${enr.programId}' requires approval.",
                            requiredAction = "APPROVE_ENROLLMENT",
                            assignedRole = "MANAGER",
                            createdAt = enr.createdAt
                        )
                    )
                }
            }

            // 7. Failed notifications
            val affComms = communications[affId] ?: emptyList()
            for (comm in affComms) {
                if (comm.status == AffiliateCommunicationStatus.FAILED) {
                    items.add(
                        AffiliateGovernanceWorkItem(
                            tenantId = tenantId,
                            workItemId = "WI-COMM-${comm.communicationId}",
                            affiliateId = affId,
                            itemType = AffiliateGovernanceWorkItemType.FAILED_NOTIFICATION,
                            priority = AffiliateGovernanceWorkItemPriority.MEDIUM,
                            status = AffiliateGovernanceWorkItemStatus.OPEN,
                            title = "Operational Communication Failed: ${comm.subject}",
                            description = "Notification '${comm.communicationId}' failed delivery to affiliate user.",
                            requiredAction = "RESEND_OR_RESOLVE_NOTIFICATION",
                            assignedRole = "STAFF",
                            createdAt = comm.createdAt
                        )
                    )
                }
            }
        }

        return items.sortedByDescending { it.priority.ordinal }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

    /**
     * Computes deterministic SHA-256 hash for a command center audit record.
     */
    fun computeAuditRecordHash(
        tenantId: String,
        auditId: String,
        affiliateId: String?,
        workItemId: String?,
        actorUserId: String,
        action: String,
        previousState: String?,
        newState: String,
        correlationId: String,
        timestamp: Long
    ): String {
        val payload =
            "$tenantId|$auditId|${affiliateId ?: ""}|${workItemId ?: ""}|$actorUserId|$action|$previousState|$newState|$correlationId|$timestamp"
        return sha256(payload)
    }

    /**
     * Computes chained SHA-256 hash connecting the previous audit block.
     */
    fun computeAuditChainHash(previousChainHash: String?, recordHash: String): String {
        val prev = previousChainHash ?: GENESIS_AFFILIATE_COMMAND_CENTER_AUDIT_BLOCK
        return sha256("$prev:$recordHash")
    }

    /**
     * Synthesizes an immutable AI Governance Handoff Contract for Step 05 Command Center.
     */
    fun synthesizeHandoffContract(
        tenantId: String,
        userId: String,
        affiliates: List<AffiliateProfile>,
        workItems: List<AffiliateGovernanceWorkItem>
    ): Module20Step05AffiliateCommandCenterHandoffContract {
        val totalAffs = affiliates.size.toLong()
        val activeAffs = affiliates.count { it.status == AffiliateStatus.ACTIVE }.toLong()

        val openItems = workItems.filter { !it.isResolved }
        val openCount = openItems.size.toLong()
        val urgentCount = openItems.count { it.priority == AffiliateGovernanceWorkItemPriority.URGENT }.toLong()
        val governanceAttention = openItems.any { it.priority == AffiliateGovernanceWorkItemPriority.URGENT || it.priority == AffiliateGovernanceWorkItemPriority.HIGH }

        val typeCounts = openItems.groupBy { it.itemType.name }.mapValues { (_, list) -> list.size.toLong() }
        val priorityCounts = openItems.groupBy { it.priority.name }.mapValues { (_, list) -> list.size.toLong() }

        val sealPayload = "$tenantId:$userId:$totalAffs:$activeAffs:$openCount:$urgentCount:$governanceAttention"
        val sealHash = sha256(sealPayload)

        return Module20Step05AffiliateCommandCenterHandoffContract(
            tenantId = tenantId,
            userId = userId,
            totalAffiliates = totalAffs,
            activeAffiliates = activeAffs,
            openWorkItemsCount = openCount,
            urgentWorkItemsCount = urgentCount,
            governanceAttentionRequired = governanceAttention,
            workItemTypeCounts = typeCounts,
            priorityCounts = priorityCounts,
            isReadOnly = true,
            integritySealHash = sealHash
        )
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
