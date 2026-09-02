package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityType
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerReceivableRepository
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableEndToEndTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var deliveryOrderDataSource: FakeDeliveryOrderDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var customerReceivableRepository: CustomerReceivableRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()
        deliveryOrderDataSource = FakeDeliveryOrderDataSource()
        financeTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financeTransactionDataSource)
        customerReceivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
    }

    @Test
    fun `full end to end customer receivable lifecycle and due management flow`() = runBlocking {
        val projectId = "PRJ-E2E-REC"
        val customerId = "CUST-E2E-001"
        val invoiceRef = "INV-E2E-999"

        // 1. Upstream Delivery Order fixture (Module 08)
        val deliveryOrder = DeliveryOrder(
            deliveryOrderId = "DO-E2E-REC-01",
            projectId = projectId,
            deliveryOrderNo = "DON-2026-888",
            customerId = customerId,
            sourceReferenceId = "SO-E2E-888",
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
        deliveryOrderDataSource.insertDeliveryOrder(deliveryOrder, listOf(DeliveryOrderLine("DOL-1", "DO-E2E-REC-01", projectId, "PROD-A", 1000.0, null)))

        // Initial inventory baseline
        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialInventoryLedger = inventoryLedgerDataSource.getEntries(projectId).size

        // 2. Step 01 Financial Recognition (Transaction & Ledger)
        val txnRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("120000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = invoiceRef,
            customerId = customerId,
            description = "Commercial invoice financial entry",
            notes = "Terms net 30",
            actorId = "staff-operator-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(txnRes is DomainResult.Success)
        val txnId = (txnRes as DomainResult.Success).data.transactionId
        financialTransactionRepository.submitTransaction(txnId, "staff-operator-1", UserRole.STAFF)
        financialTransactionRepository.postTransaction(txnId, "ACCOUNTS_RECEIVABLE", "acct-manager-1", UserRole.ACCOUNTS)

        // 3. Step 02 Create Customer Receivable obligation
        val dueDate = System.currentTimeMillis() + 86400000L
        val recRes = customerReceivableRepository.createReceivable(
            projectId = projectId,
            customerId = customerId,
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = invoiceRef,
            financialTransactionId = txnId,
            originalAmount = Money(BigDecimal("120000.00")),
            currency = "BDT",
            dueDate = dueDate,
            description = "Receivable obligation for commercial invoice",
            notes = "Net 30 days due",
            actorId = "acct-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(recRes is DomainResult.Success)
        val rec = (recRes as DomainResult.Success).data
        val recId = rec.receivableId
        assertEquals(CustomerReceivableStatus.OPEN, rec.status)
        assertEquals(Money(BigDecimal("120000.00")), rec.outstandingAmount)
        assertEquals(ReceivableAgingBucket.CURRENT, rec.agingBucket)

        // 4. Update details
        val updateRes = customerReceivableRepository.updateReceivable(
            receivableId = recId,
            description = "Receivable obligation for commercial invoice (Net 30 with 2% discount if paid in 10 days)",
            actorId = "acct-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(updateRes is DomainResult.Success)

        // 5. Query Customer Due Summary
        val summary1Res = customerReceivableRepository.getCustomerDueSummary(projectId, customerId, UserRole.ACCOUNTS)
        assertTrue(summary1Res is DomainResult.Success)
        val summary1 = (summary1Res as DomainResult.Success).data
        assertEquals(1, summary1.totalReceivablesCount)
        assertEquals(1, summary1.openReceivablesCount)
        assertEquals(Money(BigDecimal("120000.00")), summary1.totalOutstandingDue)

        // 6. Record Partial Settlement (৳40,000)
        val partialSettleRes = customerReceivableRepository.recordSettlement(
            receivableId = recId,
            settlementAmount = Money(BigDecimal("40000.00")),
            actorId = "acct-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(partialSettleRes is DomainResult.Success)
        val partialRec = (partialSettleRes as DomainResult.Success).data
        assertEquals(CustomerReceivableStatus.PARTIALLY_SETTLED, partialRec.status)
        assertEquals(Money(BigDecimal("40000.00")), partialRec.settledAmount)
        assertEquals(Money(BigDecimal("80000.00")), partialRec.outstandingAmount)

        // 7. Record Final Settlement (৳80,000)
        val finalSettleRes = customerReceivableRepository.recordSettlement(
            receivableId = recId,
            settlementAmount = Money(BigDecimal("80000.00")),
            actorId = "acct-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(finalSettleRes is DomainResult.Success)
        val finalRec = (finalSettleRes as DomainResult.Success).data
        assertEquals(CustomerReceivableStatus.SETTLED, finalRec.status)
        assertEquals(Money(BigDecimal("120000.00")), finalRec.settledAmount)
        assertEquals(Money.ZERO, finalRec.outstandingAmount)
        assertTrue(finalRec.settledAt != null)

        // 8. Verify Updated Summary
        val summary2Res = customerReceivableRepository.getCustomerDueSummary(projectId, customerId, UserRole.ACCOUNTS)
        assertTrue(summary2Res is DomainResult.Success)
        val summary2 = (summary2Res as DomainResult.Success).data
        assertEquals(Money.ZERO, summary2.totalOutstandingDue)
        assertEquals(Money(BigDecimal("120000.00")), summary2.totalSettledAmount)

        // 9. Verify Audit Trail
        val eventsRes = customerReceivableRepository.getActivityEvents(recId, UserRole.ACCOUNTS)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data
        assertEquals(4, events.size)
        assertEquals(CustomerReceivableActivityType.RECEIVABLE_CREATED, events[0].activityType)
        assertEquals(CustomerReceivableActivityType.RECEIVABLE_UPDATED, events[1].activityType)
        assertEquals(CustomerReceivableActivityType.RECEIVABLE_SETTLEMENT_RECORDED, events[2].activityType)
        assertEquals(CustomerReceivableActivityType.RECEIVABLE_SETTLEMENT_RECORDED, events[3].activityType)

        // 10. Verify Zero Inventory Mutation
        assertEquals(initialStockOuts, stockOutDataSource.observeStockOutRecords().first().size)
        assertEquals(initialStockIns, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(initialInventoryLedger, inventoryLedgerDataSource.getEntries(projectId).size)

        // 11. Verify Upstream Delivery Order unaffected
        val retrievedDO = deliveryOrderDataSource.getDeliveryOrder("DO-E2E-REC-01")
        assertEquals(DeliveryOrderStatus.DELIVERED, retrievedDO?.status)
    }
}
