package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentActivityType
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.repository.CustomerReceivableRepository
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentEndToEndTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var deliveryOrderDataSource: FakeDeliveryOrderDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var paymentDataSource: FakeCustomerPaymentDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var receivableRepository: CustomerReceivableRepository
    private lateinit var paymentRepository: CustomerPaymentRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()
        deliveryOrderDataSource = FakeDeliveryOrderDataSource()
        financeTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        paymentDataSource = FakeCustomerPaymentDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financeTransactionDataSource)
        receivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        paymentRepository = CustomerPaymentRepositoryImpl(
            paymentDataSource,
            receivableRepository,
            financialTransactionRepository
        )
    }

    @Test
    fun `full end to end customer payment and receipt workflow from commercial order to ledger posting`() = runBlocking {
        val projectId = "PRJ-E2E-PAY"
        val customerId = "CUST-E2E-001"
        val invoiceRef = "INV-E2E-PAY-001"

        // 1. Upstream Delivery Order fixture (Module 08)
        val deliveryOrder = DeliveryOrder(
            deliveryOrderId = "DO-E2E-PAY-01",
            projectId = projectId,
            deliveryOrderNo = "DON-2026-PAY-1",
            customerId = customerId,
            sourceReferenceId = "SO-E2E-PAY-1",
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
        deliveryOrderDataSource.insertDeliveryOrder(deliveryOrder, listOf(DeliveryOrderLine("DOL-1", "DO-E2E-PAY-01", projectId, "PROD-A", 1000.0, null)))

        // Initial inventory baseline
        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialInventoryLedger = inventoryLedgerDataSource.getEntries(projectId).size

        // 2. Step 01 Financial Recognition (Transaction & Ledger)
        val txnRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("150000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = invoiceRef,
            customerId = customerId,
            description = "Commercial invoice financial debit",
            notes = "Terms net 30",
            actorId = "staff-operator-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(txnRes is DomainResult.Success)
        val txnId = (txnRes as DomainResult.Success).data.transactionId
        financialTransactionRepository.submitTransaction(txnId, "staff-operator-1", UserRole.STAFF)
        financialTransactionRepository.postTransaction(txnId, "ACCOUNTS_RECEIVABLE", "acct-manager-1", UserRole.ACCOUNTS)

        // 3. Step 02 Customer Receivable creation
        val recRes = receivableRepository.createReceivable(
            projectId = projectId,
            customerId = customerId,
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = invoiceRef,
            financialTransactionId = txnId,
            originalAmount = Money(BigDecimal("150000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Commercial invoice obligation",
            notes = "Terms net 30",
            actorId = "acct-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(recRes is DomainResult.Success)
        val receivable = (recRes as DomainResult.Success).data
        val recId = receivable.receivableId

        // 4. Step 03 Create and Post Partial Payment (৳50,000 via Bank)
        val p1Res = paymentRepository.createPayment(
            projectId = projectId,
            customerId = customerId,
            receivableId = recId,
            amount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-E2E-001",
            notes = "First instalment",
            actorId = "staff-operator-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(p1Res is DomainResult.Success)
        val p1Id = (p1Res as DomainResult.Success).data.paymentId

        paymentRepository.submitPayment(p1Id, "staff-operator-1", UserRole.STAFF)
        val postP1Res = paymentRepository.postPayment(p1Id, "BANK_ACCOUNT", "acct-manager-1", UserRole.ACCOUNTS)
        assertTrue(postP1Res is DomainResult.Success)
        val postedP1 = (postP1Res as DomainResult.Success).data
        assertEquals(CustomerPaymentStatus.POSTED, postedP1.status)
        assertTrue(!postedP1.receiptId.isNullOrBlank())

        // 5. Verify partial receivable status
        val recAfterP1 = (receivableRepository.getReceivableById(recId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(CustomerReceivableStatus.PARTIALLY_SETTLED, recAfterP1.status)
        assertEquals(Money(BigDecimal("50000.00")), recAfterP1.settledAmount)
        assertEquals(Money(BigDecimal("100000.00")), recAfterP1.outstandingAmount)

        // 6. Step 03 Create and Post Final Payment (৳100,000 via Mobile Banking)
        val p2Res = paymentRepository.createPayment(
            projectId = projectId,
            customerId = customerId,
            receivableId = recId,
            amount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.MOBILE_BANKING,
            paymentReference = "BKASH-E2E-002",
            notes = "Final balance payment",
            actorId = "staff-operator-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(p2Res is DomainResult.Success)
        val p2Id = (p2Res as DomainResult.Success).data.paymentId

        paymentRepository.submitPayment(p2Id, "staff-operator-1", UserRole.STAFF)
        val postP2Res = paymentRepository.postPayment(p2Id, "MOBILE_WALLET", "acct-manager-1", UserRole.ACCOUNTS)
        assertTrue(postP2Res is DomainResult.Success)

        // 7. Verify fully settled receivable
        val finalRec = (receivableRepository.getReceivableById(recId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(CustomerReceivableStatus.SETTLED, finalRec.status)
        assertEquals(Money(BigDecimal("150000.00")), finalRec.settledAmount)
        assertEquals(Money.ZERO, finalRec.outstandingAmount)

        // 8. Verify Receipt 2 issued
        val r2Res = paymentRepository.getReceiptByPaymentId(p2Id, UserRole.ACCOUNTS)
        assertTrue(r2Res is DomainResult.Success)
        val receipt2 = (r2Res as DomainResult.Success).data
        assertEquals(Money(BigDecimal("100000.00")), receipt2.amount)
        assertEquals(CustomerPaymentMethod.MOBILE_BANKING, receipt2.paymentMethod)
        assertEquals("BKASH-E2E-002", receipt2.paymentReference)

        // 9. Verify Financial Ledger entries created for payments
        val p2TxnId = (postP2Res as DomainResult.Success).data.financialTransactionId
        assertTrue(!p2TxnId.isNullOrBlank())
        val p2Txn = (financialTransactionRepository.getTransactionById(p2TxnId!!, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.POSTED, p2Txn.transactionStatus)
        assertEquals(FinancialTransactionType.RECEIPT, p2Txn.transactionType)
        assertEquals(FinancialEntryType.CREDIT, p2Txn.entryType)

        // 10. Verify Zero Inventory Mutation
        assertEquals(initialStockOuts, stockOutDataSource.observeStockOutRecords().first().size)
        assertEquals(initialStockIns, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(initialInventoryLedger, inventoryLedgerDataSource.getEntries(projectId).size)

        // 11. Verify Upstream Delivery Order status untouched
        val retrievedDO = deliveryOrderDataSource.getDeliveryOrder("DO-E2E-PAY-01")
        assertEquals(DeliveryOrderStatus.DELIVERED, retrievedDO?.status)
    }
}
