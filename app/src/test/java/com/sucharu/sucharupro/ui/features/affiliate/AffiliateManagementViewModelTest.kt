package com.sucharu.sucharupro.ui.features.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProgramDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProgramRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
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

        val affRepo = AffiliateRepositoryImpl(fakeAffDs)
        val affService = AffiliateServiceImpl(affRepo)

        val progRepo = AffiliateProgramRepositoryImpl(fakeProgDs)
        val progService = AffiliateProgramServiceImpl(progRepo, affRepo)

        val factory = object : PostgresRepositoryFactory(fakeTx, tenantId) {
            override fun createAffiliateDataSource(tenantId: String) = fakeAffDs
            override fun createAffiliateRepository(tenantId: String) = affRepo
            override fun createAffiliateService(tenantId: String) = affService

            override fun createAffiliateProgramDataSource(tenantId: String) = fakeProgDs
            override fun createAffiliateProgramRepository(tenantId: String) = progRepo
            override fun createAffiliateProgramService(tenantId: String) = progService
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
        assertTrue(state.successMessage?.contains("created successfully") == true)
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

        // 2. Create Program
        viewModel.createProgram(
            programCode = "SUMMER_2026",
            programName = "Summer Program 2026",
            description = "Tiered rewards for summer print orders",
            startDate = System.currentTimeMillis() - 1000L,
            endDate = null,
            eligibilityPolicy = "STANDARD",
            termsReference = null,
            termsVersion = null,
            maxParticipants = 100,
            metadataJson = null
        )

        assertEquals(1, viewModel.uiState.value.programsList.size)
        val progId = viewModel.uiState.value.programsList[0].programId
        assertEquals("DRAFT", viewModel.uiState.value.programsList[0].status)

        // 3. Activate Program
        viewModel.activateProgram(progId, "Launched")
        assertEquals("ACTIVE", viewModel.uiState.value.selectedProgram?.status)

        // 4. Enroll Affiliate in Program
        viewModel.enrollAffiliate(progId, affId, "Initial partner enrollment", null, null, null)

        assertEquals(1, viewModel.uiState.value.enrollmentsList.size)
        val enrId = viewModel.uiState.value.enrollmentsList[0].enrollmentId
        assertEquals("PENDING", viewModel.uiState.value.enrollmentsList[0].enrollmentStatus)

        // 5. Approve & Activate Enrollment
        viewModel.approveEnrollment(enrId, "Verified")
        assertEquals("APPROVED", viewModel.uiState.value.selectedEnrollment?.enrollmentStatus)

        viewModel.activateEnrollment(enrId, "Activated")
        assertEquals("ACTIVE", viewModel.uiState.value.selectedEnrollment?.enrollmentStatus)

        // 6. Suspend Enrollment
        viewModel.suspendEnrollment(enrId, "Temporary check")
        assertEquals("SUSPENDED", viewModel.uiState.value.selectedEnrollment?.enrollmentStatus)
    }

    @Test
    fun testSearchAndFilterQuery() {
        val viewModel = createViewModel(adminPrincipal)

        viewModel.createAffiliate("u1", "Alpha Creator", "ALPHA_01", "CREATOR", null, null, null, null)
        viewModel.createAffiliate("u2", "Beta Partner", "BETA_01", "BUSINESS", null, null, null, null)

        assertEquals(2, viewModel.uiState.value.affiliatesList.size)

        viewModel.setSearchQuery("Alpha")
        assertEquals(1, viewModel.uiState.value.filteredAffiliates.size)
        assertEquals("Alpha Creator", viewModel.uiState.value.filteredAffiliates[0].displayName)

        viewModel.setSearchQuery("")
        viewModel.setTypeFilter("BUSINESS")
        assertEquals(1, viewModel.uiState.value.filteredAffiliates.size)
        assertEquals("Beta Partner", viewModel.uiState.value.filteredAffiliates[0].displayName)
    }

    @Test
    fun testPersonalViewWhenLoggedInAsAffiliate() {
        // Pre-populate an affiliate for usr-aff-1
        val adminVm = createViewModel(adminPrincipal)
        adminVm.createAffiliate("usr-aff-1", "My Affiliate Account", "MY_AFF_01", "INDIVIDUAL", null, null, null, "AGR-1")

        val affVm = createViewModel(affiliatePrincipal)

        val state = affVm.uiState.value
        assertTrue(state.isPersonalView)
        assertEquals(AffiliateCommandTab.PROFILE_ELIGIBILITY, state.selectedTab)
        assertEquals("My Affiliate Account", state.selectedAffiliate?.displayName)
    }
}
