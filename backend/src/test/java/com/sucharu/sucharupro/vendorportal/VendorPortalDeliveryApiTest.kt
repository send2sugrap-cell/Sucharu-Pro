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
import com.sucharu.sucharupro.domain.repository.VendorPortalDeliveryRepository
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalDeliveryApiTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var receiptRepo: VendorDeliveryReceiptRepositoryImpl
    private lateinit var qualityRepo: VendorQualityRepositoryImpl
    private lateinit var deliveryRepo: VendorPortalDeliveryRepositoryImpl

    private lateinit var poService: VendorPurchaseOrderService
    private lateinit var receiptService: VendorDeliveryReceiptService
    private lateinit var qualityService: VendorQualityService
    private lateinit var deliveryService: VendorPortalDeliveryService
    private lateinit var useCases: BackendUseCases

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor-user-api-1",
        projectId = "proj-1",
        username = "vendor1",
        role = UserRole.VENDOR,
        vendorId = "vnd-deliv-api-1"
    )

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-user-api-1",
        projectId = "proj-1",
        username = "admin1",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() = runBlocking {
        val vendorDs = FakeVendorDataSource()
        val capDs = FakeVendorCapabilityDataSource()
        val rateDs = FakeVendorServiceRateDataSource()
        val poDs = FakeVendorPurchaseOrderDataSource()
        val receiptDs = FakeVendorDeliveryReceiptDataSource()
        val qualityDs = FakeVendorQualityDataSource()
        val deliveryDs = FakeVendorPortalDeliveryDataSource()

        vendorRepo = VendorRepositoryImpl(vendorDs)
        capRepo = VendorCapabilityRepositoryImpl(capDs)
        rateRepo = VendorServiceRateRepositoryImpl(rateDs)
        poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
        receiptRepo = VendorDeliveryReceiptRepositoryImpl(receiptDs)
        qualityRepo = VendorQualityRepositoryImpl(qualityDs)
        deliveryRepo = VendorPortalDeliveryRepositoryImpl(deliveryDs)

        val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
        poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)
        receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)
        qualityService = VendorQualityServiceImpl(vendorRepo, poRepo, receiptRepo, qualityRepo)
        deliveryService = VendorPortalDeliveryServiceImpl(deliveryRepo, poService, receiptService, qualityService, vendorRepo)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createVendorPortalDeliveryService(tenantId: String): VendorPortalDeliveryService = deliveryService
            override fun createVendorPortalDeliveryRepository(tenantId: String): VendorPortalDeliveryRepository = deliveryRepo
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)

        vendorRepo.createVendor(
            Vendor(
                vendorId = "vnd-deliv-api-1",
                projectId = "proj-1",
                vendorCode = "VND-DAPI",
                vendorName = "Delivery API Vendor Corp",
                vendorCategory = VendorCategory.PRINTING,
                status = VendorStatus.ACTIVE
            )
        )

        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "po-api-1",
                projectId = "proj-1",
                orderNumber = "PO-API-1",
                vendorId = "vnd-deliv-api-1",
                requestedBy = "buyer-1",
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "USD",
                subtotal = Money(BigDecimal("1000")),
                taxAmount = Money.ZERO,
                discountAmount = Money.ZERO,
                totalAmount = Money(BigDecimal("1000")),
                items = listOf(
                    VendorPurchaseOrderItem(
                        itemId = "poi-api-1",
                        purchaseOrderId = "po-api-1",
                        itemCode = "BOX-01",
                        itemDescription = "Carton Box",
                        quantity = BigDecimal("200"),
                        unitOfMeasure = UnitOfMeasure.PIECE,
                        unitRate = Money(BigDecimal("5")),
                        lineTotal = Money(BigDecimal("1000"))
                    )
                ),
                createdAt = 1700000000000L,
                createdBy = "buyer-1"
            )
        )
        Unit
    }

    @Test
    fun testDeliveryNoticeApiFlow() = runBlocking {
        val req = CreateDeliveryNoticeRequestDto(
            purchaseOrderId = "po-api-1",
            plannedDeliveryDate = 1700100000000L,
            carrierName = "Speedy Express",
            items = listOf(
                CreateDeliveryNoticeItemRequestDto(
                    purchaseOrderItemId = "poi-api-1",
                    deliveryQuantity = 100.0
                )
            )
        )

        val notice = useCases.createVendorPortalDeliveryNotice(vendorPrincipal, req)
        assertEquals("DRAFT", notice.status)
        assertEquals(1, notice.items.size)
        assertEquals(100.0, notice.items[0].deliveryQuantity, 0.001)

        val submitted = useCases.submitVendorPortalDeliveryNotice(vendorPrincipal, notice.noticeId)
        assertEquals("SUBMITTED", submitted.status)

        val fetched = useCases.getVendorPortalDeliveryNotice(vendorPrincipal, notice.noticeId)
        assertEquals("SUBMITTED", fetched.status)
    }

    @Test
    fun testQualityInspectionResponseApiFlow() = runBlocking {
        receiptService.createReceipt(
            projectId = "proj-1",
            purchaseOrderId = "po-api-1",
            items = listOf(
                VendorDeliveryReceiptItem(
                    receiptItemId = "rcpt-item-1",
                    deliveryReceiptId = "rcpt-api-1",
                    purchaseOrderId = "po-api-1",
                    purchaseOrderItemId = "poi-api-1",
                    itemDescription = "Carton Box",
                    orderedQuantity = BigDecimal("100"),
                    receivedQuantity = BigDecimal("100"),
                    acceptedQuantity = BigDecimal("80"),
                    rejectedQuantity = BigDecimal("20")
                )
            ),
            actorId = "warehouse-1"
        )

        qualityService.createInspection(
            VendorQualityInspection(
                inspectionId = "insp-api-1",
                projectId = "proj-1",
                tenantId = "proj-1",
                inspectionReference = "QI-API-1",
                purchaseOrderId = "po-api-1",
                vendorId = "vnd-deliv-api-1",
                overallResult = InspectionResult.REJECTED,
                receivedQuantity = BigDecimal("100"),
                acceptedQuantity = BigDecimal("80"),
                rejectedQuantity = BigDecimal("20"),
                conditionalQuantity = BigDecimal.ZERO,
                items = listOf(
                    VendorQualityInspectionItem(
                        inspectionItemId = "qi-item-1",
                        inspectionId = "insp-api-1",
                        purchaseOrderItemId = "poi-api-1",
                        itemDescription = "Carton Box",
                        receivedQuantity = BigDecimal("100"),
                        acceptedQuantity = BigDecimal("80"),
                        rejectedQuantity = BigDecimal("20"),
                        conditionalQuantity = BigDecimal.ZERO
                    )
                )
            ),
            "qc-inspector-1"
        )

        val inspections = useCases.listVendorPortalQualityInspections(vendorPrincipal)
        assertEquals(1, inspections.size)
        assertEquals("QI-API-1", inspections[0].inspectionNumber)

        val respReq = RespondQualityRequestDto(
            responseType = "PROPOSE_CORRECTIVE_ACTION",
            comment = "Acknowledged moisture defect",
            correctiveActionPlan = "Replacing with shrink-wrapped pallets"
        )
        val resp = useCases.respondVendorPortalQuality(vendorPrincipal, "insp-api-1", respReq)
        assertEquals("PROPOSE_CORRECTIVE_ACTION", resp.responseType)
    }
}
