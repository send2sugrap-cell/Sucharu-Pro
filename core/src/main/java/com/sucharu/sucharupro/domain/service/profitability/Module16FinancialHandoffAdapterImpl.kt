package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.SourceIntegrityStatus
import com.sucharu.sucharupro.domain.service.businessintegrity.BusinessFinancialIntegrityService

/**
 * Production implementation of Module16FinancialHandoffAdapter.
 * Bridges Module 15's canonical financial integrity layer to Module 16 Profit & Cost Analysis.
 */
class Module16FinancialHandoffAdapterImpl(
    private val integrityService: BusinessFinancialIntegrityService
) : Module16FinancialHandoffAdapter {

    override suspend fun getVerifiedFinancialHandoff(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<ValidatedFinancialHandoff> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank for financial handoff")
        }
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank for financial handoff")
        }
        if (periodId.isBlank()) {
            return DomainResult.Error(message = "Period ID cannot be blank for financial handoff")
        }

        val contractResult = integrityService.generateModule16HandoffContract(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId
        )

        return when (contractResult) {
            is DomainResult.Success -> {
                val contract = contractResult.data
                val validationNotes = mutableListOf<String>()

                if (contract.tenantId != tenantId) {
                    return DomainResult.Error(message = "Tenant mismatch in financial handoff contract: expected $tenantId, got ${contract.tenantId}")
                }
                if (contract.projectId != projectId) {
                    return DomainResult.Error(message = "Project mismatch in financial handoff contract: expected $projectId, got ${contract.projectId}")
                }

                var integrityStatus = SourceIntegrityStatus.VERIFIED

                if (!contract.isLedgerBalanced) {
                    integrityStatus = SourceIntegrityStatus.SOURCE_CONFLICT
                    validationNotes.add("Canonical General Business Ledger is not balanced (Debits != Credits). Profitability calculations may have source conflicts.")
                }

                if (contract.isPeriodClosed && contract.closureCertificateChecksum.isNullOrBlank()) {
                    integrityStatus = SourceIntegrityStatus.PARTIALLY_VERIFIED
                    validationNotes.add("Financial period is marked CLOSED but lacks a verified cryptographic closure certificate checksum.")
                }

                if (contract.isPeriodClosed) {
                    validationNotes.add("Financial period is LOCKED and CLOSED. Analytical snapshot will be pinned to closed period baseline.")
                }

                val hasValidCertificate = contract.isPeriodClosed && !contract.closureCertificateChecksum.isNullOrBlank()

                DomainResult.Success(
                    ValidatedFinancialHandoff(
                        contract = contract,
                        integrityStatus = integrityStatus,
                        isLedgerBalanced = contract.isLedgerBalanced,
                        isPeriodClosed = contract.isPeriodClosed,
                        hasValidClosureCertificate = hasValidCertificate,
                        validationNotes = validationNotes
                    )
                )
            }
            is DomainResult.Error -> {
                DomainResult.Error(message = contractResult.message)
            }
            DomainResult.Loading -> {
                DomainResult.Error(message = "Financial handoff operation is still loading")
            }
        }
    }

    override suspend fun verifyPeriodIntegrityStatus(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<SourceIntegrityStatus> {
        return when (val res = getVerifiedFinancialHandoff(tenantId, projectId, periodId)) {
            is DomainResult.Success -> DomainResult.Success(res.data.integrityStatus)
            is DomainResult.Error -> DomainResult.Error(message = res.message)
            DomainResult.Loading -> DomainResult.Error(message = "Financial handoff operation is still loading")
        }
    }
}
