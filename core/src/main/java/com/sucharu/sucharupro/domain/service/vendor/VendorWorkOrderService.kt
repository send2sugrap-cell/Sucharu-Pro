package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorCapabilityRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.repository.VendorWorkOrderRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorWorkOrderValidator
import java.math.BigDecimal
import java.util.UUID

interface VendorWorkOrderService {
    suspend fun getWorkOrderById(projectId: String, workOrderId: String): DomainResult<VendorWorkOrder>
    suspend fun listWorkOrders(
        projectId: String,
        vendorId: String? = null,
        status: VendorWorkOrderStatus? = null,
        capabilityType: CapabilityType? = null,
        sourceReferenceType: String? = null,
        sourceReferenceId: String? = null
    ): DomainResult<List<VendorWorkOrder>>
    suspend fun listAudits(projectId: String, workOrderId: String): DomainResult<List<VendorWorkOrderAuditEvent>>
    suspend fun createWorkOrder(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        title: String,
        description: String? = null,
        quantity: BigDecimal,
        unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PIECE,
        pricingMethod: PricingMethod = PricingMethod.PER_UNIT,
        unitRate: Money? = null,
        serviceRateId: String? = null,
        sourceReferenceId: String? = null,
        sourceReferenceType: String? = null,
        scheduledStartAt: Long? = null,
        scheduledDueAt: Long? = null,
        priority: String = "NORMAL",
        notes: String? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorWorkOrder>
    suspend fun updateDraft(
        projectId: String,
        workOrderId: String,
        title: String? = null,
        description: String? = null,
        quantity: BigDecimal? = null,
        scheduledStartAt: Long? = null,
        scheduledDueAt: Long? = null,
        priority: String? = null,
        notes: String? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorWorkOrder>
    suspend fun assignVendor(
        projectId: String,
        workOrderId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        unitRate: Money? = null,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorWorkOrder>
    suspend fun releaseWorkOrder(
        projectId: String,
        workOrderId: String,
        actorId: String = "system",
        correlationId: String? = null
    ): DomainResult<VendorWorkOrder>
    suspend fun changeStatus(
        projectId: String,
        workOrderId: String,
        targetStatus: VendorWorkOrderStatus,
        actorId: String = "system",
        correlationId: String? = null,
        reason: String? = null
    ): DomainResult<VendorWorkOrder>
}

class VendorWorkOrderServiceImpl(
    private val vendorRepository: VendorRepository,
    private val capabilityRepository: VendorCapabilityRepository,
    private val rateService: VendorServiceRateService,
    private val workOrderRepository: VendorWorkOrderRepository
) : VendorWorkOrderService {

    override suspend fun getWorkOrderById(projectId: String, workOrderId: String): DomainResult<VendorWorkOrder> {
        val pId = projectId.trim()
        val wId = workOrderId.trim()
        if (pId.isBlank() || wId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and workOrderId cannot be blank."))
        }
        return workOrderRepository.findById(pId, wId)
    }

    override suspend fun listWorkOrders(
        projectId: String,
        vendorId: String?,
        status: VendorWorkOrderStatus?,
        capabilityType: CapabilityType?,
        sourceReferenceType: String?,
        sourceReferenceId: String?
    ): DomainResult<List<VendorWorkOrder>> {
        val pId = projectId.trim()
        if (pId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId cannot be blank."))
        }
        return workOrderRepository.list(
            projectId = pId,
            vendorId = vendorId?.trim()?.takeIf { it.isNotBlank() },
            status = status,
            capabilityType = capabilityType,
            sourceReferenceType = sourceReferenceType?.trim()?.takeIf { it.isNotBlank() },
            sourceReferenceId = sourceReferenceId?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    override suspend fun listAudits(projectId: String, workOrderId: String): DomainResult<List<VendorWorkOrderAuditEvent>> {
        val pId = projectId.trim()
        val wId = workOrderId.trim()
        if (pId.isBlank() || wId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and workOrderId cannot be blank."))
        }
        return workOrderRepository.listAudits(pId, wId)
    }

    override suspend fun createWorkOrder(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        title: String,
        description: String?,
        quantity: BigDecimal,
        unitOfMeasure: UnitOfMeasure,
        pricingMethod: PricingMethod,
        unitRate: Money?,
        serviceRateId: String?,
        sourceReferenceId: String?,
        sourceReferenceType: String?,
        scheduledStartAt: Long?,
        scheduledDueAt: Long?,
        priority: String,
        notes: String?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorWorkOrder> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }

        // 1. Verify vendor exists and is ACTIVE
        val vendor = when (val res = vendorRepository.findById(pId, vId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (vendor.status != VendorStatus.ACTIVE) {
            return DomainResult.Error(
                IllegalStateException("Cannot assign vendor '$vId' because vendor status is '${vendor.status.name}'.")
            )
        }

        // 2. Verify vendor possesses requested active capability
        val cap = when (val res = capabilityRepository.findByVendorAndType(pId, vId, capabilityType)) {
            is DomainResult.Success -> res.data
            else -> null
        }

        if (cap == null || !cap.status.isActive) {
            return DomainResult.Error(
                IllegalStateException("Vendor '$vId' does not possess active capability '${capabilityType.name}'.")
            )
        }

        // 3. Resolve rate snapshot and calculate estimated amount
        val (rateSnapshot, calculatedAmount, resolvedRateId) = if (unitRate != null) {
            val snapshot = VendorWorkOrderRateSnapshot(
                sourceRateId = serviceRateId,
                pricingMethod = pricingMethod,
                unitOfMeasure = unitOfMeasure,
                currency = "BDT",
                baseRate = unitRate,
                resolvedUnitRate = unitRate,
                quantityBasis = quantity,
                resolvedAt = System.currentTimeMillis()
            )
            Triple(snapshot, unitRate * quantity, serviceRateId)
        } else {
            val resolvedRate = when (val rateRes = rateService.resolveApplicableRate(
                projectId = pId,
                vendorId = vId,
                capabilityType = capabilityType,
                pricingMethod = pricingMethod,
                unitOfMeasure = unitOfMeasure,
                effectiveDate = System.currentTimeMillis()
            )) {
                is DomainResult.Success -> rateRes.data
                is DomainResult.Error -> return DomainResult.Error(
                    IllegalStateException("No applicable rate found for vendor '$vId' in capability '${capabilityType.name}': ${rateRes.message}")
                )
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading rate"))
            }

            val estimated = VendorServiceRateCalculator.calculateEstimatedCost(
                rate = resolvedRate,
                quantity = quantity
            )

            val snapshot = VendorWorkOrderRateSnapshot(
                sourceRateId = resolvedRate.rateId,
                pricingMethod = resolvedRate.pricingMethod,
                unitOfMeasure = resolvedRate.unitOfMeasure,
                currency = resolvedRate.currency,
                baseRate = resolvedRate.rateAmount,
                resolvedUnitRate = resolvedRate.rateAmount,
                quantityBasis = quantity,
                resolvedAt = System.currentTimeMillis()
            )
            Triple(snapshot, estimated, resolvedRate.rateId)
        }

        val workOrderId = "vwo_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val workOrderNumber = "VWO-${System.currentTimeMillis() % 1000000}"

        val workOrder = VendorWorkOrder(
            workOrderId = workOrderId,
            projectId = pId,
            workOrderNumber = workOrderNumber,
            vendorId = vId,
            capabilityType = capabilityType,
            serviceRateId = resolvedRateId,
            sourceReferenceId = sourceReferenceId?.trim()?.takeIf { it.isNotBlank() },
            sourceReferenceType = sourceReferenceType?.trim()?.takeIf { it.isNotBlank() },
            title = title.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            quantity = quantity,
            unitOfMeasure = unitOfMeasure,
            pricingMethod = pricingMethod,
            rateSnapshot = rateSnapshot,
            currency = "BDT",
            estimatedAmount = calculatedAmount,
            scheduledStartAt = scheduledStartAt,
            scheduledDueAt = scheduledDueAt,
            priority = priority.trim().ifBlank { "NORMAL" },
            status = VendorWorkOrderStatus.ASSIGNED,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis(),
            createdBy = actorId.trim().ifBlank { "system" },
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId.trim().ifBlank { "system" },
            version = 1L
        )

        val validation = VendorWorkOrderValidator.validate(workOrder)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        val saveRes = workOrderRepository.createWorkOrder(workOrder)
        if (saveRes is DomainResult.Success) {
            workOrderRepository.appendAudit(
                VendorWorkOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    workOrderId = workOrderId,
                    eventType = "CREATED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Work order created with vendor '$vId' for capability '${capabilityType.name}', amount: ${calculatedAmount.formatted()}"
                )
            )
        }
        return saveRes
    }

    override suspend fun updateDraft(
        projectId: String,
        workOrderId: String,
        title: String?,
        description: String?,
        quantity: BigDecimal?,
        scheduledStartAt: Long?,
        scheduledDueAt: Long?,
        priority: String?,
        notes: String?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorWorkOrder> {
        val pId = projectId.trim()
        val wId = workOrderId.trim()
        val existing = when (val res = workOrderRepository.findById(pId, wId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (!existing.status.isEditable) {
            return DomainResult.Error(
                IllegalStateException("Cannot modify work order '$wId' in '${existing.status.name}' status.")
            )
        }

        val updatedQty = quantity ?: existing.quantity
        val updatedAmount = if (quantity != null) {
            existing.rateSnapshot.resolvedUnitRate * updatedQty
        } else {
            existing.estimatedAmount
        }

        val updated = existing.copy(
            title = title?.trim()?.takeIf { it.isNotBlank() } ?: existing.title,
            description = description?.trim() ?: existing.description,
            quantity = updatedQty,
            estimatedAmount = updatedAmount,
            scheduledStartAt = scheduledStartAt ?: existing.scheduledStartAt,
            scheduledDueAt = scheduledDueAt ?: existing.scheduledDueAt,
            priority = priority?.trim() ?: existing.priority,
            notes = notes?.trim() ?: existing.notes,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val validation = VendorWorkOrderValidator.validate(updated)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        val saveRes = workOrderRepository.updateWorkOrder(updated)
        if (saveRes is DomainResult.Success) {
            workOrderRepository.appendAudit(
                VendorWorkOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    workOrderId = wId,
                    eventType = "UPDATED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Work order details updated"
                )
            )
        }
        return saveRes
    }

    override suspend fun assignVendor(
        projectId: String,
        workOrderId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        unitRate: Money?,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorWorkOrder> {
        val pId = projectId.trim()
        val wId = workOrderId.trim()
        val vId = vendorId.trim()

        val existing = when (val res = workOrderRepository.findById(pId, wId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (!existing.status.isEditable) {
            return DomainResult.Error(
                IllegalStateException("Cannot reassign vendor on work order '$wId' in status '${existing.status.name}'.")
            )
        }

        // Validate vendor and capability
        val vendor = when (val res = vendorRepository.findById(pId, vId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }
        if (vendor.status != VendorStatus.ACTIVE) {
            return DomainResult.Error(IllegalStateException("Cannot assign inactive vendor '$vId'."))
        }

        val cap = when (val res = capabilityRepository.findByVendorAndType(pId, vId, capabilityType)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        if (cap == null || !cap.status.isActive) {
            return DomainResult.Error(IllegalStateException("Vendor '$vId' does not possess active capability '${capabilityType.name}'."))
        }

        val resolvedRate = unitRate ?: when (val rateRes = rateService.resolveApplicableRate(
            projectId = pId,
            vendorId = vId,
            capabilityType = capabilityType,
            effectiveDate = System.currentTimeMillis()
        )) {
            is DomainResult.Success -> rateRes.data.rateAmount
            is DomainResult.Error -> return DomainResult.Error(IllegalStateException("Failed to resolve rate: ${rateRes.message}"))
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading rate"))
        }

        val snapshot = VendorWorkOrderRateSnapshot(
            pricingMethod = existing.pricingMethod,
            unitOfMeasure = existing.unitOfMeasure,
            currency = "BDT",
            baseRate = resolvedRate,
            resolvedUnitRate = resolvedRate,
            quantityBasis = existing.quantity,
            resolvedAt = System.currentTimeMillis()
        )

        val updated = existing.copy(
            vendorId = vId,
            capabilityType = capabilityType,
            rateSnapshot = snapshot,
            estimatedAmount = resolvedRate * existing.quantity,
            status = VendorWorkOrderStatus.ASSIGNED,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val saveRes = workOrderRepository.updateWorkOrder(updated)
        if (saveRes is DomainResult.Success) {
            workOrderRepository.appendAudit(
                VendorWorkOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    workOrderId = wId,
                    eventType = "VENDOR_ASSIGNED",
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Reassigned to vendor '$vId' for capability '${capabilityType.name}'"
                )
            )
        }
        return saveRes
    }

    override suspend fun releaseWorkOrder(
        projectId: String,
        workOrderId: String,
        actorId: String,
        correlationId: String?
    ): DomainResult<VendorWorkOrder> {
        return changeStatus(projectId, workOrderId, VendorWorkOrderStatus.RELEASED, actorId, correlationId, "Released to vendor")
    }

    override suspend fun changeStatus(
        projectId: String,
        workOrderId: String,
        targetStatus: VendorWorkOrderStatus,
        actorId: String,
        correlationId: String?,
        reason: String?
    ): DomainResult<VendorWorkOrder> {
        val pId = projectId.trim()
        val wId = workOrderId.trim()

        val existing = when (val res = workOrderRepository.findById(pId, wId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transition = VendorWorkOrderValidator.validateStatusTransition(existing.status, targetStatus)
        if (!transition.isValid) {
            return DomainResult.Error(IllegalArgumentException(transition.errorMessage))
        }

        val updateRes = workOrderRepository.updateStatus(pId, wId, targetStatus, actorId)
        if (updateRes is DomainResult.Success) {
            val eventType = when (targetStatus) {
                VendorWorkOrderStatus.RELEASED -> "RELEASED"
                VendorWorkOrderStatus.IN_PROGRESS -> if (existing.status == VendorWorkOrderStatus.ON_HOLD) "RESUMED" else "STARTED"
                VendorWorkOrderStatus.ON_HOLD -> "PUT_ON_HOLD"
                VendorWorkOrderStatus.COMPLETED -> "COMPLETED"
                VendorWorkOrderStatus.CANCELLED -> "CANCELLED"
                else -> "STATUS_CHANGED"
            }
            workOrderRepository.appendAudit(
                VendorWorkOrderAuditEvent(
                    auditId = "audit_${UUID.randomUUID().toString().take(12)}",
                    projectId = pId,
                    workOrderId = wId,
                    eventType = eventType,
                    actorId = actorId,
                    correlationId = correlationId,
                    occurredAt = System.currentTimeMillis(),
                    details = "Status transitioned from '${existing.status.name}' to '${targetStatus.name}'${reason?.let { ": $it" } ?: ""}"
                )
            )
        }
        return updateRes
    }
}
