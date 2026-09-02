package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorCapabilityRepository
import com.sucharu.sucharupro.domain.repository.VendorPurchaseOrderRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorPurchaseOrderValidator
import java.math.BigDecimal
import java.util.UUID

interface VendorPurchaseOrderService {
    suspend fun getOrderById(projectId: String, purchaseOrderId: String): DomainResult<VendorPurchaseOrder>
    suspend fun getOrderByNumber(projectId: String, orderNumber: String): DomainResult<VendorPurchaseOrder>
    suspend fun listOrders(
        projectId: String,
        vendorId: String? = null,
        status: VendorPurchaseOrderStatus? = null,
        sourceReferenceType: String? = null,
        sourceReferenceId: String? = null
    ): DomainResult<List<VendorPurchaseOrder>>
    suspend fun listRevisions(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderRevision>>
    suspend fun listAudits(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderAuditEvent>>
    suspend fun createOrder(
        projectId: String,
        vendorId: String,
        requestedBy: String,
        items: List<VendorPurchaseOrderItem>,
        expectedDeliveryDate: Long? = null,
        deliveryLocation: String? = null,
        currency: String = "BDT",
        taxAmount: Money = Money.ZERO,
        discountAmount: Money = Money.ZERO,
        notes: String? = null,
        sourceReferenceType: String? = null,
        sourceReferenceId: String? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun updateDraft(
        projectId: String,
        purchaseOrderId: String,
        items: List<VendorPurchaseOrderItem>? = null,
        expectedDeliveryDate: Long? = null,
        deliveryLocation: String? = null,
        taxAmount: Money? = null,
        discountAmount: Money? = null,
        notes: String? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun submitForApproval(
        projectId: String,
        purchaseOrderId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun approveOrder(
        projectId: String,
        purchaseOrderId: String,
        approverId: String,
        allowSelfApproval: Boolean = false,
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun issueOrder(
        projectId: String,
        purchaseOrderId: String,
        issuerId: String,
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun acknowledgeOrder(
        projectId: String,
        purchaseOrderId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun reviseOrder(
        projectId: String,
        purchaseOrderId: String,
        updatedItems: List<VendorPurchaseOrderItem>,
        reason: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun cancelOrder(
        projectId: String,
        purchaseOrderId: String,
        reason: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun closeOrder(
        projectId: String,
        purchaseOrderId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorPurchaseOrder>
}

class VendorPurchaseOrderServiceImpl(
    private val vendorRepository: VendorRepository,
    private val capabilityRepository: VendorCapabilityRepository,
    private val rateService: VendorServiceRateService,
    private val purchaseOrderRepository: VendorPurchaseOrderRepository
) : VendorPurchaseOrderService {

    override suspend fun getOrderById(projectId: String, purchaseOrderId: String): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()
        if (pId.isBlank() || poId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and purchaseOrderId cannot be blank."))
        }
        return purchaseOrderRepository.findById(pId, poId)
    }

    override suspend fun getOrderByNumber(projectId: String, orderNumber: String): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val num = orderNumber.trim()
        if (pId.isBlank() || num.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and orderNumber cannot be blank."))
        }
        return purchaseOrderRepository.findByOrderNumber(pId, num)
    }

    override suspend fun listOrders(
        projectId: String,
        vendorId: String?,
        status: VendorPurchaseOrderStatus?,
        sourceReferenceType: String?,
        sourceReferenceId: String?
    ): DomainResult<List<VendorPurchaseOrder>> {
        val pId = projectId.trim()
        if (pId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId cannot be blank."))
        }
        return purchaseOrderRepository.list(
            projectId = pId,
            vendorId = vendorId?.trim()?.takeIf { it.isNotBlank() },
            status = status,
            sourceReferenceType = sourceReferenceType?.trim()?.takeIf { it.isNotBlank() },
            sourceReferenceId = sourceReferenceId?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    override suspend fun listRevisions(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderRevision>> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()
        if (pId.isBlank() || poId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and purchaseOrderId cannot be blank."))
        }
        return purchaseOrderRepository.listRevisions(pId, poId)
    }

    override suspend fun listAudits(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderAuditEvent>> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()
        if (pId.isBlank() || poId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and purchaseOrderId cannot be blank."))
        }
        return purchaseOrderRepository.listAudits(pId, poId)
    }

    override suspend fun createOrder(
        projectId: String,
        vendorId: String,
        requestedBy: String,
        items: List<VendorPurchaseOrderItem>,
        expectedDeliveryDate: Long?,
        deliveryLocation: String?,
        currency: String,
        taxAmount: Money,
        discountAmount: Money,
        notes: String?,
        sourceReferenceType: String?,
        sourceReferenceId: String?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }

        // 1. Verify Vendor is ACTIVE
        val vendor = when (val res = vendorRepository.findById(pId, vId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (vendor.status != VendorStatus.ACTIVE) {
            return DomainResult.Error(
                IllegalStateException("Cannot create purchase order for vendor '$vId' because vendor status is '${vendor.status.name}'.")
            )
        }

        if (items.isEmpty()) {
            return DomainResult.Error(IllegalArgumentException("Purchase order must contain at least one line item."))
        }

        // 2. Validate line items & capabilities where specified
        val processedItems = mutableListOf<VendorPurchaseOrderItem>()
        var subtotal = Money.ZERO

        val purchaseOrderId = "vpo_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val orderNumber = "PO-2026-${System.currentTimeMillis() % 1000000}"

        for (item in items) {
            if (item.capabilityType != null) {
                val cap = when (val res = capabilityRepository.findByVendorAndType(pId, vId, item.capabilityType)) {
                    is DomainResult.Success -> res.data
                    else -> null
                }
                if (cap == null || !cap.status.isActive) {
                    return DomainResult.Error(
                        IllegalStateException("Vendor '$vId' does not possess active capability '${item.capabilityType.name}'.")
                    )
                }
            }

            val lineTotal = (item.unitRate * item.quantity) + item.taxAmount - item.discount
            val calculatedLineTotal = if (lineTotal.isNegative()) Money.ZERO else lineTotal
            subtotal += calculatedLineTotal

            val itemId = item.itemId.trim().ifBlank { "poi_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
            processedItems.add(
                item.copy(
                    itemId = itemId,
                    purchaseOrderId = purchaseOrderId,
                    lineTotal = calculatedLineTotal
                )
            )
        }

        val totalAmount = subtotal + taxAmount - discountAmount
        val finalTotal = if (totalAmount.isNegative()) Money.ZERO else totalAmount

        val order = VendorPurchaseOrder(
            purchaseOrderId = purchaseOrderId,
            projectId = pId,
            orderNumber = orderNumber,
            vendorId = vId,
            status = VendorPurchaseOrderStatus.DRAFT,
            orderDate = System.currentTimeMillis(),
            requestedBy = requestedBy.trim().ifBlank { actorId },
            expectedDeliveryDate = expectedDeliveryDate,
            deliveryLocation = deliveryLocation?.trim()?.takeIf { it.isNotBlank() },
            currency = currency.trim().ifBlank { "BDT" },
            subtotal = subtotal,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            totalAmount = finalTotal,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            sourceReferenceType = sourceReferenceType?.trim()?.takeIf { it.isNotBlank() },
            sourceReferenceId = sourceReferenceId?.trim()?.takeIf { it.isNotBlank() },
            items = processedItems,
            createdAt = System.currentTimeMillis(),
            createdBy = actorId.trim().ifBlank { "system" },
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId.trim().ifBlank { "system" },
            version = 1L
        )

        val valRes = VendorPurchaseOrderValidator.validate(order)
        if (!valRes.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${valRes.errorMessage}"))
        }

        val saveRes = purchaseOrderRepository.createOrder(order)
        if (saveRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = purchaseOrderId,
                    eventType = "CREATED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Purchase order created with ${processedItems.size} items, total: ${finalTotal.formatted()}"
                )
            )
        }
        return saveRes
    }

    override suspend fun updateDraft(
        projectId: String,
        purchaseOrderId: String,
        items: List<VendorPurchaseOrderItem>?,
        expectedDeliveryDate: Long?,
        deliveryLocation: String?,
        taxAmount: Money?,
        discountAmount: Money?,
        notes: String?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val existing = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (!existing.status.isEditable) {
            return DomainResult.Error(
                IllegalStateException("Cannot modify purchase order '$poId' in '${existing.status.name}' status.")
            )
        }

        val updatedItems = items ?: existing.items
        val processedItems = mutableListOf<VendorPurchaseOrderItem>()
        var subtotal = Money.ZERO

        for (item in updatedItems) {
            val lineTotal = (item.unitRate * item.quantity) + item.taxAmount - item.discount
            val calculatedLineTotal = if (lineTotal.isNegative()) Money.ZERO else lineTotal
            subtotal += calculatedLineTotal
            processedItems.add(
                item.copy(
                    itemId = item.itemId.ifBlank { "poi_${UUID.randomUUID().toString().take(12)}" },
                    purchaseOrderId = poId,
                    lineTotal = calculatedLineTotal
                )
            )
        }

        val finalTax = taxAmount ?: existing.taxAmount
        val finalDiscount = discountAmount ?: existing.discountAmount
        val total = subtotal + finalTax - finalDiscount
        val finalTotal = if (total.isNegative()) Money.ZERO else total

        val updated = existing.copy(
            items = processedItems,
            subtotal = subtotal,
            taxAmount = finalTax,
            discountAmount = finalDiscount,
            totalAmount = finalTotal,
            expectedDeliveryDate = expectedDeliveryDate ?: existing.expectedDeliveryDate,
            deliveryLocation = deliveryLocation ?: existing.deliveryLocation,
            notes = notes ?: existing.notes,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val valRes = VendorPurchaseOrderValidator.validate(updated)
        if (!valRes.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${valRes.errorMessage}"))
        }

        val saveRes = purchaseOrderRepository.updateOrder(updated)
        if (saveRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = poId,
                    eventType = "UPDATED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Draft purchase order updated"
                )
            )
        }
        return saveRes
    }

    override suspend fun submitForApproval(
        projectId: String,
        purchaseOrderId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val existing = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorPurchaseOrderValidator.validateStatusTransition(existing.status, VendorPurchaseOrderStatus.PENDING_APPROVAL)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = purchaseOrderRepository.updateStatus(pId, poId, VendorPurchaseOrderStatus.PENDING_APPROVAL, actorId)
        if (updateRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = poId,
                    eventType = "SUBMITTED_FOR_APPROVAL",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Submitted for management approval"
                )
            )
        }
        return updateRes
    }

    override suspend fun approveOrder(
        projectId: String,
        purchaseOrderId: String,
        approverId: String,
        allowSelfApproval: Boolean,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val existing = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorPurchaseOrderValidator.validateStatusTransition(existing.status, VendorPurchaseOrderStatus.APPROVED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val separationRes = VendorPurchaseOrderValidator.validateApproval(existing, approverId, allowSelfApproval)
        if (!separationRes.isValid) {
            return DomainResult.Error(IllegalArgumentException(separationRes.errorMessage))
        }

        val now = System.currentTimeMillis()
        val updateRes = purchaseOrderRepository.updateStatus(
            projectId = pId,
            purchaseOrderId = poId,
            status = VendorPurchaseOrderStatus.APPROVED,
            updatedBy = approverId,
            approvedBy = approverId,
            approvedAt = now
        )
        if (updateRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = poId,
                    eventType = "APPROVED",
                    actorId = approverId,
                    correlationId = correlationId,
                    occurredAt = now,
                    details = "Purchase order approved by '$approverId'"
                )
            )
        }
        return updateRes
    }

    override suspend fun issueOrder(
        projectId: String,
        purchaseOrderId: String,
        issuerId: String,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val existing = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorPurchaseOrderValidator.validateStatusTransition(existing.status, VendorPurchaseOrderStatus.ISSUED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val now = System.currentTimeMillis()
        val updateRes = purchaseOrderRepository.updateStatus(
            projectId = pId,
            purchaseOrderId = poId,
            status = VendorPurchaseOrderStatus.ISSUED,
            updatedBy = issuerId,
            issuedBy = issuerId,
            issuedAt = now
        )
        if (updateRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = poId,
                    eventType = "ISSUED",
                    actorId = issuerId,
                    correlationId = correlationId,
                    occurredAt = now,
                    details = "Purchase order formally issued to vendor"
                )
            )
        }
        return updateRes
    }

    override suspend fun acknowledgeOrder(
        projectId: String,
        purchaseOrderId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val existing = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorPurchaseOrderValidator.validateStatusTransition(existing.status, VendorPurchaseOrderStatus.ACKNOWLEDGED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = purchaseOrderRepository.updateStatus(pId, poId, VendorPurchaseOrderStatus.ACKNOWLEDGED, actorId)
        if (updateRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = poId,
                    eventType = "ACKNOWLEDGED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Vendor acknowledged receipt and schedule"
                )
            )
        }
        return updateRes
    }

    override suspend fun reviseOrder(
        projectId: String,
        purchaseOrderId: String,
        updatedItems: List<VendorPurchaseOrderItem>,
        reason: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val existing = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (existing.status.isTerminal) {
            return DomainResult.Error(IllegalStateException("Cannot revise closed or cancelled order '$poId'."))
        }

        var subtotal = Money.ZERO
        val processedItems = mutableListOf<VendorPurchaseOrderItem>()
        for (item in updatedItems) {
            val lineTotal = (item.unitRate * item.quantity) + item.taxAmount - item.discount
            val calculatedLineTotal = if (lineTotal.isNegative()) Money.ZERO else lineTotal
            subtotal += calculatedLineTotal
            processedItems.add(
                item.copy(
                    itemId = item.itemId.ifBlank { "poi_${UUID.randomUUID().toString().take(12)}" },
                    purchaseOrderId = poId,
                    lineTotal = calculatedLineTotal
                )
            )
        }
        val total = subtotal + existing.taxAmount - existing.discountAmount
        val finalTotal = if (total.isNegative()) Money.ZERO else total

        val currentRevisions = when (val revRes = purchaseOrderRepository.listRevisions(pId, poId)) {
            is DomainResult.Success -> revRes.data
            else -> emptyList()
        }

        val revision = VendorPurchaseOrderRevision(
            revisionId = "rev_${UUID.randomUUID().toString().take(12)}",
            projectId = pId,
            purchaseOrderId = poId,
            revisionNumber = currentRevisions.size + 1,
            previousTotalAmount = existing.totalAmount,
            newTotalAmount = finalTotal,
            changeSummary = reason.trim().ifBlank { "Items updated" },
            revisedBy = actorId,
            revisedAt = System.currentTimeMillis()
        )

        purchaseOrderRepository.recordRevision(revision)

        val revisedOrder = existing.copy(
            items = processedItems,
            subtotal = subtotal,
            totalAmount = finalTotal,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val saveRes = purchaseOrderRepository.updateOrder(revisedOrder)
        if (saveRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = poId,
                    eventType = "REVISED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Revision #${revision.revisionNumber}: $reason. Total changed from ${existing.totalAmount.formatted()} to ${finalTotal.formatted()}"
                )
            )
        }
        return saveRes
    }

    override suspend fun cancelOrder(
        projectId: String,
        purchaseOrderId: String,
        reason: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val existing = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorPurchaseOrderValidator.validateStatusTransition(existing.status, VendorPurchaseOrderStatus.CANCELLED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = purchaseOrderRepository.updateStatus(pId, poId, VendorPurchaseOrderStatus.CANCELLED, actorId)
        if (updateRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = poId,
                    eventType = "CANCELLED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Purchase order cancelled. Reason: $reason"
                )
            )
        }
        return updateRes
    }

    override suspend fun closeOrder(
        projectId: String,
        purchaseOrderId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorPurchaseOrder> {
        val pId = projectId.trim()
        val poId = purchaseOrderId.trim()

        val existing = when (val res = purchaseOrderRepository.findById(pId, poId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorPurchaseOrderValidator.validateStatusTransition(existing.status, VendorPurchaseOrderStatus.CLOSED)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = purchaseOrderRepository.updateStatus(pId, poId, VendorPurchaseOrderStatus.CLOSED, actorId)
        if (updateRes is DomainResult.Success) {
            purchaseOrderRepository.appendAudit(
                VendorPurchaseOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    purchaseOrderId = poId,
                    eventType = "CLOSED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Purchase order formally closed"
                )
            )
        }
        return updateRes
    }
}
