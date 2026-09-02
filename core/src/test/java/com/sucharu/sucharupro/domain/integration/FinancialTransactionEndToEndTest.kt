package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.finance.FinancialActivityType
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialTransactionEndToEndTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var deliveryOrderDataSource: FakeDeliveryOrderDataSource
    private lateinit var financeDataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()
        deliveryOrderDataSource = FakeDeliveryOrderDataSource()
        financeDataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(financeDataSource)
    }

    @Test
    fun `full end to end financial transaction and ledger workflow`() = runBlocking {
        val projectId = "PRJ-E2E-09"

        // 1. Upstream Delivery Order fixture (Module 08)
        val deliveryOrder = DeliveryOrder(
            deliveryOrderId = "DO-E2E-FIN",
            projectId = projectId,
            deliveryOrderNo = "DON-2026-999",
            customerId = "CUST-E2E-01",
            sourceReferenceId = "SO-E2E-01",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.HIGH,
            status = DeliveryOrderStatus.DELIVERED,
            requestedDeliveryDate = 2000L,
            notes = "Delivered successfully",
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val dLine = DeliveryOrderLine("DOL-E2E-1", "DO-E2E-FIN", projectId, "PROD-A", 500.0, null)
        deliveryOrderDataSource.insertDeliveryOrder(deliveryOrder, listOf(dLine))

        // Initial inventory baseline
        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialInventoryLedger = inventoryLedgerDataSource.getEntries(projectId).size

        // 2. Create financial transaction linked to Delivery Order
        val createRes = repository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("75000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.DELIVERY,
            referenceId = "DO-E2E-FIN",
            customerId = "CUST-E2E-01",
            description = "Delivered goods financial recognition",
            notes = "Terms net 30",
            actorId = "staff-operator-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(createRes is DomainResult.Success)
        val draftTxn = (createRes as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.DRAFT, draftTxn.transactionStatus)

        // 3. Update Draft transaction details
        val updateRes = repository.updateDraftTransaction(
            transactionId = draftTxn.transactionId,
            description = "Delivered goods financial recognition (Commercial invoice)",
            notes = "Net 30 with 2% early payment discount",
            actorId = "staff-operator-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(updateRes is DomainResult.Success)

        // 4. Submit transaction for approval
        val submitRes = repository.submitTransaction(
            transactionId = draftTxn.transactionId,
            actorId = "staff-operator-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(submitRes is DomainResult.Success)
        val pendingTxn = (submitRes as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.PENDING, pendingTxn.transactionStatus)

        // 5. Post transaction to ledger (Accounts user)
        val postRes = repository.postTransaction(
            transactionId = draftTxn.transactionId,
            accountHead = "ACCOUNTS_RECEIVABLE",
            actorId = "accounts-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(postRes is DomainResult.Success)
        val postedTxn = (postRes as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.POSTED, postedTxn.transactionStatus)

        // 6. Verify ledger entries
        val ledgerRes = repository.getLedgerEntriesByTransaction(draftTxn.transactionId, UserRole.ACCOUNTS)
        assertTrue(ledgerRes is DomainResult.Success)
        val ledgerEntries = (ledgerRes as DomainResult.Success).data
        assertEquals(1, ledgerEntries.size)
        assertEquals("ACCOUNTS_RECEIVABLE", ledgerEntries[0].accountHead)
        assertEquals(Money(BigDecimal("75000.00")), ledgerEntries[0].amount)

        // 7. Verify audit events
        val auditRes = repository.getActivityEvents(draftTxn.transactionId, UserRole.ACCOUNTS)
        assertTrue(auditRes is DomainResult.Success)
        val events = (auditRes as DomainResult.Success).data
        assertEquals(5, events.size)
        assertEquals(FinancialActivityType.TRANSACTION_CREATED, events[0].activityType)
        assertEquals(FinancialActivityType.TRANSACTION_UPDATED, events[1].activityType)
        assertEquals(FinancialActivityType.TRANSACTION_SUBMITTED, events[2].activityType)
        assertEquals(FinancialActivityType.TRANSACTION_POSTED, events[3].activityType)
        assertEquals(FinancialActivityType.LEDGER_ENTRY_POSTED, events[4].activityType)

        // 8. Verify Inventory boundary: zero inventory mutations
        assertEquals(initialStockOuts, stockOutDataSource.observeStockOutRecords().first().size)
        assertEquals(initialStockIns, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(initialInventoryLedger, inventoryLedgerDataSource.getEntries(projectId).size)

        // 9. Verify Delivery Order state remains untouched
        val retrievedDO = deliveryOrderDataSource.getDeliveryOrder("DO-E2E-FIN")
        assertEquals(DeliveryOrderStatus.DELIVERED, retrievedDO?.status)
    }
}
