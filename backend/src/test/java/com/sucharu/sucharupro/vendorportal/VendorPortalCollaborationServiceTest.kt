package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
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

class VendorPortalCollaborationServiceTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capabilityRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var woRepo: VendorWorkOrderRepositoryImpl
    private lateinit var collabRepo: VendorCollaborationRepositoryImpl
    private lateinit var service: VendorPortalCollaborationService

    private val tenantId = "tenant-collab-test"
    private val projectId = "proj-collab-test"
    private val vendorId = "vendor-collab-1"

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
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VND-COLLAB",
                    vendorName = "Collab Vendor Corp",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "po-101",
                    projectId = projectId,
                    orderNumber = "PO-101",
                    vendorId = vendorId,
                    status = VendorPurchaseOrderStatus.ISSUED,
                    subtotal = Money(BigDecimal("10000")),
                    totalAmount = Money(BigDecimal("10000")),
                    requestedBy = "admin-1"
                )
            )

            woRepo.createWorkOrder(
                VendorWorkOrder(
                    workOrderId = "wo-202",
                    projectId = projectId,
                    workOrderNumber = "WO-202",
                    vendorId = vendorId,
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

            service = VendorPortalCollaborationServiceImpl(
                collaborationRepository = collabRepo,
                vendorPurchaseOrderService = poService,
                vendorWorkOrderService = woService,
                vendorRepository = vendorRepo
            )
        }
    }

    @Test
    fun testPoAcknowledgementFlow() = runBlocking {
        val result = service.acknowledgePurchaseOrder(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            purchaseOrderId = "po-101",
            ackType = VendorPoAcknowledgementType.ACKNOWLEDGED,
            comment = "PO Accepted by vendor",
            actorId = "vendor-user-1"
        )
        assertTrue(result is DomainResult.Success)
        val ack = (result as DomainResult.Success).data
        assertEquals("po-101", ack.purchaseOrderId)
        assertEquals(VendorPoAcknowledgementType.ACKNOWLEDGED, ack.acknowledgementType)
    }

    @Test
    fun testWorkOrderAcknowledgementFlow() = runBlocking {
        val result = service.acknowledgeWorkOrder(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            workOrderId = "wo-202",
            ackType = VendorWoAcknowledgementType.ACKNOWLEDGED,
            promisedStartDate = 1740000000L,
            promisedCompletionDate = 1750000000L,
            comment = "Job accepted",
            actorId = "vendor-user-1"
        )
        assertTrue(result is DomainResult.Success)
        val ack = (result as DomainResult.Success).data
        assertEquals("wo-202", ack.workOrderId)
    }

    @Test
    fun testSubmitProgressAndListUpdates() = runBlocking {
        val submitRes = service.submitProgress(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            workOrderId = "wo-202",
            completedQuantity = BigDecimal("25.00"),
            remainingQuantity = BigDecimal("75.00"),
            progressPercentage = 25.0,
            statusSummary = "Quarter completion",
            notes = "Ahead of plan",
            actorId = "vendor-user-1"
        )
        assertTrue(submitRes is DomainResult.Success)

        val listRes = service.listProgressUpdates(tenantId, projectId, vendorId, "wo-202")
        assertTrue(listRes is DomainResult.Success)
        val updates = (listRes as DomainResult.Success).data
        assertTrue(updates.any { it.statusSummary == "Quarter completion" })
    }

    @Test
    fun testReportAcknowledgeAndResolveBlocker() = runBlocking {
        val reportRes = service.reportBlocker(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            workOrderId = "wo-202",
            purchaseOrderId = "po-101",
            category = VendorBlockerCategory.MATERIAL_UNAVAILABLE,
            severity = VendorBlockerSeverity.HIGH,
            title = "Missing yarn",
            description = "Need 50kg yarn replenishment",
            actorId = "vendor-user-1"
        )
        assertTrue(reportRes is DomainResult.Success)
        val blocker = (reportRes as DomainResult.Success).data

        val ackRes = service.acknowledgeBlocker(tenantId, projectId, blocker.blockerId, "manager-user-1")
        assertTrue(ackRes is DomainResult.Success)
        assertEquals(VendorBlockerStatus.ACKNOWLEDGED, (ackRes as DomainResult.Success).data.status)

        val resolveRes = service.resolveBlocker(tenantId, projectId, blocker.blockerId, "Yarn batch dispatched", "manager-user-1")
        assertTrue(resolveRes is DomainResult.Success)
        assertEquals(VendorBlockerStatus.RESOLVED, (resolveRes as DomainResult.Success).data.status)
    }

    @Test
    fun testThreadCreationAndMessageFilteringForVendorViewer() = runBlocking {
        val threadRes = service.getOrCreateThread(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            resourceType = VendorThreadResourceType.WORK_ORDER,
            resourceId = "wo-202",
            title = "Collaboration on WO 202",
            actorId = "vendor-user-1"
        )
        assertTrue(threadRes is DomainResult.Success)
        val threadId = (threadRes as DomainResult.Success).data.threadId

        // Post public message
        service.postMessage(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            threadId = threadId,
            messageText = "Public update for vendor",
            isInternal = false,
            visibility = VendorMessageVisibility.VENDOR_VISIBLE,
            authorName = "Vendor Rep",
            actorId = "vendor-user-1"
        )

        // Post internal message
        service.postMessage(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            threadId = threadId,
            messageText = "Private internal evaluation",
            isInternal = true,
            visibility = VendorMessageVisibility.INTERNAL_ONLY,
            authorName = "QA Manager",
            actorId = "internal-user-1"
        )

        // As vendor viewer, only vendor visible message should appear
        val vendorMsgRes = service.listMessages(tenantId, projectId, vendorId, threadId, isVendorViewer = true)
        assertTrue(vendorMsgRes is DomainResult.Success)
        val vendorMsgs = (vendorMsgRes as DomainResult.Success).data
        assertEquals(1, vendorMsgs.size)
        assertEquals("Public update for vendor", vendorMsgs[0].message)

        // As internal viewer, both messages should appear
        val internalMsgRes = service.listMessages(tenantId, projectId, vendorId, threadId, isVendorViewer = false)
        assertTrue(internalMsgRes is DomainResult.Success)
        val internalMsgs = (internalMsgRes as DomainResult.Success).data
        assertEquals(2, internalMsgs.size)
    }

    @Test
    fun testCompletionRequestSubmissionAndReview() = runBlocking {
        val subRes = service.submitCompletionRequest(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            workOrderId = "wo-202",
            completionNotes = "Finished all 100 units.",
            finalCompletedQuantity = BigDecimal("100.00"),
            evidenceReferences = listOf("gcs://evidence/photo1.jpg"),
            actorId = "vendor-user-1"
        )
        assertTrue(subRes is DomainResult.Success)
        val cr = (subRes as DomainResult.Success).data
        assertEquals(VendorCompletionStatus.REQUESTED, cr.status)

        val revRes = service.reviewCompletionRequest(
            tenantId = tenantId,
            projectId = projectId,
            workOrderId = "wo-202",
            approved = true,
            reviewNotes = "Inspection verified and approved",
            actorId = "manager-1"
        )
        assertTrue(revRes is DomainResult.Success)
        assertEquals(VendorCompletionStatus.APPROVED, (revRes as DomainResult.Success).data.status)
    }
}
