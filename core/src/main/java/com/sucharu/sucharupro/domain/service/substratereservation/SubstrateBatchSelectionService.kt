package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

/**
 * Service interface for Substrate Batch/Lot Selection, Grain Direction & Sheet Dimension Matching.
 * Module 19 Step 03.
 */
interface SubstrateBatchSelectionService {

    /**
     * Evaluates and deterministically ranks candidate batches to fulfill a production requirement.
     */
    suspend fun evaluateAndSelectBatches(
        spec: BatchLotSelectionSpecification,
        candidates: List<BatchLotInventoryCandidate>
    ): BatchLotSelectionResult

    /**
     * Retrieves an existing batch selection decision by ID.
     */
    suspend fun getSelectionResult(tenantId: String, selectionId: String): BatchLotSelectionResult?

    /**
     * Lists batch selection decisions for a given order.
     */
    suspend fun listSelectionsByOrder(tenantId: String, orderId: String): List<BatchLotSelectionResult>

    /**
     * Lists batch selection decisions for a given execution job.
     */
    suspend fun listSelectionsByJob(tenantId: String, executionJobId: String): List<BatchLotSelectionResult>

    /**
     * Confirms the selection decision and optionally links/promotes allocation sources in Module 19 Step 02 reservation.
     */
    suspend fun confirmSelectionAndAllocate(
        tenantId: String,
        selectionId: String,
        reservationService: SubstrateReservationService? = null,
        actor: String
    ): BatchLotSelectionResult

    /**
     * Exports the AI & cross-module governance handoff contract for Step 03.
     */
    suspend fun exportHandoffContract(tenantId: String, selectionId: String): Module19Step03BatchSelectionHandoffContract
}
