package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorCapabilityDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorServiceRateDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorWorkOrderDataSource
import com.sucharu.sucharupro.data.repository.VendorCapabilityRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorServiceRateRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorWorkOrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorWorkOrderServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorWorkOrderLifecycleTest {

    private lateinit var workOrderService: VendorWorkOrderServiceImpl
    private lateinit var orderId: String

    @Before
    fun setup() {
        runBlocking {
            val fakeVendorDs = FakeVendorDataSource()
            val fakeCapDs = FakeVendorCapabilityDataSource()
            val fakeRateDs = FakeVendorServiceRateDataSource()
            val fakeWorkOrderDs = FakeVendorWorkOrderDataSource()

            val vendorRepo = VendorRepositoryImpl(fakeVendorDs)
            val capRepo = VendorCapabilityRepositoryImpl(fakeCapDs)
            val rateRepo = VendorServiceRateRepositoryImpl(fakeRateDs)
            val workOrderRepo = VendorWorkOrderRepositoryImpl(fakeWorkOrderDs)

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            workOrderService = VendorWorkOrderServiceImpl(vendorRepo, capRepo, rateService, workOrderRepo)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_1",
                    projectId = "p_1",
                    vendorCode = "V-1",
                    vendorName = "Vendor 1",
                    status = VendorStatus.ACTIVE
                )
            )

            capRepo.createCapability(
                VendorCapability(
                    capabilityId = "cap_lam",
                    vendorId = "vnd_1",
                    projectId = "p_1",
                    capabilityType = CapabilityType.LAMINATION,
                    displayName = "Gloss Lamination",
                    status = CapabilityStatus.ACTIVE
                )
            )

            val created = workOrderService.createWorkOrder(
                projectId = "p_1",
                vendorId = "vnd_1",
                capabilityType = CapabilityType.LAMINATION,
                title = "Lamination Job",
                quantity = BigDecimal("100"),
                unitRate = Money(BigDecimal("2.50")),
                actorId = "user_1"
            )
            orderId = (created as DomainResult.Success).data.workOrderId
        }
    }

    @Test
    fun `full lifecycle transition ASSIGNED to RELEASED to IN_PROGRESS to ON_HOLD to IN_PROGRESS to COMPLETED`() = runBlocking {
        // ASSIGNED -> RELEASED
        val releaseRes = workOrderService.releaseWorkOrder("p_1", orderId, "user_1")
        assertTrue(releaseRes is DomainResult.Success)
        assertEquals(VendorWorkOrderStatus.RELEASED, (releaseRes as DomainResult.Success).data.status)

        // RELEASED -> IN_PROGRESS
        val startRes = workOrderService.changeStatus("p_1", orderId, VendorWorkOrderStatus.IN_PROGRESS, "user_1")
        assertTrue(startRes is DomainResult.Success)
        assertEquals(VendorWorkOrderStatus.IN_PROGRESS, (startRes as DomainResult.Success).data.status)

        // IN_PROGRESS -> ON_HOLD
        val holdRes = workOrderService.changeStatus("p_1", orderId, VendorWorkOrderStatus.ON_HOLD, "user_1", reason = "Awaiting raw paper")
        assertTrue(holdRes is DomainResult.Success)
        assertEquals(VendorWorkOrderStatus.ON_HOLD, (holdRes as DomainResult.Success).data.status)

        // ON_HOLD -> IN_PROGRESS
        val resumeRes = workOrderService.changeStatus("p_1", orderId, VendorWorkOrderStatus.IN_PROGRESS, "user_1", reason = "Paper arrived")
        assertTrue(resumeRes is DomainResult.Success)
        assertEquals(VendorWorkOrderStatus.IN_PROGRESS, (resumeRes as DomainResult.Success).data.status)

        // IN_PROGRESS -> COMPLETED
        val completeRes = workOrderService.changeStatus("p_1", orderId, VendorWorkOrderStatus.COMPLETED, "user_1")
        assertTrue(completeRes is DomainResult.Success)
        assertEquals(VendorWorkOrderStatus.COMPLETED, (completeRes as DomainResult.Success).data.status)

        // COMPLETED is terminal; cannot transition back to DRAFT or IN_PROGRESS
        val illegalRes = workOrderService.changeStatus("p_1", orderId, VendorWorkOrderStatus.IN_PROGRESS, "user_1")
        assertTrue(illegalRes is DomainResult.Error)

        // Verify audit timeline
        val audits = workOrderService.listAudits("p_1", orderId)
        assertTrue(audits is DomainResult.Success)
        val list = (audits as DomainResult.Success).data
        assertEquals(6, list.size) // CREATED, RELEASED, STARTED, PUT_ON_HOLD, RESUMED, COMPLETED
    }

    @Test
    fun `cancellation workflow from ASSIGNED status`() = runBlocking {
        val cancelRes = workOrderService.changeStatus("p_1", orderId, VendorWorkOrderStatus.CANCELLED, "user_1", reason = "Customer cancelled order")
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(VendorWorkOrderStatus.CANCELLED, (cancelRes as DomainResult.Success).data.status)

        // CANCELLED is terminal
        val restartRes = workOrderService.changeStatus("p_1", orderId, VendorWorkOrderStatus.IN_PROGRESS, "user_1")
        assertTrue(restartRes is DomainResult.Error)
    }
}
