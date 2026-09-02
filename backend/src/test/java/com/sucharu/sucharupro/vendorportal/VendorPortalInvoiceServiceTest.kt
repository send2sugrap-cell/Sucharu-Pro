package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalInvoiceService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalInvoiceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalInvoiceServiceTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-001"

    private lateinit var invoiceService: VendorPortalInvoiceService
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var receiptRepo: VendorDeliveryReceiptRepositoryImpl
    private lateinit var invoiceRepo: VendorInvoiceRepositoryImpl
    private lateinit var portalInvoiceRepo: VendorPortalInvoiceRepositoryImpl

    private lateinit var canonicalInvoiceService: VendorInvoiceService
    private lateinit var canonicalPoService: VendorPurchaseOrderService
    private lateinit var canonicalSettlementService: VendorSettlementService

    @Before
    fun setup() {
        runBlocking {
            val vendorDs = FakeVendorDataSource()
            val capDs = FakeVendorCapabilityDataSource()
            val rateDs = FakeVendorServiceRateDataSource()
            val poDs = FakeVendorPurchaseOrderDataSource()
            val receiptDs = FakeVendorDeliveryReceiptDataSource()
            val invoiceDs = FakeVendorInvoiceDataSource()
            val portalInvoiceDs = FakeVendorPortalInvoiceDataSource()
            val qualityDs = FakeVendorQualityDataSource()
            val perfDs = FakeVendorPerformanceDataSource()
            val settlementDs = FakeVendorSettlementDataSource()

            vendorRepo = VendorRepositoryImpl(vendorDs)
            val capRepo = VendorCapabilityRepositoryImpl(capDs)
            val rateRepo = VendorServiceRateRepositoryImpl(rateDs)
            poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
            receiptRepo = VendorDeliveryReceiptRepositoryImpl(receiptDs)
            invoiceRepo = VendorInvoiceRepositoryImpl(invoiceDs)
            portalInvoiceRepo = VendorPortalInvoiceRepositoryImpl(portalInvoiceDs)
            val qualityRepo = VendorQualityRepositoryImpl(qualityDs)
            val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
            val settlementRepo = VendorSettlementRepositoryImpl(settlementDs)

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            canonicalPoService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)
            val receiptService = VendorDeliveryReceiptServiceImpl(vendorRepo, poRepo, receiptRepo)
            canonicalInvoiceService = VendorInvoiceServiceImpl(vendorRepo, poRepo, receiptRepo, invoiceRepo)

            val analyticsRepo = VendorAnalyticsRepositoryImpl(
                vendorRepository = vendorRepo,
                poRepository = poRepo,
                deliveryRepository = receiptRepo,
                invoiceRepository = invoiceRepo,
                qualityRepository = qualityRepo,
                performanceRepository = perfRepo,
                settlementRepository = settlementRepo
            )

            canonicalSettlementService = VendorSettlementServiceImpl(
                settlementRepository = settlementRepo,
                analyticsRepository = analyticsRepo,
                vendorRepository = vendorRepo,
                invoiceRepository = invoiceRepo
            )

            invoiceService = VendorPortalInvoiceServiceImpl(
                invoiceRepository = portalInvoiceRepo,
                vendorInvoiceService = canonicalInvoiceService,
                vendorPurchaseOrderService = canonicalPoService,
                vendorSettlementService = canonicalSettlementService,
                vendorRepository = vendorRepo
            )

            // Seed Vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "V001",
                    vendorName = "Alpha Cement Ltd",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testCreateInvoiceSubmissionAndRetrieveSubmissionList() {
        runBlocking {
            // Create PO first
            val poRes = canonicalPoService.createOrder(
                projectId = projectId,
                vendorId = vendorId,
                requestedBy = "MANAGER-1",
                expectedDeliveryDate = System.currentTimeMillis() + 86400000,
                deliveryLocation = "Site A",
                items = listOf(
                    VendorPurchaseOrderItem(
                        itemId = "PO-ITEM-1",
                        purchaseOrderId = "TEMP-PO-1",
                        itemDescription = "Cement Bags 50kg",
                        itemCode = "CEM-50",
                        quantity = BigDecimal("100"),
                        unitRate = Money(BigDecimal("10.00")),
                        lineTotal = Money(BigDecimal("1000.00"))
                    )
                )
            )
            assertTrue(poRes is DomainResult.Success)
            val po = (poRes as DomainResult.Success).data

            val subRes = invoiceService.createInvoiceSubmission(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                purchaseOrderId = po.purchaseOrderId,
                vendorInvoiceNumber = "VINV-1001",
                invoiceDate = System.currentTimeMillis(),
                items = listOf(
                    VendorPortalInvoiceSubmissionItemInput(
                        purchaseOrderItemId = po.items[0].itemId,
                        invoicedQuantity = BigDecimal("100"),
                        unitPrice = Money(BigDecimal("10.00"))
                    )
                ),
                actorId = "VENDOR_USER"
            )
            assertTrue(subRes is DomainResult.Success)
            val sub = (subRes as DomainResult.Success).data
            assertEquals("VINV-1001", sub.vendorInvoiceNumber)
            assertEquals(VendorPortalInvoiceSubmissionStatus.DRAFT, sub.status)

            // Submit Draft
            val submitRes = invoiceService.submitInvoiceSubmission(tenantId, projectId, vendorId, sub.submissionId, "VENDOR_USER")
            assertTrue(submitRes is DomainResult.Success)
            assertEquals(VendorPortalInvoiceSubmissionStatus.SUBMITTED, (submitRes as DomainResult.Success).data.status)
        }
    }

    @Test
    fun testRespondToInvoiceAndRetrieveFinancialSummary() {
        runBlocking {
            // Create canonical invoice
            val poRes = canonicalPoService.createOrder(
                projectId = projectId,
                vendorId = vendorId,
                requestedBy = "MANAGER-1",
                expectedDeliveryDate = System.currentTimeMillis() + 86400000,
                deliveryLocation = "Site B",
                items = listOf(
                    VendorPurchaseOrderItem(
                        itemId = "PO-ITEM-2",
                        purchaseOrderId = "TEMP-PO-2",
                        itemDescription = "Steel Bars",
                        quantity = BigDecimal("50"),
                        unitRate = Money(BigDecimal("100.00")),
                        lineTotal = Money(BigDecimal("5000.00"))
                    )
                )
            )
            val po = (poRes as DomainResult.Success).data

            val invRes = canonicalInvoiceService.createInvoice(
                projectId = projectId,
                vendorId = vendorId,
                purchaseOrderId = po.purchaseOrderId,
                vendorInvoiceNumber = "VINV-2002",
                items = listOf(
                    VendorInvoiceItem(
                        itemId = "INV-ITEM-2",
                        invoiceId = "INV-01",
                        purchaseOrderItemId = po.items[0].itemId,
                        description = "Steel Bars",
                        quantity = BigDecimal("50"),
                        unitPrice = Money(BigDecimal("100.00")),
                        lineTotal = Money(BigDecimal("5000.00"))
                    )
                )
            )
            assertTrue(invRes is DomainResult.Success)
            val inv = (invRes as DomainResult.Success).data

            // Respond
            val respRes = invoiceService.respondToInvoice(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                invoiceId = inv.invoiceId,
                responseType = VendorPortalInvoiceResponseType.CLARIFY_EXCEPTION,
                comment = "Clarification regarding delivery batch 1.",
                actorId = "VENDOR_USER"
            )
            assertTrue(respRes is DomainResult.Success)

            // Financial Summary
            val summaryRes = invoiceService.getFinancialSummary(tenantId, projectId, vendorId)
            assertTrue(summaryRes is DomainResult.Success)
            val summary = (summaryRes as DomainResult.Success).data
            assertEquals(1, summary.invoiceCount)
            assertEquals(Money(BigDecimal("5000.00")), summary.totalInvoiced)
        }
    }
}
