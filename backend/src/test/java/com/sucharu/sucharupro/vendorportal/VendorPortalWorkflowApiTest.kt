package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalWorkflowDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.VendorPortalWorkflowRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalWorkflowRepository
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalWorkflowService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalWorkflowServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalWorkflowApiTest {

    private lateinit var useCases: BackendUseCases

    private val tenantId = "PRJ-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-API-01"

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor_rep_01",
        projectId = projectId,
        username = "vendor_rep",
        role = UserRole.VENDOR,
        vendorId = vendorId
    )

    @Before
    fun setup() {
        val workflowDs = FakeVendorPortalWorkflowDataSource()
        val workflowRepo = VendorPortalWorkflowRepositoryImpl(workflowDs)

        val vendorDs = FakeVendorDataSource()
        val vendorRepo = VendorRepositoryImpl(vendorDs)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        val step11Service = VendorPortalWorkflowServiceImpl(
            repository = workflowRepo,
            vendorRepository = vendorRepo
        )

        val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createVendorRepository(tenantId: String) = vendorRepo
            override fun createVendorPortalWorkflowRepository(tenantId: String): VendorPortalWorkflowRepository = workflowRepo
            override fun createVendorPortalWorkflowService(tenantId: String): VendorPortalWorkflowService = step11Service
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)

        runBlocking {
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VN-API-01",
                    vendorName = "API Test Workflow Partner",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )

            workflowRepo.saveWorkflow(
                VendorWorkflowItem(
                    workflowId = "WF-API-01",
                    tenantId = projectId,
                    projectId = projectId,
                    vendorId = vendorId,
                    correlationId = "PO-API-01",
                    workflowTitle = "Commercial Cycle PO-API-01",
                    currentStage = VendorWorkflowStage.AWARDED,
                    status = VendorWorkflowStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testGetWorkflowHubSummaryEndpoint() = runBlocking {
        val summary = useCases.getVendorWorkflowHubSummary(vendorPrincipal)
        assertEquals(vendorId, summary.vendorId)
        assertEquals(1, summary.totalActiveWorkflows)
    }

    @Test
    fun testListWorkflowsEndpoint() = runBlocking {
        val list = useCases.listVendorWorkflows(vendorPrincipal)
        assertEquals(1, list.size)
        assertEquals("WF-API-01", list[0].workflowId)
    }

    @Test
    fun testRecordAndResolveExceptionEndpoint() = runBlocking {
        val exc = useCases.recordVendorWorkflowException(
            vendorPrincipal,
            "WF-API-01",
            VendorWorkflowRecordExceptionRequest(
                category = "LOGISTICS",
                severity = "HIGH",
                title = "Truck delay",
                description = "Weather delay on highway."
            )
        )
        assertEquals("LOGISTICS", exc.category)
        assertEquals("OPEN", exc.status)

        val resolved = useCases.resolveVendorWorkflowException(
            vendorPrincipal,
            exc.exceptionId,
            VendorWorkflowResolveExceptionRequest(
                resolutionNotes = "Alternative route dispatched."
            )
        )
        assertEquals("RESOLVED", resolved.status)
    }
}
