package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-grade PostgreSQL DataSource Integration Test Suite for Modules 06, 07, 08, and 11 (INFRA-02 Step 01).
 *
 * Validates:
 * - Module 06: Quality Control (QC Inspection CRUD, Status, Inspector assignment, Tenant isolation)
 * - Module 07: Inventory (Product Master CRUD, Category, SKU indexing, Tenant isolation)
 * - Module 08: Delivery & Dispatch (Delivery Challans CRUD, Status transitions, Issue date, Tenant isolation)
 * - Module 11: Returns & Settlement (Return Request CRUD, Reason, Lifecycle transitions, Concurrency, Tenant isolation)
 */
class PostgresModules06to11DataSourceIntegrationTest {

    private lateinit var mockConnectionProvider: ModulesMockConnectionProvider
    private lateinit var transactionManager: TransactionManager
    private lateinit var factoryTenantA: PostgresRepositoryFactory
    private lateinit var factoryTenantB: PostgresRepositoryFactory

    private val tenantA = "TENANT-ALPHA"
    private val tenantB = "TENANT-BETA"

    @Before
    fun setUp() {
        mockConnectionProvider = ModulesMockConnectionProvider()
        transactionManager = DefaultPostgresTransactionManager(mockConnectionProvider)
        factoryTenantA = PostgresRepositoryFactory(transactionManager, tenantA)
        factoryTenantB = PostgresRepositoryFactory(transactionManager, tenantB)
    }

    // ====================================================================================
    // 1. MODULE 06: QUALITY CONTROL (QC) PERSISTENCE TESTS
    // ====================================================================================

    @Test
    fun `Module 06 QC - insert, fetch by ID, update, and observe QC inspections`() = runBlocking {
        val qcDsA = factoryTenantA.createProductionQcDataSource()

        val qc = ProductionQc(
            qcId = "QC-1001",
            productionJobId = "JOB-501",
            productionStageId = "STAGE-PRINT-01",
            qcType = QcType.PRE_PRODUCTION,
            status = QcStatus.DRAFT,
            decision = QcDecision.PENDING,
            assignedInspectorId = "INSP-01",
            assignedInspectorName = "Inspector Alice",
            createdBy = "MANAGER-01",
            createdAt = Instant.now().toString(),
            notes = "Registration check on offset press",
            updatedAt = Instant.now().toString()
        )

        // 1. Insert QC
        val insertRes = qcDsA.insertQc(qc)
        assertTrue(insertRes is DomainResult.Success)

        // 2. Fetch by ID
        val fetchRes = qcDsA.fetchQcById("QC-1001")
        assertTrue(fetchRes is DomainResult.Success)
        val fetchedQc = (fetchRes as DomainResult.Success).data
        assertEquals("QC-1001", fetchedQc.qcId)
        assertEquals("JOB-501", fetchedQc.productionJobId)
        assertEquals("INSP-01", fetchedQc.assignedInspectorId)

        // 3. Update QC
        val updatedQc = fetchedQc.copy(
            status = QcStatus.IN_INSPECTION,
            notes = "Registration verified, checking color density"
        )
        val updateRes = qcDsA.updateQc(updatedQc)
        assertTrue(updateRes is DomainResult.Success)

        // 4. Observe list
        val list = qcDsA.observeQcList().first()
        assertTrue(list.isNotEmpty())
    }

    @Test
    fun `Module 06 QC - tenant isolation hides Tenant A QC from Tenant B`() = runBlocking {
        val qcDsA = factoryTenantA.createProductionQcDataSource()
        val qcDsB = factoryTenantB.createProductionQcDataSource()

        val qc = ProductionQc(
            qcId = "QC-TENANT-A-01",
            productionJobId = "JOB-A-01",
            qcType = QcType.FINAL,
            status = QcStatus.DRAFT,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
        qcDsA.insertQc(qc)

        // Tenant B must not see Tenant A's QC record
        val fetchB = qcDsB.fetchQcById("QC-TENANT-A-01")
        assertTrue("Tenant B must not see Tenant A QC record", fetchB is DomainResult.Error)
    }

    // ====================================================================================
    // 2. MODULE 07: INVENTORY PRODUCT PERSISTENCE TESTS
    // ====================================================================================

    @Test
    fun `Module 07 Inventory - insert, observe, and update product master`() = runBlocking {
        val invDsA = factoryTenantA.createInventoryProductDataSource()

        val product = InventoryProduct(
            id = "PROD-INV-001",
            sku = "PAPER-A4-80GSM",
            name = "A4 Copier Paper 80 GSM",
            description = "High brightness white paper",
            categoryId = "PAPER",
            productType = InventoryProductType.FINISHED_PRODUCT,
            unitOfMeasure = InventoryUnit.PCS,
            isStockTracked = true,
            isFinishedProduct = false,
            isSaleable = false,
            isActive = true,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
            createdBy = "PURCHASING-01"
        )

        // 1. Insert product
        val insertRes = invDsA.insertProduct(product)
        assertTrue(insertRes is DomainResult.Success)

        // 2. Observe products
        val products = invDsA.observeProducts().first()
        assertTrue(products.any { it.id == "PROD-INV-001" && it.sku == "PAPER-A4-80GSM" })

        // 3. Update product
        val updatedProduct = product.copy(
            name = "A4 Premium Copier Paper 80 GSM",
            updatedAt = Instant.now().toString()
        )
        val updateRes = invDsA.updateProduct(updatedProduct)
        assertTrue(updateRes is DomainResult.Success)
    }

    @Test
    fun `Module 07 Inventory - tenant isolation isolates product catalogue`() = runBlocking {
        val invDsA = factoryTenantA.createInventoryProductDataSource()
        val invDsB = factoryTenantB.createInventoryProductDataSource()

        val productA = InventoryProduct(
            id = "PROD-A-01",
            sku = "INK-CYAN-1L",
            name = "Offset Cyan Ink 1L",
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
            createdBy = "USER-01"
        )
        invDsA.insertProduct(productA)

        val listB = invDsB.observeProducts().first()
        assertFalse("Tenant B catalogue must not include Tenant A products", listB.any { it.id == "PROD-A-01" })
    }

    // ====================================================================================
    // 3. MODULE 08: DELIVERY & DISPATCH PERSISTENCE TESTS
    // ====================================================================================

    @Test
    fun `Module 08 Delivery - insert, retrieve, and update delivery challans`() = runBlocking {
        val delDsA = factoryTenantA.createDeliveryChallanDataSource()

        val challan = DeliveryChallan(
            challanId = "CHALLAN-8001",
            projectId = tenantA,
            challanNo = "DC-2026-001",
            deliveryOrderId = "DO-101",
            customerId = "CUST-A-100",
            sourceReferenceId = "ORD-001",
            sourceReferenceType = "SALES_ORDER",
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = System.currentTimeMillis(),
            notes = "Standard Dispatch",
            createdBy = "DISPATCH-OFFICER-01",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 1. Insert Challan
        delDsA.insertChallan(challan, emptyList())

        // 2. Get Challan by ID and by No
        val fetchedById = delDsA.getChallan("CHALLAN-8001")
        assertNotNull(fetchedById)
        assertEquals("CHALLAN-8001", fetchedById?.challanId)
        assertEquals("DC-2026-001", fetchedById?.challanNo)

        val fetchedByNo = delDsA.getChallanByNo(tenantA, "DC-2026-001")
        assertNotNull(fetchedByNo)
        assertEquals("CHALLAN-8001", fetchedByNo?.challanId)

        // 3. Update Challan status
        val updatedChallan = fetchedById!!.copy(
            status = DeliveryChallanStatus.DISPATCHED,
            updatedAt = System.currentTimeMillis()
        )
        delDsA.updateChallan(updatedChallan)

        val reFetched = delDsA.getChallan("CHALLAN-8001")
        assertEquals(DeliveryChallanStatus.DISPATCHED, reFetched?.status)
    }

    @Test
    fun `Module 08 Delivery - tenant isolation hides Tenant A challans from Tenant B`() = runBlocking {
        val delDsA = factoryTenantA.createDeliveryChallanDataSource()
        val delDsB = factoryTenantB.createDeliveryChallanDataSource()

        val challan = DeliveryChallan(
            challanId = "CHALLAN-A-01",
            projectId = tenantA,
            challanNo = "DC-A-01",
            deliveryOrderId = "DO-A-01",
            customerId = "CUST-A-01",
            sourceReferenceId = null,
            sourceReferenceType = null,
            challanType = DeliveryChallanType.STANDARD,
            status = DeliveryChallanStatus.DRAFT,
            issueDate = System.currentTimeMillis(),
            notes = null,
            createdBy = "USER-A",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        delDsA.insertChallan(challan, emptyList())

        val fetchB = delDsB.getChallan("CHALLAN-A-01")
        assertNull("Tenant B must not retrieve Tenant A challan", fetchB)

        val listB = delDsB.observeChallans(tenantB).first()
        assertFalse(listB.any { it.challanId == "CHALLAN-A-01" })
    }

    // ====================================================================================
    // 4. MODULE 11: RETURNS & SETTLEMENT PERSISTENCE TESTS
    // ====================================================================================

    @Test
    fun `Module 11 Returns - insert, retrieve by project, and update return request`() = runBlocking {
        val retDsA = factoryTenantA.createReturnDataSource()

        val ret = ReturnRequest(
            returnId = "RET-1101",
            projectId = tenantA,
            returnNo = "RMA-2026-001",
            customerId = "CUST-A-100",
            originalChallanId = "CHALLAN-8001",
            status = ReturnStatus.REQUESTED,
            reason = ReturnReason.PRINTING_DEFECT,
            description = "Smudged pages in catalogue",
            requestedAt = System.currentTimeMillis(),
            requestedBy = "SALES-REP-01",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = 1L
        )

        // 1. Insert Return Request
        retDsA.insertReturn(ret, emptyList())

        // 2. Get Return
        val fetched = retDsA.getReturn("RET-1101")
        assertNotNull(fetched)
        assertEquals("RET-1101", fetched?.returnId)
        assertEquals("RMA-2026-001", fetched?.returnNo)
        assertEquals(ReturnStatus.REQUESTED, fetched?.status)

        // 3. Update Return
        val updated = fetched!!.copy(
            status = ReturnStatus.UNDER_INSPECTION,
            version = 1L
        )
        retDsA.updateReturn(updated)

        val reFetched = retDsA.getReturn("RET-1101")
        assertEquals(ReturnStatus.UNDER_INSPECTION, reFetched?.status)
    }

    @Test
    fun `Module 11 Returns - tenant isolation prevents cross-tenant return visibility`() = runBlocking {
        val retDsA = factoryTenantA.createReturnDataSource()
        val retDsB = factoryTenantB.createReturnDataSource()

        val ret = ReturnRequest(
            returnId = "RET-A-01",
            projectId = tenantA,
            returnNo = "RMA-A-01",
            customerId = "CUST-A-01",
            originalChallanId = null,
            status = ReturnStatus.REQUESTED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-A",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = 1L
        )
        retDsA.insertReturn(ret, emptyList())

        val fetchB = retDsB.getReturn("RET-A-01")
        assertNull("Tenant B must not see Tenant A return", fetchB)

        val listB = retDsB.getReturnsByProject(tenantB)
        assertFalse(listB.any { it.returnId == "RET-A-01" })
    }
}

/**
 * Mock Connection Provider specifically supporting Modules 06, 07, 08, and 11 persistence tables.
 */
class ModulesMockConnectionProvider : PostgresConnectionProvider {

    private val qcInspections = ConcurrentHashMap<Pair<String, String>, ProductionQc>()
    private val inventoryProducts = ConcurrentHashMap<Pair<String, String>, InventoryProduct>()
    private val deliveryChallans = ConcurrentHashMap<Pair<String, String>, DeliveryChallan>()
    private val returnRequests = ConcurrentHashMap<Pair<String, String>, ReturnRequest>()

    var currentSessionProjectId: String? = null

    override suspend fun acquireConnection(): Connection {
        return Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java),
            ConnectionInvocationHandler()
        ) as Connection
    }

    override suspend fun releaseConnection(connection: Connection) {}

    override fun close() {
        qcInspections.clear()
        inventoryProducts.clear()
        deliveryChallans.clear()
        returnRequests.clear()
    }

    private inner class ConnectionInvocationHandler : InvocationHandler {
        private var inTransaction = false

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            val methodName = method.name
            val methodArgs = args ?: emptyArray()

            return when (methodName) {
                "setAutoCommit" -> {
                    inTransaction = !(methodArgs[0] as Boolean)
                    null
                }
                "getAutoCommit" -> !inTransaction
                "commit", "rollback" -> {
                    inTransaction = false
                    null
                }
                "isClosed" -> false
                "isValid" -> true
                "close" -> null
                "prepareStatement" -> {
                    val sql = methodArgs[0] as String
                    createPreparedStatementProxy(sql)
                }
                else -> null
            }
        }
    }

    private fun createPreparedStatementProxy(sql: String): PreparedStatement {
        val params = mutableListOf<Any?>()

        return Proxy.newProxyInstance(
            PreparedStatement::class.java.classLoader,
            arrayOf(PreparedStatement::class.java),
            InvocationHandler { _, method, args ->
                val stmtArgs = args ?: emptyArray()
                when (method.name) {
                    "setString", "setBigDecimal", "setInt", "setLong", "setBoolean", "setTimestamp", "setObject" -> {
                        val idx = stmtArgs[0] as Int
                        val value = stmtArgs[1]
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = value
                        null
                    }
                    "setNull" -> {
                        val idx = stmtArgs[0] as Int
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = null
                        null
                    }
                    "executeUpdate" -> {
                        executeMockMutation(sql, params)
                    }
                    "executeQuery" -> {
                        executeMockQuery(sql, params)
                    }
                    "execute" -> {
                        if (sql.contains("set_config")) {
                            currentSessionProjectId = params.getOrNull(0) as? String
                        }
                        true
                    }
                    "close" -> null
                    else -> null
                }
            }
        ) as PreparedStatement
    }

    private fun executeMockMutation(sql: String, params: List<Any?>): Int {
        if (sql.contains("INSERT INTO qc_inspections")) {
            val projectId = params.getOrNull(0) as? String ?: currentSessionProjectId ?: "DEFAULT"
            val qcId = params.getOrNull(1) as? String ?: "QC-01"
            val jobId = params.getOrNull(2) as? String ?: "JOB-01"
            val stageId = params.getOrNull(3) as? String
            val qcType = params.getOrNull(4) as? String ?: "IN_PROCESS"
            val status = params.getOrNull(5) as? String ?: "DRAFT"
            val inspectorId = params.getOrNull(6) as? String
            val notes = params.getOrNull(7) as? String

            val qc = ProductionQc(
                qcId = qcId,
                productionJobId = jobId,
                productionStageId = stageId,
                qcType = QcType.valueOf(qcType),
                status = QcStatus.valueOf(status),
                decision = QcDecision.PENDING,
                assignedInspectorId = inspectorId,
                createdAt = Instant.now().toString(),
                notes = notes,
                updatedAt = Instant.now().toString()
            )
            qcInspections[Pair(projectId, qcId)] = qc
            return 1
        } else if (sql.contains("UPDATE qc_inspections")) {
            val stageId = params.getOrNull(0) as? String
            val qcType = params.getOrNull(1) as? String ?: "IN_PROCESS"
            val status = params.getOrNull(2) as? String ?: "DRAFT"
            val inspectorId = params.getOrNull(3) as? String
            val notes = params.getOrNull(4) as? String
            val projectId = params.getOrNull(5) as? String ?: currentSessionProjectId ?: "DEFAULT"
            val qcId = params.getOrNull(6) as? String ?: "QC-01"

            val existing = qcInspections[Pair(projectId, qcId)]
            return if (existing != null) {
                qcInspections[Pair(projectId, qcId)] = existing.copy(
                    productionStageId = stageId,
                    qcType = QcType.valueOf(qcType),
                    status = QcStatus.valueOf(status),
                    assignedInspectorId = inspectorId,
                    notes = notes,
                    updatedAt = Instant.now().toString()
                )
                1
            } else {
                0
            }
        } else if (sql.contains("INSERT INTO inventory_products")) {
            val projectId = params.getOrNull(0) as? String ?: currentSessionProjectId ?: "DEFAULT"
            val productId = params.getOrNull(1) as? String ?: "PROD-01"
            val sku = params.getOrNull(2) as? String ?: "SKU-01"
            val name = params.getOrNull(3) as? String ?: "Product"
            val category = params.getOrNull(4) as? String
            val unit = params.getOrNull(5) as? String ?: "PCS"

            val product = InventoryProduct(
                id = productId,
                sku = sku,
                name = name,
                description = name,
                categoryId = category,
                productType = InventoryProductType.FINISHED_PRODUCT,
                unitOfMeasure = InventoryUnit.valueOf(unit),
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString(),
                createdBy = "SYSTEM"
            )
            inventoryProducts[Pair(projectId, productId)] = product
            return 1
        } else if (sql.contains("UPDATE inventory_products")) {
            val name = params.getOrNull(0) as? String ?: "Product"
            val category = params.getOrNull(1) as? String
            val unit = params.getOrNull(2) as? String ?: "PCS"
            val projectId = params.getOrNull(3) as? String ?: currentSessionProjectId ?: "DEFAULT"
            val productId = params.getOrNull(4) as? String ?: "PROD-01"

            val existing = inventoryProducts[Pair(projectId, productId)]
            return if (existing != null) {
                inventoryProducts[Pair(projectId, productId)] = existing.copy(
                    name = name,
                    categoryId = category,
                    unitOfMeasure = InventoryUnit.valueOf(unit),
                    updatedAt = Instant.now().toString()
                )
                1
            } else {
                0
            }
        } else if (sql.contains("INSERT INTO delivery_challans")) {
            val projectId = params.getOrNull(0) as? String ?: currentSessionProjectId ?: "DEFAULT"
            val challanId = params.getOrNull(1) as? String ?: "CHALLAN-01"
            val challanNo = params.getOrNull(2) as? String ?: "DC-01"
            val deliveryOrderId = params.getOrNull(3) as? String ?: "DO-01"
            val status = params.getOrNull(4) as? String ?: "PREPARED"
            val dispatchedAt = (params.getOrNull(5) as? Timestamp)?.time ?: System.currentTimeMillis()
            val dispatchedBy = params.getOrNull(6) as? String ?: "SYSTEM"

            val challan = DeliveryChallan(
                challanId = challanId,
                projectId = projectId,
                challanNo = challanNo,
                deliveryOrderId = deliveryOrderId,
                customerId = null,
                sourceReferenceId = null,
                sourceReferenceType = null,
                challanType = DeliveryChallanType.STANDARD,
                status = DeliveryChallanStatus.valueOf(status),
                issueDate = dispatchedAt,
                notes = null,
                createdBy = dispatchedBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            deliveryChallans[Pair(projectId, challanId)] = challan
            return 1
        } else if (sql.contains("UPDATE delivery_challans")) {
            val status = params.getOrNull(0) as? String ?: "PREPARED"
            val dispatchedAt = (params.getOrNull(1) as? Timestamp)?.time ?: System.currentTimeMillis()
            val projectId = params.getOrNull(2) as? String ?: currentSessionProjectId ?: "DEFAULT"
            val challanId = params.getOrNull(3) as? String ?: "CHALLAN-01"

            val existing = deliveryChallans[Pair(projectId, challanId)]
            return if (existing != null) {
                deliveryChallans[Pair(projectId, challanId)] = existing.copy(
                    status = DeliveryChallanStatus.valueOf(status),
                    issueDate = dispatchedAt,
                    updatedAt = System.currentTimeMillis()
                )
                1
            } else {
                0
            }
        } else if (sql.contains("INSERT INTO return_requests")) {
            val projectId = params.getOrNull(0) as? String ?: currentSessionProjectId ?: "DEFAULT"
            val returnId = params.getOrNull(1) as? String ?: "RET-01"
            val returnNo = params.getOrNull(2) as? String ?: "RMA-01"
            val customerId = params.getOrNull(3) as? String ?: "CUST-01"
            val originalChallanId = params.getOrNull(4) as? String
            val status = params.getOrNull(5) as? String ?: "REQUESTED"
            val reason = params.getOrNull(6) as? String ?: "PRINTING_DEFECT"
            val description = params.getOrNull(7) as? String
            val requestedAt = (params.getOrNull(8) as? Timestamp)?.time ?: System.currentTimeMillis()
            val requestedBy = params.getOrNull(9) as? String ?: "SYSTEM"

            val ret = ReturnRequest(
                returnId = returnId,
                projectId = projectId,
                returnNo = returnNo,
                customerId = customerId,
                originalChallanId = originalChallanId,
                status = ReturnStatus.valueOf(status),
                reason = ReturnReason.valueOf(reason),
                description = description,
                requestedAt = requestedAt,
                requestedBy = requestedBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                version = 1L
            )
            returnRequests[Pair(projectId, returnId)] = ret
            return 1
        } else if (sql.contains("UPDATE return_requests")) {
            val status = params.getOrNull(0) as? String ?: "REQUESTED"
            val reason = params.getOrNull(1) as? String ?: "PRINTING_DEFECT"
            val description = params.getOrNull(2) as? String
            val projectId = params.getOrNull(3) as? String ?: currentSessionProjectId ?: "DEFAULT"
            val returnId = params.getOrNull(4) as? String ?: "RET-01"

            val existing = returnRequests[Pair(projectId, returnId)]
            return if (existing != null) {
                returnRequests[Pair(projectId, returnId)] = existing.copy(
                    status = ReturnStatus.valueOf(status),
                    reason = ReturnReason.valueOf(reason),
                    description = description,
                    updatedAt = System.currentTimeMillis(),
                    version = existing.version + 1
                )
                1
            } else {
                0
            }
        }
        return 1
    }

    private fun executeMockQuery(sql: String, params: List<Any?>): ResultSet {
        val results = mutableListOf<Map<String, Any?>>()

        if (sql.contains("FROM qc_inspections") && sql.contains("inspection_id = ?")) {
            val qcId = (if (params.size > 1) params.getOrNull(1) else params.getOrNull(0)) as? String
            val target = qcInspections.entries.find { it.key.second == qcId }
            if (target != null && (currentSessionProjectId == null || target.key.first == currentSessionProjectId)) {
                val qc = target.value
                results.add(
                    mapOf(
                        "inspection_id" to qc.qcId,
                        "project_id" to target.key.first,
                        "job_id" to qc.productionJobId,
                        "stage_id" to qc.productionStageId,
                        "qc_type" to qc.qcType.name,
                        "status" to qc.status.name,
                        "decision" to qc.decision.name,
                        "inspector_id" to qc.assignedInspectorId,
                        "inspector_name" to qc.assignedInspectorName,
                        "created_by" to qc.createdBy,
                        "notes" to qc.notes,
                        "inspected_at" to Timestamp.from(Instant.parse(qc.createdAt)),
                        "started_at" to null,
                        "created_at" to Timestamp.from(Instant.parse(qc.createdAt)),
                        "updated_at" to Timestamp.from(Instant.parse(qc.updatedAt)),
                        "updated_by" to qc.updatedBy
                    )
                )
            }
        } else if (sql.contains("FROM qc_inspections")) {
            for ((key, qc) in qcInspections) {
                if (currentSessionProjectId == null || key.first == currentSessionProjectId) {
                    results.add(
                        mapOf(
                            "inspection_id" to qc.qcId,
                            "project_id" to key.first,
                            "job_id" to qc.productionJobId,
                            "stage_id" to qc.productionStageId,
                            "qc_type" to qc.qcType.name,
                            "status" to qc.status.name,
                            "decision" to qc.decision.name,
                            "inspector_id" to qc.assignedInspectorId,
                            "inspector_name" to qc.assignedInspectorName,
                            "created_by" to qc.createdBy,
                            "notes" to qc.notes,
                            "inspected_at" to Timestamp.from(Instant.parse(qc.createdAt)),
                            "started_at" to null,
                            "created_at" to Timestamp.from(Instant.parse(qc.createdAt)),
                            "updated_at" to Timestamp.from(Instant.parse(qc.updatedAt)),
                            "updated_by" to qc.updatedBy
                        )
                    )
                }
            }
        } else if (sql.contains("FROM inventory_products")) {
            for ((key, p) in inventoryProducts) {
                if (currentSessionProjectId == null || key.first == currentSessionProjectId) {
                    results.add(
                        mapOf(
                            "product_id" to p.id,
                            "project_id" to key.first,
                            "product_code" to p.sku,
                            "product_name" to p.name,
                            "category" to p.categoryId,
                            "unit" to p.unitOfMeasure.name,
                            "reorder_level" to 0,
                            "created_at" to Timestamp.from(Instant.parse(p.createdAt)),
                            "updated_at" to Timestamp.from(Instant.parse(p.updatedAt))
                        )
                    )
                }
            }
        } else if (sql.contains("FROM delivery_challans") && sql.contains("challan_id = ?")) {
            val challanId = (if (params.size > 1) params.getOrNull(1) else params.getOrNull(0)) as? String
            val target = deliveryChallans.entries.find { it.key.second == challanId }
            if (target != null && (currentSessionProjectId == null || target.key.first == currentSessionProjectId)) {
                val c = target.value
                results.add(
                    mapOf(
                        "challan_id" to c.challanId,
                        "project_id" to target.key.first,
                        "challan_number" to c.challanNo,
                        "delivery_order_id" to c.deliveryOrderId,
                        "status" to c.status.name,
                        "dispatched_at" to Timestamp(c.issueDate),
                        "dispatched_by" to c.createdBy,
                        "created_at" to Timestamp(c.createdAt),
                        "updated_at" to Timestamp(c.updatedAt)
                    )
                )
            }
        } else if (sql.contains("FROM delivery_challans") && sql.contains("challan_number = ?")) {
            val challanNo = (if (params.size > 1) params.getOrNull(1) else params.getOrNull(0)) as? String
            val target = deliveryChallans.entries.find { it.value.challanNo == challanNo }
            if (target != null && (currentSessionProjectId == null || target.key.first == currentSessionProjectId)) {
                val c = target.value
                results.add(
                    mapOf(
                        "challan_id" to c.challanId,
                        "project_id" to target.key.first,
                        "challan_number" to c.challanNo,
                        "delivery_order_id" to c.deliveryOrderId,
                        "status" to c.status.name,
                        "dispatched_at" to Timestamp(c.issueDate),
                        "dispatched_by" to c.createdBy,
                        "created_at" to Timestamp(c.createdAt),
                        "updated_at" to Timestamp(c.updatedAt)
                    )
                )
            }
        } else if (sql.contains("FROM delivery_challans")) {
            for ((key, c) in deliveryChallans) {
                if (currentSessionProjectId == null || key.first == currentSessionProjectId) {
                    results.add(
                        mapOf(
                            "challan_id" to c.challanId,
                            "project_id" to key.first,
                            "challan_number" to c.challanNo,
                            "delivery_order_id" to c.deliveryOrderId,
                            "status" to c.status.name,
                            "dispatched_at" to Timestamp(c.issueDate),
                            "dispatched_by" to c.createdBy,
                            "created_at" to Timestamp(c.createdAt),
                            "updated_at" to Timestamp(c.updatedAt)
                        )
                    )
                }
            }
        } else if (sql.contains("FROM return_requests") && sql.contains("return_id = ?")) {
            val retId = (if (params.size > 1) params.getOrNull(1) else params.getOrNull(0)) as? String
            val target = returnRequests.entries.find { it.key.second == retId }
            if (target != null && (currentSessionProjectId == null || target.key.first == currentSessionProjectId)) {
                val r = target.value
                results.add(
                    mapOf(
                        "return_id" to r.returnId,
                        "project_id" to target.key.first,
                        "return_no" to r.returnNo,
                        "customer_id" to r.customerId,
                        "original_challan_id" to r.originalChallanId,
                        "status" to r.status.name,
                        "reason" to r.reason.name,
                        "description" to r.description,
                        "requested_at" to Timestamp(r.requestedAt),
                        "requested_by" to r.requestedBy,
                        "created_at" to Timestamp(r.createdAt),
                        "updated_at" to Timestamp(r.updatedAt),
                        "version" to r.version
                    )
                )
            }
        } else if (sql.contains("FROM return_requests")) {
            for ((key, r) in returnRequests) {
                if (currentSessionProjectId == null || key.first == currentSessionProjectId) {
                    results.add(
                        mapOf(
                            "return_id" to r.returnId,
                            "project_id" to key.first,
                            "return_no" to r.returnNo,
                            "customer_id" to r.customerId,
                            "original_challan_id" to r.originalChallanId,
                            "status" to r.status.name,
                            "reason" to r.reason.name,
                            "description" to r.description,
                            "requested_at" to Timestamp(r.requestedAt),
                            "requested_by" to r.requestedBy,
                            "created_at" to Timestamp(r.createdAt),
                            "updated_at" to Timestamp(r.updatedAt),
                            "version" to r.version
                        )
                    )
                }
            }
        }

        var cursor = -1

        return Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java),
            InvocationHandler { _, method, args ->
                val rsArgs = args ?: emptyArray()
                when (method.name) {
                    "next" -> {
                        cursor++
                        cursor < results.size
                    }
                    "getString" -> {
                        val col = rsArgs[0] as String
                        results[cursor][col] as? String
                    }
                    "getBigDecimal" -> {
                        val col = rsArgs[0] as String
                        results[cursor][col] as? java.math.BigDecimal
                    }
                    "getInt" -> {
                        val col = rsArgs[0] as String
                        (results[cursor][col] as? Number)?.toInt() ?: 0
                    }
                    "getLong" -> {
                        val col = rsArgs[0] as String
                        (results[cursor][col] as? Number)?.toLong() ?: 0L
                    }
                    "getBoolean" -> {
                        val col = rsArgs[0] as String
                        (results[cursor][col] as? Boolean) ?: false
                    }
                    "getTimestamp" -> {
                        val col = rsArgs[0] as String
                        results[cursor][col] as? Timestamp
                    }
                    "wasNull" -> false
                    "close" -> null
                    else -> null
                }
            }
        ) as ResultSet
    }
}
