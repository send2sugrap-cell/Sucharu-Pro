package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalPerformanceComplianceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPortalPerformanceComplianceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalComplianceEvidenceType
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceServiceImpl
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalPerformanceConcurrencyAndIdempotencyTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-001"

    private lateinit var portalService: VendorPortalPerformanceComplianceServiceImpl

    @Before
    fun setup() = runBlocking {
        val vendorDs = FakeVendorDataSource()
        val vendorRepo = VendorRepositoryImpl(vendorDs)

        vendorRepo.createVendor(
            Vendor(
                vendorId = vendorId,
                projectId = projectId,
                vendorName = "Concurrent Vendor Ltd",
                vendorCode = "VND-CC",
                status = VendorStatus.ACTIVE
            )
        )

        val perfDs = FakeVendorPerformanceDataSource()
        val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
        val canonicalService = VendorPerformanceServiceImpl(perfRepo, vendorRepo)

        val portalDs = FakeVendorPortalPerformanceComplianceDataSource()
        val portalRepo = VendorPortalPerformanceComplianceRepositoryImpl(portalDs)

        portalService = VendorPortalPerformanceComplianceServiceImpl(
            portalRepository = portalRepo,
            canonicalPerformanceService = canonicalService,
            vendorRepository = vendorRepo
        )
    }

    @Test
    fun testConcurrentEvidenceUploads() = runBlocking {
        val jobs = (1..20).map { i ->
            async(Dispatchers.Default) {
                portalService.uploadComplianceEvidence(
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    recordId = "REC-01",
                    requirementId = "REQ-01",
                    actionId = null,
                    evidenceType = VendorPortalComplianceEvidenceType.CERTIFICATE,
                    fileName = "cert_doc_$i.pdf",
                    fileUrl = "https://files.com/cert_$i.pdf",
                    checksum = null,
                    fileSizeBytes = 1024L,
                    mimeType = "application/pdf",
                    description = "Evidence batch $i",
                    actorId = "ACTOR-$i"
                )
            }
        }

        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val listRes = portalService.listComplianceEvidence(tenantId, projectId, vendorId, "REC-01", null)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(20, (listRes as DomainResult.Success).data.size)
    }
}
