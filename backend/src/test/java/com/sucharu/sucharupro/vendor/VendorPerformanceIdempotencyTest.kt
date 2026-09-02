package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.ComplianceRequirementType
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorComplianceRecord
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class VendorPerformanceIdempotencyTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var repo: VendorPerformanceRepositoryImpl
    private lateinit var service: VendorPerformanceServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            val ds = FakeVendorPerformanceDataSource()
            repo = VendorPerformanceRepositoryImpl(ds)
            service = VendorPerformanceServiceImpl(
                performanceRepository = repo,
                vendorRepository = vendorRepo
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-01",
                    vendorCode = "V001",
                    vendorName = "Vendor One",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testComplianceVerificationIdempotency() = runBlocking {
        val now = Instant.now()
        val record = VendorComplianceRecord(
            recordId = "REC-IDEM-01",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            requirementId = "REQ-01",
            requirementCode = "TIN",
            requirementName = "Tax Identification",
            requirementType = ComplianceRequirementType.TAX_VAT,
            effectiveDate = now,
            createdBy = "staff"
        )
        service.submitComplianceRecord(record)

        // First verification
        val ver1 = service.verifyComplianceRecord("PRJ-01", "REC-IDEM-01", "officer_1", true, notes = "Verified first time")
        assertTrue(ver1 is DomainResult.Success)

        // Second verification should succeed idempotently
        val ver2 = service.verifyComplianceRecord("PRJ-01", "REC-IDEM-01", "officer_1", true, notes = "Verified second time")
        assertTrue(ver2 is DomainResult.Success)
    }
}
