package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialActivityEventType
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalSettlementViewStatus
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSettlementAuditTest {

    private lateinit var portalRepository: VendorPortalSettlementRepositoryImpl
    private lateinit var service: VendorPortalSettlementServiceImpl

    private val tenantId = "PRJ-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-AUDIT-01"
    private val settlementId = "SETTL-AUDIT-01"

    @Before
    fun setup() {
        val portalDataSource = FakeVendorPortalSettlementDataSource()
        portalRepository = VendorPortalSettlementRepositoryImpl(portalDataSource)

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
                    vendorCode = "VND-AUDIT",
                    vendorName = "Audit Vendor",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            canonicalSettlementService.createSettlement(
                vendorId = vendorId,
                settlementNumber = "SETTL-AUDIT-01",
                totalAmount = Money(BigDecimal("25000.00")),
                settlementMethod = SettlementMethod.BANK_TRANSFER,
                allocations = listOf(
                    VendorSettlementAllocation(
                        allocationId = "ALLOC-AUD-01",
                        settlementId = "SETTL-AUDIT-01",
                        payableId = "PAY-01",
                        invoiceId = "INV-01",
                        allocatedAmount = Money(BigDecimal("25000.00")),
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
    fun testAuditTrailGeneratedForFinancialActions() = runBlocking {
        val settlementsRes = service.listSettlements(tenantId, projectId, vendorId)
        val realSettlementId = (settlementsRes as DomainResult.Success).data.first().settlementId

        // 1. Acknowledge settlement
        service.acknowledgeSettlement(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            settlementId = realSettlementId,
            status = VendorPortalSettlementViewStatus.ACKNOWLEDGED,
            idempotencyKey = "IDEM-AUD-1",
            actorId = "vendor_auditor"
        )

        // 2. Open dispute
        service.createFinancialDispute(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            category = "DEDUCTION",
            disputedAmount = Money(BigDecimal("500.00")),
            reason = "Minor deduction issue",
            actorId = "vendor_auditor"
        )

        val activitiesRes = service.listFinancialActivity(tenantId, projectId, vendorId)
        assertTrue(activitiesRes is DomainResult.Success)
        val activities = (activitiesRes as DomainResult.Success).data
        assertEquals(2, activities.size)

        val eventTypes = activities.map { it.eventType }
        assertTrue(eventTypes.contains(VendorPortalFinancialActivityEventType.SETTLEMENT_ACKNOWLEDGED))
        assertTrue(eventTypes.contains(VendorPortalFinancialActivityEventType.DISPUTE_OPENED))
    }
}
