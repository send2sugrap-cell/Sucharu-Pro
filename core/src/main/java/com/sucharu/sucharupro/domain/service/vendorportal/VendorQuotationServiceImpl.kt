package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorQuotationRepository
import com.sucharu.sucharupro.domain.repository.VendorRfqRepository
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorQuotationCalculator
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorQuotationValidator
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorRfqValidator
import java.util.UUID

class VendorQuotationServiceImpl(
    private val quotationRepository: VendorQuotationRepository,
    private val rfqRepository: VendorRfqRepository
) : VendorQuotationService {

    override suspend fun createQuotationDraft(
        quotation: VendorQuotation,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation> {
        return try {
            // 1. Verify RFQ exists and is open for bidding
            val rfq = when (val res = rfqRepository.findRfqById(quotation.rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            require(rfq.status.isBiddable) {
                "Cannot create quotation for RFQ in status ${rfq.status}. RFQ must be published/open."
            }

            VendorRfqValidator.validateDeadline(rfq.responseDeadline)

            // 2. Verify Invitation exists
            val inv = when (val res = rfqRepository.findInvitation(quotation.rfqId, quotation.vendorId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(SecurityException("Vendor '${quotation.vendorId}' is not invited to RFQ '${quotation.rfqId}'."))
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            require(inv.status != VendorRfqInvitationStatus.DECLINED) {
                "Vendor has declined the invitation to this RFQ."
            }

            // 3. Compute item line totals and grand totals
            val itemsWithCalculatedTotals = quotation.items.map { item ->
                val lineTotal = VendorQuotationCalculator.calculateLineTotal(
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discountAmount = item.discountAmount,
                    taxAmount = item.taxAmount
                )
                item.copy(lineTotal = lineTotal)
            }

            val financialSummary = VendorQuotationCalculator.calculateQuotationTotals(itemsWithCalculatedTotals)

            val draft = quotation.copy(
                status = VendorQuotationStatus.DRAFT,
                items = itemsWithCalculatedTotals,
                subtotal = financialSummary.subtotal,
                totalDiscount = financialSummary.totalDiscount,
                totalTax = financialSummary.totalTax,
                grandTotal = financialSummary.grandTotal,
                createdBy = actorId,
                createdAt = System.currentTimeMillis()
            )

            VendorQuotationValidator.validateQuotation(draft)

            val created = quotationRepository.createQuotation(draft)
            if (created is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = quotation.projectId,
                        rfqId = quotation.rfqId,
                        vendorId = quotation.vendorId,
                        quotationId = draft.quotationId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.QUOTATION_CREATED,
                        action = "CREATE_QUOTATION_DRAFT",
                        details = "Created draft quotation '${draft.quotationNumber}' (Grand Total: ${draft.grandTotal.amount})"
                    )
                )
            }
            created
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun updateQuotationDraft(
        quotation: VendorQuotation,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation> {
        return try {
            val existing = when (val res = quotationRepository.findQuotationById(quotation.quotationId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            require(existing.status.isEditable) {
                "Cannot directly modify quotation in status ${existing.status}. Modifications require formal revision."
            }

            val rfq = when (val res = rfqRepository.findRfqById(existing.rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorRfqValidator.validateDeadline(rfq.responseDeadline)

            val itemsWithCalculatedTotals = quotation.items.map { item ->
                val lineTotal = VendorQuotationCalculator.calculateLineTotal(
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discountAmount = item.discountAmount,
                    taxAmount = item.taxAmount
                )
                item.copy(lineTotal = lineTotal)
            }

            val financialSummary = VendorQuotationCalculator.calculateQuotationTotals(itemsWithCalculatedTotals)

            val updated = existing.copy(
                vendorReferenceNumber = quotation.vendorReferenceNumber,
                currency = quotation.currency,
                validityPeriodDays = quotation.validityPeriodDays,
                paymentTerms = quotation.paymentTerms,
                deliveryLeadTimeDays = quotation.deliveryLeadTimeDays,
                shippingTerms = quotation.shippingTerms,
                notes = quotation.notes,
                items = itemsWithCalculatedTotals,
                subtotal = financialSummary.subtotal,
                totalDiscount = financialSummary.totalDiscount,
                totalTax = financialSummary.totalTax,
                grandTotal = financialSummary.grandTotal,
                updatedBy = actorId,
                updatedAt = System.currentTimeMillis(),
                version = existing.version + 1
            )

            VendorQuotationValidator.validateQuotation(updated)

            val saved = quotationRepository.updateQuotation(updated)
            if (saved is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = updated.projectId,
                        rfqId = updated.rfqId,
                        vendorId = updated.vendorId,
                        quotationId = updated.quotationId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.QUOTATION_UPDATED,
                        action = "UPDATE_QUOTATION_DRAFT",
                        details = "Updated draft quotation '${updated.quotationNumber}'"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun submitQuotation(
        quotationId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation> {
        return try {
            val quotation = when (val res = quotationRepository.findQuotationById(quotationId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val rfq = when (val res = rfqRepository.findRfqById(quotation.rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            require(rfq.status.isBiddable) {
                "Cannot submit quotation. RFQ is currently in status ${rfq.status}."
            }

            VendorRfqValidator.validateDeadline(rfq.responseDeadline)
            VendorQuotationValidator.validateQuotationTransition(quotation.status, VendorQuotationStatus.SUBMITTED)
            VendorQuotationValidator.validateQuotationSubmission(quotation, rfq.items)

            val submitted = quotationRepository.updateQuotation(
                quotation.copy(
                    status = VendorQuotationStatus.SUBMITTED,
                    submittedAt = System.currentTimeMillis(),
                    submittedBy = actorId,
                    updatedBy = actorId,
                    updatedAt = System.currentTimeMillis(),
                    version = quotation.version + 1
                )
            )

            if (submitted is DomainResult.Success) {
                // Update invitation respondedAt
                val invRes = rfqRepository.findInvitation(quotation.rfqId, quotation.vendorId, tenantId)
                if (invRes is DomainResult.Success) {
                    rfqRepository.updateInvitation(
                        invRes.data.copy(
                            status = VendorRfqInvitationStatus.RESPONDED,
                            respondedAt = System.currentTimeMillis(),
                            version = invRes.data.version + 1
                        )
                    )
                }

                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = quotation.projectId,
                        rfqId = quotation.rfqId,
                        vendorId = quotation.vendorId,
                        quotationId = quotation.quotationId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.QUOTATION_SUBMITTED,
                        action = "SUBMIT_QUOTATION",
                        details = "Submitted quotation '${quotation.quotationNumber}' (Grand Total: ${quotation.grandTotal.amount})"
                    )
                )
            }
            submitted
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun withdrawQuotation(
        quotationId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation> {
        return try {
            require(reason.isNotBlank()) { "Withdrawal reason cannot be blank." }
            val quotation = when (val res = quotationRepository.findQuotationById(quotationId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorQuotationValidator.validateQuotationTransition(quotation.status, VendorQuotationStatus.WITHDRAWN)

            val withdrawn = quotationRepository.updateQuotation(
                quotation.copy(
                    status = VendorQuotationStatus.WITHDRAWN,
                    notes = "${quotation.notes ?: ""}\n[Withdrawn: $reason]".trim(),
                    updatedBy = actorId,
                    updatedAt = System.currentTimeMillis(),
                    version = quotation.version + 1
                )
            )

            if (withdrawn is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = quotation.projectId,
                        rfqId = quotation.rfqId,
                        vendorId = quotation.vendorId,
                        quotationId = quotation.quotationId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.QUOTATION_WITHDRAWN,
                        action = "WITHDRAW_QUOTATION",
                        details = "Withdrew quotation '${quotation.quotationNumber}': $reason"
                    )
                )
            }
            withdrawn
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun requestRevision(
        quotationId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation> {
        return try {
            val quotation = when (val res = quotationRepository.findQuotationById(quotationId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorQuotationValidator.validateRevisionRequest(quotation, reason)
            VendorQuotationValidator.validateQuotationTransition(quotation.status, VendorQuotationStatus.REVISION_REQUESTED)

            val updated = quotationRepository.updateQuotation(
                quotation.copy(
                    status = VendorQuotationStatus.REVISION_REQUESTED,
                    notes = "${quotation.notes ?: ""}\n[Revision Requested: $reason]".trim(),
                    updatedBy = actorId,
                    updatedAt = System.currentTimeMillis(),
                    version = quotation.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = quotation.projectId,
                        rfqId = quotation.rfqId,
                        vendorId = quotation.vendorId,
                        quotationId = quotation.quotationId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.QUOTATION_REVISION_REQUESTED,
                        action = "REQUEST_QUOTATION_REVISION",
                        details = "Requested revision for quotation '${quotation.quotationNumber}': $reason"
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun submitRevision(
        quotationId: String,
        revisedQuotation: VendorQuotation,
        reasonForRevision: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation> {
        return try {
            require(reasonForRevision.isNotBlank()) { "Reason for revision cannot be blank." }
            val current = when (val res = quotationRepository.findQuotationById(quotationId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            require(current.status in setOf(VendorQuotationStatus.REVISION_REQUESTED, VendorQuotationStatus.SUBMITTED, VendorQuotationStatus.UNDER_REVIEW)) {
                "Cannot revise quotation in status ${current.status}."
            }

            val rfq = when (val res = rfqRepository.findRfqById(current.rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorRfqValidator.validateDeadline(rfq.responseDeadline)

            // 1. Record snapshot of the previous revision
            val revisionSnapshot = VendorQuotationRevision(
                revisionId = UUID.randomUUID().toString(),
                quotationId = current.quotationId,
                rfqId = current.rfqId,
                vendorId = current.vendorId,
                tenantId = tenantId,
                projectId = current.projectId,
                revisionNumber = current.revisionNumber,
                reasonForRevision = reasonForRevision,
                snapshotSubtotal = current.subtotal,
                snapshotGrandTotal = current.grandTotal,
                itemsSnapshotJson = current.items.joinToString(";") { "${it.rfqItemId}:${it.quantity}@${it.unitPrice.amount}=${it.lineTotal.amount}" },
                revisedBy = actorId,
                revisedAt = System.currentTimeMillis()
            )
            quotationRepository.recordRevision(revisionSnapshot)

            // 2. Compute new totals
            val itemsWithCalculatedTotals = revisedQuotation.items.map { item ->
                val lineTotal = VendorQuotationCalculator.calculateLineTotal(
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discountAmount = item.discountAmount,
                    taxAmount = item.taxAmount
                )
                item.copy(lineTotal = lineTotal)
            }
            val financialSummary = VendorQuotationCalculator.calculateQuotationTotals(itemsWithCalculatedTotals)

            val updated = current.copy(
                revisionNumber = current.revisionNumber + 1,
                items = itemsWithCalculatedTotals,
                subtotal = financialSummary.subtotal,
                totalDiscount = financialSummary.totalDiscount,
                totalTax = financialSummary.totalTax,
                grandTotal = financialSummary.grandTotal,
                vendorReferenceNumber = revisedQuotation.vendorReferenceNumber ?: current.vendorReferenceNumber,
                paymentTerms = revisedQuotation.paymentTerms ?: current.paymentTerms,
                deliveryLeadTimeDays = revisedQuotation.deliveryLeadTimeDays,
                shippingTerms = revisedQuotation.shippingTerms ?: current.shippingTerms,
                notes = revisedQuotation.notes ?: current.notes,
                status = VendorQuotationStatus.REVISED,
                submittedAt = System.currentTimeMillis(),
                submittedBy = actorId,
                updatedBy = actorId,
                updatedAt = System.currentTimeMillis(),
                version = current.version + 1
            )

            VendorQuotationValidator.validateQuotationSubmission(updated, rfq.items)

            val saved = quotationRepository.updateQuotation(updated)
            if (saved is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = updated.projectId,
                        rfqId = updated.rfqId,
                        vendorId = updated.vendorId,
                        quotationId = updated.quotationId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.QUOTATION_REVISED,
                        action = "REVISE_QUOTATION",
                        details = "Revised quotation to Rev ${updated.revisionNumber} (Grand Total: ${updated.grandTotal.amount}): $reasonForRevision"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getQuotationById(quotationId: String, tenantId: String): DomainResult<VendorQuotation> =
        quotationRepository.findQuotationById(quotationId, tenantId)

    override suspend fun getQuotationForVendor(quotationId: String, vendorId: String, tenantId: String): DomainResult<VendorQuotation> {
        val qRes = quotationRepository.findQuotationById(quotationId, tenantId)
        return when (qRes) {
            is DomainResult.Success -> {
                if (qRes.data.vendorId != vendorId) {
                    DomainResult.Error(SecurityException("Quotation '$quotationId' does not belong to vendor '$vendorId'."))
                } else qRes
            }
            else -> qRes
        }
    }

    override suspend fun listQuotationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorQuotation>> =
        quotationRepository.listQuotationsByRfq(rfqId, tenantId)

    override suspend fun listQuotationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorQuotation>> =
        quotationRepository.listQuotationsByVendor(vendorId, tenantId)

    override suspend fun listRevisions(quotationId: String, tenantId: String): DomainResult<List<VendorQuotationRevision>> =
        quotationRepository.listRevisionsByQuotation(quotationId, tenantId)
}
