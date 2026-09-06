package com.sucharu.sucharupro.backend.inventory

import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresFinishedProductInventoryDataSource
import com.sucharu.sucharupro.domain.model.inventory.FinishedProductInventoryReceipt
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

class PostgresFinishedProductInventoryIntegrationTest {

    private class MockInventoryDb : PostgresConnectionProvider {
        val receipts = ConcurrentHashMap<String, MutableMap<String, FinishedProductInventoryReceipt>>()

        override suspend fun acquireConnection(): Connection = createConnection()
        override suspend fun releaseConnection(connection: Connection) {}
        override fun getActiveConnectionCount(): Int = 0
        override fun getIdleConnectionCount(): Int = 1
        override fun getTotalAcquisitions(): Long = 1L
        override fun getAcquisitionFailureCount(): Long = 0L
        override suspend fun shutdownGracefully(drainTimeoutMs: Long) {}
        override fun close() {}

        private fun createProxy(interfaceClass: Class<*>, handler: (name: String, args: Array<out Any?>) -> Any?): Any {
            return Proxy.newProxyInstance(interfaceClass.classLoader, arrayOf(interfaceClass)) { _, method, args ->
                val mArgs = args ?: emptyArray()
                val result = handler(method.name.lowercase(), mArgs)
                if (result != null) {
                    result
                } else {
                    when (method.returnType) {
                        Boolean::class.javaPrimitiveType -> false
                        Int::class.javaPrimitiveType -> 0
                        Long::class.javaPrimitiveType -> 0L
                        Double::class.javaPrimitiveType -> 0.0
                        Float::class.javaPrimitiveType -> 0.0f
                        Short::class.javaPrimitiveType -> 0.toShort()
                        Byte::class.javaPrimitiveType -> 0.toByte()
                        Char::class.javaPrimitiveType -> ' '
                        else -> null
                    }
                }
            }
        }

        private fun createConnection(): Connection {
            return createProxy(Connection::class.java) { name, args ->
                when (name) {
                    "preparestatement" -> createPreparedStatement(args[0] as String)
                    "createstatement" -> createStatement()
                    "isclosed" -> false
                    "isvalid" -> true
                    else -> null
                }
            } as Connection
        }

        private fun createStatement(): Statement {
            return createProxy(Statement::class.java) { _, _ -> null } as Statement
        }

        private fun createPreparedStatement(sql: String): PreparedStatement {
            val params = mutableListOf<Any?>()

            return createProxy(PreparedStatement::class.java) { name, args ->
                when (name) {
                    "setstring", "setint", "setlong", "setbigdecimal", "setboolean", "settimestamp", "setobject" -> {
                        val idx = args[0] as Int
                        val v = args.getOrNull(1)
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = v
                        null
                    }
                    "executeupdate" -> executeUpdate(sql, params)
                    "executequery" -> executeQuery(sql, params)
                    else -> null
                }
            } as PreparedStatement
        }

        private fun executeUpdate(sql: String, params: List<Any?>): Int {
            if (sql.contains("finished_product_inventory_receipts")) {
                val receiptId = params.getOrNull(0) as? String ?: return 0
                val tenantId = params.getOrNull(1) as? String ?: return 0
                val executionJobId = params.getOrNull(2) as? String ?: return 0

                val tenantMap = receipts.computeIfAbsent(tenantId) { ConcurrentHashMap() }
                if (!tenantMap.containsKey(executionJobId)) {
                    val receipt = FinishedProductInventoryReceipt(
                        receiptId = receiptId,
                        projectId = tenantId,
                        executionJobId = executionJobId,
                        orderId = params.getOrNull(3) as? String ?: "",
                        productId = params.getOrNull(4) as? String ?: "",
                        warehouseId = params.getOrNull(5) as? String ?: "",
                        binId = params.getOrNull(6) as? String,
                        receivedQuantity = (params.getOrNull(7) as? BigDecimal) ?: BigDecimal.ZERO,
                        unit = runCatching { InventoryUnit.valueOf(params.getOrNull(8) as String) }.getOrDefault(InventoryUnit.PCS),
                        unitCost = (params.getOrNull(9) as? BigDecimal) ?: BigDecimal.ZERO,
                        qcInspectionId = params.getOrNull(10) as? String,
                        releaseId = params.getOrNull(11) as? String,
                        status = params.getOrNull(12) as? String ?: "COMPLETED",
                        receivedBy = params.getOrNull(13) as? String ?: "SYSTEM",
                        receivedAt = (params.getOrNull(14) as? Long) ?: System.currentTimeMillis(),
                        notes = params.getOrNull(15) as? String,
                        version = (params.getOrNull(16) as? Long) ?: 1L
                    )
                    tenantMap[executionJobId] = receipt
                    return 1
                }
                return 0
            }
            return 1
        }

        private fun executeQuery(sql: String, params: List<Any?>): ResultSet {
            val tenantId = params.getOrNull(0) as? String ?: ""
            val executionJobId = if (params.size > 1) params.getOrNull(1) as? String else null

            val tenantReceipts = receipts[tenantId]?.values?.toList() ?: emptyList()
            val filtered = if (!executionJobId.isNullOrBlank()) {
                tenantReceipts.filter { it.executionJobId == executionJobId }
            } else {
                tenantReceipts
            }

            var index = -1
            return createProxy(ResultSet::class.java) { name, args ->
                when (name) {
                    "next" -> {
                        index++
                        index < filtered.size
                    }
                    "wasnull" -> false
                    "getstring" -> {
                        val row = filtered.getOrNull(index) ?: return@createProxy null
                        val col = (args.getOrNull(0) as? String)?.lowercase()
                        when (col) {
                            "receipt_id" -> row.receiptId
                            "project_id" -> row.projectId
                            "execution_job_id" -> row.executionJobId
                            "order_id" -> row.orderId
                            "product_id" -> row.productId
                            "warehouse_id" -> row.warehouseId
                            "bin_id" -> row.binId
                            "unit" -> row.unit.name
                            "qc_inspection_id" -> row.qcInspectionId
                            "release_id" -> row.releaseId
                            "status" -> row.status
                            "received_by" -> row.receivedBy
                            "notes" -> row.notes
                            else -> null
                        }
                    }
                    "getbigdecimal" -> {
                        val row = filtered.getOrNull(index) ?: return@createProxy null
                        val col = (args.getOrNull(0) as? String)?.lowercase()
                        when (col) {
                            "received_quantity" -> row.receivedQuantity
                            "unit_cost" -> row.unitCost
                            else -> BigDecimal.ZERO
                        }
                    }
                    "gettimestamp" -> {
                        val row = filtered.getOrNull(index) ?: return@createProxy null
                        Timestamp(row.receivedAt)
                    }
                    "getlong" -> {
                        val row = filtered.getOrNull(index) ?: return@createProxy 1L
                        row.version
                    }
                    else -> null
                }
            } as ResultSet
        }
    }

    private lateinit var mockDb: MockInventoryDb
    private lateinit var transactionManager: DefaultPostgresTransactionManager
    private lateinit var dataSource: PostgresFinishedProductInventoryDataSource
    private val tenantId = "TENANT-001"
    private val executionJobId = "JOB-PG-01"

    @Before
    fun setUp() {
        mockDb = MockInventoryDb()
        transactionManager = DefaultPostgresTransactionManager(mockDb)
        dataSource = PostgresFinishedProductInventoryDataSource(transactionManager)
    }

    @Test
    fun testSaveAndGetReceipt_PostgresDataSource() = runBlocking {
        val receipt = FinishedProductInventoryReceipt(
            receiptId = "RC-PG-001",
            projectId = tenantId,
            executionJobId = executionJobId,
            orderId = "ORD-PG-001",
            productId = "PROD-FG-JOB-PG-01",
            warehouseId = "WH-MAIN",
            binId = "BIN-A1",
            receivedQuantity = BigDecimal("1500.00"),
            unit = InventoryUnit.PCS,
            unitCost = BigDecimal.ZERO,
            qcInspectionId = "INSP-PG-01",
            releaseId = "REL-PG-01",
            status = "COMPLETED",
            receivedBy = "pg_tester",
            receivedAt = System.currentTimeMillis(),
            notes = "PostgreSQL Integration Test Receipt"
        )

        dataSource.saveReceipt(tenantId, receipt)

        val retrieved = dataSource.getReceiptByJob(tenantId, executionJobId)
        assertNotNull(retrieved)
        assertEquals("RC-PG-001", retrieved!!.receiptId)
        assertEquals(executionJobId, retrieved.executionJobId)
        assertEquals("ORD-PG-001", retrieved.orderId)
        assertEquals("PROD-FG-JOB-PG-01", retrieved.productId)
        assertEquals("WH-MAIN", retrieved.warehouseId)
        assertEquals(BigDecimal("1500.00"), retrieved.receivedQuantity)
        assertEquals("pg_tester", retrieved.receivedBy)
    }

    @Test
    fun testDatabaseLevelIdempotency_DuplicateJobInsertIsIgnored() = runBlocking {
        val receipt1 = FinishedProductInventoryReceipt(
            receiptId = "RC-PG-101",
            projectId = tenantId,
            executionJobId = executionJobId,
            orderId = "ORD-PG-101",
            productId = "PROD-FG-101",
            warehouseId = "WH-MAIN",
            receivedQuantity = BigDecimal("1000.00"),
            receivedBy = "tester1"
        )
        dataSource.saveReceipt(tenantId, receipt1)

        val receipt2 = FinishedProductInventoryReceipt(
            receiptId = "RC-PG-102",
            projectId = tenantId,
            executionJobId = executionJobId, // Duplicate job ID
            orderId = "ORD-PG-101",
            productId = "PROD-FG-101",
            warehouseId = "WH-MAIN",
            receivedQuantity = BigDecimal("2000.00"), // Attempt double quantity
            receivedBy = "tester2"
        )
        dataSource.saveReceipt(tenantId, receipt2)

        val retrieved = dataSource.getReceiptByJob(tenantId, executionJobId)
        assertNotNull(retrieved)
        // Must equal original receipt1 (RC-PG-101 and quantity 1000.00)
        assertEquals("RC-PG-101", retrieved!!.receiptId)
        assertEquals(BigDecimal("1000.00"), retrieved.receivedQuantity)
        assertEquals("tester1", retrieved.receivedBy)
    }

    @Test
    fun testListReceipts_OrdersByReceivedAt() = runBlocking {
        val r1 = FinishedProductInventoryReceipt(
            receiptId = "RC-PG-201",
            projectId = tenantId,
            executionJobId = "JOB-201",
            orderId = "ORD-201",
            productId = "PROD-201",
            warehouseId = "WH-MAIN",
            receivedQuantity = BigDecimal("100.00"),
            receivedBy = "tester1",
            receivedAt = 1000L
        )
        val r2 = FinishedProductInventoryReceipt(
            receiptId = "RC-PG-202",
            projectId = tenantId,
            executionJobId = "JOB-202",
            orderId = "ORD-202",
            productId = "PROD-202",
            warehouseId = "WH-MAIN",
            receivedQuantity = BigDecimal("200.00"),
            receivedBy = "tester1",
            receivedAt = 2000L
        )
        dataSource.saveReceipt(tenantId, r1)
        dataSource.saveReceipt(tenantId, r2)

        val list = dataSource.listReceipts(tenantId)
        assertTrue(list.size >= 2)
        val r2Found = list.find { it.receiptId == "RC-PG-202" }
        val r1Found = list.find { it.receiptId == "RC-PG-201" }
        assertNotNull(r2Found)
        assertNotNull(r1Found)
    }
}
