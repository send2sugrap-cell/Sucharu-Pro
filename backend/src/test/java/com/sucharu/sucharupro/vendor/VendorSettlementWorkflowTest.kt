package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementService
import com.sucharu.sucharupro.domain.service.vendor.VendorSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorSettlementWorkflowTest {

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
    fun testSettlementLifecycleAndSeparationOfDuties() = runBlocking {
        val alloc = VendorSettlementAllocation(
            allocationId = "VSA-1",
            settlementId = "",
            payableId = "PAY-1",
            allocatedAmount = Money(BigDecimal("1000.00")),
            currency = "BDT"
        )

        // 1. Create Settlement
        val createRes = settlementService.createSettlement(
            vendorId = "VND-01",
            settlementNumber = "SET-001",
            totalAmount = Money(BigDecimal("1000.00")),
            settlementMethod = SettlementMethod.BANK_TRANSFER,
            referenceNumber = "REF-001",
            notes = "Test settlement",
            allocations = listOf(alloc),
            tenantId = "TENANT-001",
            projectId = "PRJ-01",
            actorId = "user_creator"
        )
        assertTrue(createRes is DomainResult.Success)
        val created = (createRes as DomainResult.Success).data
        assertEquals(VendorSettlementStatus.DRAFT, created.status)

        // 2. Self-approval must fail due to Separation of Duties
        val selfApproveRes = settlementService.approveSettlement(created.settlementId, "TENANT-001", "user_creator")
        assertTrue(selfApproveRes is DomainResult.Error)

        // 3. Different user can approve
        val approveRes = settlementService.approveSettlement(created.settlementId, "TENANT-001", "user_manager")
        assertTrue(approveRes is DomainResult.Success)
        val approved = (approveRes as DomainResult.Success).data
        assertEquals(VendorSettlementStatus.APPROVED, approved.status)
        assertEquals("user_manager", approved.approvedBy)

        // 4. Process Settlement
        val processRes = settlementService.processSettlement(approved.settlementId, "TENANT-001", "user_accounts", UserRole.ACCOUNTS)
        assertTrue(processRes is DomainResult.Success)
        val processed = (processRes as DomainResult.Success).data
        assertEquals(VendorSettlementStatus.SETTLED, processed.status)
        assertNotNull(processed.settledAt)
        assertNotNull(processed.paymentId)

        // 5. Reconcile Settlement
        val recRes = settlementService.reconcileSettlement(processed.settlementId, "TENANT-001", "user_auditor")
        assertTrue(recRes is DomainResult.Success)
        val rec = (recRes as DomainResult.Success).data
        assertEquals(ReconciliationStatus.MATCHED, rec.status)
        assertEquals(Money.ZERO, rec.variance)
    }
}
