package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
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

class VendorPortalWorkflowEndToEndIntegrationTest {

    private lateinit var workflowDs: FakeVendorPortalWorkflowDataSource
    private lateinit var poDs: FakeVendorPurchaseOrderDataSource
    private lateinit var woDs: FakeVendorWorkOrderDataSource
    private lateinit var deliveryDs: FakeVendorPortalDeliveryDataSource
    private lateinit var invoiceDs: FakeVendorInvoiceDataSource
    private lateinit var settlementDs: FakeVendorSettlementDataSource
    private lateinit var vendorDs: FakeVendorDataSource

    private lateinit var workflowRepo: VendorPortalWorkflowRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var woRepo: VendorWorkOrderRepositoryImpl
    private lateinit var deliveryRepo: VendorPortalDeliveryRepositoryImpl
    private lateinit var invoiceRepo: VendorInvoiceRepositoryImpl
    private lateinit var settlementRepo: VendorSettlementRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl

    private lateinit var service: VendorPortalWorkflowServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-E2E-01"

    @Before
    fun setup() {
        workflowDs = FakeVendorPortalWorkflowDataSource()
        poDs = FakeVendorPurchaseOrderDataSource()
        woDs = FakeVendorWorkOrderDataSource()
        deliveryDs = FakeVendorPortalDeliveryDataSource()
        invoiceDs = FakeVendorInvoiceDataSource()
        settlementDs = FakeVendorSettlementDataSource()
        vendorDs = FakeVendorDataSource()

        workflowRepo = VendorPortalWorkflowRepositoryImpl(workflowDs)
        poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
        woRepo = VendorWorkOrderRepositoryImpl(woDs)
        deliveryRepo = VendorPortalDeliveryRepositoryImpl(deliveryDs)
        invoiceRepo = VendorInvoiceRepositoryImpl(invoiceDs)
        settlementRepo = VendorSettlementRepositoryImpl(settlementDs)
        vendorRepo = VendorRepositoryImpl(vendorDs)

        runBlocking {
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VN-E2E-01",
                    vendorName = "E2E Fabrication Partner",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )
        }

        service = VendorPortalWorkflowServiceImpl(
            repository = workflowRepo,
            vendorRepository = vendorRepo,
            purchaseOrderRepository = poRepo,
            workOrderRepository = woRepo,
            deliveryRepository = deliveryRepo,
            invoiceRepository = invoiceRepo,
            settlementRepository = settlementRepo
        )
    }

    @Test
    fun testCompleteCommercialWorkflowLifecycle() = runBlocking {
        // Step 1: PO Issued in Module 12
        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "PO-E2E-100",
                projectId = projectId,
                vendorId = vendorId,
                orderNumber = "PO-E2E-100",
                orderDate = 1756291200000L,
                requestedBy = "lead_buyer",
                subtotal = Money(BigDecimal("250000.00")),
                totalAmount = Money(BigDecimal("250000.00")),
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "BDT"
            )
        )

        // Step 2: Synchronize into Vendor Portal Workflow
        val syncRes = service.synchronizeWorkflowFromModule12(tenantId, projectId, vendorId, "PO-E2E-100")
        assertTrue(syncRes is DomainResult.Success)
        val wf = (syncRes as DomainResult.Success).data
        assertEquals(VendorWorkflowStage.AWARDED, wf.currentStage)
        assertEquals(VendorWorkflowStatus.ACTIVE, wf.status)

        // Step 3: Record Next Action (Acknowledge PO)
        workflowRepo.saveAction(
            VendorWorkflowNextAction(
                actionId = "ACT-ACK-01",
                workflowId = wf.workflowId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                actionType = VendorWorkflowActionType.ACKNOWLEDGE_PO,
                title = "Acknowledge Purchase Order #PO-E2E-100",
                description = "Confirm delivery schedule and rate terms.",
                requiredRole = "VENDOR_ADMIN",
                priority = VendorWorkflowPriority.HIGH
            )
        )

        val pendingActionsRes = service.getWorkflowNextActions(tenantId, projectId, vendorId, wf.workflowId)
        assertEquals(1, (pendingActionsRes as DomainResult.Success).data.size)

        // Step 4: Acknowledge Action
        val ackRes = service.acknowledgeWorkflowAction(tenantId, projectId, vendorId, wf.workflowId, "ACT-ACK-01", "vendor_admin_01")
        assertTrue(ackRes is DomainResult.Success)
        assertTrue((ackRes as DomainResult.Success).data.isCompleted)

        // Step 5: Timeline Event is Appended
        val timelineRes = service.getWorkflowTimeline(tenantId, projectId, vendorId, wf.workflowId)
        assertTrue((timelineRes as DomainResult.Success).data.any { it.eventType == "ACTION_COMPLETED" })

        // Step 6: PO Completed in Module 12
        poRepo.updateStatus(
            projectId = projectId,
            purchaseOrderId = "PO-E2E-100",
            status = VendorPurchaseOrderStatus.CLOSED,
            updatedBy = "buyer_admin"
        )

        // Step 7: Re-sync updates to COMPLETED stage
        val completeSync = service.synchronizeWorkflowFromModule12(tenantId, projectId, vendorId, "PO-E2E-100")
        assertEquals(VendorWorkflowStage.COMPLETED, (completeSync as DomainResult.Success).data.currentStage)
        assertEquals(VendorWorkflowStatus.COMPLETED, completeSync.data.status)
    }
}
