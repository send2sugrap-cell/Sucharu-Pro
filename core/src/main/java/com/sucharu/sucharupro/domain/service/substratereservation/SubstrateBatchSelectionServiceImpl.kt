package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateBatchSelectionRepository
import java.util.UUID

/**
 * Implementation of SubstrateBatchSelectionService.
 * Module 19 Step 03.
 */
class SubstrateBatchSelectionServiceImpl(
    private val repository: SubstrateBatchSelectionRepository
) : SubstrateBatchSelectionService {

    override suspend fun evaluateAndSelectBatches(
        spec: BatchLotSelectionSpecification,
        candidates: List<BatchLotInventoryCandidate>
    ): BatchLotSelectionResult {
        require(spec.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(spec.orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(spec.requiredSheets > 0L) { "Required sheets must be greater than zero." }
        require(spec.targetGsm > java.math.BigDecimal.ZERO) { "Target GSM must be positive." }

        // Filter candidates to ensure tenant isolation
        val tenantIsolatedCandidates = candidates.filter { it.tenantId == spec.tenantId }

        val result = BatchLotSelectionEngine.selectBatches(spec, tenantIsolatedCandidates)
        return repository.saveSelectionResult(result)
    }

    override suspend fun getSelectionResult(tenantId: String, selectionId: String): BatchLotSelectionResult? {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(selectionId.isNotBlank()) { "Selection ID cannot be blank." }
        return repository.getSelectionById(tenantId, selectionId)
    }

    override suspend fun listSelectionsByOrder(tenantId: String, orderId: String): List<BatchLotSelectionResult> {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        return repository.getSelectionsByOrder(tenantId, orderId)
    }

    override suspend fun listSelectionsByJob(tenantId: String, executionJobId: String): List<BatchLotSelectionResult> {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(executionJobId.isNotBlank()) { "Execution Job ID cannot be blank." }
        return repository.getSelectionsByJob(tenantId, executionJobId)
    }

    override suspend fun confirmSelectionAndAllocate(
        tenantId: String,
        selectionId: String,
        reservationService: SubstrateReservationService?,
        actor: String
    ): BatchLotSelectionResult {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(selectionId.isNotBlank()) { "Selection ID cannot be blank." }

        val existing = repository.getSelectionById(tenantId, selectionId)
            ?: throw IllegalArgumentException("Batch selection record not found: $selectionId")

        val confirmedAt = System.currentTimeMillis()
        repository.confirmSelection(tenantId, selectionId, actor, confirmedAt)

        // Interlock with Step 02 reservation if reservationId exists and service is provided
        val resId = existing.specification.reservationId
        if (!resId.isNullOrBlank() && reservationService != null && existing.selectedBatches.isNotEmpty()) {
            val allocationSources = existing.selectedBatches.map { batch ->
                SubstrateAllocationSource(
                    allocationId = "SAS-${UUID.randomUUID().toString().take(8).uppercase()}",
                    reservationId = resId,
                    tenantId = tenantId,
                    warehouseId = batch.warehouseId,
                    locationId = batch.locationId,
                    batchNumber = batch.batchNumber,
                    allocatedSheets = batch.allocatedSheets,
                    allocatedReams = batch.allocatedReams,
                    allocatedWeightKg = batch.allocatedWeightKg,
                    allocatedAt = confirmedAt,
                    allocatedBy = actor
                )
            }
            reservationService.allocateReservationSources(tenantId, resId, allocationSources, actor)
        }

        return existing.copy(
            isConfirmedAndAllocated = true,
            confirmedBy = actor,
            confirmedAt = confirmedAt
        )
    }

    override suspend fun exportHandoffContract(
        tenantId: String,
        selectionId: String
    ): Module19Step03BatchSelectionHandoffContract {
        val result = getSelectionResult(tenantId, selectionId)
            ?: throw IllegalArgumentException("Batch selection record not found: $selectionId")

        val batchSummaries = result.selectedBatches.map {
            SelectedBatchSummaryDto(
                batchNumber = it.batchNumber,
                lotNumber = it.lotNumber,
                warehouseId = it.warehouseId,
                allocatedSheets = it.allocatedSheets,
                allocatedReams = it.allocatedReams,
                allocatedWeightKg = it.allocatedWeightKg,
                grainDirection = it.grainDirection.name,
                isRotated = it.isRotated,
                matchScore = it.matchScore
            )
        }

        return Module19Step03BatchSelectionHandoffContract(
            contractVersion = "3.0.0",
            tenantId = tenantId,
            selectionId = result.selectionId,
            orderId = result.specification.orderId,
            orderItemId = result.specification.orderItemId,
            executionJobId = result.specification.executionJobId,
            reservationId = result.specification.reservationId,
            sku = result.specification.sku,
            status = result.status.name,
            requiredSheets = result.requiredSheets,
            allocatedSheets = result.allocatedSheets,
            deficitSheets = result.deficitSheets,
            allocatedReams = result.allocatedReams,
            allocatedWeightKg = result.allocatedWeightKg,
            isFullySatisfied = result.isFullySatisfied,
            isMultiBatchFulfillment = result.isMultiBatchFulfillment,
            targetGsm = result.specification.targetGsm,
            targetSheetWidthMm = result.specification.requiredSheetDimension.width,
            targetSheetHeightMm = result.specification.requiredSheetDimension.height,
            targetGrainDirection = result.specification.requiredGrainDirection.name,
            selectedBatchCount = result.selectedBatches.size,
            selectedBatches = batchSummaries,
            overallScore = result.overallCompatibilityScore,
            selectionExplanation = result.selectionExplanation,
            masterIntegrityHash = result.masterIntegrityHash,
            selectedBy = result.selectedBy,
            timestamp = System.currentTimeMillis()
        )
    }
}
