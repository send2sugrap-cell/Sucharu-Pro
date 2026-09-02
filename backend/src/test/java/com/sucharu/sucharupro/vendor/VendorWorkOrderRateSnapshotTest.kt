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

class VendorWorkOrderRateSnapshotTest {

    private lateinit var workOrderService: VendorWorkOrderServiceImpl
    private lateinit var rateService: VendorServiceRateServiceImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl

    @Before
    fun setup() {
        runBlocking {
            val fakeVendorDs = FakeVendorDataSource()
            val fakeCapDs = FakeVendorCapabilityDataSource()
            val fakeRateDs = FakeVendorServiceRateDataSource()
            val fakeWorkOrderDs = FakeVendorWorkOrderDataSource()

            val vendorRepo = VendorRepositoryImpl(fakeVendorDs)
            val capRepo = VendorCapabilityRepositoryImpl(fakeCapDs)
            rateRepo = VendorServiceRateRepositoryImpl(fakeRateDs)
            val workOrderRepo = VendorWorkOrderRepositoryImpl(fakeWorkOrderDs)

            rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            workOrderService = VendorWorkOrderServiceImpl(vendorRepo, capRepo, rateService, workOrderRepo)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_prn",
                    projectId = "p_1",
                    vendorCode = "V-PRN",
                    vendorName = "Print Tech",
                    status = VendorStatus.ACTIVE
                )
            )

            capRepo.createCapability(
                VendorCapability(
                    capabilityId = "cap_ctp",
                    vendorId = "vnd_prn",
                    projectId = "p_1",
                    capabilityType = CapabilityType.CTP,
                    displayName = "CTP 4-Up",
                    status = CapabilityStatus.ACTIVE
                )
            )

            rateService.createRate(
                projectId = "p_1",
                vendorId = "vnd_prn",
                capabilityType = CapabilityType.CTP,
                serviceName = "4-Up Plate Output",
                pricingMethod = PricingMethod.PER_UNIT,
                unitOfMeasure = UnitOfMeasure.PLATE,
                rateAmount = Money(BigDecimal("100.00")),
                currency = "BDT"
            )
        }
    }

    @Test
    fun `rate snapshot is immutable and unaffected by subsequent rate card updates`() = runBlocking {
        // Step 1: Create work order with initial rate of ৳100 and qty 10 -> ৳1000
        val createRes = workOrderService.createWorkOrder(
            projectId = "p_1",
            vendorId = "vnd_prn",
            capabilityType = CapabilityType.CTP,
            title = "Magazine Plates",
            quantity = BigDecimal("10"),
            unitOfMeasure = UnitOfMeasure.PLATE,
            pricingMethod = PricingMethod.PER_UNIT,
            actorId = "user_1"
        )
        assertTrue(createRes is DomainResult.Success)
        val order = (createRes as DomainResult.Success).data
        val workOrderId = order.workOrderId

        assertEquals(Money(BigDecimal("1000.00")), order.estimatedAmount)
        assertEquals(Money(BigDecimal("100.00")), order.rateSnapshot.resolvedUnitRate)

        // Step 2: Release work order
        val releaseRes = workOrderService.releaseWorkOrder("p_1", workOrderId, "user_1")
        assertTrue(releaseRes is DomainResult.Success)

        // Step 3: Later, vendor rate is updated / increased to ৳150
        val rates = rateRepo.listByVendor("p_1", "vnd_prn")
        val initialRate = (rates as DomainResult.Success).data[0]
        rateService.updateStatus("p_1", initialRate.rateId, RateStatus.SUSPENDED, "user_1")
        rateService.createRate(
            projectId = "p_1",
            vendorId = "vnd_prn",
            capabilityType = CapabilityType.CTP,
            serviceName = "4-Up Plate Output New Rate",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.PLATE,
            rateAmount = Money(BigDecimal("150.00")),
            currency = "BDT"
        )

        // Step 4: Retrieve historical work order and verify snapshot & estimated amount remain ৳100 and ৳1000
        val retrievedRes = workOrderService.getWorkOrderById("p_1", workOrderId)
        assertTrue(retrievedRes is DomainResult.Success)
        val retrieved = (retrievedRes as DomainResult.Success).data

        assertEquals(Money(BigDecimal("1000.00")), retrieved.estimatedAmount)
        assertEquals(Money(BigDecimal("100.00")), retrieved.rateSnapshot.resolvedUnitRate)
        assertEquals(Money(BigDecimal("100.00")), retrieved.rateSnapshot.baseRate)
    }
}
