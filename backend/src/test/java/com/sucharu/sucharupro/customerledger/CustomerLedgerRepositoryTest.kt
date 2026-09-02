package com.sucharu.sucharupro.customerledger

import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation
import com.sucharu.sucharupro.domain.model.customerledger.ReceivableReconciliationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerLedgerRepositoryTest {

    private lateinit var repository: CustomerLedgerRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-REC-001"

    @Before
    fun setup() {
        val dataSource = FakeCustomerLedgerDataSource()
        repository = CustomerLedgerRepositoryImpl(dataSource)
    }

    @Test
    fun testSaveAndRetrieveReconciliation() = runBlocking {
        val rec = CustomerReceivableReconciliation(
            reconciliationId = "REC-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = "CFA-001",
            status = ReceivableReconciliationStatus.CONSISTENT,
            invoiceTotalReceivable = BigDecimal("5000.0000"),
            ledgerCalculatedBalance = BigDecimal("5000.0000"),
            availableCreditBalance = BigDecimal.ZERO,
            difference = BigDecimal.ZERO,
            isConsistent = true,
            discrepancyCount = 0
        )

        val saveRes = repository.saveReconciliation(rec)
        assertTrue(saveRes is DomainResult.Success)

        val getRes = repository.getReconciliationById(tenantId, projectId, "REC-001")
        assertTrue(getRes is DomainResult.Success)
        val retrieved = (getRes as DomainResult.Success).data
        assertEquals("REC-001", retrieved.reconciliationId)
        assertEquals(ReceivableReconciliationStatus.CONSISTENT, retrieved.status)

        val listRes = repository.listReconciliations(tenantId, projectId, customerId)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }
}
