package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessledger.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerSecurityTest {

    private lateinit var dataSource: FakeBusinessLedgerDataSource
    private lateinit var repository: BusinessLedgerRepositoryImpl
    private lateinit var service: BusinessLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "USER-CUS-1",
        projectId = projectId,
        username = "customer1",
        role = UserRole.CUSTOMER
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "USER-AFF-1",
        projectId = projectId,
        username = "affiliate1",
        role = UserRole.AFFILIATE
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "USER-VEND-1",
        projectId = projectId,
        username = "vendor1",
        role = UserRole.VENDOR
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "USER-STAFF-1",
        projectId = projectId,
        username = "staff1",
        role = UserRole.STAFF
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessLedgerDataSource()
        repository = BusinessLedgerRepositoryImpl(dataSource)
        service = BusinessLedgerServiceImpl(
            repository = repository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testExternalRolesDeniedAccess() = runBlocking {
        val expCmd = PostApprovedExpenseCommand(expenseId = "EXP-101")

        // Customer denied
        val cusRes = service.postApprovedExpense(customerPrincipal, expCmd)
        assertTrue(cusRes is DomainResult.Error)
        assertTrue((cusRes as DomainResult.Error).message.contains("Access denied"))

        // Affiliate denied
        val affRes = service.postApprovedExpense(affiliatePrincipal, expCmd)
        assertTrue(affRes is DomainResult.Error)
        assertTrue((affRes as DomainResult.Error).message.contains("Access denied"))

        // Vendor denied
        val vendRes = service.postApprovedExpense(vendorPrincipal, expCmd)
        assertTrue(vendRes is DomainResult.Error)
        assertTrue((vendRes as DomainResult.Error).message.contains("Access denied"))

        // Balance query denied
        val cusBalRes = service.getBalanceSummary(customerPrincipal)
        assertTrue(cusBalRes is DomainResult.Error)
        assertTrue((cusBalRes as DomainResult.Error).message.contains("Access denied"))
    }

    @Test
    fun testStaffCannotPerformManualAdjustmentsOrReversals() = runBlocking {
        // Staff tries manual adjustment -> Denied
        val adjCmd = PostBusinessAdjustmentCommand(
            amount = BigDecimal("1000.0000"),
            isDebit = true,
            accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
            description = "Unauthorized Staff Adjustment"
        )
        val adjRes = service.postBusinessAdjustment(staffPrincipal, adjCmd)
        assertTrue(adjRes is DomainResult.Error)
        assertTrue((adjRes as DomainResult.Error).message.contains("Access denied"))

        // Pre-populate posting by manager
        val mgrAdjRes = service.postBusinessAdjustment(managerPrincipal, adjCmd.copy(description = "Manager Adjustment"))
        val posting = (mgrAdjRes as DomainResult.Success).data

        // Staff tries to reverse -> Denied
        val staffRevRes = service.reversePosting(staffPrincipal, ReversePostingCommand(postingId = posting.id, reason = "Staff attempt"))
        assertTrue(staffRevRes is DomainResult.Error)
        assertTrue((staffRevRes as DomainResult.Error).message.contains("Access denied"))

        // Manager can reverse -> Allowed
        val mgrRevRes = service.reversePosting(managerPrincipal, ReversePostingCommand(postingId = posting.id, reason = "Approved Manager Reversal"))
        assertTrue(mgrRevRes is DomainResult.Success)
    }
}
