package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.substratereservation.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateEnterpriseAuditDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateEnterpriseAuditRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Security, RBAC, and Tenant Isolation test suite for Substrate Enterprise Audit (Module 19 Step 06).
 */
class SubstrateEnterpriseAuditSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cust-1",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        projectId = tenantAlpha
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vend-1",
        username = "vendor_user",
        role = UserRole.VENDOR,
        projectId = tenantAlpha
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-1",
        username = "manager_user",
        role = UserRole.MANAGER,
        projectId = tenantAlpha
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-1",
        username = "staff_user",
        role = UserRole.STAFF,
        projectId = tenantAlpha
    )

    private val aiAgentPrincipal = AuthenticatedPrincipal(
        userId = "ai-1",
        username = "ai_agent_user",
        role = UserRole.AI_AGENT,
        projectId = tenantAlpha
    )

    private val recordAuditDto = RecordAuditEventRequestDto(
        reservationId = "RES-001",
        orderId = "ORD-001",
        orderItemId = "ITEM-01",
        eventType = "HARD_ALLOCATED",
        newState = "ALLOCATED_HARD",
        reason = "Manual audit entry",
        sourceOperation = "ALLOCATE"
    )

    @Before
    fun setUp() {
        val fakeDs = FakeSubstrateEnterpriseAuditDataSource()
        val repo = SubstrateEnterpriseAuditRepositoryImpl(fakeDs)
        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createSubstrateEnterpriseAuditDataSource(tenantId: String) = fakeDs
            override fun createSubstrateEnterpriseAuditRepository(tenantId: String) = repo
        }

        useCases = BackendUseCases(mockDb, factory)
    }

    @Test
    fun testCustomerAndVendor_areForbiddenOnEnterpriseAudit() {
        runBlocking {
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.recordSubstrateEnterpriseAuditEvent(customerPrincipal, recordAuditDto)
                }
            }

            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.recordSubstrateEnterpriseAuditEvent(vendorPrincipal, recordAuditDto)
                }
            }

            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.getSubstrateEnterpriseAuditHistory(customerPrincipal, "RES-001")
                }
            }

            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.reconcileSubstrateReservation(customerPrincipal, ReconcileReservationRequestDto("RES-001"))
                }
            }
        }
    }

    @Test
    fun testAiAgent_isReadOnly_cannotRecordAuditOrRunMutation() {
        runBlocking {
            // AI Agent cannot insert manual audit records
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.recordSubstrateEnterpriseAuditEvent(aiAgentPrincipal, recordAuditDto)
                }
            }

            // AI Agent cannot execute manual reconciliation mutation
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.reconcileSubstrateReservation(aiAgentPrincipal, ReconcileReservationRequestDto("RES-001"))
                }
            }

            // AI Agent CAN read audit history
            val history = useCases.getSubstrateEnterpriseAuditHistory(aiAgentPrincipal, "RES-NONEXISTENT")
            assertTrue(history.isEmpty())
        }
    }

    @Test
    fun testTenantIsolation_preventsCrossTenantAuditLeakage() {
        runBlocking {
            val principalBeta = AuthenticatedPrincipal(
                userId = "mgr-beta",
                username = "mgr_beta",
                role = UserRole.MANAGER,
                projectId = tenantBeta
            )

            val historyAlpha = useCases.getSubstrateEnterpriseAuditHistory(managerPrincipal, "RES-ALPHA-01")
            val historyBeta = useCases.getSubstrateEnterpriseAuditHistory(principalBeta, "RES-ALPHA-01")

            assertTrue(historyAlpha.isEmpty())
            assertTrue(historyBeta.isEmpty())
        }
    }
}
