package com.sucharu.sucharupro.ui.features.inventory.substratereservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateReservationResponseDto
import com.sucharu.sucharupro.data.api.model.substratereservation.toDto
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperMaterialSpecification
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateAllocationSource
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateRequirement
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationStatus
import com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReservationMathUtils
import com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReservationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SubstrateReservationViewModel(
    private val reservationService: SubstrateReservationService,
    private val defaultTenantId: String = "TENANT-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SubstrateReservationUiState())
    val uiState: StateFlow<SubstrateReservationUiState> = _uiState.asStateFlow()

    init {
        loadReservations()
    }

    fun selectTab(tab: SubstrateReservationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadReservations(jobId: String? = null) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            try {
                val list = if (jobId != null) {
                    reservationService.listReservationsByJob(defaultTenantId, jobId)
                } else {
                    reservationService.listAllReservations(defaultTenantId)
                }
                val dtoList = list.map { it.toDto() }
                val totalSheets = dtoList.filter { it.status != "CANCELLED" }.sumOf { it.reservedSheets }
                val totalReams = dtoList.filter { it.status != "CANCELLED" }.fold(BigDecimal.ZERO) { acc, item -> acc + item.reservedReams }
                val totalWeight = dtoList.filter { it.status != "CANCELLED" }.fold(BigDecimal.ZERO) { acc, item -> acc + item.reservedWeightKg }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeReservations = dtoList,
                        totalReservedSheets = totalSheets,
                        totalReservedReams = totalReams,
                        totalReservedWeightKg = totalWeight
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load substrate reservations.") }
            }
        }
    }

    fun resolveRequirement(
        orderId: String,
        orderItemId: String,
        materialCode: String?,
        materialName: String,
        gsm: BigDecimal,
        sheetWidthMm: BigDecimal,
        sheetHeightMm: BigDecimal,
        productiveSheets: Long,
        wasteSheets: Long
    ) {
        _uiState.update { it.copy(isResolving = true, errorMessage = null) }
        scope.launch {
            try {
                val spec = PaperMaterialSpecification(
                    materialCode = materialCode,
                    materialName = materialName,
                    stockType = PaperStockType.ART_CARD,
                    gsm = gsm,
                    sheetDimension = PrintingDimension(sheetWidthMm, sheetHeightMm, MeasurementUnit.MILLIMETERS)
                )

                val dummyProducts = listOf(
                    InventoryProduct(
                        id = "PROD-01",
                        sku = materialCode ?: "ART-300-25X36",
                        name = materialName,
                        createdAt = "2026-09-01T00:00:00Z",
                        updatedAt = "2026-09-01T00:00:00Z",
                        createdBy = "system"
                    )
                )

                val result = reservationService.resolveRequirementAndCheckAvailability(
                    tenantId = defaultTenantId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    calculationId = null,
                    materialSpec = spec,
                    productiveSheetsRequired = productiveSheets,
                    wasteSheetsRequired = wasteSheets,
                    availableInventoryProducts = dummyProducts,
                    onHandPhysicalSheets = 50000L
                )

                _uiState.update {
                    it.copy(
                        isResolving = false,
                        resolutionResult = result.toDto(),
                        successMessage = "Substrate requirement successfully resolved with ${result.confidence}!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isResolving = false, errorMessage = e.message ?: "Resolution failed.") }
            }
        }
    }

    fun createReservation(
        orderId: String,
        orderItemId: String,
        executionJobId: String?,
        productId: String,
        sku: String,
        productName: String,
        totalSheets: Long,
        isHardAllocation: Boolean,
        notes: String?
    ) {
        _uiState.update { it.copy(isSubmittingReservation = true, errorMessage = null) }
        scope.launch {
            try {
                val req = SubstrateRequirement(
                    requirementId = "REQ-${java.util.UUID.randomUUID().toString().take(12)}",
                    tenantId = defaultTenantId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    stockType = PaperStockType.ART_CARD,
                    requestedMaterialCode = sku,
                    requestedMaterialName = productName,
                    gsm = BigDecimal("300.0000"),
                    sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
                    productiveSheetsRequired = totalSheets,
                    wasteSheetsRequired = 0L,
                    totalSheetsRequired = totalSheets,
                    totalReamsRequired = SubstrateReservationMathUtils.calculateReams(totalSheets),
                    totalWeightKg = SubstrateReservationMathUtils.calculateTotalWeightKg(
                        sheets = totalSheets,
                        gsm = BigDecimal("300.0000"),
                        dimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
                    )
                )

                val res = reservationService.createReservation(
                    tenantId = defaultTenantId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    executionJobId = executionJobId,
                    workOrderId = null,
                    productId = productId,
                    sku = sku,
                    productName = productName,
                    warehouseId = "WH-MAIN-01",
                    locationId = null,
                    requirement = req,
                    isHardAllocation = isHardAllocation,
                    notes = notes,
                    actor = "manager"
                )

                _uiState.update {
                    it.copy(
                        isSubmittingReservation = false,
                        showCreateDialog = false,
                        successMessage = "Substrate reservation created successfully: ${res.reservationId}"
                    )
                }
                loadReservations()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmittingReservation = false, errorMessage = e.message ?: "Reservation creation failed.") }
            }
        }
    }

    fun createSoftReservation(
        orderId: String,
        orderItemId: String,
        productId: String,
        sku: String,
        productName: String,
        totalSheets: Long,
        softHoldDurationMinutes: Long = 120L,
        notes: String?
    ) {
        _uiState.update { it.copy(isSubmittingReservation = true, errorMessage = null) }
        scope.launch {
            try {
                val req = SubstrateRequirement(
                    requirementId = "REQ-${java.util.UUID.randomUUID().toString().take(12)}",
                    tenantId = defaultTenantId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    stockType = PaperStockType.ART_CARD,
                    requestedMaterialCode = sku,
                    requestedMaterialName = productName,
                    gsm = BigDecimal("300.0000"),
                    sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
                    productiveSheetsRequired = totalSheets,
                    wasteSheetsRequired = 0L,
                    totalSheetsRequired = totalSheets,
                    totalReamsRequired = SubstrateReservationMathUtils.calculateReams(totalSheets),
                    totalWeightKg = SubstrateReservationMathUtils.calculateTotalWeightKg(
                        sheets = totalSheets,
                        gsm = BigDecimal("300.0000"),
                        dimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)
                    )
                )

                val res = reservationService.createSoftReservation(
                    tenantId = defaultTenantId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    productId = productId,
                    sku = sku,
                    productName = productName,
                    warehouseId = "WH-MAIN-01",
                    locationId = null,
                    requirement = req,
                    softHoldDurationMinutes = softHoldDurationMinutes,
                    notes = notes,
                    actor = "manager"
                )

                _uiState.update {
                    it.copy(
                        isSubmittingReservation = false,
                        showCreateDialog = false,
                        successMessage = "Soft Substrate Hold created: ${res.reservationId} (Expires in $softHoldDurationMinutes mins)"
                    )
                }
                loadReservations()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmittingReservation = false, errorMessage = e.message ?: "Soft reservation failed.") }
            }
        }
    }

    fun promoteSoftToHard(
        reservationId: String,
        executionJobId: String,
        workOrderId: String? = null,
        warehouseId: String? = null,
        locationId: String? = null,
        batchNumber: String? = null
    ) {
        _uiState.update { it.copy(isPromoting = true, errorMessage = null) }
        scope.launch {
            try {
                val res = reservationService.promoteSoftToHard(
                    tenantId = defaultTenantId,
                    reservationId = reservationId,
                    executionJobId = executionJobId,
                    workOrderId = workOrderId,
                    allocatedWarehouseId = warehouseId,
                    allocatedLocationId = locationId,
                    allocatedBatchNumber = batchNumber,
                    actor = "manager"
                )

                _uiState.update {
                    it.copy(
                        isPromoting = false,
                        showPromoteDialog = false,
                        selectedReservation = null,
                        successMessage = "Successfully promoted reservation ${res.reservationId} to HARD for job $executionJobId."
                    )
                }
                loadReservations()
            } catch (e: Exception) {
                _uiState.update { it.copy(isPromoting = false, errorMessage = e.message ?: "Promotion to HARD failed.") }
            }
        }
    }

    fun allocateSources(
        reservationId: String,
        sources: List<SubstrateAllocationSource>
    ) {
        _uiState.update { it.copy(isAllocating = true, errorMessage = null) }
        scope.launch {
            try {
                val res = reservationService.allocateReservationSources(
                    tenantId = defaultTenantId,
                    reservationId = reservationId,
                    sources = sources,
                    actor = "manager"
                )

                _uiState.update {
                    it.copy(
                        isAllocating = false,
                        showAllocateDialog = false,
                        selectedReservation = null,
                        successMessage = "Allocation sources updated successfully for ${res.reservationId}."
                    )
                }
                loadReservations()
            } catch (e: Exception) {
                _uiState.update { it.copy(isAllocating = false, errorMessage = e.message ?: "Allocation update failed.") }
            }
        }
    }

    fun releaseReservation(reservationId: String, reason: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            try {
                reservationService.releaseReservation(defaultTenantId, reservationId, reason, "manager")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showReleaseDialog = false,
                        successMessage = "Reservation released and substrate stock restored to available pool."
                    )
                }
                loadReservations()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to release reservation.") }
            }
        }
    }

    fun openCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun closeCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }
    fun openPromoteDialog(res: SubstrateReservationResponseDto) = _uiState.update { it.copy(showPromoteDialog = true, selectedReservation = res) }
    fun closePromoteDialog() = _uiState.update { it.copy(showPromoteDialog = false, selectedReservation = null) }
    fun openAllocateDialog(res: SubstrateReservationResponseDto) = _uiState.update { it.copy(showAllocateDialog = true, selectedReservation = res) }
    fun closeAllocateDialog() = _uiState.update { it.copy(showAllocateDialog = false, selectedReservation = null) }
    fun openReleaseDialog(res: SubstrateReservationResponseDto) = _uiState.update { it.copy(showReleaseDialog = true, selectedReservation = res) }
    fun closeReleaseDialog() = _uiState.update { it.copy(showReleaseDialog = false, selectedReservation = null) }
    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, successMessage = null) }
}
