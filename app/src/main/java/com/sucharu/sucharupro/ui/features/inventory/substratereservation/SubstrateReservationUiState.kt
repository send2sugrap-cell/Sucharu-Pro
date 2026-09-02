package com.sucharu.sucharupro.ui.features.inventory.substratereservation

import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateRealTimeAvailabilityResponseDto
import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateReservationResponseDto
import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateSkuResolutionResponseDto
import java.math.BigDecimal

data class SubstrateReservationUiState(
    val isLoading: Boolean = false,
    val selectedTab: SubstrateReservationTab = SubstrateReservationTab.ACTIVE_RESERVATIONS,
    val resolutionResult: SubstrateSkuResolutionResponseDto? = null,
    val activeReservations: List<SubstrateReservationResponseDto> = emptyList(),
    val totalReservedSheets: Long = 0L,
    val totalReservedReams: BigDecimal = BigDecimal.ZERO,
    val totalReservedWeightKg: BigDecimal = BigDecimal.ZERO,
    val isResolving: Boolean = false,
    val isSubmittingReservation: Boolean = false,
    val isPromoting: Boolean = false,
    val isAllocating: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showPromoteDialog: Boolean = false,
    val showAllocateDialog: Boolean = false,
    val showReleaseDialog: Boolean = false,
    val selectedReservation: SubstrateReservationResponseDto? = null,
    val realTimeAvailability: SubstrateRealTimeAvailabilityResponseDto? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class SubstrateReservationTab(val title: String) {
    ACTIVE_RESERVATIONS("Active Holds"),
    REQUIREMENT_RESOLVER("Requirement Resolver"),
    INVENTORY_INTERLOCK("Stock Interlock"),
    AUDIT_TIMELINE("Audit History"),
    AI_HANDOFF("AI Contract")
}
