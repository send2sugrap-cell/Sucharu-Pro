package com.sucharu.sucharupro.backend.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.affiliate.wallet.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletRepository
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletService
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateWalletApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val affiliateId = "AFF-API-01"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr_01",
        projectId = projectId,
        username = "mgr_user",
        role = UserRole.MANAGER
    )

    private val guestPrincipal = AuthenticatedPrincipal(
        userId = "guest_01",
        projectId = projectId,
        username = "guest_user",
        role = UserRole.GUEST
    )

    @Before
    fun setup() = runBlocking {
        val affDs = FakeAffiliateDataSource()
        val affRepo = AffiliateRepositoryImpl(affDs)

        val walletDs = FakeAffiliateWalletDataSource()
        val walletRepo = AffiliateWalletRepositoryImpl(walletDs)
        val walletService = AffiliateWalletServiceImpl(walletRepo, affRepo)

        val ledgerDs = com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletLedgerDataSource()
        val ledgerRepo = com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletLedgerRepositoryImpl(ledgerDs)
        val ledgerService = com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletLedgerServiceImpl(walletRepo, ledgerRepo)

        // Seed Module 20 Affiliate
        affRepo.saveAffiliate(
            AffiliateProfile(
                affiliateId = affiliateId,
                tenantId = projectId,
                userId = "USER-01",
                displayName = "API Affiliate",
                affiliateCode = "AFF-API-CODE",
                status = AffiliateStatus.ACTIVE,
                affiliateType = AffiliateType.INDIVIDUAL,
                joinedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createAffiliateRepository(tenantId: String): AffiliateRepository = affRepo
            override fun createAffiliateWalletRepository(tenantId: String): AffiliateWalletRepository = walletRepo
            override fun createAffiliateWalletService(tenantId: String): AffiliateWalletService = walletService
            override fun createAffiliateWalletLedgerRepository(tenantId: String): com.sucharu.sucharupro.domain.repository.affiliate.wallet.AffiliateWalletLedgerRepository = ledgerRepo
            override fun createAffiliateWalletLedgerService(tenantId: String): com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletLedgerService = ledgerService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_getOrCreateAffiliateWallet_staff_success() = runBlocking {
        val req = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, req, customFactory)

        assertNotNull(walletResp.walletId)
        assertEquals(affiliateId, walletResp.affiliateId)
        assertEquals("BDT", walletResp.currency)
        assertEquals(AffiliateWalletStatus.ACTIVE, walletResp.status)

        // Get details
        val details = useCases.getAffiliateWalletDetails(staffPrincipal, walletResp.walletId, customFactory)
        assertNotNull(details)
        assertEquals(walletResp.walletId, details?.walletId)
    }

    @Test
    fun test02_getOrCreateAffiliateWallet_guestRole_forbidden() = runBlocking {
        val req = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        try {
            useCases.getOrCreateAffiliateWallet(guestPrincipal, req, customFactory)
            fail("Expected ForbiddenException for guest creating wallet")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test03_updateWalletStatus_manager_success() = runBlocking {
        val req = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, req, customFactory)

        val updateReq = UpdateWalletStatusRequestDto(status = AffiliateWalletStatus.SUSPENDED)
        val updatedResp = useCases.updateAffiliateWalletStatus(managerPrincipal, walletResp.walletId, updateReq, customFactory)

        assertEquals(AffiliateWalletStatus.SUSPENDED, updatedResp.status)
    }

    @Test
    fun test04_updateWalletStatus_staffRole_forbidden() = runBlocking {
        val req = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, req, customFactory)

        val updateReq = UpdateWalletStatusRequestDto(status = AffiliateWalletStatus.SUSPENDED)
        try {
            useCases.updateAffiliateWalletStatus(staffPrincipal, walletResp.walletId, updateReq, customFactory)
            fail("Expected ForbiddenException for staff updating wallet status")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test05_creditAffiliateWallet_staff_success() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(
            amount = java.math.BigDecimal("5000.00"),
            referenceId = "REF-COMM-5000",
            reason = "Affiliate commission credit"
        )
        val entryResp = useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        assertNotNull(entryResp.entryId)
        assertEquals(com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntryType.CREDIT, entryResp.entryType)
        assertEquals(java.math.BigDecimal("5000.00"), entryResp.amount)

        val balResp = useCases.getAffiliateWalletBalance(staffPrincipal, walletResp.walletId, customFactory)
        assertEquals(java.math.BigDecimal("5000.00"), balResp.currentBalance)
        assertEquals(java.math.BigDecimal("5000.00"), balResp.availableBalance)
    }

    @Test
    fun test06_debitAffiliateWallet_insufficientBalance_throwsException() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val debitReq = PostLedgerEntryRequestDto(
            amount = java.math.BigDecimal("1000.00"),
            reason = "Unauthorized overdraft debit"
        )
        try {
            useCases.debitAffiliateWallet(staffPrincipal, walletResp.walletId, debitReq, customFactory)
            fail("Expected IllegalArgumentException for debit exceeding balance")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Insufficient available balance") == true)
        }
    }

    @Test
    fun test07_reverseLedgerEntry_manager_success() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(
            amount = java.math.BigDecimal("2000.00"),
            reason = "Original credit"
        )
        val creditResp = useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        val revReq = ReverseLedgerEntryRequestDto(
            originalEntryId = creditResp.entryId,
            reason = "Reversal of invalid credit"
        )
        val revResp = useCases.reverseAffiliateWalletLedgerEntry(managerPrincipal, walletResp.walletId, revReq, customFactory)

        assertEquals(com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntryType.REVERSAL, revResp.entryType)
        assertEquals(creditResp.entryId, revResp.reversalOfEntryId)

        val balResp = useCases.getAffiliateWalletBalance(staffPrincipal, walletResp.walletId, customFactory)
        assertEquals(java.math.BigDecimal("0.00"), balResp.currentBalance)
    }

    @Test
    fun test08_reverseLedgerEntry_staffRole_forbidden() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(amount = java.math.BigDecimal("1000.00"))
        val creditResp = useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        val revReq = ReverseLedgerEntryRequestDto(originalEntryId = creditResp.entryId, reason = "Staff reversal attempt")
        try {
            useCases.reverseAffiliateWalletLedgerEntry(staffPrincipal, walletResp.walletId, revReq, customFactory)
            fail("Expected ForbiddenException when staff attempts reversal")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test09_approveAffiliatePayoutRequest_manager_success() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(amount = java.math.BigDecimal("5000.00"), reason = "Seed credit")
        useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        val submitReq = SubmitPayoutRequestDto(
            requestedAmount = java.math.BigDecimal("2000.00"),
            payoutMethodAccountName = "API Affiliate",
            payoutMethodAccountNumber = "1122334455"
        )
        val poReq = useCases.submitAffiliatePayoutRequest(staffPrincipal, walletResp.walletId, submitReq, customFactory)

        val approveReq = ApprovePayoutRequestDto(notes = "Approved by manager after verification")
        val approvedResp = useCases.approveAffiliatePayoutRequest(managerPrincipal, poReq.requestId, approveReq, customFactory)

        assertEquals(com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequestStatus.APPROVED, approvedResp.status)
        assertEquals("Approved by manager after verification", approvedResp.reviewNotes)
    }

    @Test
    fun test10_approveAffiliatePayoutRequest_selfApproval_throwsException() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(amount = java.math.BigDecimal("5000.00"), reason = "Seed credit")
        useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        val submitReq = SubmitPayoutRequestDto(
            requestedAmount = java.math.BigDecimal("2000.00"),
            payoutMethodAccountName = "API Affiliate",
            payoutMethodAccountNumber = "1122334455"
        )
        // Payout submitted by managerPrincipal ("mgr_01")
        val poReq = useCases.submitAffiliatePayoutRequest(managerPrincipal, walletResp.walletId, submitReq, customFactory)

        val approveReq = ApprovePayoutRequestDto(notes = "Manager self approval attempt")
        try {
            useCases.approveAffiliatePayoutRequest(managerPrincipal, poReq.requestId, approveReq, customFactory)
            fail("Expected IllegalArgumentException for requester self-approval")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Separation of duties violation") == true)
        }
    }

    @Test
    fun test11_rejectAffiliatePayoutRequest_manager_success() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(amount = java.math.BigDecimal("5000.00"), reason = "Seed credit")
        useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        val submitReq = SubmitPayoutRequestDto(
            requestedAmount = java.math.BigDecimal("2000.00"),
            payoutMethodAccountName = "API Affiliate",
            payoutMethodAccountNumber = "1122334455"
        )
        val poReq = useCases.submitAffiliatePayoutRequest(staffPrincipal, walletResp.walletId, submitReq, customFactory)

        val rejectReq = RejectPayoutRequestDto(rejectionReason = "Invalid account number")
        val rejectedResp = useCases.rejectAffiliatePayoutRequest(managerPrincipal, poReq.requestId, rejectReq, customFactory)

        assertEquals(com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequestStatus.REJECTED, rejectedResp.status)
        assertEquals("Invalid account number", rejectedResp.rejectionReason)

        // Verify available balance restored
        val balResp = useCases.getAffiliateWalletBalance(staffPrincipal, walletResp.walletId, customFactory)
        assertEquals(java.math.BigDecimal("5000.00"), balResp.availableBalance)
    }

    @Test
    fun test12_approveAffiliatePayoutRequest_staffRole_forbidden() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(amount = java.math.BigDecimal("5000.00"), reason = "Seed credit")
        useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        val submitReq = SubmitPayoutRequestDto(
            requestedAmount = java.math.BigDecimal("2000.00"),
            payoutMethodAccountName = "API Affiliate",
            payoutMethodAccountNumber = "1122334455"
        )
        val poReq = useCases.submitAffiliatePayoutRequest(staffPrincipal, walletResp.walletId, submitReq, customFactory)

        val approveReq = ApprovePayoutRequestDto(notes = "Staff approval attempt")
        try {
            useCases.approveAffiliatePayoutRequest(staffPrincipal, poReq.requestId, approveReq, customFactory)
            fail("Expected ForbiddenException when STAFF attempts approval")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test13_disburseAndReverseAffiliatePayout_manager_success() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(amount = java.math.BigDecimal("5000.00"), reason = "Seed credit")
        useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        val submitReq = SubmitPayoutRequestDto(
            requestedAmount = java.math.BigDecimal("2000.00"),
            payoutMethodAccountName = "API Affiliate",
            payoutMethodAccountNumber = "1122334455"
        )
        val poReq = useCases.submitAffiliatePayoutRequest(staffPrincipal, walletResp.walletId, submitReq, customFactory)
        useCases.approveAffiliatePayoutRequest(managerPrincipal, poReq.requestId, ApprovePayoutRequestDto("Approved"), customFactory)

        // Disburse payout
        val disbResp = useCases.disburseAffiliatePayout(managerPrincipal, poReq.requestId, customFactory)
        assertEquals(com.sucharu.sucharupro.domain.model.affiliate.wallet.DisbursementProviderStatus.SUCCESS, disbResp.providerStatus)

        // Balance after disbursement = 3000.00 BDT
        var balResp = useCases.getAffiliateWalletBalance(staffPrincipal, walletResp.walletId, customFactory)
        assertEquals(java.math.BigDecimal("3000.00"), balResp.availableBalance)

        // Reverse payout
        val revReq = ReversePayoutRequestDto(reversalReason = "Beneficiary bank account closed")
        val revResp = useCases.reverseAffiliatePayout(managerPrincipal, poReq.requestId, revReq, customFactory)

        assertNotNull(revResp.reversalId)
        assertEquals(java.math.BigDecimal("2000.00"), revResp.reversedAmount)

        // Balance restored to 5000.00 BDT via compensating ledger credit
        balResp = useCases.getAffiliateWalletBalance(staffPrincipal, walletResp.walletId, customFactory)
        assertEquals(java.math.BigDecimal("5000.00"), balResp.availableBalance)
    }

    @Test
    fun test14_reverseAffiliatePayout_staffRole_forbidden() = runBlocking {
        val createReq = CreateWalletRequestDto(affiliateId = affiliateId, currency = "BDT")
        val walletResp = useCases.getOrCreateAffiliateWallet(staffPrincipal, createReq, customFactory)

        val creditReq = PostLedgerEntryRequestDto(amount = java.math.BigDecimal("5000.00"), reason = "Seed credit")
        useCases.creditAffiliateWallet(staffPrincipal, walletResp.walletId, creditReq, customFactory)

        val submitReq = SubmitPayoutRequestDto(
            requestedAmount = java.math.BigDecimal("2000.00"),
            payoutMethodAccountName = "API Affiliate",
            payoutMethodAccountNumber = "1122334455"
        )
        val poReq = useCases.submitAffiliatePayoutRequest(staffPrincipal, walletResp.walletId, submitReq, customFactory)
        useCases.approveAffiliatePayoutRequest(managerPrincipal, poReq.requestId, ApprovePayoutRequestDto("Approved"), customFactory)
        useCases.disburseAffiliatePayout(managerPrincipal, poReq.requestId, customFactory)

        val revReq = ReversePayoutRequestDto(reversalReason = "Staff reversal attempt")
        try {
            useCases.reverseAffiliatePayout(staffPrincipal, poReq.requestId, revReq, customFactory)
            fail("Expected ForbiddenException when STAFF attempts reversal")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }
}
