package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperMaterialSpecification
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateReservationRepository
import java.util.UUID

class SubstrateReservationServiceImpl(
    private val repository: SubstrateReservationRepository,
    private val requirementResolver: SubstrateRequirementResolver = SubstrateRequirementResolver(),
    private val skuMatcher: SubstrateSkuMatcher = SubstrateSkuMatcher()
) : SubstrateReservationService {

    override suspend fun resolveRequirementAndCheckAvailability(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        calculationId: String?,
        materialSpec: PaperMaterialSpecification,
        productiveSheetsRequired: Long,
        wasteSheetsRequired: Long,
        availableInventoryProducts: List<InventoryProduct>,
        onHandPhysicalSheets: Long,
        warehouseId: String?,
        warehouseName: String?
    ): SubstrateSkuResolutionResult {
        val requirement = requirementResolver.resolveRequirement(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            calculationId = calculationId,
            materialSpec = materialSpec,
            productiveSheetsRequired = productiveSheetsRequired,
            wasteSheetsRequired = wasteSheetsRequired
        )

        val activeReservations = repository.listReservationsBySku(tenantId, requirement.requestedMaterialCode ?: "")
            .filter { it.status.isActiveHold }
            .sumOf { it.reservedSheets }

        return skuMatcher.matchSku(
            requirement = requirement,
            inventoryProducts = availableInventoryProducts,
            onHandPhysicalSheets = onHandPhysicalSheets,
            currentlyReservedSheets = activeReservations,
            warehouseId = warehouseId,
            warehouseName = warehouseName
        )
    }

    override suspend fun createReservation(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        executionJobId: String?,
        workOrderId: String?,
        productId: String,
        sku: String,
        productName: String,
        warehouseId: String,
        locationId: String?,
        requirement: SubstrateRequirement,
        isHardAllocation: Boolean,
        expiryTimestamp: Long?,
        notes: String?,
        actor: String
    ): SubstrateReservation {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(orderItemId.isNotBlank()) { "Order Item ID cannot be blank." }
        require(sku.isNotBlank()) { "Substrate SKU cannot be blank." }
        require(requirement.totalSheetsRequired > 0L) { "Reserved sheets must be strictly positive." }

        val idempotencyKey = SubstrateReservationMathUtils.generateDeterministicReservationNonce(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            sku = sku
        )

        // Idempotency check: return existing if present
        val existing = repository.getReservationByIdempotencyKey(tenantId, idempotencyKey)
        if (existing != null && existing.status.isActiveHold) {
            return existing
        }

        val mode = if (isHardAllocation) SubstrateReservationMode.HARD else SubstrateReservationMode.SOFT
        val initialStatus = if (isHardAllocation) SubstrateReservationStatus.ALLOCATED_HARD else SubstrateReservationStatus.RESERVED_SOFT
        val reservationId = "SRES-${UUID.randomUUID().toString().take(12)}"
        val now = System.currentTimeMillis()

        val reservation = SubstrateReservation(
            reservationId = reservationId,
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            executionJobId = executionJobId,
            workOrderId = workOrderId,
            productId = productId,
            sku = sku,
            productName = productName,
            warehouseId = warehouseId,
            locationId = locationId,
            stockType = requirement.stockType,
            gsm = requirement.gsm,
            sheetDimension = requirement.sheetDimension,
            reservedSheets = requirement.totalSheetsRequired,
            reservedReams = requirement.totalReamsRequired,
            reservedWeightKg = requirement.totalWeightKg,
            status = initialStatus,
            mode = mode,
            idempotencyKey = idempotencyKey,
            expiryTimestamp = expiryTimestamp,
            softHoldExpiresAt = if (!isHardAllocation) (now + 7200000L) else null,
            reservedBy = actor,
            reservedAt = now,
            updatedAt = now,
            notes = notes
        )

        val saved = repository.saveReservation(reservation)

        if (isHardAllocation) {
            val initialAlloc = SubstrateAllocationSource(
                allocationId = "ALOC-${UUID.randomUUID().toString().take(12)}",
                reservationId = reservationId,
                tenantId = tenantId,
                warehouseId = warehouseId,
                locationId = locationId,
                batchNumber = "BATCH-DEFAULT",
                allocatedSheets = requirement.totalSheetsRequired,
                allocatedReams = requirement.totalReamsRequired,
                allocatedWeightKg = requirement.totalWeightKg,
                allocatedAt = now,
                allocatedBy = actor
            )
            repository.saveAllocationSource(initialAlloc)
        }

        repository.saveAuditEvent(
            SubstrateReservationAuditEvent(
                eventId = "EVT-${UUID.randomUUID().toString().take(12)}",
                reservationId = reservationId,
                tenantId = tenantId,
                previousStatus = null,
                newStatus = initialStatus,
                quantityChangeSheets = requirement.totalSheetsRequired,
                actor = actor,
                reason = if (isHardAllocation) "Initial Hard Substrate Allocation created." else "Initial Soft Substrate Hold created.",
                timestamp = now
            )
        )

        return repository.getReservationById(tenantId, reservationId) ?: saved
    }

    override suspend fun createSoftReservation(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        productId: String,
        sku: String,
        productName: String,
        warehouseId: String,
        locationId: String?,
        requirement: SubstrateRequirement,
        softHoldDurationMinutes: Long,
        notes: String?,
        actor: String
    ): SubstrateReservation {
        val now = System.currentTimeMillis()
        val expiresAt = now + (softHoldDurationMinutes * 60 * 1000L)
        return createReservation(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            executionJobId = null,
            workOrderId = null,
            productId = productId,
            sku = sku,
            productName = productName,
            warehouseId = warehouseId,
            locationId = locationId,
            requirement = requirement,
            isHardAllocation = false,
            expiryTimestamp = expiresAt,
            notes = notes,
            actor = actor
        )
    }

    override suspend fun createHardReservation(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        executionJobId: String,
        workOrderId: String?,
        productId: String,
        sku: String,
        productName: String,
        warehouseId: String,
        locationId: String?,
        batchNumber: String?,
        requirement: SubstrateRequirement,
        notes: String?,
        actor: String
    ): SubstrateReservation {
        require(executionJobId.isNotBlank()) { "Execution Job ID is mandatory for hard allocations." }
        val res = createReservation(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            executionJobId = executionJobId,
            workOrderId = workOrderId,
            productId = productId,
            sku = sku,
            productName = productName,
            warehouseId = warehouseId,
            locationId = locationId,
            requirement = requirement,
            isHardAllocation = true,
            expiryTimestamp = null,
            notes = notes,
            actor = actor
        )

        if (batchNumber != null && batchNumber.isNotBlank()) {
            val updatedAlloc = SubstrateAllocationSource(
                allocationId = "ALOC-${UUID.randomUUID().toString().take(12)}",
                reservationId = res.reservationId,
                tenantId = tenantId,
                warehouseId = warehouseId,
                locationId = locationId,
                batchNumber = batchNumber,
                allocatedSheets = requirement.totalSheetsRequired,
                allocatedReams = requirement.totalReamsRequired,
                allocatedWeightKg = requirement.totalWeightKg,
                allocatedAt = System.currentTimeMillis(),
                allocatedBy = actor
            )
            repository.deleteAllocationsByReservation(tenantId, res.reservationId)
            repository.saveAllocationSource(updatedAlloc)
        }

        return repository.getReservationById(tenantId, res.reservationId) ?: res
    }

    override suspend fun promoteSoftToHard(
        tenantId: String,
        reservationId: String,
        executionJobId: String,
        workOrderId: String?,
        allocatedWarehouseId: String?,
        allocatedLocationId: String?,
        allocatedBatchNumber: String?,
        actor: String
    ): SubstrateReservation {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(reservationId.isNotBlank()) { "Reservation ID cannot be blank." }
        require(executionJobId.isNotBlank()) { "Execution Job ID is mandatory for hard promotion." }

        val existing = repository.getReservationById(tenantId, reservationId)
            ?: throw IllegalArgumentException("Substrate reservation $reservationId not found for tenant $tenantId.")

        check(existing.status == SubstrateReservationStatus.RESERVED_SOFT) {
            "Only SOFT reservations can be promoted to HARD. Current status: ${existing.status}"
        }

        val now = System.currentTimeMillis()
        val targetWarehouse = allocatedWarehouseId ?: existing.warehouseId
        val targetLocation = allocatedLocationId ?: existing.locationId
        val targetBatch = allocatedBatchNumber ?: "BATCH-PROMOTED"

        val updated = existing.copy(
            executionJobId = executionJobId,
            workOrderId = workOrderId ?: existing.workOrderId,
            warehouseId = targetWarehouse,
            locationId = targetLocation,
            status = SubstrateReservationStatus.ALLOCATED_HARD,
            mode = SubstrateReservationMode.HARD,
            promotedAt = now,
            promotedBy = actor,
            updatedAt = now,
            notes = "${existing.notes ?: ""}; Promoted to HARD for Job $executionJobId by $actor at $now".trimStart(';', ' ')
        )

        repository.saveReservation(updated)

        val allocSource = SubstrateAllocationSource(
            allocationId = "ALOC-${UUID.randomUUID().toString().take(12)}",
            reservationId = reservationId,
            tenantId = tenantId,
            warehouseId = targetWarehouse,
            locationId = targetLocation,
            batchNumber = targetBatch,
            allocatedSheets = existing.reservedSheets,
            allocatedReams = existing.reservedReams,
            allocatedWeightKg = existing.reservedWeightKg,
            allocatedAt = now,
            allocatedBy = actor
        )
        repository.saveAllocationSource(allocSource)

        repository.saveAuditEvent(
            SubstrateReservationAuditEvent(
                eventId = "EVT-${UUID.randomUUID().toString().take(12)}",
                reservationId = reservationId,
                tenantId = tenantId,
                previousStatus = SubstrateReservationStatus.RESERVED_SOFT,
                newStatus = SubstrateReservationStatus.ALLOCATED_HARD,
                quantityChangeSheets = 0L,
                actor = actor,
                reason = "Atomic Soft-to-Hard promotion for scheduled job $executionJobId against warehouse $targetWarehouse.",
                timestamp = now
            )
        )

        return repository.getReservationById(tenantId, reservationId) ?: updated
    }

    override suspend fun allocateReservationSources(
        tenantId: String,
        reservationId: String,
        sources: List<SubstrateAllocationSource>,
        actor: String
    ): SubstrateReservation {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(reservationId.isNotBlank()) { "Reservation ID cannot be blank." }
        require(sources.isNotEmpty()) { "At least one allocation source must be specified." }

        val existing = repository.getReservationById(tenantId, reservationId)
            ?: throw IllegalArgumentException("Substrate reservation $reservationId not found for tenant $tenantId.")

        val totalAllocatedSheets = sources.sumOf { it.allocatedSheets }
        require(totalAllocatedSheets == existing.reservedSheets) {
            "Total allocated sheets ($totalAllocatedSheets) must equal reservation sheets (${existing.reservedSheets})."
        }

        repository.deleteAllocationsByReservation(tenantId, reservationId)
        for (source in sources) {
            repository.saveAllocationSource(source)
        }

        repository.saveAuditEvent(
            SubstrateReservationAuditEvent(
                eventId = "EVT-${UUID.randomUUID().toString().take(12)}",
                reservationId = reservationId,
                tenantId = tenantId,
                previousStatus = existing.status,
                newStatus = existing.status,
                quantityChangeSheets = 0L,
                actor = actor,
                reason = "Multi-warehouse/lot allocation sources updated (${sources.size} sources).",
                timestamp = System.currentTimeMillis()
            )
        )

        return repository.getReservationById(tenantId, reservationId) ?: existing
    }

    override suspend fun allocateHardForJob(
        tenantId: String,
        reservationId: String,
        executionJobId: String,
        workOrderId: String?,
        actor: String
    ): SubstrateReservation {
        return promoteSoftToHard(
            tenantId = tenantId,
            reservationId = reservationId,
            executionJobId = executionJobId,
            workOrderId = workOrderId,
            actor = actor
        )
    }

    override suspend fun releaseReservation(
        tenantId: String,
        reservationId: String,
        reason: String,
        actor: String
    ): SubstrateReservation {
        val existing = repository.getReservationById(tenantId, reservationId)
            ?: throw IllegalArgumentException("Reservation $reservationId not found.")

        check(!existing.status.isTerminal) { "Cannot release reservation in terminal status ${existing.status}." }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = SubstrateReservationStatus.CANCELLED,
            updatedAt = now,
            notes = "${existing.notes ?: ""}; Released: $reason by $actor".trimStart(';', ' ')
        )

        val saved = repository.saveReservation(updated)
        repository.deleteAllocationsByReservation(tenantId, reservationId)

        repository.saveAuditEvent(
            SubstrateReservationAuditEvent(
                eventId = "EVT-${UUID.randomUUID().toString().take(12)}",
                reservationId = reservationId,
                tenantId = tenantId,
                previousStatus = existing.status,
                newStatus = SubstrateReservationStatus.CANCELLED,
                quantityChangeSheets = -existing.reservedSheets,
                actor = actor,
                reason = reason,
                timestamp = now
            )
        )

        return saved
    }

    override suspend fun getReservation(tenantId: String, reservationId: String): SubstrateReservation? {
        return repository.getReservationById(tenantId, reservationId)
    }

    override suspend fun listReservationsByJob(tenantId: String, executionJobId: String): List<SubstrateReservation> {
        return repository.listReservationsByJob(tenantId, executionJobId)
    }

    override suspend fun listReservationsByOrder(tenantId: String, orderId: String): List<SubstrateReservation> {
        return repository.listReservationsByOrder(tenantId, orderId)
    }

    override suspend fun listAllReservations(tenantId: String, limit: Int): List<SubstrateReservation> {
        return repository.listAllReservations(tenantId, limit)
    }

    override suspend fun exportHandoffContract(tenantId: String, reservationId: String): Module19Step01SubstrateReservationHandoffContract {
        val res = repository.getReservationById(tenantId, reservationId)
            ?: throw IllegalArgumentException("Reservation $reservationId not found.")

        return Module19Step01SubstrateReservationHandoffContract(
            tenantId = tenantId,
            reservationId = reservationId,
            orderId = res.orderId,
            orderItemId = res.orderItemId,
            executionJobId = res.executionJobId,
            sku = res.sku,
            productName = res.productName,
            gsm = res.gsm,
            sheetWidthMm = res.sheetDimension.width,
            sheetHeightMm = res.sheetDimension.height,
            reservedSheets = res.reservedSheets,
            reservedReams = res.reservedReams,
            reservedWeightKg = res.reservedWeightKg,
            status = res.status.name,
            isHardAllocated = res.status == SubstrateReservationStatus.ALLOCATED_HARD,
            isStockInterlocked = true,
            reservedBy = res.reservedBy,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun exportStep02HandoffContract(tenantId: String, reservationId: String): Module19Step02SubstrateReservationHandoffContract {
        val res = repository.getReservationById(tenantId, reservationId)
            ?: throw IllegalArgumentException("Reservation $reservationId not found.")

        return Module19Step02SubstrateReservationHandoffContract(
            tenantId = tenantId,
            reservationId = reservationId,
            orderId = res.orderId,
            orderItemId = res.orderItemId,
            executionJobId = res.executionJobId,
            sku = res.sku,
            productName = res.productName,
            gsm = res.gsm,
            sheetWidthMm = res.sheetDimension.width,
            sheetHeightMm = res.sheetDimension.height,
            reservedSheets = res.reservedSheets,
            reservedReams = res.reservedReams,
            reservedWeightKg = res.reservedWeightKg,
            mode = res.mode.name,
            status = res.status.name,
            isHardAllocated = res.status == SubstrateReservationStatus.ALLOCATED_HARD,
            softHoldExpiresAt = res.softHoldExpiresAt,
            promotedAt = res.promotedAt,
            promotedBy = res.promotedBy,
            allocationSourcesCount = res.allocationSources.size,
            reservedBy = res.reservedBy,
            timestamp = System.currentTimeMillis()
        )
    }
}
