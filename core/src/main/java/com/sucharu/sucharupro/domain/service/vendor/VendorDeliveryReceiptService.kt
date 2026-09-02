package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorDeliveryReceiptRepository
import com.sucharu.sucharupro.domain.repository.VendorPurchaseOrderRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorDeliveryReceiptValidator
import java.math.BigDecimal
import java.util.UUID

interface VendorDeliveryReceiptService {
    suspend fun getReceiptById(projectId: String, deliveryReceiptId: String): DomainResult<VendorDeliveryReceipt>
    suspend fun getReceiptByNumber(projectId: String, receiptNumber: String): DomainResult<VendorDeliveryReceipt>
    suspend fun listReceipts(
        projectId: String,
        vendorId: String? = null,
        purchaseOrderId: String? = null,
        status: VendorDeliveryReceiptStatus? = null
    ): DomainResult<List<VendorDeliveryReceipt>>
    suspend fun listAudits(projectId: String, deliveryReceiptId: String): DomainResult<List<VendorDeliveryReceiptAuditEvent>>
    suspend fun getReceivingSummary(projectId: String, purchaseOrderId: String): DomainResult<VendorPurchaseOrderReceivingSummary>

    suspend fun createReceipt(
        projectId: String,
        purchaseOrderId: String,
        vendorDeliveryReference: String? = null,
        warehouseId: String? = null,
        remarks: String? = null,
        items: List<VendorDeliveryReceiptItem>,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>

    suspend fun updateDraft(
        projectId: String,
        deliveryReceiptId: String,
        vendorDeliveryReference: String? = null,
        warehouseId: String? = null,
        remarks: String? = null,
        items: List<VendorDeliveryReceiptItem>? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>

    suspend fun startReceiving(
        projectId: String,
        deliveryReceiptId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>

    suspend fun recordReceived(
        projectId: String,
        deliveryReceiptId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>

    suspend fun inspectReceipt(
        projectId: String,
        deliveryReceiptId: String,
        inspectedItems: List<VendorDeliveryReceiptItem>,
        remarks: String? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>

    suspend fun acceptReceipt(
        projectId: String,
        deliveryReceiptId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>

    suspend fun partialAcceptReceipt(
        projectId: String,
        deliveryReceiptId: String,
        remarks: String? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>

    suspend fun rejectReceipt(
        projectId: String,
        deliveryReceiptId: String,
        reason: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>

    suspend fun cancelReceipt(
        projectId: String,
        deliveryReceiptId: String,
        reason: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorDeliveryReceipt>
}

class VendorDeliveryReceiptServiceImpl(
    private val vendorRepository: VendorRepository,
    private val purchaseOrderRepository: VendorPurchaseOrderRepository,
    private val receiptRepository: VendorDeliveryReceiptRepository
) : VendorDeliveryReceiptService {

    override suspend fun getReceiptById(projectId: String, deliveryReceiptId: String): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()
        if (pId.isBlank() || rId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and deliveryReceiptId cannot be blank."))
        }
        return receiptRepository.findById(pId, rId)
    }

    override suspend fun getReceiptByNumber(projectId: String, receiptNumber: String): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val num = receiptNumber.trim()
        if (pId.isBlank() || num.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and receiptNumber cannot be blank."))
        }
        return receiptRepository.findByReceiptNumber(pId, num)
    }

    override suspend fun listReceipts(
        projectId: String,
        vendorId: String?,
        purchaseOrderId: String?,
        status: VendorDeliveryReceiptStatus?
    ): DomainResult<List<VendorDeliveryReceipt>> {
        val pId = projectId.trim()
        if (pId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId cannot be blank."))
        }
        return receiptRepository.list(
            projectId = pId,
            vendorId = vendorId?.trim()?.takeIf { it.isNotBlank() },
            purchaseOrderId = purchaseOrderId?.trim()?.takeIf { it.isNotBlank() },
            status = status
        )
    }

    override suspend fun listAudits(projectId: String, deliveryReceiptId: String): DomainResult<List<VendorDeliveryReceiptAuditEvent>> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()
        if (pId.isBlank() || rId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and deliveryReceiptId cannot be blank."))
        }
        return receiptRepository.listAudits(pId, rId)
    }

    override suspend fun getReceivingSummary(projectId: String, purchaseOrderId: String): DomainResult<VendorPurchaseOrderReceivingSummary> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val po = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val receipts = when (val res = receiptRepository.list(pId, purchaseOrderId = poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val activeReceipts = receipts.filter { it.status != VendorDeliveryReceiptStatus.CANCELLED && it.status != VendorDeliveryReceiptStatus.REJECTED }

        var totalOrdered = BigDecimal.ZERO
        po.items.forEach { totalOrdered += it.quantity }

        var totalReceived = BigDecimal.ZERO
        var totalAccepted = BigDecimal.ZERO
        var totalRejected = BigDecimal.ZERO
        var totalDamaged = BigDecimal.ZERO
        var totalShort = BigDecimal.ZERO
        var lastDate: Long? = null

        for (r in activeReceipts) {
            if (lastDate == null || r.receiptDate > lastDate) {
                lastDate = r.receiptDate
            }
            for (item in r.items) {
                totalReceived += item.receivedQuantity
                totalAccepted += item.acceptedQuantity
                totalRejected += item.rejectedQuantity
                totalDamaged += item.damagedQuantity
                totalShort += item.shortQuantity
            }
        }

        val remaining = totalOrdered - totalAccepted
        val finalRemaining = if (remaining < BigDecimal.ZERO) BigDecimal.ZERO else remaining
        val isFullyReceived = totalAccepted >= totalOrdered && totalOrdered > BigDecimal.ZERO

        return DomainResult.Success(
            VendorPurchaseOrderReceivingSummary(
                purchaseOrderId = poId,
                projectId = pId,
                totalOrderedQuantity = totalOrdered,
                totalReceivedQuantity = totalReceived,
                totalAcceptedQuantity = totalAccepted,
                totalRejectedQuantity = totalRejected,
                totalDamagedQuantity = totalDamaged,
                totalShortQuantity = totalShort,
                remainingReceivableQuantity = finalRemaining,
                receiptCount = activeReceipts.size,
                isFullyReceived = isFullyReceived,
                lastReceiptDate = lastDate
            )
        )
    }

    override suspend fun createReceipt(
        projectId: String,
        purchaseOrderId: String,
        vendorDeliveryReference: String?,
        warehouseId: String?,
        remarks: String?,
        items: List<VendorDeliveryReceiptItem>,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        if (pId.isBlank() || poId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and purchaseOrderId cannot be blank."))
        }

        // 1. Verify PO exists and belongs to project
        val po = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (po.status == VendorPurchaseOrderStatus.CANCELLED || po.status == VendorPurchaseOrderStatus.CLOSED) {
            return DomainResult.Error(
                IllegalStateException("Cannot receive against purchase order '$poId' in terminal status '${po.status.name}'.")
            )
        }

        // 2. Verify Vendor exists & ACTIVE
        val vendor = when (val res = vendorRepository.findById(pId, po.vendorId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (vendor.status != VendorStatus.ACTIVE) {
            return DomainResult.Error(
                IllegalStateException("Vendor '${po.vendorId}' is in '${vendor.status.name}' status.")
            )
        }

        if (items.isEmpty()) {
            return DomainResult.Error(IllegalArgumentException("Delivery receipt must contain at least one line item."))
        }

        // 3. Existing receipts to calculate remaining
        val existingReceipts = when (val res = receiptRepository.list(pId, purchaseOrderId = poId)) {
            is DomainResult.Success -> res.data.filter { it.status != VendorDeliveryReceiptStatus.CANCELLED && it.status != VendorDeliveryReceiptStatus.REJECTED }
            else -> emptyList()
        }

        val deliveryReceiptId = "vdr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val receiptNumber = "VDR-2026-${System.currentTimeMillis() % 1000000}"

        val processedItems = mutableListOf<VendorDeliveryReceiptItem>()

        for (item in items) {
            val poItem = po.items.find { it.itemId == item.purchaseOrderItemId }
                ?: return DomainResult.Error(
                    IllegalArgumentException("Purchase order item '${item.purchaseOrderItemId}' not found on order '$poId'.")
                )

            var previouslyReceived = BigDecimal.ZERO
            var previouslyAccepted = BigDecimal.ZERO

            for (r in existingReceipts) {
                val matching = r.items.find { it.purchaseOrderItemId == poItem.itemId }
                if (matching != null) {
                    previouslyReceived += matching.receivedQuantity
                    previouslyAccepted += matching.acceptedQuantity
                }
            }

            val remainingReceivable = poItem.quantity - previouslyAccepted

            // Over-receiving protection
            if (item.receivedQuantity > remainingReceivable) {
                return DomainResult.Error(
                    IllegalArgumentException(
                        "Over-receiving blocked for item '${poItem.itemDescription}'. Requested: ${item.receivedQuantity}, Remaining receivable: $remainingReceivable"
                    )
                )
            }

            val receiptItemId = item.receiptItemId.trim().ifBlank { "vri_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
            val unitRate = poItem.unitRate
            val lineTotal = unitRate * item.receivedQuantity

            processedItems.add(
                item.copy(
                    receiptItemId = receiptItemId,
                    deliveryReceiptId = deliveryReceiptId,
                    purchaseOrderId = poId,
                    purchaseOrderItemId = poItem.itemId,
                    itemDescription = poItem.itemDescription,
                    itemCode = poItem.itemCode,
                    orderedQuantity = poItem.quantity,
                    previouslyReceivedQuantity = previouslyReceived,
                    unitOfMeasure = poItem.unitOfMeasure,
                    unitRate = unitRate,
                    lineTotal = lineTotal
                )
            )
        }

        val receipt = VendorDeliveryReceipt(
            deliveryReceiptId = deliveryReceiptId,
            projectId = pId,
            tenantId = po.projectId,
            receiptNumber = receiptNumber,
            purchaseOrderId = poId,
            vendorId = po.vendorId,
            vendorDeliveryReference = vendorDeliveryReference?.trim()?.takeIf { it.isNotBlank() },
            receiptDate = System.currentTimeMillis(),
            receivedAt = System.currentTimeMillis(),
            receivedBy = actorId.trim().ifBlank { "system" },
            status = VendorDeliveryReceiptStatus.DRAFT,
            warehouseId = warehouseId?.trim()?.takeIf { it.isNotBlank() },
            remarks = remarks?.trim()?.takeIf { it.isNotBlank() },
            items = processedItems,
            createdAt = System.currentTimeMillis(),
            createdBy = actorId,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = 1L
        )

        val valRes = VendorDeliveryReceiptValidator.validate(receipt)
        if (!valRes.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${valRes.errorMessage}"))
        }

        val saveRes = receiptRepository.createReceipt(receipt)
        if (saveRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = deliveryReceiptId,
                    purchaseOrderId = poId,
                    eventType = "RECEIPT_CREATED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Delivery receipt '$receiptNumber' created with ${processedItems.size} items"
                )
            )
        }
        return saveRes
    }

    override suspend fun updateDraft(
        projectId: String,
        deliveryReceiptId: String,
        vendorDeliveryReference: String?,
        warehouseId: String?,
        remarks: String?,
        items: List<VendorDeliveryReceiptItem>?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()

        val existing = when (val res = receiptRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (!existing.status.isEditable) {
            return DomainResult.Error(
                IllegalStateException("Cannot modify receipt '$rId' in status '${existing.status.name}'.")
            )
        }

        val updatedItems = items ?: existing.items
        val updated = existing.copy(
            vendorDeliveryReference = vendorDeliveryReference ?: existing.vendorDeliveryReference,
            warehouseId = warehouseId ?: existing.warehouseId,
            remarks = remarks ?: existing.remarks,
            items = updatedItems,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val valRes = VendorDeliveryReceiptValidator.validate(updated)
        if (!valRes.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${valRes.errorMessage}"))
        }

        val saveRes = receiptRepository.updateReceipt(updated)
        if (saveRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = rId,
                    purchaseOrderId = existing.purchaseOrderId,
                    eventType = "RECEIPT_UPDATED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Draft delivery receipt updated"
                )
            )
        }
        return saveRes
    }

    override suspend fun startReceiving(
        projectId: String,
        deliveryReceiptId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()

        val existing = when (val res = receiptRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorDeliveryReceiptValidator.validateStatusTransition(existing.status, VendorDeliveryReceiptStatus.RECEIVING)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = receiptRepository.updateStatus(pId, rId, VendorDeliveryReceiptStatus.RECEIVING, actorId)
        if (updateRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = rId,
                    purchaseOrderId = existing.purchaseOrderId,
                    eventType = "RECEIPT_STARTED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Physical receiving started"
                )
            )
        }
        return updateRes
    }

    override suspend fun recordReceived(
        projectId: String,
        deliveryReceiptId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()

        val existing = when (val res = receiptRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorDeliveryReceiptValidator.validateStatusTransition(existing.status, VendorDeliveryReceiptStatus.RECEIVED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = receiptRepository.updateStatus(pId, rId, VendorDeliveryReceiptStatus.RECEIVED, actorId)
        if (updateRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = rId,
                    purchaseOrderId = existing.purchaseOrderId,
                    eventType = "RECEIPT_RECEIVED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Physical goods receipt completed, pending inspection"
                )
            )
        }
        return updateRes
    }

    override suspend fun inspectReceipt(
        projectId: String,
        deliveryReceiptId: String,
        inspectedItems: List<VendorDeliveryReceiptItem>,
        remarks: String?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()

        val existing = when (val res = receiptRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorDeliveryReceiptValidator.validateStatusTransition(existing.status, VendorDeliveryReceiptStatus.INSPECTED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updatedReceipt = existing.copy(
            status = VendorDeliveryReceiptStatus.INSPECTED,
            items = inspectedItems,
            remarks = remarks ?: existing.remarks,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val valRes = VendorDeliveryReceiptValidator.validate(updatedReceipt)
        if (!valRes.isValid) {
            return DomainResult.Error(IllegalArgumentException("Inspection validation failed: ${valRes.errorMessage}"))
        }

        val saveRes = receiptRepository.updateReceipt(updatedReceipt)
        if (saveRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = rId,
                    purchaseOrderId = existing.purchaseOrderId,
                    eventType = "RECEIPT_INSPECTED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Quality inspection completed"
                )
            )
        }
        return saveRes
    }

    override suspend fun acceptReceipt(
        projectId: String,
        deliveryReceiptId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()

        val existing = when (val res = receiptRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorDeliveryReceiptValidator.validateStatusTransition(existing.status, VendorDeliveryReceiptStatus.ACCEPTED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        // Auto-populate acceptedQuantity = receivedQuantity if not set
        val updatedItems = existing.items.map { item ->
            if (item.acceptedQuantity == BigDecimal.ZERO && item.rejectedQuantity == BigDecimal.ZERO && item.damagedQuantity == BigDecimal.ZERO) {
                item.copy(acceptedQuantity = item.receivedQuantity)
            } else item
        }

        val updatedReceipt = existing.copy(
            status = VendorDeliveryReceiptStatus.ACCEPTED,
            items = updatedItems,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val saveRes = receiptRepository.updateReceipt(updatedReceipt)
        if (saveRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = rId,
                    purchaseOrderId = existing.purchaseOrderId,
                    eventType = "RECEIPT_ACCEPTED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Receipt fully accepted. Inventory integration triggered for reference '$rId'."
                )
            )

            // Update Purchase Order Status
            val summaryRes = getReceivingSummary(pId, existing.purchaseOrderId)
            if (summaryRes is DomainResult.Success) {
                val summary = summaryRes.data
                val newPoStatus = if (summary.isFullyReceived) {
                    VendorPurchaseOrderStatus.FULFILLED
                } else {
                    VendorPurchaseOrderStatus.PARTIALLY_FULFILLED
                }
                purchaseOrderRepository.updateStatus(
                    projectId = pId,
                    purchaseOrderId = existing.purchaseOrderId,
                    status = newPoStatus,
                    updatedBy = actorId
                )
            }
        }
        return saveRes
    }

    override suspend fun partialAcceptReceipt(
        projectId: String,
        deliveryReceiptId: String,
        remarks: String?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()

        val existing = when (val res = receiptRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorDeliveryReceiptValidator.validateStatusTransition(existing.status, VendorDeliveryReceiptStatus.PARTIALLY_ACCEPTED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updatedReceipt = existing.copy(
            status = VendorDeliveryReceiptStatus.PARTIALLY_ACCEPTED,
            remarks = remarks ?: existing.remarks,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val saveRes = receiptRepository.updateReceipt(updatedReceipt)
        if (saveRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = rId,
                    purchaseOrderId = existing.purchaseOrderId,
                    eventType = "RECEIPT_PARTIALLY_ACCEPTED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Receipt partially accepted. Remarks: $remarks"
                )
            )

            // Update Purchase Order Status to PARTIALLY_FULFILLED
            purchaseOrderRepository.updateStatus(
                projectId = pId,
                purchaseOrderId = existing.purchaseOrderId,
                status = VendorPurchaseOrderStatus.PARTIALLY_FULFILLED,
                updatedBy = actorId
            )
        }
        return saveRes
    }

    override suspend fun rejectReceipt(
        projectId: String,
        deliveryReceiptId: String,
        reason: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()

        val existing = when (val res = receiptRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorDeliveryReceiptValidator.validateStatusTransition(existing.status, VendorDeliveryReceiptStatus.REJECTED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = receiptRepository.updateStatus(pId, rId, VendorDeliveryReceiptStatus.REJECTED, actorId)
        if (updateRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = rId,
                    purchaseOrderId = existing.purchaseOrderId,
                    eventType = "RECEIPT_REJECTED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Delivery receipt rejected. Reason: $reason"
                )
            )
        }
        return updateRes
    }

    override suspend fun cancelReceipt(
        projectId: String,
        deliveryReceiptId: String,
        reason: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorDeliveryReceipt> {
        val pId = projectId.trim()
        val rId = deliveryReceiptId.trim()

        val existing = when (val res = receiptRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorDeliveryReceiptValidator.validateStatusTransition(existing.status, VendorDeliveryReceiptStatus.CANCELLED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = receiptRepository.updateStatus(pId, rId, VendorDeliveryReceiptStatus.CANCELLED, actorId)
        if (updateRes is DomainResult.Success) {
            receiptRepository.appendAudit(
                VendorDeliveryReceiptAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    deliveryReceiptId = rId,
                    purchaseOrderId = existing.purchaseOrderId,
                    eventType = "RECEIPT_CANCELLED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Delivery receipt cancelled. Reason: $reason"
                )
            )
        }
        return updateRes
    }
}
