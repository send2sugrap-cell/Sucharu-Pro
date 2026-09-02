package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.repository.DeliveryPartialSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryVerificationSettlementIntegrationTest {

    private lateinit var settlementDataSource: FakeDeliveryPartialSettlementDataSource
    private lateinit var doDataSource: FakeDeliveryOrderDataSource
    private lateinit var verificationDataSource: FakeDeliveryItemVerificationDataSource
    private lateinit var repository: DeliveryPartialSettlementRepository

    @Before
    fun setUp() {
        runBlocking {
            settlementDataSource = FakeDeliveryPartialSettlementDataSource()
            doDataSource = FakeDeliveryOrderDataSource()
            verificationDataSource = FakeDeliveryItemVerificationDataSource()

            repository = DeliveryPartialSettlementRepositoryImpl(
                settlementDataSource = settlementDataSource,
                doDataSource = doDataSource,
                verificationDataSource = verificationDataSource
            )

            val doOrder = DeliveryOrder("DO-VER-INT", "PRJ-01", "DON-VI", "CUST-01", "SO-01", "SALES_ORDER", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "user-1", 1000L, 1000L)
            val doLine = DeliveryOrderLine("DOL-VI", "DO-VER-INT", "PRJ-01", "PROD-01", 1000.0, null)
            doDataSource.insertDeliveryOrder(doOrder, listOf(doLine))

            val verif = DeliveryItemVerification("V-1", "PRJ-01", "VN-1", "DO-VER-INT", "CH-1", "DISP-1", DeliveryItemVerificationStatus.VERIFIED, null, null, null, "user-1", 1000L, null, 1000L)
            val vLine = DeliveryItemVerificationLine("VL-1", "V-1", "PRJ-01", "DL-1", "CL-1", "DOL-VI", "PROD-01", null, null, 800.0, 800.0, 0.0, createdAt = 1000L)
            verificationDataSource.insertVerification(verif, listOf(vLine))
        }
    }

    @Test
    fun `settlement adopts verified delivered quantities from Step 04 verification`() = runBlocking {
        val res = repository.initializeSettlementForDeliveryOrder("DO-VER-INT", "user-1", UserRole.ADMIN)
        assertTrue(res is DomainResult.Success)

        val settlement = (res as DomainResult.Success).data
        assertEquals(1000.0, settlement.totalOrderedQuantity, 0.001)
        assertEquals(800.0, settlement.totalDeliveredQuantity, 0.001)
        assertEquals(200.0, settlement.totalPendingQuantity, 0.001)
    }
}
