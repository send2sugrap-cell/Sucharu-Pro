package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorFinancialAnalyticsTest {

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
    fun testFinancialSummaryCalculation() = runBlocking {
        // Create an approved invoice for 10,000
        val invoice = VendorInvoice(
            invoiceId = "INV-01",
            projectId = "PRJ-01",
            tenantId = "TENANT-001",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            invoiceNumber = "INV-2026-001",
            vendorInvoiceNumber = "VINV-001",
            totalAmount = Money(BigDecimal("10000.00")),
            status = VendorInvoiceStatus.APPROVED,
            matchStatus = VendorInvoiceMatchStatus.MATCHED
        )
        invoiceRepo.createInvoice(invoice)

        // Create a settled settlement for 4,000
        val settlement = VendorSettlement(
            settlementId = "VSET-01",
            projectId = "PRJ-01",
            tenantId = "TENANT-001",
            vendorId = "VND-01",
            settlementNumber = "SET-001",
            totalAmount = Money(BigDecimal("4000.00")),
            status = VendorSettlementStatus.SETTLED
        )
        settlementRepo.createSettlement(settlement)

        val finRes = analyticsRepo.getFinancialSummary("VND-01", "TENANT-001", "PRJ-01")
        assertTrue(finRes is DomainResult.Success)
        val fin = (finRes as DomainResult.Success).data

        assertEquals(Money(BigDecimal("10000.00")), fin.totalInvoicedValue)
        assertEquals(Money(BigDecimal("10000.00")), fin.totalApprovedPayable)
        assertEquals(Money(BigDecimal("4000.00")), fin.totalSettledAmount)
        assertEquals(Money(BigDecimal("6000.00")), fin.totalOutstandingPayable)
    }
}
