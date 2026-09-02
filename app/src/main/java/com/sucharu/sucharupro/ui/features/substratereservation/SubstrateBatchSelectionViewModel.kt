package com.sucharu.sucharupro.ui.features.substratereservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.substratereservation.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.service.substratereservation.BatchLotSelectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for Substrate Batch/Lot Selection & Grain Matching Command Center.
 * Module 19 Step 03.
 */
class SubstrateBatchSelectionViewModel(
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SubstrateBatchSelectionUiState())
    val uiState: StateFlow<SubstrateBatchSelectionUiState> = _uiState.asStateFlow()

    init {
        loadDefaultSampleSelection()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setShowEvaluateDialog(show: Boolean) {
        _uiState.update { it.copy(showEvaluateDialog = show) }
    }

    fun setShowConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showConfirmDialog = show) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun evaluateSampleSelection(
        orderId: String = "ORD-2026-9041",
        orderItemId: String = "ITEM-8812",
        executionJobId: String = "JOB-OFFSET-601",
        requiredSheets: Long = 4500L,
        targetGsm: BigDecimal = BigDecimal("300.0000"),
        sheetWidthMm: BigDecimal = BigDecimal("635.0000"),
        sheetHeightMm: BigDecimal = BigDecimal("914.4000"),
        requiredGrain: String = "LONG_GRAIN",
        policy: String = "FIFO"
    ) {
        scope.launch {
            _uiState.update { it.copy(isEvaluating = true, errorMessage = null) }

            val spec = BatchLotSelectionSpecification(
                selectionId = "SBS-DEMO-01",
                tenantId = "TENANT-001",
                orderId = orderId,
                orderItemId = orderItemId,
                executionJobId = executionJobId,
                reservationId = "RES-DEMO-901",
                productId = "PROD-ART-300",
                sku = "SKU-ART-300-25X36",
                requestedMaterialName = "Premium Gloss Art Card 300 GSM",
                stockType = PaperStockType.ART_CARD,
                targetGsm = targetGsm,
                requiredSheetDimension = PrintingDimension(sheetWidthMm, sheetHeightMm, MeasurementUnit.MILLIMETERS),
                requiredGrainDirection = PaperGrainDirection.fromString(requiredGrain),
                requiredSheets = requiredSheets,
                allowSheetRotation = true,
                allowMultiBatchFulfillment = true,
                selectionPolicy = try { BatchSelectionPolicy.valueOf(policy) } catch (_: Exception) { BatchSelectionPolicy.FIFO },
                actor = "LEAD_ESTIMATOR"
            )

            val candidates = generateSampleCandidates("TENANT-001")
            val result = BatchLotSelectionEngine.selectBatches(spec, candidates)
            val dto = result.toDto()

            _uiState.update {
                it.copy(
                    isEvaluating = false,
                    currentSelection = dto,
                    recentSelections = listOf(dto),
                    successMessage = "Batch/Lot candidate evaluation completed successfully. Status: ${dto.status}",
                    showEvaluateDialog = false
                )
            }
        }
    }

    fun confirmSelection() {
        scope.launch {
            val current = _uiState.value.currentSelection ?: return@launch
            _uiState.update { it.copy(isConfirming = true) }

            val updated = current.copy(
                isConfirmedAndAllocated = true,
                confirmedAt = System.currentTimeMillis(),
                confirmedBy = "SYSTEM_SUPERVISOR"
            )

            _uiState.update {
                it.copy(
                    isConfirming = false,
                    currentSelection = updated,
                    successMessage = "Selection decision confirmed! Allocated ${updated.allocatedSheets} sheets to job ${updated.executionJobId}.",
                    showConfirmDialog = false
                )
            }
        }
    }

    fun exportHandoffJson() {
        val current = _uiState.value.currentSelection ?: return
        val json = """
        {
          "contractVersion": "3.0.0",
          "tenantId": "${current.tenantId}",
          "selectionId": "${current.selectionId}",
          "orderId": "${current.orderId}",
          "orderItemId": "${current.orderItemId}",
          "executionJobId": "${current.executionJobId}",
          "reservationId": "${current.reservationId}",
          "sku": "${current.sku}",
          "status": "${current.status}",
          "requiredSheets": ${current.requiredSheets},
          "allocatedSheets": ${current.allocatedSheets},
          "deficitSheets": ${current.deficitSheets},
          "allocatedReams": "${current.allocatedReams}",
          "allocatedWeightKg": "${current.allocatedWeightKg}",
          "isFullySatisfied": ${current.isFullySatisfied},
          "isMultiBatchFulfillment": ${current.isMultiBatchFulfillment},
          "targetGsm": "${current.targetGsm}",
          "targetSheetDimensions": "${current.requiredSheetWidthMm} x ${current.requiredSheetHeightMm} mm",
          "targetGrainDirection": "${current.requiredGrainDirection}",
          "selectedBatchCount": ${current.selectedBatches.size},
          "primaryBatch": "${current.primarySelectedBatchNumber}",
          "primaryLot": "${current.primarySelectedLotNumber}",
          "overallScore": "${current.overallCompatibilityScore}",
          "masterIntegrityHash": "${current.masterIntegrityHash}",
          "selectedBy": "${current.selectedBy}",
          "timestamp": ${System.currentTimeMillis()}
        }
        """.trimIndent()

        _uiState.update { it.copy(jsonHandoffPreview = json) }
    }

    private fun loadDefaultSampleSelection() {
        evaluateSampleSelection()
    }

    private fun generateSampleCandidates(tenantId: String): List<BatchLotInventoryCandidate> {
        return listOf(
            BatchLotInventoryCandidate(
                candidateId = "CAND-001",
                tenantId = tenantId,
                warehouseId = "WH-MAIN-01",
                warehouseName = "Central Paper Hub",
                locationId = "LOC-A1-R4",
                productId = "PROD-ART-300",
                sku = "SKU-ART-300-25X36",
                productName = "Premium Gloss Art Card 300 GSM (25x36)",
                batchNumber = "BATCH-2026-B01",
                lotNumber = "LOT-99201",
                supplierLotReference = "SUPP-SFI-441",
                stockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
                grainDirection = PaperGrainDirection.LONG_GRAIN,
                onHandPhysicalSheets = 6000L,
                reservedSheets = 1000L,
                hardAllocatedSheets = 0L,
                usableSheets = 5000L,
                receivedTimestamp = System.currentTimeMillis() - 86400000L * 10,
                qualityRating = BigDecimal("0.9800")
            ),
            BatchLotInventoryCandidate(
                candidateId = "CAND-002",
                tenantId = tenantId,
                warehouseId = "WH-MAIN-01",
                warehouseName = "Central Paper Hub",
                locationId = "LOC-A2-R1",
                productId = "PROD-ART-300",
                sku = "SKU-ART-300-36X25",
                productName = "Premium Gloss Art Card 300 GSM (36x25 Rotated)",
                batchNumber = "BATCH-2026-B02",
                lotNumber = "LOT-99202",
                supplierLotReference = "SUPP-SFI-442",
                stockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                sheetDimension = PrintingDimension(BigDecimal("914.4000"), BigDecimal("635.0000"), MeasurementUnit.MILLIMETERS),
                grainDirection = PaperGrainDirection.SHORT_GRAIN,
                onHandPhysicalSheets = 8000L,
                reservedSheets = 0L,
                hardAllocatedSheets = 0L,
                usableSheets = 8000L,
                receivedTimestamp = System.currentTimeMillis() - 86400000L * 25,
                qualityRating = BigDecimal("0.9500")
            ),
            BatchLotInventoryCandidate(
                candidateId = "CAND-003",
                tenantId = tenantId,
                warehouseId = "WH-SECONDARY",
                warehouseName = "Secondary Buffer Store",
                locationId = "LOC-B1-R2",
                productId = "PROD-ART-300",
                sku = "SKU-ART-300-28X40",
                productName = "Oversized Gloss Art Card 300 GSM (28x40)",
                batchNumber = "BATCH-2026-B03",
                lotNumber = "LOT-99203",
                supplierLotReference = "SUPP-SFI-449",
                stockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                sheetDimension = PrintingDimension(BigDecimal("711.2000"), BigDecimal("1016.0000"), MeasurementUnit.MILLIMETERS),
                grainDirection = PaperGrainDirection.LONG_GRAIN,
                onHandPhysicalSheets = 3500L,
                reservedSheets = 500L,
                hardAllocatedSheets = 0L,
                usableSheets = 3000L,
                receivedTimestamp = System.currentTimeMillis() - 86400000L * 40,
                qualityRating = BigDecimal("0.9000")
            )
        )
    }
}
