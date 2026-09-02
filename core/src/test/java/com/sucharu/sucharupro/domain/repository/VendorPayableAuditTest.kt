package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorPayableActivityType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableAuditTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `payable lifecycle produces chronological immutable audit trail`() = runBlocking {
        val createRes = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-AUDIT-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Audit trail test",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val payableId = (createRes as DomainResult.Success).data.payableId

        repository.updateDraftPayable(payableId, notes = "Updated notes", actorId = "staff-1", callerRole = UserRole.STAFF)
        repository.submitPayable(payableId, "staff-1", UserRole.STAFF)
        repository.approvePayable(payableId, "acct-1", UserRole.ACCOUNTS)
        repository.recordSettlement(payableId, Money(BigDecimal("20000.00")), "acct-1", UserRole.ACCOUNTS)

        val eventsRes = repository.getActivityEvents(payableId, UserRole.ACCOUNTS)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data

        assertEquals(5, events.size)
        assertEquals(VendorPayableActivityType.PAYABLE_CREATED, events[0].activityType)
        assertEquals(VendorPayableActivityType.PAYABLE_UPDATED, events[1].activityType)
        assertEquals(VendorPayableActivityType.PAYABLE_SUBMITTED, events[2].activityType)
        assertEquals(VendorPayableActivityType.PAYABLE_APPROVED, events[3].activityType)
        assertEquals(VendorPayableActivityType.PAYABLE_SETTLEMENT_RECORDED, events[4].activityType)
    }
}
