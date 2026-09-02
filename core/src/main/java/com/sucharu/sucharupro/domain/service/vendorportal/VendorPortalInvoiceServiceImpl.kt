package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalInvoiceRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.service.vendor.VendorInvoiceService
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderService
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementService
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalInvoiceValidator
import java.math.BigDecimal
import java.util.UUID

/**
 * Implementation of VendorPortalInvoiceService enforcing multi-tenant isolation,
 * role authorizations, invariant validation, and integrating with canonical Module 12 aggregates.
 */
class VendorPortalInvoiceServiceImpl(
    private val invoiceRepository: VendorPortalInvoiceRepository,
    private val vendorInvoiceService: VendorInvoiceService,
    private val vendorPurchaseOrderService: VendorPurchaseOrderService,
    private val vendorSettlementService: VendorSettlementService,
    private val vendorRepository: VendorRepository
) : VendorPortalInvoiceService {

    override suspend fun listInvoices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorInvoiceStatus?,
        matchStatus: VendorInvoiceMatchStatus?
    ): DomainResult<List<VendorPortalInvoiceSummary>> {
        val vendorRes = vendorRepository.findById(projectId, vendorId)
        if (vendorRes is DomainResult.Error) return vendorRes
        val vendor = (vendorRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(NoSuchElementException("Vendor '$vendorId' not found"))

        val canonicalInvoicesRes = vendorInvoiceService.listInvoices(
            projectId = projectId,
            vendorId = vendorId,
            status = status,
            matchStatus = matchStatus
        )
        if (canonicalInvoicesRes is DomainResult.Error) return canonicalInvoicesRes
        val canonicalInvoices = (canonicalInvoicesRes as DomainResult.Success).data

        // Fetch settlements to map paid/approved amounts accurately
        val settlementsRes = vendorSettlementService.listSettlements(
            vendorId = vendorId,
            projectId = projectId,
            tenantId = tenantId
        )
        val settlements = (settlementsRes as? DomainResult.Success)?.data ?: emptyList()

        val summaries = canonicalInvoices.map { inv ->
            val poRes = vendorPurchaseOrderService.getOrderById(projectId, inv.purchaseOrderId)
            val orderNumber = (poRes as? DomainResult.Success)?.data?.orderNumber ?: "PO-${inv.purchaseOrderId}"

            val allocatedPaid = settlements
                .filter { it.status == VendorSettlementStatus.SETTLED }
                .flatMap { it.allocations }
                .filter { it.invoiceId == inv.invoiceId }
                .fold(Money.ZERO) { acc, alloc -> acc.plus(alloc.allocatedAmount) }

            val approvedAmount = if (inv.status == VendorInvoiceStatus.APPROVED || inv.status == VendorInvoiceStatus.POSTED) {
                inv.totalAmount
            } else {
                Money.ZERO
            }

            val outstanding = if (inv.status == VendorInvoiceStatus.APPROVED || inv.status == VendorInvoiceStatus.POSTED) {
                val rem = inv.totalAmount.amount.subtract(allocatedPaid.amount).max(BigDecimal.ZERO)
                Money(rem)
            } else {
                Money.ZERO
            }

            val paymentStatus = when {
                allocatedPaid.amount >= inv.totalAmount.amount && inv.totalAmount.amount > BigDecimal.ZERO -> VendorPortalPaymentStatus.PAID
                allocatedPaid.amount > BigDecimal.ZERO -> VendorPortalPaymentStatus.PARTIALLY_PAID
                inv.status == VendorInvoiceStatus.APPROVED || inv.status == VendorInvoiceStatus.POSTED -> VendorPortalPaymentStatus.APPROVED
                inv.status == VendorInvoiceStatus.REJECTED -> VendorPortalPaymentStatus.REJECTED
                inv.status == VendorInvoiceStatus.CANCELLED -> VendorPortalPaymentStatus.CANCELLED
                else -> VendorPortalPaymentStatus.PENDING
            }

            VendorPortalInvoiceSummary(
                invoiceId = inv.invoiceId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                purchaseOrderId = inv.purchaseOrderId,
                orderNumber = orderNumber,
                invoiceNumber = inv.invoiceNumber,
                vendorInvoiceNumber = inv.vendorInvoiceNumber,
                invoiceDate = inv.invoiceDate,
                receivedDate = inv.receivedDate,
                currency = inv.currency,
                subtotal = inv.subtotal,
                taxAmount = inv.taxAmount,
                discountAmount = inv.discountAmount,
                shippingAmount = inv.shippingAmount,
                otherCharges = inv.otherCharges,
                totalAmount = inv.totalAmount,
                approvedAmount = approvedAmount,
                paidAmount = allocatedPaid,
                outstandingAmount = outstanding,
                status = inv.status,
                matchStatus = inv.matchStatus,
                paymentStatus = paymentStatus,
                exceptionCount = 0,
                notes = inv.notes,
                createdAt = inv.createdAt,
                updatedAt = inv.updatedAt
            )
        }

        return DomainResult.Success(summaries)
    }

    override suspend fun getInvoiceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): DomainResult<VendorPortalInvoiceSummary> {
        val invRes = vendorInvoiceService.getInvoiceById(projectId, invoiceId)
        if (invRes is DomainResult.Error) return invRes
        val inv = (invRes as DomainResult.Success).data

        if (inv.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Access denied: Invoice does not belong to vendor '$vendorId'."))
        }

        val poRes = vendorPurchaseOrderService.getOrderById(projectId, inv.purchaseOrderId)
        val orderNumber = (poRes as? DomainResult.Success)?.data?.orderNumber ?: "PO-${inv.purchaseOrderId}"

        val settlementsRes = vendorSettlementService.listSettlements(vendorId = vendorId, projectId = projectId, tenantId = tenantId)
        val settlements = (settlementsRes as? DomainResult.Success)?.data ?: emptyList()

        val allocatedPaid = settlements
            .filter { it.status == VendorSettlementStatus.SETTLED }
            .flatMap { it.allocations }
            .filter { it.invoiceId == inv.invoiceId }
            .fold(Money.ZERO) { acc, alloc -> acc.plus(alloc.allocatedAmount) }

        val approvedAmount = if (inv.status == VendorInvoiceStatus.APPROVED || inv.status == VendorInvoiceStatus.POSTED) {
            inv.totalAmount
        } else {
            Money.ZERO
        }

        val outstanding = if (inv.status == VendorInvoiceStatus.APPROVED || inv.status == VendorInvoiceStatus.POSTED) {
            val rem = inv.totalAmount.amount.subtract(allocatedPaid.amount).max(BigDecimal.ZERO)
            Money(rem)
        } else {
            Money.ZERO
        }

        val paymentStatus = when {
            allocatedPaid.amount >= inv.totalAmount.amount && inv.totalAmount.amount > BigDecimal.ZERO -> VendorPortalPaymentStatus.PAID
            allocatedPaid.amount > BigDecimal.ZERO -> VendorPortalPaymentStatus.PARTIALLY_PAID
            inv.status == VendorInvoiceStatus.APPROVED || inv.status == VendorInvoiceStatus.POSTED -> VendorPortalPaymentStatus.APPROVED
            inv.status == VendorInvoiceStatus.REJECTED -> VendorPortalPaymentStatus.REJECTED
            inv.status == VendorInvoiceStatus.CANCELLED -> VendorPortalPaymentStatus.CANCELLED
            else -> VendorPortalPaymentStatus.PENDING
        }

        return DomainResult.Success(
            VendorPortalInvoiceSummary(
                invoiceId = inv.invoiceId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                purchaseOrderId = inv.purchaseOrderId,
                orderNumber = orderNumber,
                invoiceNumber = inv.invoiceNumber,
                vendorInvoiceNumber = inv.vendorInvoiceNumber,
                invoiceDate = inv.invoiceDate,
                receivedDate = inv.receivedDate,
                currency = inv.currency,
                subtotal = inv.subtotal,
                taxAmount = inv.taxAmount,
                discountAmount = inv.discountAmount,
                shippingAmount = inv.shippingAmount,
                otherCharges = inv.otherCharges,
                totalAmount = inv.totalAmount,
                approvedAmount = approvedAmount,
                paidAmount = allocatedPaid,
                outstandingAmount = outstanding,
                status = inv.status,
                matchStatus = inv.matchStatus,
                paymentStatus = paymentStatus,
                exceptionCount = 0,
                notes = inv.notes,
                createdAt = inv.createdAt,
                updatedAt = inv.updatedAt
            )
        )
    }

    override suspend fun getThreeWayMatch(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): DomainResult<VendorPortalInvoiceMatchSummary> {
        val invRes = vendorInvoiceService.getInvoiceById(projectId, invoiceId)
        if (invRes is DomainResult.Error) return invRes
        val inv = (invRes as DomainResult.Success).data
        if (inv.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Access denied: Invoice does not belong to vendor '$vendorId'."))
        }

        val matchRes = vendorInvoiceService.getMatchResult(projectId, invoiceId)
        if (matchRes is DomainResult.Error) return matchRes
        val match = (matchRes as DomainResult.Success).data

        val lineSummaries = match.lines.map { line ->
            VendorPortalInvoiceMatchLineSummary(
                matchLineId = line.matchLineId,
                invoiceItemId = line.invoiceItemId,
                purchaseOrderItemId = line.purchaseOrderItemId,
                deliveryReceiptItemId = line.deliveryReceiptItemId,
                description = line.description,
                orderedQuantity = line.orderedQuantity,
                receivedQuantity = line.receivedQuantity,
                acceptedQuantity = line.receivedQuantity, // canonical received as benchmark
                invoicedQuantity = line.invoicedQuantity,
                orderedUnitPrice = line.orderedUnitPrice,
                invoicedUnitPrice = line.invoicedUnitPrice,
                quantityVariance = line.quantityVariance,
                priceVariance = line.priceVariance,
                amountVariance = line.amountVariance,
                matchStatus = line.matchStatus,
                exceptionReason = line.exceptionReason
            )
        }

        return DomainResult.Success(
            VendorPortalInvoiceMatchSummary(
                matchId = match.matchId,
                invoiceId = match.invoiceId,
                purchaseOrderId = match.purchaseOrderId,
                matchStatus = match.matchStatus,
                matchedAt = match.matchedAt,
                subtotalVariance = match.subtotalVariance,
                quantityVariance = match.quantityVariance,
                priceVariance = match.priceVariance,
                taxVariance = match.taxVariance,
                totalVariance = match.totalVariance,
                currencyMismatch = match.currencyMismatch,
                vendorMismatch = match.vendorMismatch,
                exceptionCount = match.exceptionCount,
                lines = lineSummaries
            )
        )
    }

    override suspend fun createInvoiceSubmission(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String,
        vendorInvoiceNumber: String,
        invoiceDate: Long,
        currency: String,
        shippingAmount: BigDecimal?,
        otherCharges: BigDecimal?,
        notes: String?,
        items: List<VendorPortalInvoiceSubmissionItemInput>,
        actorId: String
    ): DomainResult<VendorPortalInvoiceSubmission> {
        val vendorRes = vendorRepository.findById(projectId, vendorId)
        if (vendorRes is DomainResult.Error) return vendorRes

        val poRes = vendorPurchaseOrderService.getOrderById(projectId, purchaseOrderId)
        if (poRes is DomainResult.Error) return poRes
        val po = (poRes as DomainResult.Success).data
        if (po.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Unauthorized access to purchase order '$purchaseOrderId' for different vendor."))
        }

        val submissionId = UUID.randomUUID().toString()
        val poItemsMap = po.items.associateBy { it.itemId }

        var subtotal = BigDecimal.ZERO
        var totalTax = BigDecimal.ZERO

        val submissionItems = items.map { input ->
            val poItem = poItemsMap[input.purchaseOrderItemId]
                ?: return DomainResult.Error(IllegalArgumentException("Purchase order item '${input.purchaseOrderItemId}' does not belong to PO '$purchaseOrderId'."))

            val unitPrice = input.unitPrice ?: poItem.unitRate
            val lineTax = input.taxAmount ?: Money.ZERO
            val lineTotal = Money(unitPrice.amount.multiply(input.invoicedQuantity).add(lineTax.amount))

            subtotal = subtotal.add(unitPrice.amount.multiply(input.invoicedQuantity))
            totalTax = totalTax.add(lineTax.amount)

            VendorPortalInvoiceSubmissionItem(
                itemId = UUID.randomUUID().toString(),
                submissionId = submissionId,
                tenantId = tenantId,
                purchaseOrderItemId = input.purchaseOrderItemId,
                deliveryReceiptItemId = input.deliveryReceiptItemId,
                itemName = poItem.itemDescription,
                itemCode = poItem.itemCode,
                invoicedQuantity = input.invoicedQuantity,
                unitOfMeasure = poItem.unitOfMeasure.name,
                unitPrice = unitPrice,
                taxAmount = lineTax,
                lineTotal = lineTotal,
                remarks = input.remarks
            )
        }

        val shipMoney = Money(shippingAmount ?: BigDecimal.ZERO)
        val otherMoney = Money(otherCharges ?: BigDecimal.ZERO)
        val grandTotal = Money(subtotal.add(totalTax).add(shipMoney.amount).add(otherMoney.amount))

        val submission = VendorPortalInvoiceSubmission(
            submissionId = submissionId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            purchaseOrderId = purchaseOrderId,
            orderNumber = po.orderNumber,
            vendorInvoiceNumber = vendorInvoiceNumber,
            invoiceDate = invoiceDate,
            currency = currency,
            subtotalAmount = Money(subtotal),
            taxAmount = Money(totalTax),
            discountAmount = Money.ZERO,
            shippingAmount = shipMoney,
            otherCharges = otherMoney,
            totalAmount = grandTotal,
            notes = notes,
            status = VendorPortalInvoiceSubmissionStatus.DRAFT,
            items = submissionItems,
            createdAt = System.currentTimeMillis(),
            createdBy = actorId,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        try {
            VendorPortalInvoiceValidator.validateInvoiceSubmission(submission)
        } catch (e: Exception) {
            return DomainResult.Error(e)
        }

        val saved = invoiceRepository.saveSubmission(submission)
        if (saved is DomainResult.Success) {
            invoiceRepository.recordAuditEvent(
                VendorPortalInvoiceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    targetType = "INVOICE_SUBMISSION",
                    targetId = submissionId,
                    action = "CREATED_DRAFT",
                    actorId = actorId,
                    actorRole = "VENDOR_USER"
                )
            )
        }
        return saved
    }

    override suspend fun getInvoiceSubmission(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String
    ): DomainResult<VendorPortalInvoiceSubmission> {
        val res = invoiceRepository.findSubmissionById(tenantId, projectId, vendorId, submissionId)
        if (res is DomainResult.Error) return res
        val submission = (res as DomainResult.Success).data
            ?: return DomainResult.Error(NoSuchElementException("Invoice submission '$submissionId' not found."))
        return DomainResult.Success(submission)
    }

    override suspend fun listInvoiceSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String?,
        status: VendorPortalInvoiceSubmissionStatus?
    ): DomainResult<List<VendorPortalInvoiceSubmission>> {
        return invoiceRepository.listSubmissions(tenantId, projectId, vendorId, purchaseOrderId, status)
    }

    override suspend fun submitInvoiceSubmission(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String,
        actorId: String
    ): DomainResult<VendorPortalInvoiceSubmission> {
        val existingRes = getInvoiceSubmission(tenantId, projectId, vendorId, submissionId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        try {
            VendorPortalInvoiceValidator.validateSubmissionStatusTransition(existing.status, VendorPortalInvoiceSubmissionStatus.SUBMITTED)
        } catch (e: Exception) {
            return DomainResult.Error(e)
        }

        val updated = existing.copy(
            status = VendorPortalInvoiceSubmissionStatus.SUBMITTED,
            submittedAt = System.currentTimeMillis(),
            submittedBy = actorId,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )

        val saved = invoiceRepository.saveSubmission(updated)
        if (saved is DomainResult.Success) {
            invoiceRepository.recordAuditEvent(
                VendorPortalInvoiceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    targetType = "INVOICE_SUBMISSION",
                    targetId = submissionId,
                    action = "SUBMITTED",
                    actorId = actorId,
                    actorRole = "VENDOR_USER"
                )
            )
        }
        return saved
    }

    override suspend fun cancelInvoiceSubmission(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String,
        reason: String,
        actorId: String
    ): DomainResult<VendorPortalInvoiceSubmission> {
        val existingRes = getInvoiceSubmission(tenantId, projectId, vendorId, submissionId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        try {
            VendorPortalInvoiceValidator.validateSubmissionStatusTransition(existing.status, VendorPortalInvoiceSubmissionStatus.CANCELLED)
        } catch (e: Exception) {
            return DomainResult.Error(e)
        }

        val updated = existing.copy(
            status = VendorPortalInvoiceSubmissionStatus.CANCELLED,
            rejectionReason = reason,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )

        val saved = invoiceRepository.saveSubmission(updated)
        if (saved is DomainResult.Success) {
            invoiceRepository.recordAuditEvent(
                VendorPortalInvoiceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    targetType = "INVOICE_SUBMISSION",
                    targetId = submissionId,
                    action = "CANCELLED",
                    actorId = actorId,
                    actorRole = "VENDOR_USER",
                    payload = "Reason: $reason"
                )
            )
        }
        return saved
    }

    override suspend fun respondToInvoice(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String,
        exceptionId: String?,
        responseType: VendorPortalInvoiceResponseType,
        comment: String,
        proposedCorrection: String?,
        evidenceReferences: List<String>,
        actorId: String
    ): DomainResult<VendorPortalInvoiceResponse> {
        val invRes = vendorInvoiceService.getInvoiceById(projectId, invoiceId)
        if (invRes is DomainResult.Error) return invRes
        val inv = (invRes as DomainResult.Success).data
        if (inv.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Access denied: Invoice does not belong to vendor '$vendorId'."))
        }

        val response = VendorPortalInvoiceResponse(
            responseId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            invoiceId = invoiceId,
            exceptionId = exceptionId,
            responseType = responseType,
            comment = comment,
            proposedCorrection = proposedCorrection,
            evidenceReferences = evidenceReferences,
            respondedBy = actorId,
            respondedAt = System.currentTimeMillis()
        )

        try {
            VendorPortalInvoiceValidator.validateInvoiceResponse(response)
        } catch (e: Exception) {
            return DomainResult.Error(e)
        }

        val saved = invoiceRepository.saveResponse(response)
        if (saved is DomainResult.Success) {
            invoiceRepository.recordAuditEvent(
                VendorPortalInvoiceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    targetType = "INVOICE_RESPONSE",
                    targetId = response.responseId,
                    action = responseType.name,
                    actorId = actorId,
                    actorRole = "VENDOR_USER",
                    payload = comment
                )
            )
        }
        return saved
    }

    override suspend fun listInvoiceResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): DomainResult<List<VendorPortalInvoiceResponse>> {
        return invoiceRepository.listResponses(tenantId, projectId, vendorId, invoiceId)
    }

    override suspend fun uploadFinancialEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String,
        evidenceType: VendorPortalFinancialEvidenceType,
        filename: String,
        fileReference: String,
        mimeType: String,
        sizeBytes: Long,
        actorId: String
    ): DomainResult<VendorPortalFinancialEvidence> {
        val evidence = VendorPortalFinancialEvidence(
            evidenceId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            entityType = entityType,
            entityId = entityId,
            evidenceType = evidenceType,
            filename = filename,
            fileReference = fileReference,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            uploadedBy = actorId,
            uploadedAt = System.currentTimeMillis()
        )

        try {
            VendorPortalInvoiceValidator.validateFinancialEvidence(evidence)
        } catch (e: Exception) {
            return DomainResult.Error(e)
        }

        val saved = invoiceRepository.saveEvidence(evidence)
        if (saved is DomainResult.Success) {
            invoiceRepository.recordAuditEvent(
                VendorPortalInvoiceAuditEvent(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    targetType = "FINANCIAL_EVIDENCE",
                    targetId = evidence.evidenceId,
                    action = "UPLOADED",
                    actorId = actorId,
                    actorRole = "VENDOR_USER",
                    payload = "$filename ($evidenceType)"
                )
            )
        }
        return saved
    }

    override suspend fun listFinancialEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): DomainResult<List<VendorPortalFinancialEvidence>> {
        return invoiceRepository.listEvidence(tenantId, projectId, vendorId, entityType, entityId)
    }

    override suspend fun listPayments(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalPaymentStatus?
    ): DomainResult<List<VendorPortalPaymentSummary>> {
        val settlementsRes = vendorSettlementService.listSettlements(
            vendorId = vendorId,
            projectId = projectId,
            tenantId = tenantId
        )
        if (settlementsRes is DomainResult.Error) return settlementsRes
        val settlements = (settlementsRes as DomainResult.Success).data

        val summaries = settlements.map { s ->
            val pStatus = when (s.status) {
                VendorSettlementStatus.DRAFT, VendorSettlementStatus.ELIGIBLE, VendorSettlementStatus.PROCESSING, VendorSettlementStatus.RECONCILIATION_REQUIRED -> VendorPortalPaymentStatus.PENDING
                VendorSettlementStatus.APPROVED -> VendorPortalPaymentStatus.APPROVED
                VendorSettlementStatus.SETTLED -> VendorPortalPaymentStatus.PAID
                VendorSettlementStatus.REJECTED -> VendorPortalPaymentStatus.REJECTED
                VendorSettlementStatus.CANCELLED -> VendorPortalPaymentStatus.CANCELLED
                VendorSettlementStatus.FAILED -> VendorPortalPaymentStatus.REJECTED
            }

            val maskedRef = s.referenceNumber?.let { ref ->
                if (ref.length > 4) "****" + ref.takeLast(4) else ref
            } ?: "N/A"

            VendorPortalPaymentSummary(
                settlementId = s.settlementId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                settlementNumber = s.settlementNumber,
                settlementDate = s.settlementDate,
                currency = s.currency,
                totalAmount = s.totalAmount,
                paymentStatus = pStatus,
                paymentMethod = s.settlementMethod.name.replace("_", " "),
                referenceNumber = maskedRef,
                relatedInvoiceIds = s.allocations.mapNotNull { it.invoiceId },
                notes = s.notes,
                settledAt = s.settledAt
            )
        }.filter { status == null || it.paymentStatus == status }

        return DomainResult.Success(summaries)
    }

    override suspend fun getPaymentById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        settlementId: String
    ): DomainResult<VendorPortalPaymentSummary> {
        val sRes = vendorSettlementService.getSettlementById(settlementId, tenantId)
        if (sRes is DomainResult.Error) return sRes
        val s = (sRes as DomainResult.Success).data
            ?: return DomainResult.Error(NoSuchElementException("Settlement '$settlementId' not found."))

        if (s.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Access denied: Settlement does not belong to vendor '$vendorId'."))
        }

        val pStatus = when (s.status) {
            VendorSettlementStatus.DRAFT, VendorSettlementStatus.ELIGIBLE, VendorSettlementStatus.PROCESSING, VendorSettlementStatus.RECONCILIATION_REQUIRED -> VendorPortalPaymentStatus.PENDING
            VendorSettlementStatus.APPROVED -> VendorPortalPaymentStatus.APPROVED
            VendorSettlementStatus.SETTLED -> VendorPortalPaymentStatus.PAID
            VendorSettlementStatus.REJECTED -> VendorPortalPaymentStatus.REJECTED
            VendorSettlementStatus.CANCELLED -> VendorPortalPaymentStatus.CANCELLED
            VendorSettlementStatus.FAILED -> VendorPortalPaymentStatus.REJECTED
        }

        val maskedRef = s.referenceNumber?.let { ref ->
            if (ref.length > 4) "****" + ref.takeLast(4) else ref
        } ?: "N/A"

        return DomainResult.Success(
            VendorPortalPaymentSummary(
                settlementId = s.settlementId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                settlementNumber = s.settlementNumber,
                settlementDate = s.settlementDate,
                currency = s.currency,
                totalAmount = s.totalAmount,
                paymentStatus = pStatus,
                paymentMethod = s.settlementMethod.name.replace("_", " "),
                referenceNumber = maskedRef,
                relatedInvoiceIds = s.allocations.mapNotNull { it.invoiceId },
                notes = s.notes,
                settledAt = s.settledAt
            )
        )
    }

    override suspend fun getFinancialSummary(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalFinancialKpiSummary> {
        val invoicesRes = listInvoices(tenantId, projectId, vendorId)
        if (invoicesRes is DomainResult.Error) return invoicesRes
        val invoices = (invoicesRes as DomainResult.Success).data

        var totalInvoiced = BigDecimal.ZERO
        var totalApproved = BigDecimal.ZERO
        var totalPaid = BigDecimal.ZERO
        var totalOutstanding = BigDecimal.ZERO
        var totalDisputed = BigDecimal.ZERO
        var totalOnHold = BigDecimal.ZERO

        var paidCount = 0
        var outstandingCount = 0

        for (inv in invoices) {
            totalInvoiced = totalInvoiced.add(inv.totalAmount.amount)
            totalApproved = totalApproved.add(inv.approvedAmount.amount)
            totalPaid = totalPaid.add(inv.paidAmount.amount)
            totalOutstanding = totalOutstanding.add(inv.outstandingAmount.amount)

            if (inv.matchStatus == VendorInvoiceMatchStatus.MISMATCH || inv.matchStatus == VendorInvoiceMatchStatus.EXCEPTION) {
                totalDisputed = totalDisputed.add(inv.totalAmount.amount)
            }

            if (inv.paymentStatus == VendorPortalPaymentStatus.PAID) {
                paidCount++
            } else if (inv.outstandingAmount.amount > BigDecimal.ZERO) {
                outstandingCount++
            }
        }

        val currency = invoices.firstOrNull()?.currency ?: "BDT"

        return DomainResult.Success(
            VendorPortalFinancialKpiSummary(
                vendorId = vendorId,
                currency = currency,
                totalInvoiced = Money(totalInvoiced),
                totalApproved = Money(totalApproved),
                totalPaid = Money(totalPaid),
                totalOutstanding = Money(totalOutstanding),
                totalDisputed = Money(totalDisputed),
                totalOnHold = Money(totalOnHold),
                invoiceCount = invoices.size,
                outstandingInvoiceCount = outstandingCount,
                paidInvoiceCount = paidCount
            )
        )
    }

    override suspend fun getFinancialActivityTimeline(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String?
    ): DomainResult<List<VendorPortalFinancialActivity>> {
        val auditEventsRes = invoiceRepository.listAuditEvents(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            targetType = if (invoiceId != null) "INVOICE_SUBMISSION" else null,
            targetId = invoiceId
        )
        val auditEvents = (auditEventsRes as? DomainResult.Success)?.data ?: emptyList()

        val activities = auditEvents.map { ev ->
            VendorPortalFinancialActivity(
                activityId = ev.auditId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                entityType = ev.targetType,
                entityId = ev.targetId,
                eventType = ev.action,
                title = "Financial Event: ${ev.action.replace("_", " ")}",
                description = ev.payload ?: "Action executed on ${ev.targetType} '${ev.targetId}'",
                amount = null,
                actorId = ev.actorId,
                timestamp = ev.createdAt
            )
        }

        return DomainResult.Success(activities)
    }
}
