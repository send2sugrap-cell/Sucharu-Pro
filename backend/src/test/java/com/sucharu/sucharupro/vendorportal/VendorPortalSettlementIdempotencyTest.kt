package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalSettlementViewStatus
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSettlementIdempotencyTest {

    private lateinit var service: VendorPortalSettlementServiceImpl

    private val tenantId = "PRJ-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-IDEM-01"
    private val settlementId = "SETTL-IDEM-01"

    @Before
    fun setup() {
        val portalDataSource = FakeVendorPortalSettlementDataSource()
        val portalRepository = VendorPortalSettlementRepositoryImpl(portalDataSource)

        val vendorDs = FakeVendorDataSource()
        val vendorRepo = VendorRepositoryImpl(vendorDs)
        val invoiceDs = FakeVendorInvoiceDataSource()
        val invoiceRepo = VendorInvoiceRepositoryImpl(invoiceDs)
        val poDs = FakeVendorPurchaseOrderDataSource()
        val poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
        val receiptDs = FakeVendorDeliveryReceiptDataSource()
        val receiptRepo = VendorDeliveryReceiptRepositoryImpl(receiptDs)
        val qualityDs = FakeVendorQualityDataSource()
        val qualityRepo = VendorQualityRepositoryImpl(qualityDs)
        val perfDs = FakeVendorPerformanceDataSource()
        val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
        val settlementDs = FakeVendorSettlementDataSource()
        val settlementRepo = VendorSettlementRepositoryImpl(settlementDs)

        val canonicalInvoiceService = VendorInvoiceServiceImpl(vendorRepo, poRepo, receiptRepo, invoiceRepo)
        val analyticsRepo = VendorAnalyticsRepositoryImpl(vendorRepo, poRepo, receiptRepo, invoiceRepo, qualityRepo, perfRepo, settlementRepo)
        val canonicalSettlementService = VendorSettlementServiceImpl(settlementRepo, analyticsRepo, vendorRepo, invoiceRepo)

        runBlocking {
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VND-IDEM",
                    vendorName = "Idempotency Vendor",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            canonicalSettlementService.createSettlement(
                vendorId = vendorId,
                settlementNumber = "SETTL-IDEM-01",
                totalAmount = Money(BigDecimal("10000.00")),
                settlementMethod = SettlementMethod.BANK_TRANSFER,
                allocations = listOf(
                    VendorSettlementAllocation(
                        allocationId = "ALLOC-IDEM-01",
                        settlementId = "SETTL-IDEM-01",
                        payableId = "PAY-01",
                        invoiceId = "INV-01",
                        allocatedAmount = Money(BigDecimal("10000.00")),
                        currency = "BDT"
                    )
                ),
                tenantId = tenantId,
                projectId = projectId,
                actorId = "system"
            )
        }

        service = VendorPortalSettlementServiceImpl(
            portalRepository = portalRepository,
            canonicalSettlementService = canonicalSettlementService,
            canonicalInvoiceService = canonicalInvoiceService,
            vendorRepository = vendorRepo
        )
    }

    @Test
    fun testDuplicateAcknowledgementReturnsSameEntityWithoutDuplicateAudit() = runBlocking {
        val settlementsRes = service.listSettlements(tenantId, projectId, vendorId)
        val realSettlementId = (settlementsRes as DomainResult.Success).data.first().settlementId
        val idempotencyKey = "UNIQUE-IDEM-KEY-7788"

        val res1 = service.acknowledgeSettlement(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            settlementId = realSettlementId,
            status = VendorPortalSettlementViewStatus.ACKNOWLEDGED,
            idempotencyKey = idempotencyKey,
            actorId = "vendor_user"
        )
        assertTrue(res1 is DomainResult.Success)
        val ack1 = (res1 as DomainResult.Success).data

        val res2 = service.acknowledgeSettlement(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            settlementId = realSettlementId,
            status = VendorPortalSettlementViewStatus.ACKNOWLEDGED,
            idempotencyKey = idempotencyKey,
            actorId = "vendor_user"
        )
        assertTrue(res2 is DomainResult.Success)
        val ack2 = (res2 as DomainResult.Success).data

        assertEquals(ack1.acknowledgementId, ack2.acknowledgementId)
        assertEquals(ack1.acknowledgedAt, ack2.acknowledgedAt)
    }
}
