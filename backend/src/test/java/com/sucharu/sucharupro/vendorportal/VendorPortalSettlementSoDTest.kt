package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialDisputeStatus
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSettlementSoDTest {

    private lateinit var canonicalSettlementService: VendorSettlementService
    private lateinit var service: VendorPortalSettlementServiceImpl

    private val tenantId = "PRJ-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-SOD-001"

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
        canonicalSettlementService = VendorSettlementServiceImpl(settlementRepo, analyticsRepo, vendorRepo, invoiceRepo)

        runBlocking {
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VND-SOD",
                    vendorName = "SoD Vendor Test",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            canonicalSettlementService.createSettlement(
                vendorId = vendorId,
                settlementNumber = "SETTL-SOD-01",
                totalAmount = Money(BigDecimal("1000.00")),
                settlementMethod = SettlementMethod.BANK_TRANSFER,
                allocations = listOf(
                    VendorSettlementAllocation(
                        allocationId = "ALLOC-SOD-01",
                        settlementId = "SETTL-SOD-01",
                        payableId = "PAY-01",
                        invoiceId = "INV-01",
                        allocatedAmount = Money(BigDecimal("1000.00")),
                        currency = "BDT"
                    )
                ),
                tenantId = tenantId,
                projectId = projectId,
                actorId = "creator_user"
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
    fun testVendorCannotSelfResolveDispute() = runBlocking {
        val createRes = service.createFinancialDispute(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            category = "DEDUCTION",
            disputedAmount = Money(BigDecimal("1000.00")),
            reason = "Unjustified deduction",
            actorId = "vendor_user"
        )
        assertTrue(createRes is DomainResult.Success)
        val dispute = (createRes as DomainResult.Success).data

        // When vendor responds, status moves to UNDER_REVIEW, never directly to RESOLVED or CLOSED
        val respondRes = service.respondToFinancialDispute(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            disputeId = dispute.disputeId,
            remarks = "We demand full resolution",
            actorId = "vendor_user",
            actorRole = "VENDOR"
        )
        assertTrue(respondRes is DomainResult.Success)
        val updated = (respondRes as DomainResult.Success).data
        assertNotEquals(VendorPortalFinancialDisputeStatus.RESOLVED, updated.status)
        assertNotEquals(VendorPortalFinancialDisputeStatus.CLOSED, updated.status)
        assertEquals(VendorPortalFinancialDisputeStatus.UNDER_REVIEW, updated.status)
    }

    @Test
    fun testVendorCannotApproveCanonicalSettlement() = runBlocking {
        // Attempting to approve settlement by the same user who created it must fail SoD check
        val settlementsRes = canonicalSettlementService.listSettlements(vendorId = vendorId, projectId = projectId, tenantId = tenantId)
        val settlement = (settlementsRes as DomainResult.Success).data.first()

        val result = canonicalSettlementService.approveSettlement(
            settlementId = settlement.settlementId,
            tenantId = tenantId,
            actorId = "creator_user"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("separation of duties", ignoreCase = true) || result.message.contains("creator", ignoreCase = true) || result.message.contains("cannot approve", ignoreCase = true))
    }
}
