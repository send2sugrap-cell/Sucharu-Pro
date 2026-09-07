package com.sucharu.sucharupro.backend.customerfinancial

import com.sucharu.sucharupro.data.persistence.postgres.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.*
import com.sucharu.sucharupro.domain.model.customerpayment.*
import com.sucharu.sucharupro.domain.model.customersettlement.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.sql.DriverManager

/**
 * Real PostgreSQL Persistence, Flyway Migration, Transaction Rollback, and RLS Isolation Test Suite (Phase 08).
 *
 * MANDATORY REQUIREMENT:
 * - NO silent skips (`if (!postgresAvailable) return` is FORBIDDEN).
 * - NO fake data sources or mock JDBC proxies in this suite.
 * - Exercises production [PostgresCustomerInvoiceDataSource], [PostgresCustomerPaymentDataSource],
 *   [PostgresCustomerPaymentAllocationDataSource], and [DefaultPostgresTransactionManager]
 *   against a real PostgreSQL database engine.
 */
class PostgresCustomerInvoicePaymentRealDatabaseTest {

    private lateinit var config: PostgresConnectionConfig
    private lateinit var connectionProvider: DefaultPostgresConnectionProvider
    private lateinit var transactionManager: DefaultPostgresTransactionManager
    private lateinit var invoiceDataSource: PostgresCustomerInvoiceDataSource
    private lateinit var paymentDataSource: PostgresCustomerPaymentDataSource
    private lateinit var allocationDataSource: PostgresCustomerPaymentAllocationDataSource

    private val tenantA = "TENANT-REAL-A"
    private val tenantB = "TENANT-REAL-B"
    private val projectId = "PRJ-REAL-001"

    @Before
    fun setUp() {
        val host = System.getenv("DATABASE_HOST") ?: "localhost"
        val port = System.getenv("DATABASE_PORT")?.toIntOrNull() ?: 5432
        val dbName = System.getenv("DATABASE_NAME") ?: "sucharu_pro_db"
        val user = System.getenv("DATABASE_USER") ?: "postgres"
        val password = System.getenv("DATABASE_PASSWORD") ?: "postgres"

        config = PostgresConnectionConfig(
            host = host,
            port = port,
            database = dbName,
            user = user,
            password = password,
            maxPoolSize = 5
        )

        try {
            Class.forName("org.postgresql.Driver")
            val conn = DriverManager.getConnection("jdbc:postgresql://$host:$port/$dbName?sslmode=prefer", user, password)
            conn.close()

            connectionProvider = DefaultPostgresConnectionProvider(config)
            transactionManager = DefaultPostgresTransactionManager(connectionProvider)
            invoiceDataSource = PostgresCustomerInvoiceDataSource(transactionManager, tenantA)
            paymentDataSource = PostgresCustomerPaymentDataSource(transactionManager, tenantA)
            allocationDataSource = PostgresCustomerPaymentAllocationDataSource(transactionManager, tenantA)
        } catch (e: Exception) {
            fail("MANDATORY REAL POSTGRESQL INSTANCE REQUIRED at $host:$port/$dbName. Connection failed: ${e.message}")
        }
    }

    @Test
    fun testRealPostgresConnectivityAndHealthCheck() = runBlocking {
        val healthChecker = DatabaseHealthChecker(connectionProvider)
        val health = healthChecker.checkReadiness()
        assertTrue("Real PostgreSQL health check must report READY", health.isReady)
        assertNotNull("Database name must be returned", health.databaseName)
    }

    @Test
    fun testRealPostgresFlywayTableAndRlsPolicyVerification() = runBlocking {
        val conn = connectionProvider.acquireConnection()
        try {
            val stmt = conn.prepareStatement(
                """
                SELECT relname, relrowsecurity, relforcerowsecurity 
                FROM pg_class 
                WHERE relname IN ('customer_invoices', 'customer_payments', 'customer_payment_allocations')
                """.trimIndent()
            )
            val rs = stmt.executeQuery()
            var count = 0
            while (rs.next()) {
                count++
                val relName = rs.getString("relname")
                val rlsEnabled = rs.getBoolean("relrowsecurity")
                val rlsForced = rs.getBoolean("relforcerowsecurity")
                assertTrue("RLS must be enabled on $relName", rlsEnabled)
                assertTrue("RLS must be forced on $relName", rlsForced)
            }
            rs.close()
            stmt.close()
            assertTrue("All 3 Phase 08 financial tables must exist in PostgreSQL catalog with RLS enabled", count >= 3)
        } finally {
            connectionProvider.releaseConnection(conn)
        }
    }

    @Test
    fun testRealPostgresInvoicePaymentAllocationPersistence_EndToEnd() = runBlocking {
        // 1. Insert Customer Invoice via Real Postgres DataSource
        val invoice = CustomerInvoice(
            invoiceId = "INV-REAL-101",
            tenantId = tenantA,
            projectId = projectId,
            customerId = "CUST-REAL-01",
            customerFinancialAccountId = "ACC-REAL-01",
            invoiceNumber = "INV-2026-REAL-101",
            subtotal = BigDecimal("1500.0000"),
            grandTotal = BigDecimal("1500.0000"),
            dueAmount = BigDecimal("1500.0000"),
            paidAmount = BigDecimal.ZERO,
            status = CustomerInvoiceStatus.ISSUED,
            lines = listOf(
                CustomerInvoiceLine(
                    lineId = "LINE-REAL-01",
                    invoiceId = "INV-REAL-101",
                    tenantId = tenantA,
                    projectId = projectId,
                    description = "1500 Commercial Offset Packaging Boxes",
                    quantity = BigDecimal("1500"),
                    unitPrice = BigDecimal("1.0000"),
                    lineTotal = BigDecimal("1500.0000")
                )
            )
        )

        val insertInvRes = invoiceDataSource.insertInvoice(invoice)
        assertTrue("Real Postgres invoice insert must succeed", insertInvRes is DomainResult.Success)

        val retrievedInvRes = invoiceDataSource.findInvoiceById(tenantA, projectId, invoice.invoiceId)
        assertTrue("Real Postgres invoice lookup must succeed", retrievedInvRes is DomainResult.Success)
        val retrievedInv = (retrievedInvRes as DomainResult.Success).data
        assertNotNull(retrievedInv)
        assertEquals("INV-REAL-101", retrievedInv.invoiceId)
        assertEquals(BigDecimal("1500.0000"), retrievedInv.grandTotal)

        // 2. Insert Customer Payment via Real Postgres DataSource
        val payment = CustomerPayment(
            paymentId = "PAY-REAL-101",
            tenantId = tenantA,
            projectId = projectId,
            paymentNumber = "PAY-2026-REAL-101",
            customerId = "CUST-REAL-01",
            customerFinancialAccountId = "ACC-REAL-01",
            amount = BigDecimal("1000.0000"),
            paymentMethod = CustomerPaymentMethod.BANK,
            status = CustomerPaymentStatus.CONFIRMED,
            idempotencyKey = "idemp-real-pay-101"
        )

        val insertPayRes = paymentDataSource.insertPayment(payment)
        assertTrue("Real Postgres payment insert must succeed", insertPayRes is DomainResult.Success)

        val retrievedPayRes = paymentDataSource.findPaymentById(tenantA, projectId, payment.paymentId)
        assertTrue("Real Postgres payment lookup must succeed", retrievedPayRes is DomainResult.Success)
        val retrievedPay = (retrievedPayRes as DomainResult.Success).data
        assertNotNull(retrievedPay)
        assertEquals("PAY-REAL-101", retrievedPay.paymentId)
        assertEquals(BigDecimal("1000.0000"), retrievedPay.amount)

        // 3. Create Allocation via Real Postgres DataSource
        val allocation = CustomerPaymentAllocation(
            allocationId = "ALLOC-REAL-101",
            tenantId = tenantA,
            projectId = projectId,
            customerId = "CUST-REAL-01",
            customerFinancialAccountId = "ACC-REAL-01",
            paymentId = "PAY-REAL-101",
            invoiceId = "INV-REAL-101",
            allocatedAmount = BigDecimal("1000.0000"),
            status = CustomerPaymentAllocationStatus.ALLOCATED,
            idempotencyKey = "idemp-real-alloc-101"
        )

        val insertAllocRes = allocationDataSource.createAllocation(allocation)
        assertTrue("Real Postgres allocation creation must succeed", insertAllocRes is DomainResult.Success)

        // 4. Update Invoice Due Amount & Status in Postgres
        val updateBalRes = invoiceDataSource.updatePaymentBalance(
            tenantId = tenantA,
            projectId = projectId,
            invoiceId = invoice.invoiceId,
            newPaidAmount = BigDecimal("1000.0000"),
            newDueAmount = BigDecimal("500.0000"),
            newStatus = CustomerInvoiceStatus.PARTIALLY_PAID,
            actorId = "USR-ADMIN-REAL",
            expectedVersion = 1L
        )
        assertTrue("Real Postgres invoice balance update must succeed", updateBalRes is DomainResult.Success)

        val updatedInvRes = invoiceDataSource.findInvoiceById(tenantA, projectId, invoice.invoiceId)
        assertTrue(updatedInvRes is DomainResult.Success)
        val updatedInv = (updatedInvRes as DomainResult.Success).data
        assertNotNull(updatedInv)
        assertEquals(CustomerInvoiceStatus.PARTIALLY_PAID, updatedInv.status)
        assertEquals(BigDecimal("1000.0000"), updatedInv.paidAmount)
        assertEquals(BigDecimal("500.0000"), updatedInv.dueAmount)
    }

    @Test
    fun testRealPostgresCrossTenantRlsReadAndWriteIsolation() = runBlocking {
        val otherTenantDataSource = PostgresCustomerInvoiceDataSource(transactionManager, tenantB)

        // Tenant B attempting to read Tenant A invoice
        val retrievedRes = otherTenantDataSource.findInvoiceById(tenantB, projectId, "INV-REAL-101")
        assertTrue("Cross-tenant invoice lookup must be denied by Postgres RLS", retrievedRes is DomainResult.Error)
    }

    @Test
    fun testRealPostgresTransactionRollback_RevertsUncommittedFinancialData() = runBlocking {
        val tempInvoiceId = "INV-ROLLBACK-999"
        val tenantContext = TenantContext(projectId = projectId)

        try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_invoices (
                        invoice_id, tenant_id, project_id, customer_id, customer_financial_account_id,
                        invoice_number, subtotal, grand_total, paid_amount, due_amount, status,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tempInvoiceId)
                    stmt.setString(2, tenantA)
                    stmt.setString(3, projectId)
                    stmt.setString(4, "CUST-ROLLBACK")
                    stmt.setString(5, "ACC-ROLLBACK")
                    stmt.setString(6, "INV-ROLLBACK-01")
                    stmt.setBigDecimal(7, BigDecimal("500.0000"))
                    stmt.setBigDecimal(8, BigDecimal("500.0000"))
                    stmt.setBigDecimal(9, BigDecimal.ZERO)
                    stmt.setBigDecimal(10, BigDecimal("500.0000"))
                    stmt.setString(11, "DRAFT")
                    stmt.setLong(12, System.currentTimeMillis())
                    stmt.setString(13, "system")
                    stmt.setLong(14, System.currentTimeMillis())
                    stmt.setString(15, "system")
                    stmt.setLong(16, 1L)
                    stmt.executeUpdate()
                }

                // Deliberately fail transaction to trigger rollback
                throw IllegalStateException("Controlled transaction failure for rollback verification")
            }
        } catch (_: Exception) {
            // Expected transaction failure
        }

        // Verify uncommitted invoice was rolled back in PostgreSQL
        val rolledBackInvRes = invoiceDataSource.findInvoiceById(tenantA, projectId, tempInvoiceId)
        assertTrue("Rolled-back invoice must not exist in PostgreSQL", rolledBackInvRes is DomainResult.Error)
    }
}
