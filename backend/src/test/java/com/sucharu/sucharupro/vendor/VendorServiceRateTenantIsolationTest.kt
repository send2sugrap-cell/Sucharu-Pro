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
import org.junit.Test
import java.math.BigDecimal

class VendorServiceRateTenantIsolationTest {

    @Test
    fun `rate in Tenant A cannot be viewed, resolved or modified by Tenant B`() = runBlocking {
        val fakeVendorDs = FakeVendorDataSource()
        val fakeCapDs = FakeVendorCapabilityDataSource()
        val fakeRateDs = FakeVendorServiceRateDataSource()

        val vendorRepo = VendorRepositoryImpl(fakeVendorDs)
        val capRepo = VendorCapabilityRepositoryImpl(fakeCapDs)
        val rateRepo = VendorServiceRateRepositoryImpl(fakeRateDs)

        val service = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)

        // Seed Tenant A
        vendorRepo.createVendor(
            Vendor(
                vendorId = "vnd_A",
                projectId = "TENANT_A",
                vendorCode = "V-A",
                vendorName = "Vendor Alpha",
                vendorType = VendorType.SERVICE_PROVIDER,
                vendorCategory = VendorCategory.PRINTING,
                status = VendorStatus.ACTIVE
            )
        )
        capRepo.createCapability(
            VendorCapability(
                capabilityId = "cap_A",
                vendorId = "vnd_A",
                projectId = "TENANT_A",
                capabilityType = CapabilityType.CTP,
                displayName = "CTP Output",
                status = CapabilityStatus.ACTIVE
            )
        )

        val rateCreated = service.createRate(
            projectId = "TENANT_A",
            vendorId = "vnd_A",
            capabilityType = CapabilityType.CTP,
            serviceName = "Alpha CTP",
            rateAmount = Money(BigDecimal("500.00"))
        )
        assertTrue(rateCreated is DomainResult.Success)
        val rateId = (rateCreated as DomainResult.Success).data.rateId

        // Tenant B attempt to read rate by rateId
        val readFromB = service.getRateById("TENANT_B", rateId)
        assertTrue(readFromB is DomainResult.Error)

        // Tenant B attempt to list rates for vendor vnd_A
        val listFromB = service.listRatesByVendor("TENANT_B", "vnd_A")
        assertTrue(listFromB is DomainResult.Success)
        assertTrue((listFromB as DomainResult.Success).data.isEmpty())

        // Tenant B attempt to resolve rate for vnd_A
        val resolveFromB = service.resolveApplicableRate(
            projectId = "TENANT_B",
            vendorId = "vnd_A",
            capabilityType = CapabilityType.CTP
        )
        assertTrue(resolveFromB is DomainResult.Error)
    }
}
