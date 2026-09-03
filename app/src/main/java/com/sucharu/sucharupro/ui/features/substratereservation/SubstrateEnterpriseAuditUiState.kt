package com.sucharu.sucharupro.ui.features.substratereservation

import com.sucharu.sucharupro.data.api.model.substratereservation.*

data class SubstrateEnterpriseAuditUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTab: EnterpriseAuditTab = EnterpriseAuditTab.OVERVIEW,
    val governanceSummary: EnterpriseReservationGovernanceSummaryDto? = null,
    val auditEvents: List<SubstrateEnterpriseAuditRecordDto> = emptyList(),
    val activeReconciliation: SubstrateReservationReconciliationDto? = null,
    val integrityResult: SubstrateIntegrityVerificationResultDto? = null,
    val aiHandoffContract: Module19Step06EnterpriseReservationHandoffContractDto? = null,
    val selectedReservationId: String = "RES-AUTO-001",
    val isReadOnly: Boolean = false
)

enum class EnterpriseAuditTab(val title: String) {
    OVERVIEW("Enterprise Overview"),
    AUDIT_TRAIL("Lifecycle Audit Trail"),
    RECONCILIATION("Reconciliation & Exceptions"),
    INTEGRITY_SECURITY("Integrity & RLS"),
    AI_HANDOFF("Cross-Module AI Handoff")
}
