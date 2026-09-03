package com.sucharu.sucharupro.ui.features.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProfileDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProgramDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProfileRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProgramRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.service.affiliate.AffiliateProfileServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.AffiliateProgramServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.AffiliateServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateManagementViewModelTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var fakeAffDs: FakeAffiliateDataSource
    private lateinit var fakeProgDs: FakeAffiliateProgramDataSource
    private lateinit var fakeProfileDs: FakeAffiliateProfileDataSource
    private lateinit var fakeCommDs: com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateCommunicationDataSource

    private val tenantId = "TENANT-ALPHA"
    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        username = "admin_user",
        role = UserRole.ADMIN,
        projectId = tenantId
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "usr-aff-1",
        username = "aff_user",
        role = UserRole.AFFILIATE,
        projectId = tenantId
    )

    private val fakeTx = object : TransactionManager {
        override suspend fun <T> inTransaction(
            tenantContext: com.sucharu.sucharupro.data.persistence.postgres.TenantContext,
            block: suspend (com.sucharu.sucharupro.data.persistence.postgres.TransactionContext) -> T
        ): T {
            val dummyConn = java.lang.reflect.Proxy.newProxyInstance(
                java.sql.Connection::class.java.classLoader,
                arrayOf(java.sql.Connection::class.java)
            ) { _, _, _ -> null } as java.sql.Connection

            val dummyCtx = com.sucharu.sucharupro.data.persistence.postgres.TransactionContext(
                tenantContext = tenantContext,
                sqlExecutor = com.sucharu.sucharupro.data.persistence.postgres.SqlExecutor(dummyConn),
                connection = dummyConn
            )
            return block(dummyCtx)
        }

        override suspend fun <T> inReadOnly(
            tenantContext: com.sucharu.sucharupro.data.persistence.postgres.TenantContext,
            block: suspend (com.sucharu.sucharupro.data.persistence.postgres.TransactionContext) -> T
        ): T = inTransaction(tenantContext, block)
    }

    @Before
    fun setUp() {
        fakeAffDs = FakeAffiliateDataSource()
        fakeProgDs = FakeAffiliateProgramDataSource()
        fakeProfileDs = FakeAffiliateProfileDataSource()
        fakeCommDs = com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateCommunicationDataSource()

        val affRepo = AffiliateRepositoryImpl(fakeAffDs)
        val affService = AffiliateServiceImpl(affRepo)

        val progRepo = AffiliateProgramRepositoryImpl(fakeProgDs)
        val progService = AffiliateProgramServiceImpl(progRepo, affRepo)

        val profileRepo = AffiliateProfileRepositoryImpl(fakeProfileDs)
        val profileService = AffiliateProfileServiceImpl(profileRepo, affRepo)

        val commRepo = com.sucharu.sucharupro.data.repository.affiliate.AffiliateCommunicationRepositoryImpl(fakeCommDs)
        val commService = com.sucharu.sucharupro.domain.service.affiliate.AffiliateCommunicationServiceImpl(commRepo, affRepo)

        val factory = object : PostgresRepositoryFactory(fakeTx, tenantId) {
            override fun createAffiliateDataSource(tenantId: String) = fakeAffDs
            override fun createAffiliateRepository(tenantId: String) = affRepo
            override fun createAffiliateService(tenantId: String) = affService

            override fun createAffiliateProgramDataSource(tenantId: String) = fakeProgDs
            override fun createAffiliateProgramRepository(tenantId: String) = progRepo
            override fun createAffiliateProgramService(tenantId: String) = progService

            override fun createAffiliateProfileDataSource(tenantId: String) = fakeProfileDs
            override fun createAffiliateProfileRepository(tenantId: String) = profileRepo
            override fun createAffiliateProfileService(tenantId: String) = profileService

            override fun createAffiliateCommunicationDataSource(tenantId: String) = fakeCommDs
            override fun createAffiliateCommunicationRepository(tenantId: String) = commRepo
            override fun createAffiliateCommunicationService(tenantId: String) = commService
        }

        useCases = BackendUseCases(fakeTx, factory)
    }

    private fun createViewModel(principal: AuthenticatedPrincipal): AffiliateManagementViewModel {
        return AffiliateManagementViewModel(
            useCases = useCases,
            principal = principal,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInitialStateAndTabSelection() {
        val viewModel = createViewModel(adminPrincipal)

        val state = viewModel.uiState.value
        assertEquals(AffiliateCommandTab.OVERVIEW, state.selectedTab)
        assertFalse(state.isPersonalView)

        viewModel.selectTab(AffiliateCommandTab.PROGRAMS)
        assertEquals(AffiliateCommandTab.PROGRAMS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(AffiliateCommandTab.COMMUNICATION_CENTER)
        assertEquals(AffiliateCommandTab.COMMUNICATION_CENTER, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testCreateAndActivateAffiliateThroughViewModel() {
        val viewModel = createViewModel(adminPrincipal)

        viewModel.createAffiliate(
            userId = "usr-001",
            displayName = "Top Graphics BD",
            affiliateCode = "TOP_BD",
            affiliateType = "INDIVIDUAL",
            contactPhone = "+8801711111111",
            contactEmail = "top@bd.com",
            taxIdOrGst = null,
            agreementReference = "AGR-001"
        )

        val state = viewModel.uiState.value
        assertTrue(state.successMessage?.contains("created with code") == true)
        assertEquals(1, state.affiliatesList.size)
        assertEquals("PENDING", state.affiliatesList[0].status)

        val affId = state.affiliatesList[0].affiliateId

        // Activate
        viewModel.activateAffiliate(affId, "Admin approval")

        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.successMessage?.contains("ACTIVATED") == true)
        assertEquals("ACTIVE", updatedState.selectedAffiliate?.status)
    }

    @Test
    fun testProgramAndEnrollmentLifecycleThroughViewModel() = kotlinx.coroutines.runBlocking {
        val viewModel = createViewModel(adminPrincipal)

        // 1. Create Affiliate & Activate
        viewModel.createAffiliate("u1", "Alpha Creator", "ALPHA_01", "CREATOR", null, null, null, "AGR-1")
        val affId = viewModel.uiState.value.affiliatesList[0].affiliateId
        viewModel.acceptAgreement(affId, "AGR-1")
        val currentAff = fakeAffDs.findById(tenantId, affId)!!
        fakeAffDs.saveAffiliate(currentAff.copy(verificationState = com.sucharu.sucharupro.domain.model.affiliate.VerificationState.VERIFIED))
        viewModel.activateAffiliate(affId, "Approved")

        // 2. Create Program & Activate
        viewModel.createProgram("VIP2026", "VIP Affiliate Program", "Exclusive program", System.currentTimeMillis(), null)
        val progId = viewModel.uiState.value.programsList[0].programId
        viewModel.activateProgram(progId, "Launched")

        // 3. Enroll Affiliate
        viewModel.enrollAffiliate(progId, affId, "Top tier influencer")
        val stateAfterEnroll = viewModel.uiState.value
        assertEquals(1, stateAfterEnroll.enrollmentsList.size)
        val enrId = stateAfterEnroll.enrollmentsList[0].enrollmentId

        // 4. Approve & Activate Enrollment
        viewModel.approveEnrollment(enrId)
        viewModel.activateEnrollment(enrId)

        val finalState = viewModel.uiState.value
        assertEquals("ACTIVE", finalState.selectedEnrollment?.enrollmentStatus)
    }

    @Test
    fun testOperationalProfileAndVerificationWorkflowThroughViewModel() = kotlinx.coroutines.runBlocking {
        val viewModel = createViewModel(adminPrincipal)

        // Create and activate affiliate
        viewModel.createAffiliate("u2", "Verified Partner", "VERIF_01", "BUSINESS", null, null, null, "AGR-2")
        val affId = viewModel.uiState.value.affiliatesList[0].affiliateId

        // Upsert operational profile
        viewModel.upsertOperationalProfile(
            affiliateId = affId,
            displayName = "Verified Partner Inc",
            legalName = "Verified Partner LLC",
            businessType = "BUSINESS",
            contactEmail = "partner@verified.com",
            city = "Chittagong",
            country = "Bangladesh",
            taxIdOrGst = "9988776655"
        )

        val profileState = viewModel.uiState.value
        assertNotNull(profileState.selectedOperationalProfile)
        assertTrue((profileState.selectedCompleteness?.score ?: 0) > 40)

        // Request verification
        viewModel.requestVerification(affId, "IDENTITY", "Passport KYC")
        val verifState = viewModel.uiState.value
        assertEquals(1, verifState.selectedVerifications.size)
        val verId = verifState.selectedVerifications[0].verificationId

        // Approve verification
        viewModel.approveVerification(verId, "Passport document matches identity")
        val afterApproval = viewModel.uiState.value
        assertEquals("VERIFIED", afterApproval.selectedVerifications[0].status)
    }

    @Test
    fun testCommunicationCenterWorkflowThroughViewModel() = kotlinx.coroutines.runBlocking {
        val viewModel = createViewModel(adminPrincipal)

        // Create affiliate
        viewModel.createAffiliate("u3", "Comm Partner", "COMM_01", "INDIVIDUAL", null, null, null, "AGR-3")
        val affId = viewModel.uiState.value.affiliatesList[0].affiliateId

        // Send communication
        viewModel.sendAffiliateCommunication(
            affiliateId = affId,
            communicationType = "GOVERNANCE",
            title = "Mandatory Compliance Update",
            message = "Please accept new tax governance terms."
        )

        val stateAfterSend = viewModel.uiState.value
        assertEquals(1, stateAfterSend.communicationsList.size)
        val commId = stateAfterSend.communicationsList[0].communicationId

        // Mark communication read
        viewModel.markCommunicationRead(commId)
        val stateAfterRead = viewModel.uiState.value
        assertTrue(stateAfterRead.communicationsList[0].isRead)

        // Update notification preference
        viewModel.updateNotificationPreference(
            affiliateId = affId,
            communicationType = "PROGRAM",
            inAppEnabled = true,
            pushEnabled = false,
            emailEnabled = true,
            smsEnabled = false
        )

        val finalState = viewModel.uiState.value
        assertTrue(finalState.notificationPreferences.isNotEmpty())
    }
}
