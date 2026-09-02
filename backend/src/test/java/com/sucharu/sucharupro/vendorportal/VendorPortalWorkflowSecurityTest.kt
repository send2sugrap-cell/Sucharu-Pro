package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalWorkflowDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalWorkflowRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalWorkflowServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalWorkflowSecurityTest {

    private lateinit var workflowDataSource: FakeVendorPortalWorkflowDataSource
    private lateinit var workflowRepo: VendorPortalWorkflowRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var service: VendorPortalWorkflowServiceImpl

    private val testVendor1 = Vendor(
        vendorId = "VND-001",
        projectId = "PRJ-001",
        vendorCode = "VN-001",
        vendorName = "Apex Steel Ltd",
        vendorCategory = VendorCategory.RAW_MATERIALS,
        status = VendorStatus.ACTIVE
    )

    private val testVendor2 = Vendor(
        vendorId = "VND-002",
        projectId = "PRJ-001",
        vendorCode = "VN-002",
        vendorName = "Bashundhara Cement Ltd",
        vendorCategory = VendorCategory.RAW_MATERIALS,
        status = VendorStatus.ACTIVE
    )

    @Before
    fun setup() {
        workflowDataSource = FakeVendorPortalWorkflowDataSource()
        val vendorDs = FakeVendorDataSource()

        workflowRepo = VendorPortalWorkflowRepositoryImpl(workflowDataSource)
        vendorRepo = VendorRepositoryImpl(vendorDs)

        runBlocking {
            vendorRepo.createVendor(testVendor1)
            vendorRepo.createVendor(testVendor2)
        }

        service = VendorPortalWorkflowServiceImpl(
            repository = workflowRepo,
            vendorRepository = vendorRepo
        )
    }

    @Test
    fun testVendorIsolationOnWorkflows() = runBlocking {
        // Seed workflow for Vendor 1
        workflowRepo.saveWorkflow(
            VendorWorkflowItem(
                workflowId = "WF-001",
                tenantId = "TENANT-001",
                projectId = "PRJ-001",
                vendorId = "VND-001",
                correlationId = "PO-001",
                workflowTitle = "Apex Steel PO-001",
                currentStage = VendorWorkflowStage.AWARDED,
                status = VendorWorkflowStatus.ACTIVE
            )
        )

        // Seed workflow for Vendor 2
        workflowRepo.saveWorkflow(
            VendorWorkflowItem(
                workflowId = "WF-002",
                tenantId = "TENANT-001",
                projectId = "PRJ-001",
                vendorId = "VND-002",
                correlationId = "PO-002",
                workflowTitle = "Bashundhara PO-002",
                currentStage = VendorWorkflowStage.AWARDED,
                status = VendorWorkflowStatus.ACTIVE
            )
        )

        // Vendor 1 queries workflows
        val v1List = service.listWorkflows("TENANT-001", "PRJ-001", "VND-001")
        assertTrue(v1List is DomainResult.Success)
        val v1Items = (v1List as DomainResult.Success).data
        assertEquals(1, v1Items.size)
        assertEquals("WF-001", v1Items[0].workflowId)

        // Vendor 2 queries workflows
        val v2List = service.listWorkflows("TENANT-001", "PRJ-001", "VND-002")
        assertTrue(v2List is DomainResult.Success)
        val v2Items = (v2List as DomainResult.Success).data
        assertEquals(1, v2Items.size)
        assertEquals("WF-002", v2Items[0].workflowId)

        // Vendor 1 attempts to read Vendor 2 workflow directly -> Error not found / forbidden
        val crossRes = service.getWorkflowDetails("TENANT-001", "PRJ-001", "VND-001", "WF-002")
        assertTrue(crossRes is DomainResult.Error)
    }
}
