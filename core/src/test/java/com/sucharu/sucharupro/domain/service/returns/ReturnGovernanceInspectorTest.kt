package com.sucharu.sucharupro.domain.service.returns

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionType
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ReturnGovernanceInspector] exception detection engine (Module 11 Step 06 Chunk 02).
 */
class ReturnGovernanceInspectorTest {

    private val projectId = "PRJ-INSPECT-01"
    private val nowMillis = 1_700_000_000_000L
    private val oneHourMillis = 3_600_000L

    @Test
    fun `test aging uninspected exception detection`() {
        val returnOld = ReturnRequest(
            returnId = "RET-OLD-01",
            projectId = projectId,
            returnNo = "RN-OLD-01",
            customerId = "C-01",
            originalChallanId = "CH-01",
            status = ReturnStatus.UNDER_INSPECTION,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-01",
            createdAt = nowMillis - (50 * oneHourMillis) // 50h old (limit 48h)
        )

        val exceptions = ReturnGovernanceInspector.inspect(
            projectId = projectId,
            returns = listOf(returnOld),
            settlements = emptyList(),
            nowMillis = nowMillis
        )

        val aging = exceptions.find { it.exceptionType == ReturnExceptionType.AGING_UNINSPECTED }
        assertNotNull(aging)
        assertEquals("RET-OLD-01", aging!!.returnId)
        assertEquals("HIGH", aging.severity)
    }

    @Test
    fun `test unsettled processed exception detection`() {
        val processedUnsettled = ReturnRequest(
            returnId = "RET-PROC-01",
            projectId = projectId,
            returnNo = "RN-PROC-01",
            customerId = "C-01",
            originalChallanId = "CH-01",
            status = ReturnStatus.PROCESSED,
            reason = ReturnReason.DAMAGED,
            requestedBy = "USER-01",
            createdAt = nowMillis - (60 * oneHourMillis),
            updatedAt = nowMillis - (50 * oneHourMillis) // 50h since processed
        )

        val exceptions = ReturnGovernanceInspector.inspect(
            projectId = projectId,
            returns = listOf(processedUnsettled),
            settlements = emptyList(),
            nowMillis = nowMillis
        )

        val unsettled = exceptions.find { it.exceptionType == ReturnExceptionType.UNSETTLED_PROCESSED }
        assertNotNull(unsettled)
        assertEquals("RET-PROC-01", unsettled!!.returnId)
    }

    @Test
    fun `test high value return exception detection`() {
        val ret = ReturnRequest(
            returnId = "RET-HV-01",
            projectId = projectId,
            returnNo = "RN-HV-01",
            customerId = "C-01",
            originalChallanId = "CH-01",
            status = ReturnStatus.PROCESSED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-01",
            createdAt = nowMillis
        )
        val settlement = ReturnSettlement(
            settlementId = "SET-HV-01",
            returnId = "RET-HV-01",
            projectId = projectId,
            customerId = "C-01",
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            amount = Money(85000.0), // Exceeds default 50k
            status = ReturnSettlementStatus.COMPLETED,
            settledBy = "ACCOUNTS-01",
            idempotencyKey = "K-HV-01"
        )

        val exceptions = ReturnGovernanceInspector.inspect(
            projectId = projectId,
            returns = listOf(ret),
            settlements = listOf(settlement),
            nowMillis = nowMillis
        )

        val highVal = exceptions.find { it.exceptionType == ReturnExceptionType.HIGH_VALUE_RETURN }
        assertNotNull(highVal)
        assertEquals("RET-HV-01", highVal!!.returnId)
        assertEquals("CRITICAL", highVal.severity)
    }

    @Test
    fun `test high return rate anomaly detection`() {
        val returns = (1..15).map { i ->
            ReturnRequest(
                returnId = "RET-$i",
                projectId = projectId,
                returnNo = "RN-$i",
                customerId = "C-01",
                originalChallanId = "CH-01",
                status = ReturnStatus.PROCESSED,
                reason = ReturnReason.PRINTING_DEFECT,
                requestedBy = "USER-01",
                createdAt = nowMillis
            )
        }

        // 15 returns out of 100 dispatches = 15.0% (> 10% limit)
        val exceptions = ReturnGovernanceInspector.inspect(
            projectId = projectId,
            returns = returns,
            settlements = emptyList(),
            totalDispatchedCount = 100,
            nowMillis = nowMillis
        )

        val rateAlert = exceptions.find { it.exceptionType == ReturnExceptionType.HIGH_RETURN_RATE }
        assertNotNull(rateAlert)
        assertEquals(15.0, rateAlert!!.actualValue, 0.001)
    }
}
