package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-End Analytics Integration test suite for Return Management (Module 11 Step 06 Chunk 05).
 */
class ReturnAnalyticsEndToEndTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var analyticsDataSource: FakeReturnAnalyticsDataSource
    private lateinit var returnRepository: ReturnRepositoryImpl
    private lateinit var analyticsRepository: ReturnAnalyticsRepositoryImpl

    private val projectId = "PRJ-E2E-ANALYTICS"
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        analyticsDataSource = FakeReturnAnalyticsDataSource()
        returnRepository = ReturnRepositoryImpl(returnDataSource)
        analyticsRepository = ReturnAnalyticsRepositoryImpl(returnDataSource, analyticsDataSource)
    }

    @Test
    fun `test complete analytics aggregation across multi-customer multi-reason dataset`() = runBlocking {
        // Return 1: Processed & Settled via CREDIT_NOTE (Printing defect, 100 requested, 80 accepted, 20 rejected)
        val ret1 = ReturnRequest(
            returnId = "RET-E2E-1",
            projectId = projectId,
            returnNo = "RN-E2E-1",
            customerId = "CUST-A",
            originalChallanId = "CH-1",
            status = ReturnStatus.PROCESSED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-A",
            createdAt = now - 200_000L,
            updatedAt = now - 100_000L
        )
        val items1 = listOf(
            ReturnItem("RI-1", "RET-E2E-1", "PROD-1", "CI-1", 100, 80, 20, "PCS")
        )
        val set1 = ReturnSettlement(
            settlementId = "SET-E2E-1",
            returnId = "RET-E2E-1",
            projectId = projectId,
            customerId = "CUST-A",
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            amount = Money(20000.0),
            status = ReturnSettlementStatus.COMPLETED,
            settledBy = "ADMIN",
            settledAt = now - 50_000L,
            idempotencyKey = "K-SET-1"
        )

        // Return 2: Processed & Settled via REFUND (Damaged, 50 requested, 0 accepted, 50 rejected)
        val ret2 = ReturnRequest(
            returnId = "RET-E2E-2",
            projectId = projectId,
            returnNo = "RN-E2E-2",
            customerId = "CUST-B",
            originalChallanId = "CH-2",
            status = ReturnStatus.PROCESSED,
            reason = ReturnReason.DAMAGED,
            requestedBy = "USER-B",
            createdAt = now - 180_000L,
            updatedAt = now - 80_000L
        )
        val items2 = listOf(
            ReturnItem("RI-2", "RET-E2E-2", "PROD-2", "CI-2", 50, 0, 50, "PCS")
        )
        val set2 = ReturnSettlement(
            settlementId = "SET-E2E-2",
            returnId = "RET-E2E-2",
            projectId = projectId,
            customerId = "CUST-B",
            resolutionType = ReturnResolutionType.REFUND,
            amount = Money(15000.0),
            status = ReturnSettlementStatus.COMPLETED,
            settledBy = "ADMIN",
            settledAt = now - 40_000L,
            idempotencyKey = "K-SET-2"
        )

        // Return 3: Open (Under Inspection)
        val ret3 = ReturnRequest(
            returnId = "RET-E2E-3",
            projectId = projectId,
            returnNo = "RN-E2E-3",
            customerId = "CUST-C",
            originalChallanId = "CH-3",
            status = ReturnStatus.UNDER_INSPECTION,
            reason = ReturnReason.BINDING_DEFECT,
            requestedBy = "USER-C",
            createdAt = now - 50_000L
        )
        val items3 = listOf(
            ReturnItem("RI-3", "RET-E2E-3", "PROD-1", "CI-3", 30, 0, 0, "PCS")
        )

        returnDataSource.insertReturn(ret1, items1)
        returnDataSource.insertOrUpdateSettlement(set1)

        returnDataSource.insertReturn(ret2, items2)
        returnDataSource.insertOrUpdateSettlement(set2)

        returnDataSource.insertReturn(ret3, items3)

        // Execute analytics query
        val summaryRes = analyticsRepository.getAnalyticsSummary(
            projectId = projectId,
            period = ReturnAnalyticsPeriod.ALL_TIME,
            totalDispatchedCount = 60,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )

        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data

        // Total returns = 3, Dispatches = 60 -> Return rate = 5.0%
        assertEquals(3, summary.totalReturns)
        assertEquals(5.0, summary.returnRate, 0.001)
        assertEquals(1, summary.openReturns)
        assertEquals(2, summary.processedReturns)
        assertEquals(2, summary.settledReturns)
        assertEquals(180, summary.totalRequestedQuantity)
        assertEquals(80, summary.totalAcceptedQuantity)
        assertEquals(70, summary.totalRejectedQuantity)
        assertEquals(Money(35000.0), summary.totalSettledValue)

        // Defect breakdown verification
        val defectRes = analyticsRepository.getDefectBreakdown(
            projectId = projectId,
            period = ReturnAnalyticsPeriod.ALL_TIME,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(defectRes is DomainResult.Success)
        val defects = (defectRes as DomainResult.Success).data
        val printDefect = defects.find { it.reason == ReturnReason.PRINTING_DEFECT }
        assertNotNull(printDefect)
        assertEquals(1, printDefect!!.count)
        assertEquals(100, printDefect.quantity)

        // Financial breakdown verification
        val finRes = analyticsRepository.getFinancialBreakdown(
            projectId = projectId,
            period = ReturnAnalyticsPeriod.ALL_TIME,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(finRes is DomainResult.Success)
        val fins = (finRes as DomainResult.Success).data
        val creditNoteFin = fins.find { it.resolutionType == ReturnResolutionType.CREDIT_NOTE }
        assertNotNull(creditNoteFin)
        assertEquals(Money(20000.0), creditNoteFin!!.totalAmount)

        val refundFin = fins.find { it.resolutionType == ReturnResolutionType.REFUND }
        assertNotNull(refundFin)
        assertEquals(Money(15000.0), refundFin!!.totalAmount)
    }
}
