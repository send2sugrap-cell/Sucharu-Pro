package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalDeliveryServiceTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var receiptRepo: VendorDeliveryReceiptRepositoryImpl
    private lateinit var qualityRepo: VendorQualityRepositoryImpl

    private lateinit var poService: VendorPurchaseOrderService
    private lateinit var receiptService: VendorDeliveryReceiptService
    private lateinit var qualityService: VendorQualityService
    private lateinit var deliveryRepo: VendorPortalDeliveryRepositoryImpl
    private lateinit var service: VendorPortalDeliveryService

    private val tenantId = "tenant-deliv-svc"
    private val projectId = "proj-deliv-svc"
    private val vendorId = "vendor-deliv-1"

    @Before
    fun setup() = runBlocking {
        vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
        capRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
        rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
        poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())
        receiptRepo = VendorDeliveryReceiptRepositoryImpl(FakeVendorDeliveryReceiptDataSource())
        qualityRepo = VendorQualityRepositoryImpl(FakeVendorQualityDataSource())

        val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
        poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)
        receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)
        qualityService = VendorQualityServiceImpl(vendorRepo, poRepo, receiptRepo, qualityRepo)

        deliveryRepo = VendorPortalDeliveryRepositoryImpl(FakeVendorPortalDeliveryDataSource())
        service = VendorPortalDeliveryServiceImpl(deliveryRepo, poService, receiptService, qualityService, vendorRepo)

        // Seed Vendor
        vendorRepo.createVendor(
            Vendor(
                vendorId = vendorId,
                projectId = projectId,
                vendorCode = "VND-DELIV",
                vendorName = "Delivery Vendor Corp",
                vendorCategory = VendorCategory.RAW_MATERIALS,
                status = VendorStatus.ACTIVE
            )
        )

        // Seed Purchase Order
        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "po-201",
                projectId = projectId,
                orderNumber = "PO-201",
                vendorId = vendorId,
                requestedBy = "buyer-1",
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "USD",
                subtotal = Money(BigDecimal("5000")),
                taxAmount = Money.ZERO,
                discountAmount = Money.ZERO,
                totalAmount = Money(BigDecimal("5000")),
                items = listOf(
                    VendorPurchaseOrderItem(
                        itemId = "poi-201-1",
                        purchaseOrderId = "po-201",
                        itemCode = "MAT-01",
                        itemDescription = "Cement Bags",
                        quantity = BigDecimal("500"),
                        unitOfMeasure = UnitOfMeasure.PIECE,
                        unitRate = Money(BigDecimal("10")),
                        lineTotal = Money(BigDecimal("5000"))
                    )
                ),
                createdAt = 1700000000000L,
                createdBy = "buyer-1"
            )
        )
        Unit
    }

    @Test
    fun testCreateAndSubmitDeliveryNoticeLifecycle() = runBlocking {
        val createRes = service.createDeliveryNotice(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            purchaseOrderId = "po-201",
            plannedDeliveryDate = 1700100000000L,
            carrierName = "Swift Freight",
            trackingNumber = "SF-998877",
            vehicleNumber = "TRK-900",
            driverName = "John Doe",
            driverPhone = "+15550199",
            vendorNotes = "Palletized shipment",
            items = listOf(
                VendorPortalDeliveryNoticeItemInput(
                    purchaseOrderItemId = "poi-201-1",
                    deliveryQuantity = BigDecimal("200")
                )
            ),
            actorId = "vendor-user-1"
        )

        assertTrue("Failed to create delivery notice: $createRes", createRes is DomainResult.Success)
        val notice = (createRes as DomainResult.Success).data
        assertEquals(VendorPortalDeliveryNoticeStatus.DRAFT, notice.status)
        assertEquals(BigDecimal("200"), notice.items[0].deliveryQuantity)

        // Submit Notice
        val submitRes = service.submitDeliveryNotice(tenantId, projectId, vendorId, notice.noticeId, "vendor-user-1")
        assertTrue("Failed to submit notice", submitRes is DomainResult.Success)
        val submitted = (submitRes as DomainResult.Success).data
        assertEquals(VendorPortalDeliveryNoticeStatus.SUBMITTED, submitted.status)
    }

    @Test
    fun testReceivingSummaryCalculation() = runBlocking {
        // Create canonical receipt for 150 items
        receiptService.createReceipt(
            projectId = projectId,
            purchaseOrderId = "po-201",
            items = listOf(
                VendorDeliveryReceiptItem(
                    receiptItemId = "rcpt-item-201",
                    deliveryReceiptId = "rcpt-201",
                    purchaseOrderId = "po-201",
                    purchaseOrderItemId = "poi-201-1",
                    itemDescription = "Cement Bags",
                    orderedQuantity = BigDecimal("500"),
                    receivedQuantity = BigDecimal("150"),
                    acceptedQuantity = BigDecimal("140"),
                    rejectedQuantity = BigDecimal("10")
                )
            ),
            actorId = "warehouse-1"
        )

        val summaryRes = service.getReceivingSummary(tenantId, projectId, vendorId, "po-201")
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data

        assertEquals(BigDecimal("500"), summary.totalOrderedQuantity)
        assertEquals(BigDecimal("150"), summary.totalReceivedQuantity)
        assertEquals(BigDecimal("140"), summary.totalAcceptedQuantity)
        assertEquals(BigDecimal("10"), summary.totalRejectedQuantity)
        assertEquals(BigDecimal("360"), summary.totalRemainingQuantity)
        assertEquals(1, summary.receiptCount)
    }

    @Test
    fun testQualityCollaborationAndResponse() = runBlocking {
        val inspRes = qualityService.createInspection(
            VendorQualityInspection(
                inspectionId = "insp-201",
                projectId = projectId,
                tenantId = tenantId,
                vendorId = vendorId,
                purchaseOrderId = "po-201",
                inspectionReference = "QI-201",
                receivedQuantity = BigDecimal("150"),
                acceptedQuantity = BigDecimal("140"),
                rejectedQuantity = BigDecimal("10"),
                conditionalQuantity = BigDecimal.ZERO,
                overallResult = InspectionResult.CONDITIONAL,
                items = listOf(
                    VendorQualityInspectionItem(
                        inspectionItemId = "qi-item-201",
                        inspectionId = "insp-201",
                        purchaseOrderItemId = "poi-201-1",
                        itemDescription = "Cement Bags",
                        receivedQuantity = BigDecimal("150"),
                        acceptedQuantity = BigDecimal("140"),
                        rejectedQuantity = BigDecimal("10"),
                        conditionalQuantity = BigDecimal.ZERO,
                        defectCount = 1
                    )
                )
            ),
            "qc-inspector-1"
        )
        val insp = (inspRes as DomainResult.Success).data

        // Record a defect
        qualityService.addDefect(
            VendorDefect(
                defectId = "def-201",
                projectId = projectId,
                tenantId = tenantId,
                inspectionId = insp.inspectionId,
                inspectionItemId = insp.items[0].inspectionItemId,
                vendorId = vendorId,
                defectType = VendorDefectType.PACKAGING_DAMAGE,
                severity = VendorDefectSeverity.HIGH,
                description = "Bags torn and moisture ingress",
                quantityAffected = BigDecimal("10"),
                detectedBy = "qc-inspector-1"
            ),
            "qc-inspector-1"
        )

        // Vendor lists quality inspections
        val inspectionsRes = service.listQualityInspections(tenantId, projectId, vendorId)
        assertTrue(inspectionsRes is DomainResult.Success)
        val inspections = (inspectionsRes as DomainResult.Success).data
        assertEquals(1, inspections.size)
        assertEquals(1, inspections[0].defects.size)

        // Vendor responds with CAPA plan
        val responseRes = service.respondToQuality(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            inspectionId = insp.inspectionId,
            rejectionId = null,
            responseType = VendorPortalQualityResponseType.PROPOSE_CORRECTIVE_ACTION,
            comment = "We will upgrade wrapping film",
            correctiveActionPlan = "Switching to 150-micron weatherproof film for all future dispatches",
            promisedReplacementDate = null,
            evidenceReferences = listOf("wrap_specs.pdf"),
            actorId = "vendor-user-1"
        )
        assertTrue(responseRes is DomainResult.Success)
        val resp = (responseRes as DomainResult.Success).data
        assertEquals(VendorPortalQualityResponseType.PROPOSE_CORRECTIVE_ACTION, resp.responseType)
    }
}
