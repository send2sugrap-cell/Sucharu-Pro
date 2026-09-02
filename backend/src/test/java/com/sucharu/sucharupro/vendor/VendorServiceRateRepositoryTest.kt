package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorServiceRateDataSource
import com.sucharu.sucharupro.data.repository.VendorServiceRateRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorServiceRateRepositoryTest {

    private lateinit var repository: VendorServiceRateRepositoryImpl

    @Before
    fun setup() {
        val fakeDs = FakeVendorServiceRateDataSource()
        repository = VendorServiceRateRepositoryImpl(fakeDs)
    }

    private fun sampleRate(rateId: String = "rate_1", code: String = "RC_1", capability: CapabilityType = CapabilityType.CTP): VendorServiceRate {
        return VendorServiceRate(
            rateId = rateId,
            projectId = "tenant_test",
            vendorId = "vnd_001",
            capabilityType = capability,
            rateCode = code,
            serviceName = "CTP Output",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.PLATE,
            rateAmount = Money(BigDecimal("750.00")),
            effectiveFrom = 1000L,
            effectiveTo = 2000L,
            status = RateStatus.ACTIVE
        )
    }

    @Test
    fun `save and find rate by ID and rateCode`() = runBlocking {
        val rate = sampleRate()
        val created = repository.createRate(rate)
        assertTrue(created is DomainResult.Success)

        val foundById = repository.findById("tenant_test", "rate_1")
        assertTrue(foundById is DomainResult.Success)
        assertEquals("RC_1", (foundById as DomainResult.Success).data.rateCode)

        val foundByCode = repository.findByRateCode("tenant_test", "RC_1")
        assertTrue(foundByCode is DomainResult.Success)
        assertEquals("rate_1", (foundByCode as DomainResult.Success).data.rateId)
    }

    @Test
    fun `optimistic concurrency and status update`() = runBlocking {
        val rate = sampleRate()
        repository.createRate(rate)

        val statusUpdate = repository.updateStatus("tenant_test", "rate_1", RateStatus.SUSPENDED, "admin_user")
        assertTrue(statusUpdate is DomainResult.Success)
        assertEquals(RateStatus.SUSPENDED, (statusUpdate as DomainResult.Success).data.status)
        assertEquals(2L, statusUpdate.data.version)

        // Attempting update on old version 1L should fail with concurrency conflict
        val conflictUpdate = repository.updateRate(rate.copy(version = 1L))
        assertTrue(conflictUpdate is DomainResult.Error)
        assertTrue((conflictUpdate as DomainResult.Error).message.contains("conflict"))
    }

    @Test
    fun `listByVendor and listByCapability filter correctly`() = runBlocking {
        repository.createRate(sampleRate("r1", "C1", CapabilityType.CTP))
        repository.createRate(sampleRate("r2", "C2", CapabilityType.LAMINATION))

        val vendorRates = repository.listByVendor("tenant_test", "vnd_001")
        assertTrue(vendorRates is DomainResult.Success)
        assertEquals(2, (vendorRates as DomainResult.Success).data.size)

        val ctpRates = repository.listByCapability("tenant_test", CapabilityType.CTP)
        assertTrue(ctpRates is DomainResult.Success)
        assertEquals(1, (ctpRates as DomainResult.Success).data.size)
        assertEquals("r1", ctpRates.data.first().rateId)
    }
}
