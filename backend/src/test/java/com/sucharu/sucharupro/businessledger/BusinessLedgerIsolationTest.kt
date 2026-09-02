package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.businessledger.PostBusinessAdjustmentCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerIsolationTest {

    private lateinit var dataSource: FakeBusinessLedgerDataSource
    private lateinit var repository: BusinessLedgerRepositoryImpl

    private val tenantA = "TENANT-AAA"
    private val projectA = "PRJ-AAA"

    private val tenantB = "TENANT-BBB"
    private val projectB = "PRJ-BBB"

    private val managerProjectA = AuthenticatedPrincipal(
        userId = "USER-A",
        projectId = projectA,
        username = "mgrA",
        role = UserRole.MANAGER
    )

    private val managerProjectB = AuthenticatedPrincipal(
        userId = "USER-B",
        projectId = projectB,
        username = "mgrB",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        dataSource = FakeBusinessLedgerDataSource()
        repository = BusinessLedgerRepositoryImpl(dataSource)
    }

    @Test
    fun testTenantAndProjectIsolation() = runBlocking {
        val serviceA = BusinessLedgerServiceImpl(repository = repository, defaultTenantId = tenantA)
        val serviceB = BusinessLedgerServiceImpl(repository = repository, defaultTenantId = tenantB)

        // Create adjustment in Project A
        val postARes = serviceA.postBusinessAdjustment(
            managerProjectA,
            PostBusinessAdjustmentCommand(
                amount = BigDecimal("5000.0000"),
                isDebit = true,
                accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                description = "Project A Operational Cost"
            )
        )
        assertTrue(postARes is DomainResult.Success)
        val postingA = (postARes as DomainResult.Success).data

        // Verify Project B cannot see Project A posting by ID
        val getByB = serviceB.getPostingById(managerProjectB, postingA.id)
        assertTrue(getByB is DomainResult.Error)
        assertTrue((getByB as DomainResult.Error).message.contains("not found"))

        // Verify Project B listing is empty
        val listB = serviceB.listPostings(managerProjectB, BusinessLedgerPostingFilter())
        assertTrue((listB as DomainResult.Success).data.isEmpty())

        // Verify Project B balance is 0
        val balB = serviceB.getBalanceSummary(managerProjectB)
        assertEquals(BigDecimal("0.0000"), (balB as DomainResult.Success).data.totalDebit)
    }
}
