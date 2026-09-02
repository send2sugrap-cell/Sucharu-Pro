package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorDeliveryReceiptRepository
import com.sucharu.sucharupro.domain.repository.VendorInvoiceRepository
import com.sucharu.sucharupro.domain.repository.VendorPurchaseOrderRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorInvoiceValidator
import java.math.BigDecimal
import java.util.UUID

interface VendorInvoiceService {
    suspend fun getInvoiceById(projectId: String, invoiceId: String): DomainResult<VendorInvoice>
    suspend fun getInvoiceByNumber(projectId: String, invoiceNumber: String): DomainResult<VendorInvoice>
    suspend fun listInvoices(
        projectId: String,
        vendorId: String? = null,
        purchaseOrderId: String? = null,
        status: VendorInvoiceStatus? = null,
        matchStatus: VendorInvoiceMatchStatus? = null
    ): DomainResult<List<VendorInvoice>>

    suspend fun createInvoice(
        projectId: String,
        vendorId: String,
        purchaseOrderId: String,
        vendorInvoiceNumber: String,
        invoiceDate: Long = System.currentTimeMillis(),
        receivedDate: Long = System.currentTimeMillis(),
        currency: String = "BDT",
        shippingAmount: Money = Money.ZERO,
        otherCharges: Money = Money.ZERO,
        notes: String? = null,
        items: List<VendorInvoiceItem>,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorInvoice>

    suspend fun updateDraft(
        projectId: String,
        invoiceId: String,
        vendorInvoiceNumber: String? = null,
        invoiceDate: Long? = null,
        shippingAmount: Money? = null,
        otherCharges: Money? = null,
        notes: String? = null,
        items: List<VendorInvoiceItem>? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorInvoice>

    suspend fun submitInvoice(
        projectId: String,
        invoiceId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorInvoice>

    suspend fun executeThreeWayMatch(
        projectId: String,
        invoiceId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorInvoiceMatch>

    suspend fun getMatchResult(projectId: String, invoiceId: String): DomainResult<VendorInvoiceMatch>
    suspend fun listExceptions(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceException>>
    suspend fun resolveException(
        projectId: String,
        exceptionId: String,
        resolutionNotes: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorInvoiceException>

    suspend fun approveInvoice(
        projectId: String,
        invoiceId: String,
        actorId: String = "system",
        allowSelfApproval: Boolean = false,
        correlationId: String? = null
    ): DomainResult<VendorInvoice>

    suspend fun postInvoice(
        projectId: String,
        invoiceId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorInvoice>

    suspend fun rejectInvoice(
        projectId: String,
        invoiceId: String,
        reason: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorInvoice>

    suspend fun cancelInvoice(
        projectId: String,
        invoiceId: String,
        reason: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorInvoice>

    suspend fun listAudits(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceAuditEvent>>
}

class VendorInvoiceServiceImpl(
    private val vendorRepository: VendorRepository,
    private val purchaseOrderRepository: VendorPurchaseOrderRepository,
    private val receiptRepository: VendorDeliveryReceiptRepository,
    private val invoiceRepository: VendorInvoiceRepository
) : VendorInvoiceService {

    override suspend fun getInvoiceById(projectId: String, invoiceId: String): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()
        if (pId.isBlank() || invId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and invoiceId cannot be blank."))
        }
        return invoiceRepository.findById(pId, invId)
    }

    override suspend fun getInvoiceByNumber(projectId: String, invoiceNumber: String): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val num = invoiceNumber.trim()
        if (pId.isBlank() || num.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and invoiceNumber cannot be blank."))
        }
        return invoiceRepository.findByInvoiceNumber(pId, num)
    }

    override suspend fun listInvoices(
        projectId: String,
        vendorId: String?,
        purchaseOrderId: String?,
        status: VendorInvoiceStatus?,
        matchStatus: VendorInvoiceMatchStatus?
    ): DomainResult<List<VendorInvoice>> {
        val pId = projectId.trim()
        if (pId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId cannot be blank."))
        }
        return invoiceRepository.list(
            projectId = pId,
            vendorId = vendorId?.trim()?.takeIf { it.isNotBlank() },
            purchaseOrderId = purchaseOrderId?.trim()?.takeIf { it.isNotBlank() },
            status = status,
            matchStatus = matchStatus
        )
    }

    override suspend fun createInvoice(
        projectId: String,
        vendorId: String,
        purchaseOrderId: String,
        vendorInvoiceNumber: String,
        invoiceDate: Long,
        receivedDate: Long,
        currency: String,
        shippingAmount: Money,
        otherCharges: Money,
        notes: String?,
        items: List<VendorInvoiceItem>,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        val poId = purchaseOrderId.trim()
        val vInvNum = vendorInvoiceNumber.trim()

        if (pId.isBlank() || vId.isBlank() || poId.isBlank() || vInvNum.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Required identifiers cannot be blank."))
        }

        // 1. Validate Vendor
        val vendor = when (val res = vendorRepository.findById(pId, vId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (vendor.status != VendorStatus.ACTIVE) {
            return DomainResult.Error(IllegalStateException("Vendor '$vId' is in '${vendor.status.name}' status."))
        }

        // 2. Validate Purchase Order
        val po = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (po.vendorId != vId) {
            return DomainResult.Error(IllegalArgumentException("Purchase order '$poId' vendor '${po.vendorId}' does not match invoice vendor '$vId'."))
        }

        if (po.status == VendorPurchaseOrderStatus.CANCELLED) {
            return DomainResult.Error(IllegalStateException("Cannot invoice cancelled purchase order '$poId'."))
        }

        // 3. Duplicate Invoice check
        val existingDup = invoiceRepository.findByVendorInvoiceNumber(pId, vId, vInvNum)
        if (existingDup is DomainResult.Success) {
            return DomainResult.Error(
                IllegalStateException("Duplicate invoice: Vendor '$vId' invoice number '$vInvNum' already exists in project '$pId'.")
            )
        }

        if (items.isEmpty()) {
            return DomainResult.Error(IllegalArgumentException("Vendor invoice must contain at least one line item."))
        }

        val invoiceId = "vinv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val invoiceNumber = "INV-2026-${System.currentTimeMillis() % 1000000}"

        var subtotal = Money.ZERO
        var totalTax = Money.ZERO
        var totalDiscount = Money.ZERO

        val processedItems = items.mapIndexed { index, item ->
            val poItem = po.items.find { it.itemId == item.purchaseOrderItemId }
                ?: return DomainResult.Error(
                    IllegalArgumentException("Purchase order item '${item.purchaseOrderItemId}' not found on order '$poId'.")
                )

            val itemId = item.itemId.trim().ifBlank { "vii_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
            val itemLineTotal = (item.unitPrice * item.quantity) + item.taxAmount - item.discountAmount

            subtotal += item.unitPrice * item.quantity
            totalTax += item.taxAmount
            totalDiscount += item.discountAmount

            item.copy(
                itemId = itemId,
                invoiceId = invoiceId,
                purchaseOrderItemId = poItem.itemId,
                description = item.description.ifBlank { poItem.itemDescription },
                unitOfMeasure = poItem.unitOfMeasure,
                lineTotal = itemLineTotal,
                sequence = index + 1
            )
        }

        val totalAmount = subtotal + totalTax - totalDiscount + shippingAmount + otherCharges

        val invoice = VendorInvoice(
            invoiceId = invoiceId,
            projectId = pId,
            tenantId = po.projectId,
            vendorId = vId,
            purchaseOrderId = poId,
            invoiceNumber = invoiceNumber,
            vendorInvoiceNumber = vInvNum,
            invoiceDate = invoiceDate,
            receivedDate = receivedDate,
            currency = currency,
            subtotal = subtotal,
            taxAmount = totalTax,
            discountAmount = totalDiscount,
            shippingAmount = shippingAmount,
            otherCharges = otherCharges,
            totalAmount = totalAmount,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            status = VendorInvoiceStatus.DRAFT,
            matchStatus = VendorInvoiceMatchStatus.NOT_MATCHED,
            items = processedItems,
            createdAt = System.currentTimeMillis(),
            createdBy = actorId,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = 1L
        )

        val valRes = VendorInvoiceValidator.validate(invoice)
        if (!valRes.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${valRes.errorMessage}"))
        }

        val saveRes = invoiceRepository.createInvoice(invoice)
        if (saveRes is DomainResult.Success) {
            invoiceRepository.appendAudit(
                VendorInvoiceAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invoiceId,
                    eventType = "INVOICE_CREATED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Invoice '$invoiceNumber' ($vInvNum) created for PO '$poId'"
                )
            )
        }
        return saveRes
    }

    override suspend fun updateDraft(
        projectId: String,
        invoiceId: String,
        vendorInvoiceNumber: String?,
        invoiceDate: Long?,
        shippingAmount: Money?,
        otherCharges: Money?,
        notes: String?,
        items: List<VendorInvoiceItem>?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()

        val existing = when (val res = invoiceRepository.findById(pId, invId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (!existing.status.isEditable) {
            return DomainResult.Error(IllegalStateException("Cannot modify invoice '$invId' in status '${existing.status.name}'."))
        }

        val updatedItems = items ?: existing.items
        var subtotal = Money.ZERO
        var totalTax = Money.ZERO
        var totalDiscount = Money.ZERO

        val reprocessedItems = updatedItems.mapIndexed { idx, itm ->
            val lineTotal = (itm.unitPrice * itm.quantity) + itm.taxAmount - itm.discountAmount
            subtotal += itm.unitPrice * itm.quantity
            totalTax += itm.taxAmount
            totalDiscount += itm.discountAmount
            itm.copy(lineTotal = lineTotal, sequence = idx + 1)
        }

        val ship = shippingAmount ?: existing.shippingAmount
        val oth = otherCharges ?: existing.otherCharges
        val total = subtotal + totalTax - totalDiscount + ship + oth

        val updated = existing.copy(
            vendorInvoiceNumber = vendorInvoiceNumber ?: existing.vendorInvoiceNumber,
            invoiceDate = invoiceDate ?: existing.invoiceDate,
            shippingAmount = ship,
            otherCharges = oth,
            subtotal = subtotal,
            taxAmount = totalTax,
            discountAmount = totalDiscount,
            totalAmount = total,
            notes = notes ?: existing.notes,
            items = reprocessedItems,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val valRes = VendorInvoiceValidator.validate(updated)
        if (!valRes.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${valRes.errorMessage}"))
        }

        val saveRes = invoiceRepository.updateInvoice(updated)
        if (saveRes is DomainResult.Success) {
            invoiceRepository.appendAudit(
                VendorInvoiceAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invId,
                    eventType = "INVOICE_UPDATED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Draft invoice updated"
                )
            )
        }
        return saveRes
    }

    override suspend fun submitInvoice(
        projectId: String,
        invoiceId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()

        val existing = when (val res = invoiceRepository.findById(pId, invId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorInvoiceValidator.validateStatusTransition(existing.status, VendorInvoiceStatus.SUBMITTED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = invoiceRepository.updateStatus(pId, invId, VendorInvoiceStatus.SUBMITTED, updatedBy = actorId)
        if (updateRes is DomainResult.Success) {
            invoiceRepository.appendAudit(
                VendorInvoiceAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invId,
                    eventType = "INVOICE_SUBMITTED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Invoice submitted for review and matching"
                )
            )
        }
        return updateRes
    }

    override suspend fun executeThreeWayMatch(
        projectId: String,
        invoiceId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorInvoiceMatch> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()

        val invoice = when (val res = invoiceRepository.findById(pId, invId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val po = when (val res = purchaseOrderRepository.findById(pId, invoice.purchaseOrderId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val receipts = when (val res = receiptRepository.list(pId, purchaseOrderId = invoice.purchaseOrderId)) {
            is DomainResult.Success -> res.data.filter { it.status.isAccepted }
            else -> emptyList()
        }

        val matchId = "match_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val matchLines = mutableListOf<VendorInvoiceMatchLine>()
        val exceptions = mutableListOf<VendorInvoiceException>()

        var totalQtyVariance = BigDecimal.ZERO
        var totalPriceVariance = Money.ZERO
        var totalSubtotalVariance = Money.ZERO
        var totalTaxVariance = Money.ZERO
        var exceptionCount = 0

        val vendorMismatch = invoice.vendorId != po.vendorId
        if (vendorMismatch) {
            exceptions.add(
                VendorInvoiceException(
                    exceptionId = "ex_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invId,
                    matchId = matchId,
                    exceptionType = VendorInvoiceExceptionType.VENDOR_MISMATCH,
                    description = "Invoice vendor '${invoice.vendorId}' does not match PO vendor '${po.vendorId}'"
                )
            )
            exceptionCount++
        }

        val currencyMismatch = invoice.currency != po.currency
        if (currencyMismatch) {
            exceptions.add(
                VendorInvoiceException(
                    exceptionId = "ex_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invId,
                    matchId = matchId,
                    exceptionType = VendorInvoiceExceptionType.CURRENCY_MISMATCH,
                    description = "Invoice currency '${invoice.currency}' does not match PO currency '${po.currency}'"
                )
            )
            exceptionCount++
        }

        for (item in invoice.items) {
            val poItem = po.items.find { it.itemId == item.purchaseOrderItemId }

            if (poItem == null) {
                exceptions.add(
                    VendorInvoiceException(
                        exceptionId = "ex_${UUID.randomUUID().toString().take(12)}",
                        projectId = pId,
                        invoiceId = invId,
                        matchId = matchId,
                        exceptionType = VendorInvoiceExceptionType.INVALID_SOURCE_REFERENCE,
                        description = "PO item '${item.purchaseOrderItemId}' not found on PO '${po.purchaseOrderId}'"
                    )
                )
                exceptionCount++
                continue
            }

            // Calculate accepted quantity across receipts
            var acceptedQty = BigDecimal.ZERO
            var deliveryReceiptItemId: String? = null
            for (r in receipts) {
                val matchingReceiptItem = r.items.find { it.purchaseOrderItemId == poItem.itemId }
                if (matchingReceiptItem != null) {
                    acceptedQty += matchingReceiptItem.acceptedQuantity
                    if (deliveryReceiptItemId == null) {
                        deliveryReceiptItemId = matchingReceiptItem.receiptItemId
                    }
                }
            }

            val qtyVariance = item.quantity - acceptedQty
            val priceVariance = item.unitPrice - poItem.unitRate
            val expectedLineTotal = poItem.unitRate * acceptedQty
            val amountVariance = item.lineTotal - expectedLineTotal

            totalQtyVariance += qtyVariance
            totalPriceVariance += priceVariance
            totalSubtotalVariance += amountVariance

            var lineStatus = VendorInvoiceMatchStatus.MATCHED
            var exceptionReason: String? = null

            if (!priceVariance.isZero()) {
                lineStatus = VendorInvoiceMatchStatus.MISMATCH
                exceptionReason = "Price variance: Invoiced ${item.unitPrice.formatted()} vs PO ${poItem.unitRate.formatted()}"
                exceptions.add(
                    VendorInvoiceException(
                        exceptionId = "ex_${UUID.randomUUID().toString().take(12)}",
                        projectId = pId,
                        invoiceId = invId,
                        matchId = matchId,
                        exceptionType = VendorInvoiceExceptionType.PRICE_VARIANCE,
                        description = "Item '${item.description}': $exceptionReason"
                    )
                )
                exceptionCount++
            }

            if (acceptedQty == BigDecimal.ZERO && item.quantity > BigDecimal.ZERO) {
                lineStatus = VendorInvoiceMatchStatus.EXCEPTION
                exceptionReason = "Receipt missing or 0 accepted quantity"
                exceptions.add(
                    VendorInvoiceException(
                        exceptionId = "ex_${UUID.randomUUID().toString().take(12)}",
                        projectId = pId,
                        invoiceId = invId,
                        matchId = matchId,
                        exceptionType = VendorInvoiceExceptionType.RECEIPT_MISSING,
                        description = "Item '${item.description}': No accepted delivery receipt found"
                    )
                )
                exceptionCount++
            } else if (item.quantity > acceptedQty) {
                lineStatus = VendorInvoiceMatchStatus.EXCEPTION
                exceptionReason = "Invoiced quantity (${item.quantity}) exceeds accepted quantity ($acceptedQty)"
                exceptions.add(
                    VendorInvoiceException(
                        exceptionId = "ex_${UUID.randomUUID().toString().take(12)}",
                        projectId = pId,
                        invoiceId = invId,
                        matchId = matchId,
                        exceptionType = VendorInvoiceExceptionType.UNRECEIVED_QUANTITY,
                        description = "Item '${item.description}': $exceptionReason"
                    )
                )
                exceptionCount++
            }

            matchLines.add(
                VendorInvoiceMatchLine(
                    matchLineId = "vml_${UUID.randomUUID().toString().take(12)}",
                    matchId = matchId,
                    invoiceItemId = item.itemId,
                    purchaseOrderItemId = poItem.itemId,
                    deliveryReceiptItemId = deliveryReceiptItemId,
                    description = item.description,
                    orderedQuantity = poItem.quantity,
                    receivedQuantity = acceptedQty,
                    invoicedQuantity = item.quantity,
                    orderedUnitPrice = poItem.unitRate,
                    invoicedUnitPrice = item.unitPrice,
                    quantityVariance = qtyVariance,
                    priceVariance = priceVariance,
                    amountVariance = amountVariance,
                    matchStatus = lineStatus,
                    exceptionReason = exceptionReason
                )
            )
        }

        val overallMatchStatus = when {
            exceptionCount == 0 && matchLines.all { it.matchStatus == VendorInvoiceMatchStatus.MATCHED } -> VendorInvoiceMatchStatus.MATCHED
            matchLines.any { it.matchStatus == VendorInvoiceMatchStatus.MATCHED } -> VendorInvoiceMatchStatus.PARTIAL_MATCH
            else -> VendorInvoiceMatchStatus.EXCEPTION
        }

        val match = VendorInvoiceMatch(
            matchId = matchId,
            projectId = pId,
            invoiceId = invId,
            purchaseOrderId = invoice.purchaseOrderId,
            matchStatus = overallMatchStatus,
            matchedAt = System.currentTimeMillis(),
            matchedBy = actorId,
            subtotalVariance = totalSubtotalVariance,
            quantityVariance = totalQtyVariance,
            priceVariance = totalPriceVariance,
            taxVariance = totalTaxVariance,
            totalVariance = totalSubtotalVariance + totalTaxVariance,
            currencyMismatch = currencyMismatch,
            vendorMismatch = vendorMismatch,
            unmatchedLineCount = matchLines.count { it.matchStatus != VendorInvoiceMatchStatus.MATCHED },
            exceptionCount = exceptionCount,
            lines = matchLines,
            version = 1L
        )

        invoiceRepository.saveMatch(match)
        for (ex in exceptions) {
            invoiceRepository.saveException(ex)
        }

        val newInvoiceStatus = if (overallMatchStatus == VendorInvoiceMatchStatus.MATCHED) {
            VendorInvoiceStatus.MATCHED
        } else {
            VendorInvoiceStatus.UNDER_REVIEW
        }

        invoiceRepository.updateStatus(
            projectId = pId,
            invoiceId = invId,
            status = newInvoiceStatus,
            matchStatus = overallMatchStatus,
            updatedBy = actorId
        )

        invoiceRepository.appendAudit(
            VendorInvoiceAuditEvent(
                auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                projectId = pId,
                invoiceId = invId,
                eventType = "MATCH_EXECUTED",
                actorId = actorId,
                correlationId = correlationId,
                occurredAt = System.currentTimeMillis(),
                details = "3-Way Match executed: Result = ${overallMatchStatus.name}, Exceptions = $exceptionCount"
            )
        )

        return DomainResult.Success(match)
    }

    override suspend fun getMatchResult(projectId: String, invoiceId: String): DomainResult<VendorInvoiceMatch> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()
        return invoiceRepository.findMatchByInvoiceId(pId, invId)
    }

    override suspend fun listExceptions(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceException>> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()
        return invoiceRepository.listExceptions(pId, invId)
    }

    override suspend fun resolveException(
        projectId: String,
        exceptionId: String,
        resolutionNotes: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorInvoiceException> {
        val pId = projectId.trim()
        val exId = exceptionId.trim()
        val notes = resolutionNotes.trim()

        if (notes.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Resolution notes are required."))
        }

        val resolveRes = invoiceRepository.resolveException(pId, exId, actorId, notes)
        if (resolveRes is DomainResult.Success) {
            invoiceRepository.appendAudit(
                VendorInvoiceAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = resolveRes.data.invoiceId,
                    eventType = "EXCEPTION_RESOLVED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Exception '$exId' (${resolveRes.data.exceptionType.name}) resolved: $notes"
                )
            )
        }
        return resolveRes
    }

    override suspend fun approveInvoice(
        projectId: String,
        invoiceId: String,
        actorId: String,
        allowSelfApproval: Boolean,
        correlationId: String?
    ): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()

        val existing = when (val res = invoiceRepository.findById(pId, invId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        // Separation of duties
        if (existing.createdBy == actorId && !allowSelfApproval) {
            return DomainResult.Error(
                IllegalStateException("Separation of duties violation: Creator '$actorId' cannot approve own invoice '$invId'.")
            )
        }

        val transition = VendorInvoiceValidator.validateStatusTransition(existing.status, VendorInvoiceStatus.APPROVED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = invoiceRepository.updateStatus(pId, invId, VendorInvoiceStatus.APPROVED, updatedBy = actorId)
        if (updateRes is DomainResult.Success) {
            invoiceRepository.appendAudit(
                VendorInvoiceAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invId,
                    eventType = "INVOICE_APPROVED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Invoice approved for financial payable processing"
                )
            )
        }
        return updateRes
    }

    override suspend fun postInvoice(
        projectId: String,
        invoiceId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()

        val existing = when (val res = invoiceRepository.findById(pId, invId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorInvoiceValidator.validateStatusTransition(existing.status, VendorInvoiceStatus.POSTED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = invoiceRepository.updateStatus(pId, invId, VendorInvoiceStatus.POSTED, updatedBy = actorId)
        if (updateRes is DomainResult.Success) {
            invoiceRepository.appendAudit(
                VendorInvoiceAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invId,
                    eventType = "INVOICE_POSTED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Invoice posted to payable boundary"
                )
            )
        }
        return updateRes
    }

    override suspend fun rejectInvoice(
        projectId: String,
        invoiceId: String,
        reason: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()

        val existing = when (val res = invoiceRepository.findById(pId, invId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorInvoiceValidator.validateStatusTransition(existing.status, VendorInvoiceStatus.REJECTED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = invoiceRepository.updateStatus(pId, invId, VendorInvoiceStatus.REJECTED, updatedBy = actorId)
        if (updateRes is DomainResult.Success) {
            invoiceRepository.appendAudit(
                VendorInvoiceAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invId,
                    eventType = "INVOICE_REJECTED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Invoice rejected: $reason"
                )
            )
        }
        return updateRes
    }

    override suspend fun cancelInvoice(
        projectId: String,
        invoiceId: String,
        reason: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorInvoice> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()

        val existing = when (val res = invoiceRepository.findById(pId, invId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorInvoiceValidator.validateStatusTransition(existing.status, VendorInvoiceStatus.CANCELLED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = invoiceRepository.updateStatus(pId, invId, VendorInvoiceStatus.CANCELLED, updatedBy = actorId)
        if (updateRes is DomainResult.Success) {
            invoiceRepository.appendAudit(
                VendorInvoiceAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    invoiceId = invId,
                    eventType = "INVOICE_CANCELLED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Invoice cancelled: $reason"
                )
            )
        }
        return updateRes
    }

    override suspend fun listAudits(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceAuditEvent>> {
        val pId = projectId.trim()
        val invId = invoiceId.trim()
        return invoiceRepository.listAudits(pId, invId)
    }
}
