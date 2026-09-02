package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorCapabilityDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorServiceRateDataSource
import com.sucharu.sucharupro.data.repository.VendorCapabilityRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorServiceRateRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorServiceRateServiceTest {

    private lateinit var service: VendorServiceRateServiceImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl

    @Before
    fun setup() {
        runBlocking {
            val fakeVendorDs = FakeVendorDataSource()
            val fakeCapDs = FakeVendorCapabilityDataSource()
            val fakeRateDs = FakeVendorServiceRateDataSource()

            vendorRepo = VendorRepositoryImpl(fakeVendorDs)
            capRepo = VendorCapabilityRepositoryImpl(fakeCapDs)
            rateRepo = VendorServiceRateRepositoryImpl(fakeRateDs)

            service = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)

            // Seed an active vendor and an active capability
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
        }
    }

    @Test
    fun `cannot create rate if vendor does not have the capability`() = runBlocking {
        val res = service.createRate(
            projectId = "p_1",
            vendorId = "vnd_active",
            capabilityType = CapabilityType.DIE_CUTTING, // Not registered for this vendor
            serviceName = "Die Cutting",
            rateAmount = Money(BigDecimal("1.50"))
        )
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("does not possess active capability"))
    }

    @Test
    fun `successful rate creation and cost estimation`() = runBlocking {
        val createRes = service.createRate(
            projectId = "p_1",
            vendorId = "vnd_active",
            capabilityType = CapabilityType.CTP,
            serviceName = "CTP Output Service",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.PLATE,
            rateAmount = Money(BigDecimal("800.00")),
            effectiveFrom = 1000L,
            effectiveTo = 5000L
        )
        assertTrue(createRes is DomainResult.Success)
        val rate = (createRes as DomainResult.Success).data
        assertEquals("CTP Output Service", rate.serviceName)

        val estimateRes = service.estimateCost(
            projectId = "p_1",
            rateId = rate.rateId,
            quantity = BigDecimal("4")
        )
        assertTrue(estimateRes is DomainResult.Success)
        assertEquals(Money(BigDecimal("3200.00")), (estimateRes as DomainResult.Success).data)
    }

    @Test
    fun `cannot create overlapping active rates for same vendor and capability`() = runBlocking {
        service.createRate(
            projectId = "p_1",
            vendorId = "vnd_active",
            capabilityType = CapabilityType.CTP,
            serviceName = "CTP Normal",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.PIECE,
            rateAmount = Money(BigDecimal("800.00")),
            effectiveFrom = 1000L,
            effectiveTo = 2000L
        )

        // Attempting overlapping rate (1500 -> 2500)
        val overlapRes = service.createRate(
            projectId = "p_1",
            vendorId = "vnd_active",
            capabilityType = CapabilityType.CTP,
            serviceName = "CTP Overlap",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.PIECE,
            rateAmount = Money(BigDecimal("850.00")),
            effectiveFrom = 1500L,
            effectiveTo = 2500L
        )
        assertTrue(overlapRes is DomainResult.Error)
        assertTrue((overlapRes as DomainResult.Error).message.contains("overlapping"))
    }
}
