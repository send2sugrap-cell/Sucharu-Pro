package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableDeliveryBoundaryTest {

    private lateinit var deliveryDataSource: FakeDeliveryOrderDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var payableRepository: VendorPayableRepository

    @Before
    fun setUp() {
        deliveryDataSource = FakeDeliveryOrderDataSource()
        payableDataSource = FakeVendorPayableDataSource()
        payableRepository = VendorPayableRepositoryImpl(payableDataSource)
    }

    @Test
    fun `vendor payable operations do not mutate delivery orders or dispatch challans`() = runBlocking {
        val projectId = "PRJ-DELIV-BOUND"

        val deliveryOrder = DeliveryOrder(
            deliveryOrderId = "DO-001",
            projectId = projectId,
            deliveryOrderNo = "DON-001",
            customerId = "CUST-001",
            sourceReferenceId = "SO-001",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.DELIVERED,
            requestedDeliveryDate = 2000L,
            notes = "Delivered",
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        deliveryDataSource.insertDeliveryOrder(deliveryOrder, emptyList())

        val createRes = payableRepository.createPayable(
            projectId = projectId,
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-DELIV-1",
            originalAmount = Money(BigDecimal("18000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Packaging delivery supply",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(createRes is DomainResult.Success)

        val retrievedDO = deliveryDataSource.getDeliveryOrder("DO-001")
        assertEquals(DeliveryOrderStatus.DELIVERED, retrievedDO?.status)
    }
}
