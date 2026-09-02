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

class VendorServiceRateResolutionTest {

    private lateinit var service: VendorServiceRateServiceImpl

    @Before
    fun setup() {
        runBlocking {
        val fakeVendorDs = FakeVendorDataSource()
        val fakeCapDs = FakeVendorCapabilityDataSource()
        val fakeRateDs = FakeVendorServiceRateDataSource()

        val vendorRepo = VendorRepositoryImpl(fakeVendorDs)
        val capRepo = VendorCapabilityRepositoryImpl(fakeCapDs)
        val rateRepo = VendorServiceRateRepositoryImpl(fakeRateDs)

        service = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)

        vendorRepo.createVendor(
            Vendor(
                vendorId = "vnd_lam",
                projectId = "p_proj",
                vendorCode = "V-LAM",
                vendorName = "Lam Experts",
                vendorType = VendorType.SERVICE_PROVIDER,
                vendorCategory = VendorCategory.FINISHING,
                status = VendorStatus.ACTIVE
            )
        )

        capRepo.createCapability(
            VendorCapability(
                capabilityId = "cap_lam",
                vendorId = "vnd_lam",
                projectId = "p_proj",
                capabilityType = CapabilityType.LAMINATION,
                displayName = "Thermal Lamination",
                status = CapabilityStatus.ACTIVE
            )
        )

        // Historical rate: Jan 1 -> Jun 30 (timestamp 1000 -> 6000), ৳10
        service.createRate(
            projectId = "p_proj",
            vendorId = "vnd_lam",
            capabilityType = CapabilityType.LAMINATION,
            serviceName = "Lamination H1",
            pricingMethod = PricingMethod.PER_AREA,
            unitOfMeasure = UnitOfMeasure.SQ_FT,
            rateAmount = Money(BigDecimal("10.00")),
            effectiveFrom = 1000L,
            effectiveTo = 6000L
        )

        // Current rate: Jul 1 -> open ended (timestamp 6001 -> null), ৳12
        service.createRate(
            projectId = "p_proj",
            vendorId = "vnd_lam",
            capabilityType = CapabilityType.LAMINATION,
            serviceName = "Lamination H2",
            pricingMethod = PricingMethod.PER_AREA,
            unitOfMeasure = UnitOfMeasure.SQ_FT,
            rateAmount = Money(BigDecimal("12.00")),
            effectiveFrom = 6001L,
            effectiveTo = null
        )
        }
    }

    @Test
    fun `resolves correct rate based on effective timestamp`() = runBlocking {
        // Resolve for date 3000 (within H1) -> should resolve to ৳10
        val resH1 = service.resolveApplicableRate(
            projectId = "p_proj",
            vendorId = "vnd_lam",
            capabilityType = CapabilityType.LAMINATION,
            effectiveDate = 3000L
        )
        assertTrue(resH1 is DomainResult.Success)
        assertEquals(Money(BigDecimal("10.00")), (resH1 as DomainResult.Success).data.rateAmount)

        // Resolve for date 7000 (within H2) -> should resolve to ৳12
        val resH2 = service.resolveApplicableRate(
            projectId = "p_proj",
            vendorId = "vnd_lam",
            capabilityType = CapabilityType.LAMINATION,
            effectiveDate = 7000L
        )
        assertTrue(resH2 is DomainResult.Success)
        assertEquals(Money(BigDecimal("12.00")), (resH2 as DomainResult.Success).data.rateAmount)
    }

    @Test
    fun `fails closed when date is out of any active rate bounds`() = runBlocking {
        val outOfBounds = service.resolveApplicableRate(
            projectId = "p_proj",
            vendorId = "vnd_lam",
            capabilityType = CapabilityType.LAMINATION,
            effectiveDate = 500L // before 1000L
        )
        assertTrue(outOfBounds is DomainResult.Error)
    }
}
