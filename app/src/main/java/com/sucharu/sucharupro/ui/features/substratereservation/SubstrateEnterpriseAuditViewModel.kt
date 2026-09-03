package com.sucharu.sucharupro.ui.features.substratereservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.substratereservation.*
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.service.substratereservation.SubstrateEnterpriseAuditEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Substrate Enterprise Audit, Reconciliation & AI Handoff Command Center.
 * Module 19 Step 06.
 */
class SubstrateEnterpriseAuditViewModel(
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SubstrateEnterpriseAuditUiState())
    val uiState: StateFlow<SubstrateEnterpriseAuditUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun selectTab(tab: EnterpriseAuditTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun setReservationId(reservationId: String) {
        _uiState.update { it.copy(selectedReservationId = reservationId) }
        loadAuditHistory(reservationId)
    }

    fun loadInitialData() {
        loadGovernanceSummary()
        loadAuditHistory(_uiState.value.selectedReservationId)
        runReconciliation(_uiState.value.selectedReservationId)
        verifyAuditIntegrity(_uiState.value.selectedReservationId)
        generateAiHandoff(_uiState.value.selectedReservationId)
    }

    fun loadGovernanceSummary() {
        val summary = EnterpriseReservationGovernanceSummaryDto(
            totalReservationsAudited = 142L,
            activeHardAllocations = 88L,
            activeSoftReservations = 54L,
            reconciledHealthyCount = 138L,
            discrepanciesDetectedCount = 4L,
            integrityVerifiedIntactCount = 142L,
            integrityViolationsCount = 0L,
            pendingReplenishmentAlertsCount = 2L,
            activeReleaseReviewsCount = 1L
        )
        _uiState.update { it.copy(governanceSummary = summary) }
    }

    fun loadAuditHistory(reservationId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val mockAudits = createSampleAuditChain(reservationId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        auditEvents = mockAudits.map { rec -> rec.toDto() }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun runReconciliation(reservationId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val reconciliation = SubstrateEnterpriseAuditEngine.reconcileReservation(
                    tenantId = "TENANT-001",
                    reservationId = reservationId,
                    orderId = "ORD-2026-9041",
                    jobId = "JOB-2026-1122",
                    sku = "ART-300-25X36",
                    requiredSheets = 10000L,
                    reservedSheets = 10000L,
                    physicalOnHandSheets = 15000L,
                    allocatedBatchSheets = 10000L,
                    releasableSheets = 0L,
                    consumedSheets = 0L,
                    committedSheets = 10000L,
                    replenishmentRequiredSheets = 0L,
                    isProductionInProgress = false,
                    reservationStatus = SubstrateReservationStatus.ALLOCATED_HARD,
                    reconciledBy = "enterprise_auditor",
                    notes = "Scheduled automated reconciliation verification."
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeReconciliation = reconciliation.toDto(),
                        successMessage = "Reconciliation completed: Status ${reconciliation.status.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun verifyAuditIntegrity(reservationId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val sampleChain = createSampleAuditChain(reservationId)
                val verificationResult = SubstrateEnterpriseAuditEngine.verifyAuditChain(
                    tenantId = "TENANT-001",
                    reservationId = reservationId,
                    records = sampleChain,
                    verifiedBy = "security_officer"
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        integrityResult = verificationResult.toDto(),
                        successMessage = "Integrity verified: Chain is ${verificationResult.status.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun generateAiHandoff(reservationId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val mockReservation = SubstrateReservation(
                    reservationId = reservationId,
                    tenantId = "TENANT-001",
                    orderId = "ORD-2026-9041",
                    orderItemId = "ITEM-01",
                    executionJobId = "JOB-2026-1122",
                    workOrderId = null,
                    productId = "PROD-ART300",
                    sku = "ART-300-25X36",
                    productName = "Art Card 300 GSM (25x36)",
                    warehouseId = "WH-CENTRAL-01",
                    locationId = null,
                    stockType = com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType.ART_CARD,
                    gsm = java.math.BigDecimal("300.0000"),
                    sheetDimension = com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension(
                        java.math.BigDecimal("635.0000"),
                        java.math.BigDecimal("914.4000"),
                        com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit.MILLIMETERS
                    ),
                    reservedSheets = 10000L,
                    reservedReams = java.math.BigDecimal("20.0000"),
                    reservedWeightKg = java.math.BigDecimal("400.0000"),
                    status = SubstrateReservationStatus.ALLOCATED_HARD,
                    mode = SubstrateReservationMode.HARD,
                    idempotencyKey = "IDEMP-01",
                    expiryTimestamp = null,
                    softHoldExpiresAt = null,
                    promotedAt = null,
                    promotedBy = null,
                    reservedBy = "system_scheduler",
                    reservedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    notes = null,
                    allocationSources = emptyList()
                )

                val contract = SubstrateEnterpriseAuditEngine.synthesizeEnterpriseHandoffContract(
                    tenantId = "TENANT-001",
                    reservation = mockReservation,
                    batchSummary = "LOT-2026-09A (10,000 sheets)",
                    grainCompatibility = "GRAIN_COMPATIBLE (Long Grain)",
                    replenishmentState = "NORMAL",
                    supplierAlertSent = false,
                    releaseDecision = "NO_RELEASE_REQUIRED",
                    releasableSheets = 0L,
                    consumedSheets = 0L,
                    productionCommitmentState = "COMMITTED",
                    reconciliation = null,
                    integrityResult = null,
                    auditTrailCount = 6,
                    latestAuditHash = "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069"
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        aiHandoffContract = contract.toDto(),
                        successMessage = "Cross-Module AI Handoff contract synthesized (v6.0.0)."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun createSampleAuditChain(reservationId: String): List<SubstrateEnterpriseAuditRecord> {
        val records = mutableListOf<SubstrateEnterpriseAuditRecord>()
        var prevHash: String? = null

        val stages = listOf(
            Triple(ReservationAuditEventType.REQUIREMENT_RESOLVED, "Requirement calculated from estimation", "RESOLVED"),
            Triple(ReservationAuditEventType.INVENTORY_INTERLOCKED, "Stock matched against physical master", "MATCHED"),
            Triple(ReservationAuditEventType.SOFT_RESERVED, "Soft hold placed for quotation validation", "RESERVED_SOFT"),
            Triple(ReservationAuditEventType.HARD_ALLOCATED, "Hard allocation committed for scheduled job", "ALLOCATED_HARD"),
            Triple(ReservationAuditEventType.BATCH_LOT_SELECTED, "FEFO strategy allocated LOT-2026-09A", "BATCH_ASSIGNED"),
            Triple(ReservationAuditEventType.RECONCILIATION_EVALUATED, "Reconciliation evaluated: 0 discrepancies", "HEALTHY")
        )

        var time = 1756880000000L
        for ((eventType, reason, state) in stages) {
            val corrId = UUID.randomUUID().toString()
            val recordHash = SubstrateEnterpriseAuditEngine.computeRecordHash(
                tenantId = "TENANT-001",
                reservationId = reservationId,
                reservationVersion = 1L,
                jobId = "JOB-2026-1122",
                orderId = "ORD-2026-9041",
                orderItemId = "ITEM-01",
                eventType = eventType,
                previousState = null,
                newState = state,
                actorType = AuditActorType.SYSTEM,
                actorId = "system_scheduler",
                role = "SYSTEM",
                timestamp = time,
                correlationId = corrId,
                sourceOperation = eventType.name
            )
            val chainHash = SubstrateEnterpriseAuditEngine.computeChainHash(prevHash, recordHash)

            records.add(
                SubstrateEnterpriseAuditRecord(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = "TENANT-001",
                    reservationId = reservationId,
                    reservationVersion = 1L,
                    jobId = "JOB-2026-1122",
                    orderId = "ORD-2026-9041",
                    orderItemId = "ITEM-01",
                    eventType = eventType,
                    previousState = null,
                    newState = state,
                    actorType = AuditActorType.SYSTEM,
                    actorId = "system_scheduler",
                    role = "SYSTEM",
                    permissionContext = "SUBSTRATE_AUTO_RESERVATION",
                    timestamp = time,
                    reason = reason,
                    correlationId = corrId,
                    sourceOperation = eventType.name,
                    recordHash = recordHash,
                    previousAuditHash = prevHash,
                    chainHash = chainHash
                )
            )
            prevHash = chainHash
            time += 10000L
        }

        return records
    }
}
