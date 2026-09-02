package com.sucharu.sucharupro.domain.service.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.datasource.businessreconciliation.DiscrepancyFilter
import com.sucharu.sucharupro.data.datasource.businessreconciliation.ReconciliationRunFilter
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.common.DomainResult

data class CreateReconciliationRunCommand(
    val periodId: String,
    val runNumber: String? = null,
    val runType: ReconciliationRunType = ReconciliationRunType.FULL_PERIOD,
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class AssignDiscrepancyCommand(
    val discrepancyId: String,
    val assignedTo: String
)

data class ResolveDiscrepancyCommand(
    val discrepancyId: String,
    val resolutionNote: String,
    val correlationId: String? = null
)

data class WaiveDiscrepancyCommand(
    val discrepancyId: String,
    val waiverReason: String,
    val correlationId: String? = null
)

data class RejectDiscrepancyCommand(
    val discrepancyId: String,
    val rejectionReason: String,
    val correlationId: String? = null
)

data class ApproveReconciliationCommand(
    val runId: String,
    val notes: String? = null,
    val correlationId: String? = null
)

data class LinkCorrectionCommand(
    val discrepancyId: String,
    val correctionType: String,
    val correctionId: String,
    val note: String? = null,
    val correlationId: String? = null
)

interface BusinessFinancialReconciliationService {
    suspend fun createReconciliationRun(
        principal: AuthenticatedPrincipal,
        command: CreateReconciliationRunCommand
    ): DomainResult<BusinessFinancialReconciliationRun>

    suspend fun executeReconciliationRun(
        principal: AuthenticatedPrincipal,
        runId: String,
        correlationId: String? = null
    ): DomainResult<BusinessFinancialReconciliationRun>

    suspend fun getReconciliationRunById(
        principal: AuthenticatedPrincipal,
        runId: String
    ): DomainResult<BusinessFinancialReconciliationRun>

    suspend fun listReconciliationRuns(
        principal: AuthenticatedPrincipal,
        filter: ReconciliationRunFilter = ReconciliationRunFilter()
    ): DomainResult<List<BusinessFinancialReconciliationRun>>

    suspend fun getDiscrepancyById(
        principal: AuthenticatedPrincipal,
        discrepancyId: String
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy>

    suspend fun listDiscrepancies(
        principal: AuthenticatedPrincipal,
        filter: DiscrepancyFilter = DiscrepancyFilter()
    ): DomainResult<List<BusinessFinancialReconciliationDiscrepancy>>

    suspend fun assignDiscrepancy(
        principal: AuthenticatedPrincipal,
        command: AssignDiscrepancyCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy>

    suspend fun resolveDiscrepancy(
        principal: AuthenticatedPrincipal,
        command: ResolveDiscrepancyCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy>

    suspend fun waiveDiscrepancy(
        principal: AuthenticatedPrincipal,
        command: WaiveDiscrepancyCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy>

    suspend fun rejectDiscrepancy(
        principal: AuthenticatedPrincipal,
        command: RejectDiscrepancyCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy>

    suspend fun approveReconciliationRun(
        principal: AuthenticatedPrincipal,
        command: ApproveReconciliationCommand
    ): DomainResult<BusinessFinancialReconciliationRun>

    suspend fun linkCorrection(
        principal: AuthenticatedPrincipal,
        command: LinkCorrectionCommand
    ): DomainResult<BusinessFinancialReconciliationDiscrepancy>

    suspend fun getPeriodCloseReadiness(
        principal: AuthenticatedPrincipal,
        periodId: String
    ): DomainResult<PeriodCloseReadiness>

    suspend fun getDashboardSummary(
        principal: AuthenticatedPrincipal,
        periodId: String? = null
    ): DomainResult<ReconciliationDashboardSummary>

    suspend fun listAuditEvents(
        principal: AuthenticatedPrincipal,
        runId: String? = null,
        discrepancyId: String? = null
    ): DomainResult<List<BusinessFinancialReconciliationAuditEvent>>
}
