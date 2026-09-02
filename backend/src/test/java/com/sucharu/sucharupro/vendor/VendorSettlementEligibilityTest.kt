package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementService
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorSettlementEligibilityTest {

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
        }
    }

    @Test
    fun testSuspendedVendorIsBlocked() = runBlocking {
        vendorRepo.createVendor(
            Vendor(
                vendorId = "VND-SUSPENDED",
                projectId = "PRJ-01",
                vendorCode = "VC-SUSP",
                vendorName = "Suspended Vendor",
                legalName = "Suspended Vendor Ltd",
                status = VendorStatus.SUSPENDED
            )
        )

        val elRes = settlementService.evaluateEligibility("VND-SUSPENDED", null, "PRJ-01")
        assertTrue(elRes is DomainResult.Success)
        val el = (elRes as DomainResult.Success).data
        assertFalse(el.isEligible)
        assertEquals(SettlementEligibility.INELIGIBLE_VENDOR_SUSPENDED, el.status)
    }

    @Test
    fun testNonExistentVendorIsBlocked() = runBlocking {
        val elRes = settlementService.evaluateEligibility("NON-EXISTENT", null, "PRJ-01")
        assertTrue(elRes is DomainResult.Success)
        val el = (elRes as DomainResult.Success).data
        assertFalse(el.isEligible)
    }
}
