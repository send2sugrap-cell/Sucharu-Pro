package com.sucharu.sucharupro.domain.service.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service contract for the Commercial Commitment and Quotation -> Order Conversion Engine.
 * Module 17 Step 03.
 */
interface CommercialCommitmentService {

    suspend fun evaluateEligibility(tenantId: String, quotationId: String): DomainResult<ConversionEligibility>

    suspend fun prepareCommitment(
        tenantId: String,
        quotationId: String,
        targetVersionNumber: Int?,
        request: ConvertQuotationToOrderRequest?,
        actor: String
    ): DomainResult<CommercialCommitment>

    suspend fun convertQuotationToOrder(
        tenantId: String,
        request: ConvertQuotationToOrderRequest
    ): DomainResult<ConversionResult>

    suspend fun getCommitment(tenantId: String, commitmentId: String): DomainResult<CommercialCommitment?>

    suspend fun getCommitmentByQuotation(tenantId: String, quotationId: String): DomainResult<CommercialCommitment?>

    suspend fun listCommitments(tenantId: String, limit: Int = 50): DomainResult<List<CommercialCommitment>>

    suspend fun listCommitmentEvents(tenantId: String, commitmentId: String): DomainResult<List<CommercialCommitmentEvent>>

    suspend fun reconcileCommitment(tenantId: String, commitmentId: String): DomainResult<CommercialCommitmentReconciliationResult>

    suspend fun exportHandoffContract(tenantId: String, commitmentId: String): DomainResult<Module17Step03CommercialCommitmentHandoffContract>
}
