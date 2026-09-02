package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalWorkflowDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPurchaseOrderDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalWorkflowRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPurchaseOrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalWorkflowServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalWorkflowServiceTest {

    private lateinit var workflowDataSource: FakeVendorPortalWorkflowDataSource
    private lateinit var poDataSource: FakeVendorPurchaseOrderDataSource
    private lateinit var workflowRepo: VendorPortalWorkflowRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var service: VendorPortalWorkflowServiceImpl

    private val testVendor = Vendor(
        vendorId = "VND-001",
        projectId = "PRJ-001",
        vendorCode = "VN-001",
        vendorName = "Apex Steel Ltd",
        vendorCategory = VendorCategory.RAW_MATERIALS,
        status = VendorStatus.ACTIVE
    )

    @Before
    fun setup() {
        workflowDataSource = FakeVendorPortalWorkflowDataSource()
        poDataSource = FakeVendorPurchaseOrderDataSource()
        val vendorDs = FakeVendorDataSource()

        workflowRepo = VendorPortalWorkflowRepositoryImpl(workflowDataSource)
        poRepo = VendorPurchaseOrderRepositoryImpl(poDataSource)
        vendorRepo = VendorRepositoryImpl(vendorDs)

        runBlocking {
            vendorRepo.createVendor(testVendor)
        }

        service = VendorPortalWorkflowServiceImpl(
            repository = workflowRepo,
            vendorRepository = vendorRepo,
            purchaseOrderRepository = poRepo
        )
    }

    @Test
    fun testSynchronizeWorkflowFromPo() = runBlocking {
        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "PO-2026-999",
                projectId = "PRJ-001",
                vendorId = "VND-001",
                orderNumber = "PO-2026-999",
                orderDate = 1756291200000L,
                requestedBy = "buyer_01",
                subtotal = Money(BigDecimal("150000.00")),
                totalAmount = Money(BigDecimal("150000.00")),
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "BDT"
            )
        )

        val syncRes = service.synchronizeWorkflowFromModule12("TENANT-001", "PRJ-001", "VND-001", "PO-2026-999")
        assertTrue(syncRes is DomainResult.Success)
        val wf = (syncRes as DomainResult.Success).data
        assertEquals("VND-001", wf.vendorId)
        assertEquals(VendorWorkflowStage.AWARDED, wf.currentStage)
        assertEquals("Commercial Order #PO-2026-999", wf.workflowTitle)
    }

    @Test
    fun testRecordAndResolveException() = runBlocking {
        val wf = VendorWorkflowItem(
            workflowId = "WF-101",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            correlationId = "PO-101",
            workflowTitle = "Commercial Order #PO-101",
            currentStage = VendorWorkflowStage.PRODUCTION_IN_PROGRESS,
            status = VendorWorkflowStatus.ACTIVE
        )
        workflowRepo.saveWorkflow(wf)

        val recordRes = service.recordWorkflowException(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            workflowId = "WF-101",
            category = "MATERIAL_SHORTAGE",
            severity = VendorWorkflowPriority.HIGH,
            title = "Delay in raw material batch",
            description = "Raw steel batch shipment delayed by 2 days.",
            actorId = "vendor_rep"
        )
        assertTrue(recordRes is DomainResult.Success)
        val exc = (recordRes as DomainResult.Success).data
        assertEquals(VendorWorkflowExceptionStatus.OPEN, exc.status)

        val resolveRes = service.resolveWorkflowException(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            exceptionId = exc.exceptionId,
            resolutionNotes = "Alternative supplier sourced; schedule recovered.",
            actorId = "vendor_rep"
        )
        assertTrue(resolveRes is DomainResult.Success)
        assertEquals(VendorWorkflowExceptionStatus.RESOLVED, (resolveRes as DomainResult.Success).data.status)
    }

    @Test
    fun testHubSummaryAggregation() = runBlocking {
        workflowRepo.saveWorkflow(
            VendorWorkflowItem(
                workflowId = "WF-ACTIVE-1",
                tenantId = "TENANT-001",
                projectId = "PRJ-001",
                vendorId = "VND-001",
                correlationId = "PO-01",
                workflowTitle = "Order 1",
                currentStage = VendorWorkflowStage.PRODUCTION_IN_PROGRESS,
                status = VendorWorkflowStatus.ACTIVE
            )
        )

        val summaryRes = service.getWorkflowHubSummary("TENANT-001", "PRJ-001", "VND-001")
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(1, summary.totalActiveWorkflows)
        assertEquals(0, summary.completedWorkflows)
    }
}
