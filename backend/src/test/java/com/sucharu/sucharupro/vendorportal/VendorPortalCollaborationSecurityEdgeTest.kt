package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.ReviewCompletionRequestDto
import com.sucharu.sucharupro.data.api.model.SubmitProgressRequestDto
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalCollaborationSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vendor-user-1",
        username = "vendor1",
        role = UserRole.VENDOR,
        projectId = "default-tenant",
        vendorId = "vendor-1"
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-user-1",
        username = "staff1",
        role = UserRole.STAFF,
        projectId = "default-tenant"
    )

    @Before
    fun setup() {
        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }
        val factory = PostgresRepositoryFactory(fakeTxManager)
        useCases = BackendUseCases(fakeTxManager, factory)
    }

    @Test
    fun testVendorRoleCannotReviewCompletionRequestSeparationOfDuties() = runBlocking {
        try {
            useCases.reviewVendorPortalCompletionRequest(
                vendorPrincipal,
                "wo-202",
                ReviewCompletionRequestDto(
                    approved = true,
                    reviewNotes = "Self approval attempt"
                )
            )
            fail("Expected ForbiddenException/SecurityException when vendor role attempts internal review")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is SecurityException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testStaffRoleCannotSubmitVendorProgressWithoutVendorContext() = runBlocking {
        try {
            useCases.submitVendorPortalProgress(
                staffPrincipal,
                "wo-202",
                SubmitProgressRequestDto(
                    completedQuantity = 10.0,
                    remainingQuantity = 90.0,
                    statusSummary = "Staff reporting progress"
                )
            )
            fail("Expected exception when staff submits progress without effectiveVendorId")
        } catch (e: Exception) {
            assertTrue(e is ForbiddenException || e is SecurityException || e is IllegalArgumentException)
        }
    }
}
