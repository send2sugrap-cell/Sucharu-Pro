package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.data.datasource.businessreconciliation.DiscrepancyFilter
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.businessreconciliation.ReconciliationRunFilter
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReconciliationRepositoryTest {

    private lateinit var dataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var repository: BusinessFinancialReconciliationRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    @Before
    fun setup() {
        dataSource = FakeBusinessFinancialReconciliationDataSource()
        repository = BusinessFinancialReconciliationRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndFindRun() = runBlocking {
        val run = BusinessFinancialReconciliationRun(
            id = "RUN-100",
            tenantId = tenantId,
            projectId = projectId,
            periodId = "PER-2026-08",
            runNumber = "REC-100",
            runType = ReconciliationRunType.FULL_PERIOD,
            createdBy = "USR-01",
            checksum = "test-checksum"
        )
        val created = repository.createRun(run)
        assertEquals("RUN-100", created.id)

        val byId = repository.findRunById("RUN-100", tenantId, projectId)
        assertNotNull(byId)
        assertEquals("REC-100", byId?.runNumber)

        val byNumber = repository.findRunByNumber("REC-100", tenantId, projectId)
        assertNotNull(byNumber)
    }

    @Test
    fun testCreateAndListDiscrepancies() = runBlocking {
        val run = BusinessFinancialReconciliationRun(
            id = "RUN-101",
            tenantId = tenantId,
            projectId = projectId,
            periodId = "PER-2026-08",
            runNumber = "REC-101",
            createdBy = "USR-01",
            checksum = "test-checksum"
        )
        repository.createRun(run)

        val disc = BusinessFinancialReconciliationDiscrepancy(
            id = "DISC-100",
            tenantId = tenantId,
            projectId = projectId,
            reconciliationRunId = "RUN-101",
            periodId = "PER-2026-08",
            discrepancyType = FinancialDiscrepancyType.AMOUNT_MISMATCH,
            severity = DiscrepancySeverity.CRITICAL,
            sourceType = "BUSINESS_EXPENSE",
            sourceId = "EXP-101",
            expectedAmount = BigDecimal("5000.0000"),
            actualAmount = BigDecimal("4500.0000"),
            differenceAmount = BigDecimal("500.0000"),
            description = "Expense amount mismatch"
        )
        repository.createDiscrepancy(disc)

        val list = repository.listDiscrepancies(tenantId, projectId, DiscrepancyFilter(reconciliationRunId = "RUN-101"))
        assertEquals(1, list.size)
        assertEquals("DISC-100", list[0].id)
        assertEquals(DiscrepancySeverity.CRITICAL, list[0].severity)
    }

    @Test
    fun testSaveAndFindSnapshot() = runBlocking {
        val snapshot = BusinessFinancialReconciliationSnapshot(
            id = "SNP-100",
            tenantId = tenantId,
            projectId = projectId,
            reconciliationRunId = "RUN-100",
            periodId = "PER-2026-08",
            snapshotData = "checked=10, matched=10",
            checksum = "sha-256-hash"
        )
        repository.saveSnapshot(snapshot)

        val found = repository.findSnapshotByRunId("RUN-100", tenantId, projectId)
        assertNotNull(found)
        assertEquals("sha-256-hash", found?.checksum)
    }

    @Test
    fun testRecordAndListAuditEvents() = runBlocking {
        val event = BusinessFinancialReconciliationAuditEvent(
            id = "EVT-100",
            tenantId = tenantId,
            projectId = projectId,
            reconciliationRunId = "RUN-100",
            eventType = "RUN_CREATED",
            actorId = "USR-01",
            actorRole = "ADMIN",
            reason = "Test audit event"
        )
        repository.recordAuditEvent(event)

        val list = repository.listAuditEvents(tenantId, projectId, runId = "RUN-100")
        assertEquals(1, list.size)
        assertEquals("RUN_CREATED", list[0].eventType)
    }
}
