package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentSecurityTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val customer = AuthenticatedPrincipal(userId = "CUST-001", username = "customer_user", role = UserRole.CUSTOMER, projectId = projectId)
    private val vendor = AuthenticatedPrincipal(userId = "VEND-001", username = "vendor_user", role = UserRole.VENDOR, projectId = projectId)
    private val affiliate = AuthenticatedPrincipal(userId = "AFF-001", username = "affiliate_user", role = UserRole.AFFILIATE, projectId = projectId)
    private val guest = AuthenticatedPrincipal(userId = "GUEST-001", username = "guest_user", role = UserRole.GUEST, projectId = projectId)

    @Before
    fun setup() {
        adjDataSource = FakeBusinessFinancialAdjustmentDataSource()
        adjRepository = BusinessFinancialAdjustmentRepositoryImpl(adjDataSource)
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        ledgerService = BusinessLedgerServiceImpl(ledgerRepository, defaultTenantId = tenantId)

        service = BusinessFinancialAdjustmentServiceImpl(
            repository = adjRepository,
            ledgerService = ledgerService,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testUnauthorizedRolesCannotCreateAdjustments() = runBlocking {
        val createCmd = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-SEC-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal("-500.0000"),
            reason = "Unauthorized Attempt",
            justification = "Attempt to create adjustment without role",
            periodId = "PER-2026-08"
        )

        val custRes = service.createAdjustment(customer, createCmd)
        assertTrue(custRes is DomainResult.Error)

        val vendRes = service.createAdjustment(vendor, createCmd)
        assertTrue(vendRes is DomainResult.Error)

        val affRes = service.createAdjustment(affiliate, createCmd)
        assertTrue(affRes is DomainResult.Error)

        val guestRes = service.createAdjustment(guest, createCmd)
        assertTrue(guestRes is DomainResult.Error)
    }

    @Test
    fun testUnauthorizedRolesCannotCreateWriteOffs() = runBlocking {
        val woCmd = CreateWriteOffCommand(
            writeOffNumber = "WO-SEC-001",
            sourceType = AdjustmentSourceType.CUSTOMER_INVOICE,
            sourceId = "INV-101",
            writeOffType = BusinessFinancialWriteOffType.BAD_DEBT,
            eligibleBalance = BigDecimal("1000.0000"),
            amount = BigDecimal("1000.0000"),
            reason = "Unauthorized write-off attempt",
            justification = "Attempt by customer to write off balance",
            periodId = "PER-2026-08"
        )

        val custRes = service.createWriteOff(customer, woCmd)
        assertTrue(custRes is DomainResult.Error)
    }
}
