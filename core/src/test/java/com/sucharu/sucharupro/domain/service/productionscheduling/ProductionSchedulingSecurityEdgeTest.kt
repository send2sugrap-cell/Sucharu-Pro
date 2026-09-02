package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.productionscheduling.CreateProductionScheduleRequestDto
import com.sucharu.sucharupro.data.api.model.productionscheduling.SupersedeProductionScheduleRequestDto
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.data.datasource.productionscheduling.FakeProductionSchedulingDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionscheduling.ProductionSchedulingRepositoryImpl
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecutionStatus
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionSchedulingSecurityEdgeTest {

    private lateinit var schedulingDataSource: FakeProductionSchedulingDataSource
    private lateinit var executionDataSource: FakeProductionExecutionDataSource
    private lateinit var repositoryFactory: PostgresRepositoryFactory
    private lateinit var useCases: BackendUseCases

    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        username = "admin@sucharu.com",
        role = UserRole.ADMIN,
        projectId = tenantAlpha
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-1",
        username = "manager@sucharu.com",
        role = UserRole.MANAGER,
        projectId = tenantAlpha
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-1",
        username = "staff@sucharu.com",
        role = UserRole.STAFF,
        projectId = tenantAlpha
    )

    private val aiAgentPrincipal = AuthenticatedPrincipal(
        userId = "ai-1",
        username = "ai-agent",
        role = UserRole.AI_AGENT,
        projectId = tenantAlpha
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cust-1",
        username = "cust@client.com",
        role = UserRole.CUSTOMER,
        projectId = tenantAlpha
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "ven-1",
        username = "vendor@supplier.com",
        role = UserRole.VENDOR,
        projectId = tenantAlpha
    )

    private val betaAdminPrincipal = AuthenticatedPrincipal(
        userId = "admin-beta",
        username = "admin@beta.com",
        role = UserRole.ADMIN,
        projectId = tenantBeta
    )

    private fun createJob(tenant: String, jobId: String): ProductionJobExecution {
        val spec = ProductionJobSpecification(
            specId = "SPEC-$jobId",
            jobTitle = "Security Job",
            productType = "PRINT",
            orderedQuantity = 500L,
            plannedQuantity = 550L,
            finishedWidthMm = BigDecimal("210.0000"),
            finishedHeightMm = BigDecimal("297.0000"),
            substrateType = "ART_PAPER",
            substrateGsm = 150,
            substrateBrand = null,
            parentSheetWidthMm = BigDecimal("640.0000"),
            parentSheetHeightMm = BigDecimal("900.0000"),
            pressSheetWidthMm = BigDecimal("640.0000"),
            pressSheetHeightMm = BigDecimal("450.0000"),
            impositionUps = 2,
            printingMethod = "OFFSET",
            colorsFront = 4,
            colorsBack = 4,
            specFingerprint = "SPEC-FP-$jobId"
        )

        return ProductionJobExecution(
            executionJobId = jobId,
            tenantId = tenant,
            projectId = tenant,
            orderId = "ORD-$jobId",
            orderNumber = "SO-$jobId",
            orderItemId = "ITEM-$jobId",
            customerId = "CUST-001",
            quotationId = null,
            quotationVersionNumber = null,
            commercialCommitmentId = null,
            planningId = "PLAN-$jobId",
            planningVersion = 1,
            title = "Security Test Job",
            priority = OrderPriority.NORMAL,
            status = ProductionJobExecutionStatus.READY,
            specification = spec,
            plannedQuantity = BigDecimal("550.0000"),
            workOrders = listOf(
                ProductionWorkOrder(
                    workOrderId = "WO-$jobId-1",
                    executionJobId = jobId,
                    tenantId = tenant,
                    sequenceNumber = 1,
                    stageType = ProductionStageType.DESIGN,
                    operationCode = "DSN",
                    operationName = "Design",
                    targetWorkCenter = "PREPRESS_DESK",
                    plannedQuantity = BigDecimal("550.0000")
                )
            ),
            jobFingerprint = "FP-$jobId",
            integrityHash = "HASH-$jobId",
            createdAt = 1700000000000L,
            createdBy = "tester",
            updatedAt = 1700000000000L
        )
    }

    @Before
    fun setup() {
        schedulingDataSource = FakeProductionSchedulingDataSource()
        executionDataSource = FakeProductionExecutionDataSource()

        val mockDb = MockPostgresEventDatabase()

        repositoryFactory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createProductionSchedulingDataSource(tenantId: String) = schedulingDataSource
            override fun createProductionSchedulingRepository(tenantId: String) = ProductionSchedulingRepositoryImpl(schedulingDataSource)
            override fun createProductionExecutionDataSource(tenantId: String) = executionDataSource
            override fun createProductionExecutionRepository(tenantId: String) = ProductionExecutionRepositoryImpl(executionDataSource)
        }

        useCases = BackendUseCases(mockDb, repositoryFactory)
    }

    @Test
    fun testManagerAndAdminCanCreateAndApproveSchedule() {
        runBlocking {
            val job = createJob(tenantAlpha, "JOB-SEC-01")
            executionDataSource.saveJobExecution(job)

            val sched = useCases.createProductionSchedule(
                principal = managerPrincipal,
                executionJobId = job.executionJobId,
                reqDto = CreateProductionScheduleRequestDto()
            )
            assertNotNull(sched)

            val approved = useCases.approveProductionSchedule(
                principal = adminPrincipal,
                scheduleId = sched.scheduleId
            )
            assertEquals("APPROVED", approved.status)
        }
    }

    @Test
    fun testStaffCannotCreateOrApproveSchedule() {
        runBlocking {
            val job = createJob(tenantAlpha, "JOB-SEC-02")
            executionDataSource.saveJobExecution(job)

            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.createProductionSchedule(
                        principal = staffPrincipal,
                        executionJobId = job.executionJobId,
                        reqDto = CreateProductionScheduleRequestDto()
                    )
                }
            }
        }
    }

    @Test
    fun testCustomerAndVendorDeniedFromScheduling() {
        runBlocking {
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.listProductionSchedules(customerPrincipal)
                }
            }

            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.listProductionSchedules(vendorPrincipal)
                }
            }
        }
    }

    @Test
    fun testAiAgentCanInspectHandoffContract() {
        runBlocking {
            val job = createJob(tenantAlpha, "JOB-SEC-03")
            executionDataSource.saveJobExecution(job)

            val sched = useCases.createProductionSchedule(
                principal = managerPrincipal,
                executionJobId = job.executionJobId,
                reqDto = CreateProductionScheduleRequestDto()
            )

            val handoff = useCases.exportProductionSchedulingHandoff(
                principal = aiAgentPrincipal,
                scheduleId = sched.scheduleId
            )
            assertNotNull(handoff)
            assertEquals(sched.scheduleId, handoff.scheduleId)
        }
    }

    @Test
    fun testTenantIsolationPreventsCrossTenantAccess() {
        runBlocking {
            val jobAlpha = createJob(tenantAlpha, "JOB-ALPHA")
            executionDataSource.saveJobExecution(jobAlpha)

            val schedAlpha = useCases.createProductionSchedule(
                principal = adminPrincipal,
                executionJobId = jobAlpha.executionJobId,
                reqDto = CreateProductionScheduleRequestDto()
            )

            // Beta admin in Tenant Beta cannot find Tenant Alpha's schedule
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    useCases.getProductionScheduleById(
                        principal = betaAdminPrincipal,
                        scheduleId = schedAlpha.scheduleId
                    )
                }
            }
        }
    }
}
