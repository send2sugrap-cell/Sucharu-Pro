package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.AdjustmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentRepositoryTest {

    private lateinit var dataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var repository: BusinessFinancialAdjustmentRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    @Before
    fun setup() {
        dataSource = FakeBusinessFinancialAdjustmentDataSource()
        repository = BusinessFinancialAdjustmentRepositoryImpl(dataSource)
    }

    @Test
    fun testSaveAndFindAdjustment() = runBlocking {
        val adj = BusinessFinancialAdjustment(
            id = "ADJ-001",
            tenantId = tenantId,
            projectId = projectId,
            adjustmentNumber = "ADJ-2026-0001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            originalAmount = BigDecimal("10000.0000"),
            adjustmentAmount = BigDecimal("-1000.0000"),
            effectiveAmount = BigDecimal("9000.0000"),
            currency = "BDT",
            reason = "Discount correction",
            justification = "Approved by manager with vendor acknowledgement",
            periodId = "PER-2026-08",
            createdBy = "USR-001"
        )

        repository.saveAdjustment(adj)

        val found = repository.findAdjustmentById("ADJ-001", tenantId, projectId)
        assertNotNull(found)
        assertEquals("ADJ-2026-0001", found?.adjustmentNumber)
        assertEquals(BigDecimal("9000.0000"), found?.effectiveAmount)

        val foundByNum = repository.findAdjustmentByNumber("ADJ-2026-0001", tenantId, projectId)
        assertNotNull(foundByNum)
        assertEquals("ADJ-001", foundByNum?.id)
    }

    @Test
    fun testSaveAndListRefundsAndWriteOffs() = runBlocking {
        val ref = BusinessFinancialRefund(
            id = "REF-001",
            tenantId = tenantId,
            projectId = projectId,
            refundNumber = "REF-2026-0001",
            sourceType = AdjustmentSourceType.CUSTOMER_INVOICE,
            sourceId = "INV-101",
            customerId = "CUST-001",
            eligibleBalance = BigDecimal("5000.0000"),
            requestedAmount = BigDecimal("2000.0000"),
            refundReason = "Overpayment refund to customer bank account",
            periodId = "PER-2026-08",
            requestedBy = "USR-001"
        )
        repository.saveRefund(ref)

        val refunds = repository.listRefunds(tenantId, projectId)
        assertEquals(1, refunds.size)
        assertEquals("REF-001", refunds[0].id)

        val wo = BusinessFinancialWriteOff(
            id = "WO-001",
            tenantId = tenantId,
            projectId = projectId,
            writeOffNumber = "WO-2026-0001",
            sourceType = AdjustmentSourceType.CUSTOMER_INVOICE,
            sourceId = "INV-102",
            writeOffType = BusinessFinancialWriteOffType.BAD_DEBT,
            eligibleBalance = BigDecimal("3000.0000"),
            amount = BigDecimal("3000.0000"),
            reason = "Customer company liquidation",
            justification = "Legal bankruptcy order received, uncollectible balance write-off",
            periodId = "PER-2026-08",
            requestedBy = "USR-001"
        )
        repository.saveWriteOff(wo)

        val writeOffs = repository.listWriteOffs(tenantId, projectId)
        assertEquals(1, writeOffs.size)
        assertEquals(BusinessFinancialWriteOffType.BAD_DEBT, writeOffs[0].writeOffType)
    }

    @Test
    fun testAuditEventsRecording() = runBlocking {
        val auditEvent = BusinessFinancialAdjustmentAuditEvent(
            id = "AUD-001",
            tenantId = tenantId,
            projectId = projectId,
            entityType = "ADJUSTMENT",
            entityId = "ADJ-001",
            eventType = "APPROVED",
            actorId = "USR-MGR",
            actorRole = "MANAGER",
            previousStatus = "SUBMITTED",
            newStatus = "APPROVED",
            reason = "Approved"
        )
        repository.recordAuditEvent(auditEvent)

        val events = repository.listAuditEvents(tenantId, projectId, entityId = "ADJ-001")
        assertEquals(1, events.size)
        assertEquals("APPROVED", events[0].eventType)
    }
}
