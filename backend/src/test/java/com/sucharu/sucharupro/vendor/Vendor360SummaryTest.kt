package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Vendor360SummaryTest {

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
    fun testVendor360Composition() = runBlocking {
        val v360Res = analyticsRepo.getVendor360Summary("VND-01", "PRJ-01")
        assertTrue(v360Res is DomainResult.Success)
        val v360 = (v360Res as DomainResult.Success).data

        assertEquals("VND-01", v360.vendorId)
        assertEquals("Apex Supplies", v360.vendorName)
        assertNotNull(v360.financial)
        assertNotNull(v360.operational)
        assertNotNull(v360.quality)
        assertNotNull(v360.delivery)
        assertNotNull(v360.invoice)
        assertNotNull(v360.performance)
        assertNotNull(v360.compliance)
        assertNotNull(v360.risk)
    }
}
