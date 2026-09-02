package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlement
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalSettlementRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.service.vendor.VendorInvoiceService
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementService
import java.util.UUID

/**
 * Production implementation of VendorPortalSettlementService (Module 13 Step 09).
 * Delegates canonical financial authority to Module 12 without duplication.
 */
class VendorPortalSettlementServiceImpl(
    private val portalRepository: VendorPortalSettlementRepository,
    private val canonicalSettlementService: VendorSettlementService,
    private val canonicalInvoiceService: VendorInvoiceService,
    private val vendorRepository: VendorRepository
) : VendorPortalSettlementService {

    // =========================================================================
    // 1. SETTLEMENTS & ALLOCATIONS
    // =========================================================================

    override suspend fun listSettlements(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalSettlementSummary>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val canonicalResult = canonicalSettlementService.listSettlements(
            vendorId = vendorId,
            projectId = projectId,
            tenantId = tenantId
        )

        return when (canonicalResult) {
            is DomainResult.Success -> {
                val summaries = canonicalResult.data.map { s ->
                    val ack = when (val a = portalRepository.findAcknowledgementBySettlementId(tenantId, projectId, vendorId, s.settlementId)) {
                        is DomainResult.Success -> a.data
                        else -> null
                    }
                    projectSettlementSummary(s, ack)
                }
                DomainResult.Success(summaries)
            }
            is DomainResult.Error -> DomainResult.Error(canonicalResult.exception, canonicalResult.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun getSettlementById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String
    ): DomainResult<VendorPortalSettlementSummary> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return when (val canonicalResult = canonicalSettlementService.getSettlementById(settlementId, tenantId)) {
            is DomainResult.Success -> {
                val s = canonicalResult.data
                if (s == null || s.vendorId != vendorId || s.projectId != projectId) {
                    return DomainResult.Error(message = "Settlement '$settlementId' not found for vendor '$vendorId'.")
                }
                val ack = when (val a = portalRepository.findAcknowledgementBySettlementId(tenantId, projectId, vendorId, settlementId)) {
                    is DomainResult.Success -> a.data
                    else -> null
                }
                DomainResult.Success(projectSettlementSummary(s, ack))
            }
            is DomainResult.Error -> DomainResult.Error(canonicalResult.exception, canonicalResult.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun getSettlementAllocations(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String
    ): DomainResult<List<VendorPortalSettlementAllocationProjection>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return when (val canonicalResult = canonicalSettlementService.getSettlementById(settlementId, tenantId)) {
            is DomainResult.Success -> {
                val s = canonicalResult.data
                if (s == null || s.vendorId != vendorId || s.projectId != projectId) {
                    return DomainResult.Error(message = "Settlement '$settlementId' not found for vendor '$vendorId'.")
                }
                val projections = s.allocations.map { alloc ->
                    VendorPortalSettlementAllocationProjection(
                        allocationId = alloc.allocationId,
                        settlementId = alloc.settlementId,
                        payableId = alloc.payableId,
                        invoiceId = alloc.invoiceId,
                        invoiceNumber = alloc.invoiceId?.let { "INV-${it.takeLast(6).uppercase()}" },
                        purchaseOrderId = null,
                        orderNumber = null,
                        allocatedAmount = alloc.allocatedAmount,
                        currency = alloc.currency,
                        allocatedAt = alloc.createdAt
                    )
                }
                DomainResult.Success(projections)
            }
            is DomainResult.Error -> DomainResult.Error(canonicalResult.exception, canonicalResult.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun acknowledgeSettlement(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String,
        status: VendorPortalSettlementViewStatus,
        idempotencyKey: String,
        discrepancyFlag: Boolean,
        discrepancyNotes: String?,
        evidenceReferences: List<String>,
        actorId: String
    ): DomainResult<VendorPortalSettlementAcknowledgement> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        // Idempotency check
        when (val existing = portalRepository.findAcknowledgementByIdempotencyKey(tenantId, projectId, vendorId, idempotencyKey)) {
            is DomainResult.Success -> {
                if (existing.data != null) return DomainResult.Success(existing.data!!)
            }
            is DomainResult.Error -> return DomainResult.Error(existing.exception, existing.message)
            DomainResult.Loading -> {}
        }

        // Verify settlement existence & ownership
        val settlement = when (val s = canonicalSettlementService.getSettlementById(settlementId, tenantId)) {
            is DomainResult.Success -> s.data ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")
            is DomainResult.Error -> return DomainResult.Error(s.exception, s.message)
            DomainResult.Loading -> return DomainResult.Loading
        }
        if (settlement.vendorId != vendorId || settlement.projectId != projectId) {
            return DomainResult.Error(message = "Unauthorized access to settlement '$settlementId'.")
        }

        val acknowledgement = VendorPortalSettlementAcknowledgement(
            acknowledgementId = "ACK-${UUID.randomUUID()}",
            settlementId = settlementId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            acknowledgedBy = actorId,
            acknowledgedAt = System.currentTimeMillis(),
            status = if (discrepancyFlag) VendorPortalSettlementViewStatus.ACKNOWLEDGED_WITH_DISCREPANCY else status,
            idempotencyKey = idempotencyKey,
            discrepancyFlag = discrepancyFlag,
            discrepancyNotes = discrepancyNotes,
            evidenceReferences = evidenceReferences
        )

        return when (val saveRes = portalRepository.saveAcknowledgement(acknowledgement)) {
            is DomainResult.Success -> {
                portalRepository.recordActivity(
                    VendorPortalFinancialActivityEvent(
                        activityId = "ACT-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        eventType = VendorPortalFinancialActivityEventType.SETTLEMENT_ACKNOWLEDGED,
                        entityType = "SETTLEMENT",
                        entityId = settlementId,
                        actorId = actorId,
                        actorRole = "VENDOR",
                        description = "Settlement '${settlement.settlementNumber}' acknowledged with status '$status'.",
                        occurredAt = System.currentTimeMillis()
                    )
                )
                DomainResult.Success(saveRes.data)
            }
            is DomainResult.Error -> DomainResult.Error(saveRes.exception, saveRes.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    // =========================================================================
    // 2. RECONCILIATION CASES
    // =========================================================================

    override suspend fun listReconciliationCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        status: VendorPortalReconciliationCaseStatus?
    ): DomainResult<List<VendorPortalReconciliationCase>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return portalRepository.listReconciliationCases(tenantId, projectId, vendorId, settlementId, invoiceId, status)
    }

    override suspend fun getReconciliationCaseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String
    ): DomainResult<VendorPortalReconciliationCase> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return when (val res = portalRepository.findReconciliationCaseById(tenantId, projectId, vendorId, caseId)) {
            is DomainResult.Success -> {
                val case = res.data ?: return DomainResult.Error(message = "Reconciliation case '$caseId' not found.")
                DomainResult.Success(case)
            }
            is DomainResult.Error -> DomainResult.Error(res.exception, res.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun createReconciliationQuery(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        subject: String,
        claimedAmount: Money,
        systemAmount: Money,
        notes: String?,
        actorId: String
    ): DomainResult<VendorPortalReconciliationCase> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val currency = "BDT"
        val varianceAmount = Money((claimedAmount.amount - systemAmount.amount).abs())
        val caseId = "REC-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val initialEvent = VendorPortalReconciliationEvent(
            eventId = "EVT-${UUID.randomUUID()}",
            caseId = caseId,
            actorId = actorId,
            actorRole = "VENDOR",
            action = "QUERY_CREATED",
            remarks = "Reconciliation query opened: $subject",
            timestamp = now
        )

        val newCase = VendorPortalReconciliationCase(
            caseId = caseId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            settlementId = settlementId,
            invoiceId = invoiceId,
            caseNumber = "REC-${(now % 1000000)}",
            subject = subject,
            status = VendorPortalReconciliationCaseStatus.OPEN,
            claimedAmount = claimedAmount,
            systemAmount = systemAmount,
            varianceAmount = varianceAmount,
            currency = currency,
            notes = notes,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            events = listOf(initialEvent)
        )

        return when (val saveRes = portalRepository.saveReconciliationCase(newCase)) {
            is DomainResult.Success -> {
                portalRepository.appendReconciliationEvent(tenantId, projectId, vendorId, caseId, initialEvent)
                portalRepository.recordActivity(
                    VendorPortalFinancialActivityEvent(
                        activityId = "ACT-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        eventType = VendorPortalFinancialActivityEventType.RECONCILIATION_QUERY_CREATED,
                        entityType = "RECONCILIATION",
                        entityId = caseId,
                        actorId = actorId,
                        actorRole = "VENDOR",
                        description = "Reconciliation query '${newCase.caseNumber}' opened with variance of ${varianceAmount.amount} $currency.",
                        occurredAt = now
                    )
                )
                DomainResult.Success(saveRes.data)
            }
            is DomainResult.Error -> DomainResult.Error(saveRes.exception, saveRes.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun respondToReconciliation(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        remarks: String,
        actorId: String,
        actorRole: String
    ): DomainResult<VendorPortalReconciliationCase> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val existing = when (val res = portalRepository.findReconciliationCaseById(tenantId, projectId, vendorId, caseId)) {
            is DomainResult.Success -> res.data ?: return DomainResult.Error(message = "Reconciliation case '$caseId' not found.")
            is DomainResult.Error -> return DomainResult.Error(res.exception, res.message)
            DomainResult.Loading -> return DomainResult.Loading
        }

        val now = System.currentTimeMillis()
        val nextStatus = if (actorRole.uppercase().contains("VENDOR")) {
            VendorPortalReconciliationCaseStatus.INTERNAL_RESPONSE_REQUIRED
        } else {
            VendorPortalReconciliationCaseStatus.VENDOR_RESPONSE_REQUIRED
        }

        val event = VendorPortalReconciliationEvent(
            eventId = "EVT-${UUID.randomUUID()}",
            caseId = caseId,
            actorId = actorId,
            actorRole = actorRole,
            action = "RESPONSE_SUBMITTED",
            remarks = remarks,
            timestamp = now
        )

        val updated = existing.copy(
            status = nextStatus,
            updatedAt = now,
            events = existing.events + event
        )

        return when (val saveRes = portalRepository.saveReconciliationCase(updated)) {
            is DomainResult.Success -> {
                portalRepository.appendReconciliationEvent(tenantId, projectId, vendorId, caseId, event)
                portalRepository.recordActivity(
                    VendorPortalFinancialActivityEvent(
                        activityId = "ACT-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        eventType = VendorPortalFinancialActivityEventType.RECONCILIATION_RESPONSE_SUBMITTED,
                        entityType = "RECONCILIATION",
                        entityId = caseId,
                        actorId = actorId,
                        actorRole = actorRole,
                        description = "Response submitted for reconciliation case '${existing.caseNumber}'.",
                        occurredAt = now
                    )
                )
                DomainResult.Success(saveRes.data)
            }
            is DomainResult.Error -> DomainResult.Error(saveRes.exception, saveRes.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    // =========================================================================
    // 3. FINANCIAL DISPUTES
    // =========================================================================

    override suspend fun listFinancialDisputes(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        status: VendorPortalFinancialDisputeStatus?
    ): DomainResult<List<VendorPortalFinancialDispute>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return portalRepository.listFinancialDisputes(tenantId, projectId, vendorId, settlementId, invoiceId, status)
    }

    override suspend fun getFinancialDisputeById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<VendorPortalFinancialDispute> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return when (val res = portalRepository.findFinancialDisputeById(tenantId, projectId, vendorId, disputeId)) {
            is DomainResult.Success -> {
                val dispute = res.data ?: return DomainResult.Error(message = "Financial dispute '$disputeId' not found.")
                DomainResult.Success(dispute)
            }
            is DomainResult.Error -> DomainResult.Error(res.exception, res.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun createFinancialDispute(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String?,
        invoiceId: String?,
        category: String,
        priority: String,
        disputedAmount: Money,
        proposedResolutionAmount: Money?,
        reason: String,
        actorId: String
    ): DomainResult<VendorPortalFinancialDispute> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val disputeId = "DISP-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val initialEvent = VendorPortalFinancialDisputeEvent(
            eventId = "EVT-${UUID.randomUUID()}",
            disputeId = disputeId,
            actorId = actorId,
            actorRole = "VENDOR",
            action = "DISPUTE_CREATED",
            remarks = "Dispute created: $reason",
            timestamp = now
        )

        val newDispute = VendorPortalFinancialDispute(
            disputeId = disputeId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            settlementId = settlementId,
            invoiceId = invoiceId,
            disputeNumber = "DISP-${(now % 1000000)}",
            category = category,
            priority = priority,
            status = VendorPortalFinancialDisputeStatus.SUBMITTED,
            disputedAmount = disputedAmount,
            proposedResolutionAmount = proposedResolutionAmount,
            currency = "BDT",
            reason = reason,
            resolutionNotes = null,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            resolvedBy = null,
            resolvedAt = null,
            events = listOf(initialEvent)
        )

        return when (val saveRes = portalRepository.saveFinancialDispute(newDispute)) {
            is DomainResult.Success -> {
                portalRepository.appendFinancialDisputeEvent(tenantId, projectId, vendorId, disputeId, initialEvent)
                portalRepository.recordActivity(
                    VendorPortalFinancialActivityEvent(
                        activityId = "ACT-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        eventType = VendorPortalFinancialActivityEventType.DISPUTE_OPENED,
                        entityType = "FINANCIAL_DISPUTE",
                        entityId = disputeId,
                        actorId = actorId,
                        actorRole = "VENDOR",
                        description = "Financial dispute '${newDispute.disputeNumber}' submitted for amount ${disputedAmount.amount} BDT.",
                        occurredAt = now
                    )
                )
                DomainResult.Success(saveRes.data)
            }
            is DomainResult.Error -> DomainResult.Error(saveRes.exception, saveRes.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun respondToFinancialDispute(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        remarks: String,
        actorId: String,
        actorRole: String,
        proposedResolutionAmount: Money?
    ): DomainResult<VendorPortalFinancialDispute> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val existing = when (val res = portalRepository.findFinancialDisputeById(tenantId, projectId, vendorId, disputeId)) {
            is DomainResult.Success -> res.data ?: return DomainResult.Error(message = "Financial dispute '$disputeId' not found.")
            is DomainResult.Error -> return DomainResult.Error(res.exception, res.message)
            DomainResult.Loading -> return DomainResult.Loading
        }

        // Separation of Duties (SoD) Rule:
        // A vendor cannot unilaterally mark their own dispute as RESOLVED or CLOSED.
        val isVendor = actorRole.uppercase().contains("VENDOR")
        val now = System.currentTimeMillis()

        val nextStatus = if (isVendor) {
            VendorPortalFinancialDisputeStatus.UNDER_REVIEW
        } else {
            VendorPortalFinancialDisputeStatus.RESPONSE_REQUIRED
        }

        val event = VendorPortalFinancialDisputeEvent(
            eventId = "EVT-${UUID.randomUUID()}",
            disputeId = disputeId,
            actorId = actorId,
            actorRole = actorRole,
            action = "RESPONSE_POSTED",
            remarks = remarks,
            timestamp = now
        )

        val updated = existing.copy(
            status = nextStatus,
            proposedResolutionAmount = proposedResolutionAmount ?: existing.proposedResolutionAmount,
            updatedAt = now,
            events = existing.events + event
        )

        return when (val saveRes = portalRepository.saveFinancialDispute(updated)) {
            is DomainResult.Success -> {
                portalRepository.appendFinancialDisputeEvent(tenantId, projectId, vendorId, disputeId, event)
                portalRepository.recordActivity(
                    VendorPortalFinancialActivityEvent(
                        activityId = "ACT-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        eventType = VendorPortalFinancialActivityEventType.DISPUTE_RESPONSE_POSTED,
                        entityType = "FINANCIAL_DISPUTE",
                        entityId = disputeId,
                        actorId = actorId,
                        actorRole = actorRole,
                        description = "Response posted to dispute '${existing.disputeNumber}'.",
                        occurredAt = now
                    )
                )
                DomainResult.Success(saveRes.data)
            }
            is DomainResult.Error -> DomainResult.Error(saveRes.exception, saveRes.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    // =========================================================================
    // 4. PAYMENTS & PROJECTIONS
    // =========================================================================

    override suspend fun listPaymentHistory(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<List<VendorPortalPaymentSummary>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return when (val settlementsRes = canonicalSettlementService.listSettlements(vendorId = vendorId, projectId = projectId, tenantId = tenantId)) {
            is DomainResult.Success -> {
                val payments = settlementsRes.data.map { s ->
                    val status = when (s.status) {
                        com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus.SETTLED -> VendorPortalPaymentStatus.PAID
                        com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus.PROCESSING -> VendorPortalPaymentStatus.APPROVED
                        com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus.APPROVED -> VendorPortalPaymentStatus.APPROVED
                        com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus.DRAFT -> VendorPortalPaymentStatus.PENDING
                        com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus.RECONCILIATION_REQUIRED -> VendorPortalPaymentStatus.PENDING
                        com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus.CANCELLED -> VendorPortalPaymentStatus.CANCELLED
                        else -> VendorPortalPaymentStatus.PENDING
                    }
                    val maskedRef = s.referenceNumber?.let { if (it.length > 4) "****" + it.takeLast(4) else it } ?: s.paymentId?.let { "****" + it.takeLast(4) }
                    VendorPortalPaymentSummary(
                        settlementId = s.settlementId,
                        tenantId = s.tenantId,
                        projectId = s.projectId,
                        vendorId = s.vendorId,
                        settlementNumber = s.settlementNumber,
                        settlementDate = s.settlementDate,
                        currency = s.currency,
                        totalAmount = s.totalAmount,
                        paymentStatus = status,
                        paymentMethod = s.settlementMethod.name,
                        referenceNumber = maskedRef,
                        relatedInvoiceIds = s.allocations.mapNotNull { it.invoiceId },
                        notes = s.notes,
                        settledAt = s.settledAt
                    )
                }
                DomainResult.Success(payments)
            }
            is DomainResult.Error -> DomainResult.Error(settlementsRes.exception, settlementsRes.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    // =========================================================================
    // 5. FINANCIAL EVIDENCE
    // =========================================================================

    override suspend fun uploadEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String,
        evidenceType: VendorPortalSettlementEvidenceType,
        fileName: String,
        fileUrl: String,
        checksum: String?,
        fileSizeBytes: Long,
        mimeType: String?,
        description: String?,
        actorId: String
    ): DomainResult<VendorPortalFinancialSettlementEvidence> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val evidence = VendorPortalFinancialSettlementEvidence(
            evidenceId = "EVD-${UUID.randomUUID()}",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            entityType = entityType,
            entityId = entityId,
            evidenceType = evidenceType,
            fileName = fileName,
            fileUrl = fileUrl,
            checksum = checksum,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType ?: "application/pdf",
            description = description,
            uploadedBy = actorId,
            uploadedAt = System.currentTimeMillis()
        )

        return when (val res = portalRepository.saveEvidence(evidence)) {
            is DomainResult.Success -> {
                portalRepository.recordActivity(
                    VendorPortalFinancialActivityEvent(
                        activityId = "ACT-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        eventType = VendorPortalFinancialActivityEventType.EVIDENCE_ATTACHED,
                        entityType = entityType,
                        entityId = entityId,
                        actorId = actorId,
                        actorRole = "VENDOR",
                        description = "Evidence '$fileName' uploaded for $entityType '$entityId'.",
                        occurredAt = System.currentTimeMillis()
                    )
                )
                DomainResult.Success(res.data)
            }
            is DomainResult.Error -> DomainResult.Error(res.exception, res.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): DomainResult<List<VendorPortalFinancialSettlementEvidence>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return portalRepository.listEvidence(tenantId, projectId, vendorId, entityType, entityId)
    }

    // =========================================================================
    // 6. FINANCIAL THREADS & MESSAGES
    // =========================================================================

    override suspend fun listThreads(
        tenantId: String,
        projectId: String,
        vendorId: String,
        contextType: String?,
        contextId: String?
    ): DomainResult<List<VendorPortalFinancialThread>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return portalRepository.listThreads(tenantId, projectId, vendorId, contextType, contextId)
    }

    override suspend fun getThreadById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): DomainResult<VendorPortalFinancialThread> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return when (val res = portalRepository.findThreadById(tenantId, projectId, vendorId, threadId)) {
            is DomainResult.Success -> {
                val t = res.data ?: return DomainResult.Error(message = "Thread '$threadId' not found.")
                DomainResult.Success(t)
            }
            is DomainResult.Error -> DomainResult.Error(res.exception, res.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun postMessage(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String,
        content: String,
        evidenceReferences: List<String>,
        actorId: String,
        actorRole: String
    ): DomainResult<VendorPortalFinancialMessage> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val thread = when (val t = portalRepository.findThreadById(tenantId, projectId, vendorId, threadId)) {
            is DomainResult.Success -> t.data ?: return DomainResult.Error(message = "Thread '$threadId' not found.")
            is DomainResult.Error -> return DomainResult.Error(t.exception, t.message)
            DomainResult.Loading -> return DomainResult.Loading
        }

        val message = VendorPortalFinancialMessage(
            messageId = "MSG-${UUID.randomUUID()}",
            threadId = threadId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            senderId = actorId,
            senderRole = actorRole,
            content = content,
            evidenceReferences = evidenceReferences,
            timestamp = System.currentTimeMillis()
        )

        return when (val res = portalRepository.saveMessage(message)) {
            is DomainResult.Success -> {
                portalRepository.recordActivity(
                    VendorPortalFinancialActivityEvent(
                        activityId = "ACT-${UUID.randomUUID()}",
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        eventType = VendorPortalFinancialActivityEventType.MESSAGE_SENT,
                        entityType = "THREAD",
                        entityId = threadId,
                        actorId = actorId,
                        actorRole = actorRole,
                        description = "Message sent in thread '${thread.subject}'.",
                        occurredAt = System.currentTimeMillis()
                    )
                )
                DomainResult.Success(res.data)
            }
            is DomainResult.Error -> DomainResult.Error(res.exception, res.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    override suspend fun listMessages(
        tenantId: String,
        projectId: String,
        vendorId: String,
        threadId: String
    ): DomainResult<List<VendorPortalFinancialMessage>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return portalRepository.listMessages(tenantId, projectId, vendorId, threadId)
    }

    // =========================================================================
    // 7. ACTIVITY, ANALYTICS & WORKSPACE
    // =========================================================================

    override suspend fun listFinancialActivity(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): DomainResult<List<VendorPortalFinancialActivityEvent>> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        return portalRepository.listActivities(tenantId, projectId, vendorId, entityType, entityId)
    }

    override suspend fun getFinancialAnalytics(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalSettlementAnalyticsSummary> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val disputes = when (val d = portalRepository.listFinancialDisputes(tenantId, projectId, vendorId)) {
            is DomainResult.Success -> d.data
            else -> emptyList()
        }
        val reconciliations = when (val r = portalRepository.listReconciliationCases(tenantId, projectId, vendorId)) {
            is DomainResult.Success -> r.data
            else -> emptyList()
        }

        val canonicalSummary = when (val cs = canonicalSettlementService.getFinancialSummary(vendorId, tenantId, projectId)) {
            is DomainResult.Success -> cs.data
            else -> null
        }

        val currency = canonicalSummary?.currency ?: "BDT"
        val totalDisputed = disputes
            .filter { it.status != VendorPortalFinancialDisputeStatus.CLOSED && it.status != VendorPortalFinancialDisputeStatus.REJECTED }
            .fold(Money.ZERO) { acc, d -> acc + d.disputedAmount }

        val activeDisputes = disputes.count { it.status != VendorPortalFinancialDisputeStatus.RESOLVED && it.status != VendorPortalFinancialDisputeStatus.CLOSED && it.status != VendorPortalFinancialDisputeStatus.REJECTED }
        val pendingRecon = reconciliations.count { it.status != VendorPortalReconciliationCaseStatus.RESOLVED && it.status != VendorPortalReconciliationCaseStatus.CLOSED && it.status != VendorPortalReconciliationCaseStatus.CANCELLED }

        val resolvedDisputes = disputes.count { it.status == VendorPortalFinancialDisputeStatus.RESOLVED || it.status == VendorPortalFinancialDisputeStatus.CLOSED }
        val totalDisputes = disputes.size
        val resRate = if (totalDisputes > 0) (resolvedDisputes.toDouble() / totalDisputes.toDouble()) * 100.0 else 100.0

        val analytics = VendorPortalSettlementAnalyticsSummary(
            vendorId = vendorId,
            currency = currency,
            totalSettledAmount = canonicalSummary?.totalSettledAmount ?: Money.ZERO,
            totalOutstandingAmount = canonicalSummary?.totalOutstandingPayable ?: Money.ZERO,
            totalDisputedAmount = totalDisputed,
            totalReconciledAmount = canonicalSummary?.totalSettledAmount ?: Money.ZERO,
            activeDisputeCount = activeDisputes,
            pendingReconciliationCount = pendingRecon,
            averageSettlementCycleDays = canonicalSummary?.paymentCycleDays ?: 0.0,
            disputeResolutionRate = resRate
        )

        return DomainResult.Success(analytics)
    }

    override suspend fun getFinancialWorkspace(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalFinancialWorkspace> {
        val vendorCheck = validateVendor(tenantId, projectId, vendorId)
        if (vendorCheck is DomainResult.Error) return vendorCheck

        val settlements = when (val s = listSettlements(tenantId, projectId, vendorId)) {
            is DomainResult.Success -> s.data
            else -> emptyList()
        }

        val reconciliations = when (val r = portalRepository.listReconciliationCases(tenantId, projectId, vendorId)) {
            is DomainResult.Success -> r.data
            else -> emptyList()
        }

        val disputes = when (val d = portalRepository.listFinancialDisputes(tenantId, projectId, vendorId)) {
            is DomainResult.Success -> d.data
            else -> emptyList()
        }

        val activities = when (val a = portalRepository.listActivities(tenantId, projectId, vendorId)) {
            is DomainResult.Success -> a.data.take(10)
            else -> emptyList()
        }

        val analytics = when (val an = getFinancialAnalytics(tenantId, projectId, vendorId)) {
            is DomainResult.Success -> an.data
            else -> VendorPortalSettlementAnalyticsSummary(vendorId = vendorId)
        }

        val workspace = VendorPortalFinancialWorkspace(
            settlementOverview = settlements,
            outstandingBalance = analytics.totalOutstandingAmount,
            pendingReconciliations = reconciliations.filter { it.status != VendorPortalReconciliationCaseStatus.RESOLVED && it.status != VendorPortalReconciliationCaseStatus.CLOSED },
            openDisputes = disputes.filter { it.status != VendorPortalFinancialDisputeStatus.RESOLVED && it.status != VendorPortalFinancialDisputeStatus.CLOSED && it.status != VendorPortalFinancialDisputeStatus.REJECTED },
            recentActivity = activities,
            analytics = analytics
        )

        return DomainResult.Success(workspace)
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private suspend fun validateVendor(tenantId: String, projectId: String, vendorId: String): DomainResult<Unit> {
        return when (val v = vendorRepository.findById(projectId, vendorId)) {
            is DomainResult.Success -> DomainResult.Success(Unit)
            is DomainResult.Error -> DomainResult.Error(v.exception, v.message)
            DomainResult.Loading -> DomainResult.Loading
        }
    }

    private fun projectSettlementSummary(
        s: VendorSettlement,
        ack: VendorPortalSettlementAcknowledgement?
    ): VendorPortalSettlementSummary {
        val maskedRef = s.referenceNumber?.let { if (it.length > 4) "****" + it.takeLast(4) else it }
            ?: s.paymentId?.let { "****" + it.takeLast(4) }

        return VendorPortalSettlementSummary(
            settlementId = s.settlementId,
            tenantId = s.tenantId,
            projectId = s.projectId,
            vendorId = s.vendorId,
            settlementNumber = s.settlementNumber,
            settlementDate = s.settlementDate,
            currency = s.currency,
            grossAmount = s.totalAmount, // Canonical total amount represents net payable
            deductions = Money.ZERO,
            credits = Money.ZERO,
            netPayable = s.totalAmount,
            status = s.status,
            settlementMethod = s.settlementMethod,
            maskedPaymentReference = maskedRef,
            notes = s.notes,
            approvedAt = s.approvedAt,
            settledAt = s.settledAt,
            allocationCount = s.allocations.size,
            acknowledgementStatus = ack?.status ?: VendorPortalSettlementViewStatus.VIEW_ONLY
        )
    }
}
