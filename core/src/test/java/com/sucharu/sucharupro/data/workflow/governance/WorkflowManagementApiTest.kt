package com.sucharu.sucharupro.data.workflow.governance

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.workflow.control.WorkflowControlPlaneService
import com.sucharu.sucharupro.data.workflow.postgres.*
import com.sucharu.sucharupro.domain.workflow.governance.CreateWorkflowDefinitionRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class WorkflowManagementApiTest {

    private lateinit var router: BackendRouter
    private lateinit var controlPlaneService: WorkflowControlPlaneService

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "usr_admin_1",
        projectId = "PRJ-TEST",
        username = "admin_user",
        role = UserRole.ADMIN,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    @Before
    fun setup() {
        val mockDb = MockPostgresEventDatabase()
        val repoFactory = PostgresRepositoryFactory(mockDb)

        controlPlaneService = WorkflowControlPlaneService(
            definitionRepository = PostgresWorkflowDefinitionRepository(mockDb),
            instanceRepository = PostgresWorkflowInstanceRepository(mockDb),
            stepExecutionRepository = PostgresWorkflowStepExecutionRepository(mockDb),
            compensationRepository = PostgresWorkflowCompensationRepository(mockDb),
            approvalRepository = PostgresWorkflowApprovalRepository(mockDb),
            idempotencyStore = PostgresWorkflowIdempotencyStore(mockDb)
        )

        val securityContext = BackendSecurityContext()
        securityContext.registerToken("token-admin", adminPrincipal)

        val useCases = BackendUseCases(mockDb, repoFactory)

        val mockConnProvider = object : PostgresConnectionProvider {
            override suspend fun acquireConnection(): Connection {
                val mockRs = Proxy.newProxyInstance(
                    ResultSet::class.java.classLoader,
                    arrayOf(ResultSet::class.java)
                ) { _, method, _ ->
                    if (method.name == "next") true
                    else if (method.name == "getInt") 1
                    else null
                } as ResultSet

                val mockStmt = Proxy.newProxyInstance(
                    Statement::class.java.classLoader,
                    arrayOf(Statement::class.java)
                ) { _, method, _ ->
                    if (method.name == "executeQuery") mockRs
                    else null
                } as Statement

                return Proxy.newProxyInstance(
                    Connection::class.java.classLoader,
                    arrayOf(Connection::class.java)
                ) { _, method, _ ->
                    if (method.name == "createStatement") mockStmt
                    else if (method.name == "isClosed") false
                    else null
                } as Connection
            }

            override suspend fun releaseConnection(connection: Connection) {}
            override fun close() {}
        }

        val healthChecker = DatabaseHealthChecker(mockConnProvider)

        router = BackendRouter(
            securityContext = securityContext,
            useCases = useCases,
            healthChecker = healthChecker,
            workflowControlPlaneService = controlPlaneService
        )
    }

    @Test
    fun testGetAndCreateWorkflowDefinitionsApi() = runBlocking {
        // 1. Create Workflow Definition via POST /api/v1/admin/workflows
        val createReq = CreateWorkflowDefinitionRequest(
            name = "Commercial Print Run",
            description = "High volume brochure production",
            category = "PRINT"
        )
        val postResponse = router.handleRequest(
            HttpRequest(
                method = "POST",
                path = "/api/v1/admin/workflows",
                headers = mapOf("Authorization" to "Bearer token-admin"),
                body = createReq
            )
        )
        assertEquals(201, postResponse.statusCode)

        // 2. List Definitions via GET /api/v1/admin/workflows
        val getResponse = router.handleRequest(
            HttpRequest(
                method = "GET",
                path = "/api/v1/admin/workflows",
                headers = mapOf("Authorization" to "Bearer token-admin")
            )
        )
        assertEquals(200, getResponse.statusCode)
        val successBody = getResponse.body as ApiSuccessResponse<*>
        val list = successBody.data as List<*>
        assertTrue(list.isNotEmpty())
    }

    @Test
    fun testWorkflowMetricsAndAuditApi() = runBlocking {
        // GET /api/v1/admin/workflow-metrics
        val metricsResp = router.handleRequest(
            HttpRequest(
                method = "GET",
                path = "/api/v1/admin/workflow-metrics",
                headers = mapOf("Authorization" to "Bearer token-admin")
            )
        )
        assertEquals(200, metricsResp.statusCode)

        // GET /api/v1/admin/workflow-audit
        val auditResp = router.handleRequest(
            HttpRequest(
                method = "GET",
                path = "/api/v1/admin/workflow-audit",
                headers = mapOf("Authorization" to "Bearer token-admin")
            )
        )
        assertEquals(200, auditResp.statusCode)
    }
}
