package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalSettlementDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalSettlementRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSettlementIsolationTest {

    private lateinit var dataSource: FakeVendorPortalSettlementDataSource
    private lateinit var repository: VendorPortalSettlementRepositoryImpl

    @Before
    fun setup() {
        dataSource = FakeVendorPortalSettlementDataSource()
        repository = VendorPortalSettlementRepositoryImpl(dataSource)
    }

    @Test
    fun testTenantAndProjectIsolationOnReconciliations() = runBlocking {
        val caseProjectA = VendorPortalReconciliationCase(
            caseId = "REC-PA-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-A",
            vendorId = "VND-100",
            caseNumber = "REC-PA-01",
            subject = "Project A inquiry",
            claimedAmount = Money(BigDecimal("5000.00")),
            systemAmount = Money(BigDecimal("4000.00")),
            varianceAmount = Money(BigDecimal("1000.00")),
            createdBy = "vendor_rep"
        )

        val caseProjectB = VendorPortalReconciliationCase(
            caseId = "REC-PB-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-B",
            vendorId = "VND-100",
            caseNumber = "REC-PB-01",
            subject = "Project B inquiry",
            claimedAmount = Money(BigDecimal("9000.00")),
            systemAmount = Money(BigDecimal("8000.00")),
            varianceAmount = Money(BigDecimal("1000.00")),
            createdBy = "vendor_rep"
        )

        repository.saveReconciliationCase(caseProjectA)
        repository.saveReconciliationCase(caseProjectB)

        // Querying for PRJ-A must return only Project A cases
        val resA = repository.listReconciliationCases("TENANT-001", "PRJ-A", "VND-100")
        assertTrue(resA is DomainResult.Success)
        val listA = (resA as DomainResult.Success).data
        assertEquals(1, listA.size)
        assertEquals("REC-PA-01", listA.first().caseId)

        // Direct lookup of Project B case under Project A context must return null
        val crossLookup = repository.findReconciliationCaseById("TENANT-001", "PRJ-A", "VND-100", "REC-PB-01")
        assertTrue(crossLookup is DomainResult.Success)
        assertNull((crossLookup as DomainResult.Success).data)
    }

    @Test
    fun testVendorIsolationOnFinancialDisputes() = runBlocking {
        val disputeVendor1 = VendorPortalFinancialDispute(
            disputeId = "DISP-V1",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-1",
            disputeNumber = "DISP-1",
            category = "PAYMENT",
            disputedAmount = Money(BigDecimal("3000.00")),
            reason = "Vendor 1 dispute",
            createdBy = "v1_user"
        )

        val disputeVendor2 = VendorPortalFinancialDispute(
            disputeId = "DISP-V2",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-2",
            disputeNumber = "DISP-2",
            category = "PAYMENT",
            disputedAmount = Money(BigDecimal("7000.00")),
            reason = "Vendor 2 dispute",
            createdBy = "v2_user"
        )

        repository.saveFinancialDispute(disputeVendor1)
        repository.saveFinancialDispute(disputeVendor2)

        val res1 = repository.listFinancialDisputes("TENANT-001", "PRJ-001", "VND-1")
        assertTrue(res1 is DomainResult.Success)
        val list1 = (res1 as DomainResult.Success).data
        assertEquals(1, list1.size)
        assertEquals("DISP-V1", list1.first().disputeId)

        // Vendor 1 cannot see Vendor 2's dispute
        val crossLookup = repository.findFinancialDisputeById("TENANT-001", "PRJ-001", "VND-1", "DISP-V2")
        assertTrue(crossLookup is DomainResult.Success)
        assertNull((crossLookup as DomainResult.Success).data)
    }
}
