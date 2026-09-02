package com.sucharu.sucharupro.domain.service.customercreditcontrol

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercreditcontrol.*
import java.math.BigDecimal

/**
 * Service interface contract for Customer Credit Limits, Payment Terms & Receivable Risk Control (Module 14 Step 07).
 */
interface CustomerCreditControlService {

    suspend fun getOrCreateCreditProfile(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditProfileEntity>

    suspend fun updateCreditProfile(
        tenantId: String,
        projectId: String,
        customerId: String,
        creditLimit: BigDecimal,
        currency: String = "BDT",
        paymentTermsType: CustomerPaymentTermsType,
        creditDays: Int,
        requiresAdvance: Boolean,
        notes: String?,
        actorId: String,
        actorRole: String,
        reason: String
    ): DomainResult<CustomerCreditProfileEntity>

    suspend fun placeFinancialHold(
        tenantId: String,
        projectId: String,
        customerId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCreditProfileEntity>

    suspend fun releaseFinancialHold(
        tenantId: String,
        projectId: String,
        customerId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCreditProfileEntity>

    suspend fun evaluateCredit(
        tenantId: String,
        projectId: String,
        request: CustomerCreditCheckRequest
    ): DomainResult<CustomerCreditCheckResult>

    suspend fun getReceivableRiskSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerReceivableRiskSummary>

    suspend fun getReceivableAgingReport(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<CustomerReceivableAgingReport>

    suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<List<CustomerCreditControlAuditEvent>>
}
