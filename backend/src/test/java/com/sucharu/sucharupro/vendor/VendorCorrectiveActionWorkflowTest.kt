package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class VendorCorrectiveActionWorkflowTest {

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
                    vendorName = "Vendor 1",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testCorrectiveActionFullWorkflow() = runBlocking {
        val action = VendorCorrectiveAction(
            actionId = "CAPA-100",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            sourceType = "QUALITY",
            sourceId = "DISPUTE-01",
            issueDescription = "High defect rate on corrugated boxes batch #42",
            rootCause = "Die-cutting machine calibration failure",
            actionPlan = "Re-calibrate all die cutters and add pre-shipment inspection checklist",
            assignedTo = "vendor_quality_mgr",
            assignedToName = "Quality Manager",
            priority = CorrectiveActionPriority.HIGH,
            dueDate = Instant.now().plusSeconds(86400 * 14),
            createdBy = "qc_inspector"
        )

        // 1. Create (DRAFT/OPEN)
        val createRes = service.createCorrectiveAction(action)
        assertTrue(createRes is DomainResult.Success)
        val created = (createRes as DomainResult.Success).data
        assertEquals(CorrectiveActionStatus.OPEN, created.status)

        // 2. Start (IN_PROGRESS)
        val startRes = service.startCorrectiveAction("PRJ-01", "CAPA-100", "vendor_quality_mgr", "Initiated calibration overhaul")
        assertTrue(startRes is DomainResult.Success)
        val started = (startRes as DomainResult.Success).data
        assertEquals(CorrectiveActionStatus.IN_PROGRESS, started.status)

        // 3. Submit for Verification (PENDING_VERIFICATION)
        val subVerRes = service.submitCorrectiveActionForVerification("PRJ-01", "CAPA-100", "vendor_quality_mgr", "Calibration completed, 5 sample batches passed")
        assertTrue(subVerRes is DomainResult.Success)
        val subVer = (subVerRes as DomainResult.Success).data
        assertEquals(CorrectiveActionStatus.PENDING_VERIFICATION, subVer.status)

        // 4. Verify (VERIFIED)
        val verRes = service.verifyCorrectiveAction("PRJ-01", "CAPA-100", "qc_lead", "Confirmed calibration test certificates")
        assertTrue(verRes is DomainResult.Success)
        val verified = (verRes as DomainResult.Success).data
        assertEquals(CorrectiveActionStatus.VERIFIED, verified.status)
        assertNotNull(verified.verifiedAt)

        // 5. Close (CLOSED)
        val closeRes = service.closeCorrectiveAction("PRJ-01", "CAPA-100", "qc_lead", "Issue resolved and permanently verified")
        assertTrue(closeRes is DomainResult.Success)
        val closed = (closeRes as DomainResult.Success).data
        assertEquals(CorrectiveActionStatus.CLOSED, closed.status)
        assertNotNull(closed.closedAt)
    }
}
