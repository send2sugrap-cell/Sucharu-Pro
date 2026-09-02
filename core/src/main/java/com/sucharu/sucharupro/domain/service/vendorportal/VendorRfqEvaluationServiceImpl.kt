package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorQuotationRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.repository.VendorRfqRepository
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorQuotationCalculator
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorQuotationValidator
import java.util.UUID

class VendorRfqEvaluationServiceImpl(
    private val rfqRepository: VendorRfqRepository,
    private val quotationRepository: VendorQuotationRepository,
    private val vendorRepository: VendorRepository
) : VendorRfqEvaluationService {

    override suspend fun getComparisonSnapshot(
        rfqId: String,
        tenantId: String
    ): DomainResult<VendorRfqComparisonSnapshot> {
        return try {
            val rfq = when (val res = rfqRepository.findRfqById(rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val invitations = (rfqRepository.listInvitationsByRfq(rfqId, tenantId) as? DomainResult.Success)?.data ?: emptyList()
            val quotations = (quotationRepository.listQuotationsByRfq(rfqId, tenantId) as? DomainResult.Success)?.data ?: emptyList()
            val evaluations = (quotationRepository.listEvaluationsByRfq(rfqId, tenantId) as? DomainResult.Success)?.data ?: emptyList()
            val evalMap = evaluations.associateBy { it.quotationId }

            // Resolve vendor codes and names
            val vendorInfoMap = mutableMapOf<String, Pair<String, String>>()
            for (q in quotations) {
                val vRes = vendorRepository.findById(rfq.projectId, q.vendorId)
                if (vRes is DomainResult.Success) {
                    vendorInfoMap[q.vendorId] = Pair(vRes.data.vendorCode, vRes.data.vendorName)
                }
            }

            val snapshot = VendorQuotationCalculator.generateComparisonSnapshot(
                rfq = rfq,
                totalInvited = invitations.size,
                quotations = quotations,
                evaluations = evalMap,
                vendorInfoMap = vendorInfoMap
            )
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordEvaluation(
        evaluation: VendorRfqEvaluation,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfqEvaluation> {
        return try {
            val rfq = when (val res = rfqRepository.findRfqById(evaluation.rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val quotation = when (val res = quotationRepository.findQuotationById(evaluation.quotationId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            require(quotation.rfqId == rfq.rfqId) { "Quotation does not belong to the target RFQ." }

            // Separation of Duties: Quotation submitter cannot evaluate their own bid
            require(actorId != quotation.submittedBy && actorId != quotation.vendorId) {
                "Separation of Duties violation: Vendor or quotation submitter cannot evaluate the quotation."
            }

            // Calculate weighted scores
            val calculatedScores = evaluation.scores.map { score ->
                val weighted = VendorQuotationCalculator.calculateWeightedScore(score.rawScore, score.weightPercent)
                score.copy(weightedScore = weighted)
            }
            val totalScore = VendorQuotationCalculator.calculateTotalEvaluationScore(calculatedScores)

            val evaluated = evaluation.copy(
                scores = calculatedScores,
                totalScore = totalScore,
                evaluatorUserId = actorId,
                evaluatedAt = System.currentTimeMillis()
            )

            VendorQuotationValidator.validateEvaluation(evaluated)

            val saved = quotationRepository.saveEvaluation(evaluated)
            if (saved is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = rfq.projectId,
                        rfqId = rfq.rfqId,
                        vendorId = quotation.vendorId,
                        quotationId = quotation.quotationId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.EVALUATION_CREATED,
                        action = "RECORD_EVALUATION",
                        details = "Recorded evaluation for quotation '${quotation.quotationNumber}' (Score: $totalScore, Decision: ${evaluated.decision})"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun approveEvaluation(
        evaluationId: String,
        rfqId: String,
        tenantId: String,
        approverId: String
    ): DomainResult<VendorRfqEvaluation> {
        return try {
            val evaluations = (quotationRepository.listEvaluationsByRfq(rfqId, tenantId) as? DomainResult.Success)?.data ?: emptyList()
            val evaluation = evaluations.firstOrNull { it.evaluationId == evaluationId }
                ?: return DomainResult.Error(NoSuchElementException("Evaluation '$evaluationId' not found."))

            // Separation of Duties: Evaluator cannot approve own evaluation
            require(approverId != evaluation.evaluatorUserId) {
                "Separation of Duties violation: Evaluator '$approverId' cannot approve their own evaluation scorecard."
            }

            val approved = evaluation.copy(
                approvedBy = approverId,
                approvedAt = System.currentTimeMillis(),
                version = evaluation.version + 1
            )

            val saved = quotationRepository.saveEvaluation(approved)
            if (saved is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = evaluation.projectId,
                        rfqId = evaluation.rfqId,
                        vendorId = evaluation.vendorId,
                        quotationId = evaluation.quotationId,
                        actorUserId = approverId,
                        eventType = VendorRfqAuditEventType.EVALUATION_APPROVED,
                        action = "APPROVE_EVALUATION",
                        details = "Approved evaluation scorecard for quotation '${evaluation.quotationId}'"
                    )
                )
            }
            saved
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun awardRfq(
        rfqId: String,
        winningQuotationId: String,
        awardReason: String,
        tenantId: String,
        awardedBy: String
    ): DomainResult<VendorRfq> {
        return try {
            val rfq = when (val res = rfqRepository.findRfqById(rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val quotation = when (val res = quotationRepository.findQuotationById(winningQuotationId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorQuotationValidator.validateAwardDecision(rfq, quotation, awardedBy, awardReason)

            val awardDecision = VendorRfqAwardDecision(
                awardId = UUID.randomUUID().toString(),
                rfqId = rfqId,
                winningVendorId = quotation.vendorId,
                winningQuotationId = winningQuotationId,
                awardReason = awardReason,
                awardedAmount = quotation.grandTotal,
                awardedBy = awardedBy,
                awardedAt = System.currentTimeMillis()
            )

            // Update RFQ to AWARDED
            val awardedRfq = rfq.copy(
                status = VendorRfqStatus.AWARDED,
                awardDecision = awardDecision,
                updatedBy = awardedBy,
                updatedAt = System.currentTimeMillis(),
                version = rfq.version + 1
            )
            val updatedRfq = rfqRepository.updateRfq(awardedRfq)

            // Mark winning quotation as ACCEPTED and other quotations as REJECTED
            if (updatedRfq is DomainResult.Success) {
                quotationRepository.updateQuotation(
                    quotation.copy(
                        status = VendorQuotationStatus.ACCEPTED,
                        updatedBy = awardedBy,
                        updatedAt = System.currentTimeMillis(),
                        version = quotation.version + 1
                    )
                )

                val allQuotes = (quotationRepository.listQuotationsByRfq(rfqId, tenantId) as? DomainResult.Success)?.data ?: emptyList()
                for (other in allQuotes) {
                    if (other.quotationId != winningQuotationId && other.status.isSubmittedOrActive) {
                        quotationRepository.updateQuotation(
                            other.copy(
                                status = VendorQuotationStatus.REJECTED,
                                notes = "${other.notes ?: ""}\n[Rejected: RFQ awarded to vendor ${quotation.vendorId}]".trim(),
                                updatedBy = awardedBy,
                                updatedAt = System.currentTimeMillis(),
                                version = other.version + 1
                            )
                        )
                    }
                }

                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = rfq.projectId,
                        rfqId = rfq.rfqId,
                        vendorId = quotation.vendorId,
                        quotationId = winningQuotationId,
                        actorUserId = awardedBy,
                        eventType = VendorRfqAuditEventType.RFQ_AWARDED,
                        action = "AWARD_RFQ",
                        details = "Awarded RFQ '${rfq.rfqNumber}' to vendor '${quotation.vendorId}' with quote '${quotation.quotationNumber}' (Amount: ${quotation.grandTotal.amount}): $awardReason"
                    )
                )
            }
            updatedRfq
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listEvaluations(rfqId: String, tenantId: String): DomainResult<List<VendorRfqEvaluation>> =
        quotationRepository.listEvaluationsByRfq(rfqId, tenantId)
}
