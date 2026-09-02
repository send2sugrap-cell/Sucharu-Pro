package com.sucharu.sucharupro.domain.service.returns

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionType
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import java.util.UUID

/**
 * Pure, rule-based governance inspection engine for detecting SLA violations, anomalies, and policy exceptions (Module 11 Step 06).
 */
object ReturnGovernanceInspector {

    /**
     * Inspects project returns and settlements against governance policies and SLA limits.
     */
    fun inspect(
        projectId: String,
        returns: List<ReturnRequest>,
        settlements: List<ReturnSettlement>,
        totalDispatchedCount: Int? = null,
        maxUninspectedHours: Long = 48L,
        maxUnreceivedHours: Long = 72L,
        maxUnsettledHours: Long = 48L,
        highValueThreshold: Money = Money(50000.0),
        maxSlaTurnaroundDays: Double = 7.0,
        highReturnRateThreshold: Double = 10.0,
        nowMillis: Long = System.currentTimeMillis()
    ): List<ReturnException> {
        val projectReturns = returns.filter { it.projectId == projectId }
        val projectSettlements = settlements.filter { it.projectId == projectId }
        val settlementMap = projectSettlements.associateBy { it.returnId }

        val exceptions = mutableListOf<ReturnException>()
        val oneHourMillis = 3_600_000L
        val oneDayMillis = 86_400_000.0

        for (ret in projectReturns) {
            // 1. Aging Uninspected: in UNDER_INSPECTION or REQUESTED for too long
            if (ret.status == ReturnStatus.UNDER_INSPECTION || ret.status == ReturnStatus.REQUESTED) {
                val ageHours = (nowMillis - ret.createdAt) / oneHourMillis
                if (ageHours > maxUninspectedHours) {
                    val idempKey = "$projectId:${ret.returnId}:AGING_UNINSPECTED"
                    exceptions.add(
                        ReturnException(
                            exceptionId = UUID.nameUUIDFromBytes(idempKey.toByteArray()).toString(),
                            projectId = projectId,
                            returnId = ret.returnId,
                            exceptionType = ReturnExceptionType.AGING_UNINSPECTED,
                            severity = "HIGH",
                            status = ReturnExceptionStatus.OPEN,
                            detectedAt = nowMillis,
                            thresholdValue = maxUninspectedHours.toDouble(),
                            actualValue = ageHours.toDouble(),
                            description = "Return ${ret.returnNo} has been pending inspection for $ageHours hours (SLA limit: $maxUninspectedHours hrs).",
                            idempotencyKey = idempKey
                        )
                    )
                }
            }

            // 2. Aging Unreceived: in APPROVED status waiting for warehouse stock receipt
            if (ret.status == ReturnStatus.APPROVED) {
                val ageHours = (nowMillis - ret.updatedAt) / oneHourMillis
                if (ageHours > maxUnreceivedHours) {
                    val idempKey = "$projectId:${ret.returnId}:AGING_UNRECEIVED"
                    exceptions.add(
                        ReturnException(
                            exceptionId = UUID.nameUUIDFromBytes(idempKey.toByteArray()).toString(),
                            projectId = projectId,
                            returnId = ret.returnId,
                            exceptionType = ReturnExceptionType.AGING_UNRECEIVED,
                            severity = "MEDIUM",
                            status = ReturnExceptionStatus.OPEN,
                            detectedAt = nowMillis,
                            thresholdValue = maxUnreceivedHours.toDouble(),
                            actualValue = ageHours.toDouble(),
                            description = "Approved Return ${ret.returnNo} has not been received by warehouse for $ageHours hours (SLA limit: $maxUnreceivedHours hrs).",
                            idempotencyKey = idempKey
                        )
                    )
                }
            }

            // 3. Unsettled Processed: in PROCESSED status without completed settlement
            if (ret.status == ReturnStatus.PROCESSED) {
                val settlement = settlementMap[ret.returnId]
                if (settlement == null || settlement.status != ReturnSettlementStatus.COMPLETED) {
                    val ageHours = (nowMillis - ret.updatedAt) / oneHourMillis
                    if (ageHours > maxUnsettledHours) {
                        val idempKey = "$projectId:${ret.returnId}:UNSETTLED_PROCESSED"
                        exceptions.add(
                            ReturnException(
                                exceptionId = UUID.nameUUIDFromBytes(idempKey.toByteArray()).toString(),
                                projectId = projectId,
                                returnId = ret.returnId,
                                exceptionType = ReturnExceptionType.UNSETTLED_PROCESSED,
                                severity = "HIGH",
                                status = ReturnExceptionStatus.OPEN,
                                detectedAt = nowMillis,
                                thresholdValue = maxUnsettledHours.toDouble(),
                                actualValue = ageHours.toDouble(),
                                description = "Return ${ret.returnNo} is processed but remains unsettled for $ageHours hours (SLA limit: $maxUnsettledHours hrs).",
                                idempotencyKey = idempKey
                            )
                        )
                    }
                }
            }

            // 4. High-Value Return Alert
            val settlement = settlementMap[ret.returnId]
            if (settlement != null && settlement.amount > highValueThreshold) {
                val idempKey = "$projectId:${ret.returnId}:HIGH_VALUE_RETURN"
                exceptions.add(
                    ReturnException(
                        exceptionId = UUID.nameUUIDFromBytes(idempKey.toByteArray()).toString(),
                        projectId = projectId,
                        returnId = ret.returnId,
                        exceptionType = ReturnExceptionType.HIGH_VALUE_RETURN,
                        severity = "CRITICAL",
                        status = ReturnExceptionStatus.OPEN,
                        detectedAt = nowMillis,
                        thresholdValue = highValueThreshold.amount.toDouble(),
                        actualValue = settlement.amount.amount.toDouble(),
                        description = "Return ${ret.returnNo} settled with high financial impact of ${settlement.amount.formatted()} (Threshold: ${highValueThreshold.formatted()}).",
                        idempotencyKey = idempKey
                    )
                )
            }

            // 5. SLA Turnaround Breach
            val finalTime = settlement?.settledAt ?: if (ret.status.isTerminal) ret.updatedAt else 0L
            if (finalTime > ret.createdAt) {
                val durationDays = (finalTime - ret.createdAt) / oneDayMillis
                if (durationDays > maxSlaTurnaroundDays) {
                    val idempKey = "$projectId:${ret.returnId}:SLA_BREACH"
                    exceptions.add(
                        ReturnException(
                            exceptionId = UUID.nameUUIDFromBytes(idempKey.toByteArray()).toString(),
                            projectId = projectId,
                            returnId = ret.returnId,
                            exceptionType = ReturnExceptionType.SLA_BREACH,
                            severity = "HIGH",
                            status = ReturnExceptionStatus.OPEN,
                            detectedAt = nowMillis,
                            thresholdValue = maxSlaTurnaroundDays,
                            actualValue = durationDays,
                            description = "Return ${ret.returnNo} total turnaround exceeded SLA: ${String.format(java.util.Locale.US, "%.1f", durationDays)} days (Limit: $maxSlaTurnaroundDays days).",
                            idempotencyKey = idempKey
                        )
                    )
                }
            }
        }

        // 6. High Return Rate Alert (Project-level anomaly)
        if (totalDispatchedCount != null && totalDispatchedCount > 0) {
            val returnRate = (projectReturns.size.toDouble() / totalDispatchedCount.toDouble()) * 100.0
            if (returnRate > highReturnRateThreshold) {
                val idempKey = "$projectId:PROJECT:HIGH_RETURN_RATE"
                exceptions.add(
                    ReturnException(
                        exceptionId = UUID.nameUUIDFromBytes(idempKey.toByteArray()).toString(),
                        projectId = projectId,
                        returnId = null,
                        exceptionType = ReturnExceptionType.HIGH_RETURN_RATE,
                        severity = "CRITICAL",
                        status = ReturnExceptionStatus.OPEN,
                        detectedAt = nowMillis,
                        thresholdValue = highReturnRateThreshold,
                        actualValue = returnRate,
                        description = "Project $projectId return rate is ${String.format(java.util.Locale.US, "%.2f", returnRate)}%, exceeding policy limit of $highReturnRateThreshold%.",
                        idempotencyKey = idempKey
                    )
                )
            }
        }

        return exceptions
    }
}
