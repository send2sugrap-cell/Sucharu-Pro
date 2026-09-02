package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementService
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorCrossModuleIntegrationTest {

    private lateinit var vendorDs: FakeVendorDataSource
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var settlementDs: FakeVendorSettlementDataSource
    private lateinit var settlementRepo: VendorSettlementRepositoryImpl
    private lateinit var poDs: FakeVendorPurchaseOrderDataSource
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var deliveryDs: FakeVendorDeliveryReceiptDataSource
    private lateinit var deliveryRepo: VendorDeliveryReceiptRepositoryImpl
    private lateinit var invoiceDs: FakeVendorInvoiceDataSource
    private lateinit var invoiceRepo: VendorInvoiceRepositoryImpl
    private lateinit var qualityDs: FakeVendorQualityDataSource
    private lateinit var qualityRepo: VendorQualityRepositoryImpl
    private lateinit var perfDs: FakeVendorPerformanceDataSource
    private lateinit var perfRepo: VendorPerformanceRepositoryImpl
    private lateinit var analyticsRepo: VendorAnalyticsRepositoryImpl
    private lateinit var settlementService: VendorSettlementService

    @Before
    fun setUp() {
        runBlocking {
            vendorDs = FakeVendorDataSource()
            vendorRepo = VendorRepositoryImpl(vendorDs)
            settlementDs = FakeVendorSettlementDataSource()
            settlementRepo = VendorSettlementRepositoryImpl(settlementDs)
            poDs = FakeVendorPurchaseOrderDataSource()
            poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
            deliveryDs = FakeVendorDeliveryReceiptDataSource()
            deliveryRepo = VendorDeliveryReceiptRepositoryImpl(deliveryDs)
            invoiceDs = FakeVendorInvoiceDataSource()
            invoiceRepo = VendorInvoiceRepositoryImpl(invoiceDs)
            qualityDs = FakeVendorQualityDataSource()
            qualityRepo = VendorQualityRepositoryImpl(qualityDs)
            perfDs = FakeVendorPerformanceDataSource()
            perfRepo = VendorPerformanceRepositoryImpl(perfDs)

            analyticsRepo = VendorAnalyticsRepositoryImpl(
                vendorRepository = vendorRepo,
                poRepository = poRepo,
                deliveryRepository = deliveryRepo,
                invoiceRepository = invoiceRepo,
                qualityRepository = qualityRepo,
                performanceRepository = perfRepo,
                settlementRepository = settlementRepo
            )

            settlementService = VendorSettlementServiceImpl(
                settlementRepository = settlementRepo,
                analyticsRepository = analyticsRepo,
                vendorRepository = vendorRepo,
                invoiceRepository = invoiceRepo
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-01",
                    vendorCode = "VC-01",
                    vendorName = "Apex Supplies",
                    legalName = "Apex Supplies Ltd",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testCompleteCrossModuleFlowFromPoToSettlementAnd360() = runBlocking {
        // 1. Purchase Order
        val po = VendorPurchaseOrder(
            purchaseOrderId = "PO-01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            orderNumber = "PO-2026-001",
            status = VendorPurchaseOrderStatus.APPROVED,
            requestedBy = "USR-01",
            subtotal = Money(BigDecimal("50000.00")),
            totalAmount = Money(BigDecimal("50000.00"))
        )
        poRepo.createOrder(po)

        // 2. Invoice
        val invoice = VendorInvoice(
            invoiceId = "INV-01",
            projectId = "PRJ-01",
            tenantId = "TENANT-001",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            invoiceNumber = "INV-2026-001",
            vendorInvoiceNumber = "VINV-001",
            totalAmount = Money(BigDecimal("50000.00")),
            status = VendorInvoiceStatus.APPROVED,
            matchStatus = VendorInvoiceMatchStatus.MATCHED
        )
        invoiceRepo.createInvoice(invoice)

        // 3. Settlement
        val alloc = VendorSettlementAllocation(
            allocationId = "VSA-50000",
            settlementId = "",
            payableId = "PAY-50000",
            invoiceId = "INV-01",
            allocatedAmount = Money(BigDecimal("50000.00")),
            currency = "BDT"
        )
        val setRes = settlementService.createSettlement(
            vendorId = "VND-01",
            settlementNumber = "SET-50000",
            totalAmount = Money(BigDecimal("50000.00")),
            settlementMethod = SettlementMethod.BANK_TRANSFER,
            referenceNumber = "REF-50000",
            allocations = listOf(alloc),
            tenantId = "PRJ-01",
            projectId = "PRJ-01",
            actorId = "user_procurement"
        )
        assertTrue(setRes is DomainResult.Success)
        val settlement = (setRes as DomainResult.Success).data

        // 4. Approve & Process
        settlementService.approveSettlement(settlement.settlementId, "PRJ-01", "user_finance_head")
        settlementService.processSettlement(settlement.settlementId, "PRJ-01", "user_treasury")

        // 5. Query Vendor 360
        val v360Res = settlementService.getVendor360Summary("VND-01", "PRJ-01")
        assertTrue(v360Res is DomainResult.Success)
        val v360 = (v360Res as DomainResult.Success).data

        assertEquals(Money(BigDecimal("50000.00")), v360.financial.totalPoValue)
        assertEquals(Money(BigDecimal("50000.00")), v360.financial.totalInvoicedValue)
        assertEquals(Money(BigDecimal("50000.00")), v360.financial.totalSettledAmount)
        assertEquals(Money.ZERO, v360.financial.totalOutstandingPayable)
    }
}
