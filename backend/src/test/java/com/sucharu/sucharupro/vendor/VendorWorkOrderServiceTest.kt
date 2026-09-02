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

class VendorWorkOrderServiceTest {

    private lateinit var workOrderService: VendorWorkOrderServiceImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateService: VendorServiceRateServiceImpl
    private lateinit var workOrderRepo: VendorWorkOrderRepositoryImpl

    @Before
    fun setup() {
        runBlocking {
            val fakeVendorDs = FakeVendorDataSource()
            val fakeCapDs = FakeVendorCapabilityDataSource()
            val fakeRateDs = FakeVendorServiceRateDataSource()
            val fakeWorkOrderDs = FakeVendorWorkOrderDataSource()

            vendorRepo = VendorRepositoryImpl(fakeVendorDs)
            capRepo = VendorCapabilityRepositoryImpl(fakeCapDs)
            val rateRepo = VendorServiceRateRepositoryImpl(fakeRateDs)
            workOrderRepo = VendorWorkOrderRepositoryImpl(fakeWorkOrderDs)

            rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            workOrderService = VendorWorkOrderServiceImpl(vendorRepo, capRepo, rateService, workOrderRepo)

            // Seed active vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_active",
                    projectId = "p_1",
                    vendorCode = "V-ACT",
                    vendorName = "Standard Press",
                    vendorType = VendorType.SERVICE_PROVIDER,
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            // Seed active capability
            capRepo.createCapability(
                VendorCapability(
                    capabilityId = "cap_ctp",
                    vendorId = "vnd_active",
                    projectId = "p_1",
                    capabilityType = CapabilityType.CTP,
                    displayName = "CTP Output",
                    status = CapabilityStatus.ACTIVE
                )
            )

            // Seed active rate
            rateService.createRate(
                projectId = "p_1",
                vendorId = "vnd_active",
                capabilityType = CapabilityType.CTP,
                serviceName = "Standard CTP Plate",
                pricingMethod = PricingMethod.PER_UNIT,
                unitOfMeasure = UnitOfMeasure.PLATE,
                rateAmount = Money(BigDecimal("150.00")),
                currency = "BDT"
            )
        }
    }

    @Test
    fun `successful work order creation with automated rate resolution and amount calculation`() = runBlocking {
        val res = workOrderService.createWorkOrder(
            projectId = "p_1",
            vendorId = "vnd_active",
            capabilityType = CapabilityType.CTP,
            title = "Book Cover Plates",
            quantity = BigDecimal("4"),
            unitOfMeasure = UnitOfMeasure.PLATE,
            pricingMethod = PricingMethod.PER_UNIT,
            actorId = "user_1"
        )

        assertTrue(res is DomainResult.Success)
        val order = (res as DomainResult.Success).data
        assertEquals("p_1", order.projectId)
        assertEquals("vnd_active", order.vendorId)
        assertEquals(CapabilityType.CTP, order.capabilityType)
        assertEquals(VendorWorkOrderStatus.ASSIGNED, order.status)
        assertEquals(Money(BigDecimal("600.00")), order.estimatedAmount)
        assertEquals(Money(BigDecimal("150.00")), order.rateSnapshot.resolvedUnitRate)

        // Verify audit event recorded
        val audits = workOrderService.listAudits("p_1", order.workOrderId)
        assertTrue(audits is DomainResult.Success)
        assertEquals(1, (audits as DomainResult.Success).data.size)
        assertEquals("CREATED", audits.data[0].eventType)
    }

    @Test
    fun `cannot create work order if vendor is inactive or does not possess capability`() = runBlocking {
        // Attempt on non-existent capability (e.g. SPOT_UV)
        val resNoCap = workOrderService.createWorkOrder(
            projectId = "p_1",
            vendorId = "vnd_active",
            capabilityType = CapabilityType.SPOT_UV,
            title = "Spot UV Job",
            quantity = BigDecimal("100"),
            actorId = "user_1"
        )
        assertTrue(resNoCap is DomainResult.Error)
        assertTrue((resNoCap as DomainResult.Error).message.contains("does not possess active capability"))

        // Attempt on inactive vendor
        vendorRepo.createVendor(
            Vendor(
                vendorId = "vnd_suspended",
                projectId = "p_1",
                vendorCode = "V-SUS",
                vendorName = "Suspended Press",
                status = VendorStatus.SUSPENDED
            )
        )
        val resSuspended = workOrderService.createWorkOrder(
            projectId = "p_1",
            vendorId = "vnd_suspended",
            capabilityType = CapabilityType.CTP,
            title = "Plate Job",
            quantity = BigDecimal("10"),
            actorId = "user_1"
        )
        assertTrue(resSuspended is DomainResult.Error)
        assertTrue((resSuspended as DomainResult.Error).message.contains("vendor status is 'SUSPENDED'"))
    }
}
