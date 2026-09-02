package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperMaterialSpecification
import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.math.BigDecimal

interface SubstrateReservationService {

    /**
     * Resolves upstream material demand and matches against canonical Module 06 inventory products.
     */
    suspend fun resolveRequirementAndCheckAvailability(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        calculationId: String? = null,
        materialSpec: PaperMaterialSpecification,
        productiveSheetsRequired: Long,
        wasteSheetsRequired: Long,
        availableInventoryProducts: List<InventoryProduct>,
        onHandPhysicalSheets: Long,
        warehouseId: String? = "WH-MAIN-01",
        warehouseName: String? = "Central Paper Warehouse"
    ): SubstrateSkuResolutionResult

    /**
     * General reservation creation method (backward-compatible with Step 01).
     */
    suspend fun createReservation(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        executionJobId: String? = null,
        workOrderId: String? = null,
        productId: String,
        sku: String,
        productName: String,
        warehouseId: String,
        locationId: String? = null,
        requirement: SubstrateRequirement,
        isHardAllocation: Boolean = false,
        expiryTimestamp: Long? = null,
        notes: String? = null,
        actor: String
    ): SubstrateReservation

    /**
     * Creates an authoritative Soft Reservation (RESERVED_SOFT) for pre-production or quotation claims.
     */
    suspend fun createSoftReservation(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        productId: String,
        sku: String,
        productName: String,
        warehouseId: String,
        locationId: String? = null,
        requirement: SubstrateRequirement,
        softHoldDurationMinutes: Long = 120L,
        notes: String? = null,
        actor: String
    ): SubstrateReservation

    /**
     * Creates an authoritative Hard Reservation (ALLOCATED_HARD) with validated physical allocation.
     */
    suspend fun createHardReservation(
        tenantId: String,
        orderId: String,
        orderItemId: String,
        executionJobId: String,
        workOrderId: String? = null,
        productId: String,
        sku: String,
        productName: String,
        warehouseId: String,
        locationId: String? = null,
        batchNumber: String? = null,
        requirement: SubstrateRequirement,
        notes: String? = null,
        actor: String
    ): SubstrateReservation

    /**
     * Atomically promotes a soft hold reservation (RESERVED_SOFT) to hard allocated (ALLOCATED_HARD)
     * after re-verifying real-time authoritative availability.
     */
    suspend fun promoteSoftToHard(
        tenantId: String,
        reservationId: String,
        executionJobId: String,
        workOrderId: String? = null,
        allocatedWarehouseId: String? = null,
        allocatedLocationId: String? = null,
        allocatedBatchNumber: String? = null,
        actor: String
    ): SubstrateReservation

    /**
     * Associates physical warehouse and batch allocation sources to a hard reservation.
     */
    suspend fun allocateReservationSources(
        tenantId: String,
        reservationId: String,
        sources: List<SubstrateAllocationSource>,
        actor: String
    ): SubstrateReservation

    /**
     * Promotes a soft hold reservation to hard allocated for a scheduled production job (Step 01 alias).
     */
    suspend fun allocateHardForJob(
        tenantId: String,
        reservationId: String,
        executionJobId: String,
        workOrderId: String? = null,
        actor: String
    ): SubstrateReservation

    /**
     * Releases or cancels an active reservation, restoring available inventory.
     */
    suspend fun releaseReservation(
        tenantId: String,
        reservationId: String,
        reason: String,
        actor: String
    ): SubstrateReservation

    /**
     * Returns a reservation by ID.
     */
    suspend fun getReservation(tenantId: String, reservationId: String): SubstrateReservation?

    /**
     * Lists all reservations for a given execution job.
     */
    suspend fun listReservationsByJob(tenantId: String, executionJobId: String): List<SubstrateReservation>

    /**
     * Lists all reservations for a given order.
     */
    suspend fun listReservationsByOrder(tenantId: String, orderId: String): List<SubstrateReservation>

    /**
     * Lists all reservations across the tenant with pagination limit.
     */
    suspend fun listAllReservations(tenantId: String, limit: Int = 50): List<SubstrateReservation>

    /**
     * Exports the AI & cross-module governance handoff contract for Step 01.
     */
    suspend fun exportHandoffContract(tenantId: String, reservationId: String): Module19Step01SubstrateReservationHandoffContract

    /**
     * Exports the extended AI & cross-module governance handoff contract for Step 02.
     */
    suspend fun exportStep02HandoffContract(tenantId: String, reservationId: String): Module19Step02SubstrateReservationHandoffContract
}
