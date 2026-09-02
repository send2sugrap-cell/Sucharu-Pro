package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSettlementServiceTest {

    private lateinit var portalDataSource: FakeVendorPortalSettlementDataSource
    private lateinit var portalRepository: VendorPortalSettlementRepositoryImpl
    private lateinit var service: VendorPortalSettlementServiceImpl

    private val tenantId = "PRJ-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-TEST-001"

    @Before
    fun setup() {
        portalDataSource = FakeVendorPortalSettlementDataSource()
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
                    vendorCode = "VND-CODE-01",
                    vendorName = "Apex Print Solutions",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            // Seed canonical settlement
            canonicalSettlementService.createSettlement(
                vendorId = vendorId,
                settlementNumber = "SETTL-TEST-101",
                totalAmount = Money(BigDecimal("85000.00")),
                settlementMethod = SettlementMethod.BANK_TRANSFER,
                referenceNumber = "TRX-998877",
                notes = "Initial test settlement",
                allocations = listOf(
                    VendorSettlementAllocation(
                        allocationId = "ALLOC-01",
                        settlementId = "SETTL-TEST-101",
                        payableId = "PAY-01",
                        invoiceId = "INV-01",
                        allocatedAmount = Money(BigDecimal("85000.00")),
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
    fun testListSettlementsAndProjection() = runBlocking {
        val result = service.listSettlements(tenantId, projectId, vendorId)
        assertTrue(result is DomainResult.Success)
        val list = (result as DomainResult.Success).data
        assertEquals(1, list.size)
        val s = list.first()
        assertEquals("SETTL-TEST-101", s.settlementNumber)
        assertEquals("****8877", s.maskedPaymentReference)
        assertEquals(VendorPortalSettlementViewStatus.VIEW_ONLY, s.acknowledgementStatus)
    }

    @Test
    fun testAcknowledgeSettlementIdempotently() = runBlocking {
        val settlementsRes = service.listSettlements(tenantId, projectId, vendorId)
        val settlement = (settlementsRes as DomainResult.Success).data.first()

        val ackRes1 = service.acknowledgeSettlement(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            settlementId = settlement.settlementId,
            status = VendorPortalSettlementViewStatus.ACKNOWLEDGED,
            idempotencyKey = "KEY-ACK-001",
            actorId = "vendor_user"
        )
        assertTrue(ackRes1 is DomainResult.Success)
        val ack1 = (ackRes1 as DomainResult.Success).data
        assertEquals(VendorPortalSettlementViewStatus.ACKNOWLEDGED, ack1.status)

        // Repeat with same idempotency key
        val ackRes2 = service.acknowledgeSettlement(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            settlementId = settlement.settlementId,
            status = VendorPortalSettlementViewStatus.ACKNOWLEDGED,
            idempotencyKey = "KEY-ACK-001",
            actorId = "vendor_user"
        )
        assertTrue(ackRes2 is DomainResult.Success)
        val ack2 = (ackRes2 as DomainResult.Success).data
        assertEquals(ack1.acknowledgementId, ack2.acknowledgementId)
    }

    @Test
    fun testReconciliationLifecycle() = runBlocking {
        val createRes = service.createReconciliationQuery(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            subject = "Rate dispute on item #5",
            claimedAmount = Money(BigDecimal("12000.00")),
            systemAmount = Money(BigDecimal("10000.00")),
            notes = "Special rush fee was omitted",
            actorId = "vendor_user"
        )
        assertTrue(createRes is DomainResult.Success)
        val c = (createRes as DomainResult.Success).data
        assertEquals(0, BigDecimal("2000.00").compareTo(c.varianceAmount.amount))
        assertEquals(VendorPortalReconciliationCaseStatus.OPEN, c.status)

        val respondRes = service.respondToReconciliation(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            caseId = c.caseId,
            remarks = "Attaching proof of approval from production manager",
            actorId = "vendor_user",
            actorRole = "VENDOR"
        )
        assertTrue(respondRes is DomainResult.Success)
        val updatedCase = (respondRes as DomainResult.Success).data
        assertEquals(VendorPortalReconciliationCaseStatus.INTERNAL_RESPONSE_REQUIRED, updatedCase.status)
        assertEquals(2, updatedCase.events.size)
    }

    @Test
    fun testFinancialDisputeCreationAndResponse() = runBlocking {
        val createRes = service.createFinancialDispute(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            category = "WITHHOLDING_TAX_OVERCHARGE",
            priority = "HIGH",
            disputedAmount = Money(BigDecimal("4500.00")),
            proposedResolutionAmount = Money(BigDecimal("2000.00")),
            reason = "Exempt tax certificate submitted earlier",
            actorId = "vendor_admin"
        )
        assertTrue(createRes is DomainResult.Success)
        val d = (createRes as DomainResult.Success).data
        assertEquals(VendorPortalFinancialDisputeStatus.SUBMITTED, d.status)
        assertEquals(BigDecimal("4500.00"), d.disputedAmount.amount)

        val respondRes = service.respondToFinancialDispute(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            disputeId = d.disputeId,
            remarks = "Updated tax document reference #TX-882 attached",
            actorId = "vendor_admin",
            actorRole = "VENDOR"
        )
        assertTrue(respondRes is DomainResult.Success)
        val updatedD = (respondRes as DomainResult.Success).data
        assertEquals(VendorPortalFinancialDisputeStatus.UNDER_REVIEW, updatedD.status)
    }

    @Test
    fun testFinancialCollaborationThreadsAndMessages() = runBlocking {
        val threadRes = portalRepository.saveThread(
            VendorPortalFinancialThread(
                threadId = "TH-001",
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                contextType = "SETTLEMENT",
                contextId = "SETTL-TEST-101",
                subject = "Tax deduction inquiry",
                createdBy = "vendor_user"
            )
        )
        assertTrue(threadRes is DomainResult.Success)

        val msgRes = service.postMessage(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            threadId = "TH-001",
            content = "Could you please clarify line #2 deduction?",
            actorId = "vendor_user",
            actorRole = "VENDOR"
        )
        assertTrue(msgRes is DomainResult.Success)

        val messagesRes = service.listMessages(tenantId, projectId, vendorId, "TH-001")
        assertTrue(messagesRes is DomainResult.Success)
        assertEquals(1, (messagesRes as DomainResult.Success).data.size)
    }

    @Test
    fun testGetFinancialWorkspace() = runBlocking {
        val wsRes = service.getFinancialWorkspace(tenantId, projectId, vendorId)
        assertTrue(wsRes is DomainResult.Success)
        val ws = (wsRes as DomainResult.Success).data
        assertEquals(1, ws.settlementOverview.size)
        assertNotNull(ws.analytics)
    }
}
