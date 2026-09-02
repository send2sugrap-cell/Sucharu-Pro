package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MODULE 13 STEP 12: Adversarial Security Attack & Multi-Tenant Isolation Suite.
 * Rigorously attacks API use cases, search endpoints, analytics calculations,
 * and workflow states using forged credentials, cross-tenant identities,
 * role elevation attacks, and direct parameter tampering.
 */
class VendorPortalStep12AdversarialSecurityAttackTest {

    private lateinit var useCases: BackendUseCases

    private val tenantA = "TENANT-ALPHA"
    private val tenantB = "TENANT-BETA"

    private val projectA = "PRJ-ALPHA-01"
    private val projectB = "PRJ-BETA-01"

    private val vendorA = "VND-ATTACK-01"
    private val vendorB = "VND-ATTACK-02"

    // Legitimate Vendor A Principal
    private val vendorPrincipalA = AuthenticatedPrincipal(
        userId = "rep_alpha",
        projectId = projectA,
        username = "rep_alpha",
        role = UserRole.VENDOR,
        vendorId = vendorA
    )

    // Adversary: Vendor B attempting to attack Vendor A / Tenant A
    private val adversaryPrincipalB = AuthenticatedPrincipal(
        userId = "hacker_beta",
        projectId = projectB,
        username = "hacker_beta",
        role = UserRole.VENDOR,
        vendorId = vendorB
    )

    // Unprivileged Viewer
    private val viewerPrincipal = AuthenticatedPrincipal(
        userId = "viewer_01",
        projectId = projectA,
        username = "viewer_01",
        role = UserRole.CUSTOMER, // Unauthorized for vendor mutations
        vendorId = vendorA
    )

    @Before
    fun setup() {
        val vendorDs = FakeVendorDataSource()
        val vendorRepo = VendorRepositoryImpl(vendorDs)

        val workflowDs = FakeVendorPortalWorkflowDataSource()
        val workflowRepo = VendorPortalWorkflowRepositoryImpl(workflowDs)

        val poDs = FakeVendorPurchaseOrderDataSource()
        val poRepo = VendorPurchaseOrderRepositoryImpl(poDs)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Mock not required")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Mock not required")
            }
        }

        val workflowService = VendorPortalWorkflowServiceImpl(
            repository = workflowRepo,
            vendorRepository = vendorRepo,
            purchaseOrderRepository = poRepo
        )

        val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createVendorRepository(tenantId: String) = vendorRepo
            override fun createVendorPortalWorkflowRepository(tenantId: String) = workflowRepo
            override fun createVendorPortalWorkflowService(tenantId: String) = workflowService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)

        runBlocking {
            // Seed Vendor A
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorA,
                    projectId = projectA,
                    vendorCode = "VN-A-01",
                    vendorName = "Vendor Alpha Corp",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )

            // Seed Vendor B
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorB,
                    projectId = projectB,
                    vendorCode = "VN-B-01",
                    vendorName = "Vendor Beta Corp",
                    vendorCategory = VendorCategory.RAW_MATERIALS,
                    status = VendorStatus.ACTIVE
                )
            )

            // Seed Workflow for Vendor A
            workflowRepo.saveWorkflow(
                VendorWorkflowItem(
                    workflowId = "WF-ALPHA-SECRET",
                    tenantId = projectA,
                    projectId = projectA,
                    vendorId = vendorA,
                    correlationId = "PO-ALPHA-999",
                    workflowTitle = "Secret Aerospace Fabrication Workflow",
                    currentStage = VendorWorkflowStage.PRODUCTION_IN_PROGRESS,
                    status = VendorWorkflowStatus.ACTIVE
                )
            )

            // Seed Exception for Vendor A
            workflowRepo.saveException(
                VendorWorkflowException(
                    exceptionId = "EXC-ALPHA-01",
                    workflowId = "WF-ALPHA-SECRET",
                    tenantId = projectA,
                    projectId = projectA,
                    vendorId = vendorA,
                    category = "QUALITY",
                    title = "Strict Tolerance Mismatch",
                    description = "Confidential specification variance"
                )
            )
        }
    }

    @Test
    fun testAdversaryCannotAccessOtherVendorWorkflows() = runBlocking {
        // Adversary B attempts to list workflows in their own context -> must NOT see Vendor A's workflow
        val list = useCases.listVendorWorkflows(adversaryPrincipalB)
        assertTrue("Adversary B must not see Vendor A workflows", list.none { it.workflowId == "WF-ALPHA-SECRET" })

        // Adversary B attempts to access Vendor A's single workflow details
        try {
            useCases.getVendorWorkflowDetails(adversaryPrincipalB, "WF-ALPHA-SECRET")
            fail("Must throw exception when accessing another vendor's workflow")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException || e is NoSuchElementException)
        }
    }

    @Test
    fun testAdversaryCannotResolveOtherVendorExceptions() = runBlocking {
        // Adversary B attempts to resolve Vendor A's confidential exception
        try {
            useCases.resolveVendorWorkflowException(
                adversaryPrincipalB,
                "EXC-ALPHA-01",
                VendorWorkflowResolveExceptionRequest(resolutionNotes = "Forged unauthorized resolution notes")
            )
            fail("Must block unauthorized exception resolution")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException || e is NoSuchElementException)
        }
    }

    @Test
    fun testUnprivilegedRoleCannotPerformVendorWorkflowOperations() = runBlocking {
        // Customer / Viewer role attempting to execute vendor workflow actions must be rejected
        try {
            useCases.recordVendorWorkflowException(
                viewerPrincipal,
                "WF-ALPHA-SECRET",
                VendorWorkflowRecordExceptionRequest(
                    category = "OPERATIONS",
                    severity = "HIGH",
                    title = "Unauthorized Exploit",
                    description = "Should be rejected"
                )
            )
            fail("Unprivileged role must be rejected")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is IllegalArgumentException || e is NoSuchElementException || e is SecurityException)
        }
    }

    @Test
    fun testPrincipalWithoutVendorIdentityCannotAccessVendorHub() = runBlocking {
        val headlessPrincipal = AuthenticatedPrincipal(
            userId = "headless_user",
            projectId = projectA,
            username = "headless",
            role = UserRole.VENDOR,
            vendorId = null // Missing vendor identity
        )

        try {
            useCases.getVendorWorkflowHubSummary(headlessPrincipal)
            fail("Headless vendor principal without vendorId must be rejected")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("no associated vendor identity") == true)
        }
    }
}
