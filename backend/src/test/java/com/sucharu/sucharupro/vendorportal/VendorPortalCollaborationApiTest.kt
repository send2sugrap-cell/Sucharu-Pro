package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorWorkOrderServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalCollaborationService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalCollaborationServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalCollaborationApiTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capabilityRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var woRepo: VendorWorkOrderRepositoryImpl
    private lateinit var collabRepo: VendorCollaborationRepositoryImpl
    private lateinit var useCases: BackendUseCases

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor-user-1",
        projectId = "proj-1",
        username = "vendor1",
        role = UserRole.VENDOR,
        vendorId = "vnd-api-1"
    )

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-user-1",
        projectId = "proj-1",
        username = "admin",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            capabilityRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
            rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
            poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())
            woRepo = VendorWorkOrderRepositoryImpl(FakeVendorWorkOrderDataSource())
            collabRepo = VendorCollaborationRepositoryImpl(FakeVendorCollaborationDataSource())

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd-api-1",
                    projectId = "proj-1",
                    vendorCode = "VND-API",
                    vendorName = "API Matrix Corp",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "po-101",
                    projectId = "proj-1",
                    orderNumber = "PO-101",
                    vendorId = "vnd-api-1",
                    status = VendorPurchaseOrderStatus.ISSUED,
                    subtotal = Money(BigDecimal("10000")),
                    totalAmount = Money(BigDecimal("10000")),
                    requestedBy = "admin-1"
                )
            )

            woRepo.createWorkOrder(
                VendorWorkOrder(
                    workOrderId = "wo-202",
                    projectId = "proj-1",
                    workOrderNumber = "WO-202",
                    vendorId = "vnd-api-1",
                    capabilityType = CapabilityType.PRINTING,
                    title = "Printing Batch 1",
                    quantity = BigDecimal("100.00"),
                    estimatedAmount = Money(BigDecimal("5000")),
                    status = VendorWorkOrderStatus.ASSIGNED
                )
            )

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capabilityRepo, rateRepo)
            val poService = VendorPurchaseOrderServiceImpl(vendorRepo, capabilityRepo, rateService, poRepo)
            val woService = VendorWorkOrderServiceImpl(vendorRepo, capabilityRepo, rateService, woRepo)

            val fakeTxManager = object : TransactionManager {
                override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
                override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
            }

            val customRepoFactory = object : PostgresRepositoryFactory(fakeTxManager) {
                override fun createVendorPortalCollaborationService(tenantId: String): VendorPortalCollaborationService {
                    return VendorPortalCollaborationServiceImpl(
                        collaborationRepository = collabRepo,
                        vendorPurchaseOrderService = poService,
                        vendorWorkOrderService = woService,
                        vendorRepository = vendorRepo
                    )
                }
            }

            useCases = BackendUseCases(fakeTxManager, customRepoFactory)
        }
    }

    @Test
    fun testAcknowledgePoViaUseCases() = runBlocking {
        val ack = useCases.acknowledgeVendorPortalPurchaseOrder(
            vendorPrincipal,
            "po-101",
            AcknowledgePurchaseOrderRequestDto(
                acknowledgementType = "ACKNOWLEDGED",
                comment = "Order accepted via API"
            )
        )
        assertNotNull(ack)
        assertEquals("ACKNOWLEDGED", ack.acknowledgementType)
    }

    @Test
    fun testSubmitProgressViaUseCases() = runBlocking {
        val update = useCases.submitVendorPortalProgress(
            vendorPrincipal,
            "wo-202",
            SubmitProgressRequestDto(
                completedQuantity = 50.0,
                remainingQuantity = 50.0,
                progressPercentage = 50.0,
                statusSummary = "Half way completed"
            )
        )
        assertNotNull(update)
        assertEquals(50.0, update.completedQuantity, 0.001)
    }

    @Test
    fun testReportBlockerViaUseCases() = runBlocking {
        val blocker = useCases.reportVendorPortalBlocker(
            vendorPrincipal,
            "wo-202",
            ReportBlockerRequestDto(
                workOrderId = "wo-202",
                category = "MATERIAL_UNAVAILABLE",
                severity = "HIGH",
                title = "Fabric batch color mismatch",
                description = "Received shade 4B instead of 4A"
            )
        )
        assertNotNull(blocker)
        assertEquals("OPEN", blocker.status)
        assertEquals("HIGH", blocker.severity)
    }

    @Test
    fun testThreadAndMessageViaUseCases() = runBlocking {
        val thread = useCases.getOrCreateVendorPortalThread(
            vendorPrincipal,
            CreateCollaborationThreadRequestDto(
                resourceType = "WORK_ORDER",
                resourceId = "wo-202",
                title = "WO-202 Discussion"
            )
        )
        assertNotNull(thread)
        assertEquals("WO-202 Discussion", thread.title)

        val msg = useCases.postVendorPortalCollaborationMessage(
            vendorPrincipal,
            thread.threadId,
            PostCollaborationMessageRequestDto(
                message = "Hello team, starting fabric inspection now."
            )
        )
        assertNotNull(msg)
        assertEquals("Hello team, starting fabric inspection now.", msg.message)

        val list = useCases.listVendorPortalCollaborationMessages(vendorPrincipal, thread.threadId)
        assertNotNull(list)
        assertEquals(1, list.size)
    }

    @Test
    fun testCompletionRequestAndReviewViaUseCases() = runBlocking {
        val compl = useCases.submitVendorPortalCompletionRequest(
            vendorPrincipal,
            "wo-202",
            SubmitCompletionRequestDto(
                completionNotes = "Finished 100 units.",
                finalCompletedQuantity = 100.0
            )
        )
        assertNotNull(compl)
        assertEquals("REQUESTED", compl.status)

        val reviewed = useCases.reviewVendorPortalCompletionRequest(
            adminPrincipal,
            "wo-202",
            ReviewCompletionRequestDto(
                approved = true,
                reviewNotes = "Signed off"
            )
        )
        assertNotNull(reviewed)
        assertEquals("APPROVED", reviewed.status)
    }
}
