package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorRfqEvaluationService {

    suspend fun getComparisonSnapshot(
        rfqId: String,
        tenantId: String
    ): DomainResult<VendorRfqComparisonSnapshot>

    suspend fun recordEvaluation(
        evaluation: VendorRfqEvaluation,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfqEvaluation>

    suspend fun approveEvaluation(
        evaluationId: String,
        rfqId: String,
        tenantId: String,
        approverId: String
    ): DomainResult<VendorRfqEvaluation>

    suspend fun awardRfq(
        rfqId: String,
        winningQuotationId: String,
        awardReason: String,
        tenantId: String,
        awardedBy: String
    ): DomainResult<VendorRfq>

    suspend fun listEvaluations(
        rfqId: String,
        tenantId: String
    ): DomainResult<List<VendorRfqEvaluation>>
}
