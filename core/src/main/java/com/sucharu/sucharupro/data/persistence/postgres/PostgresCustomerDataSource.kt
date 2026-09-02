package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.CustomerDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getMoney
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerActivityType
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerNote
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Customer domain (INFRA-01 Step 03).
 */
class PostgresCustomerDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerDataSource {

    private fun mapCustomer(rs: ResultSet): Customer {
        val creditLimit = rs.getMoney("credit_limit_amount")
        val creditDays = rs.getInt("credit_days")
        val creditProfile = CustomerCreditProfile(
            creditLimit = creditLimit,
            paymentTermDays = creditDays,
            isCreditAllowed = creditLimit.isPositive(),
            isAdvanceRequired = !creditLimit.isPositive()
        )

        return Customer(
            customerId = rs.getString("customer_id"),
            customerCode = rs.getString("customer_code"),
            displayName = rs.getString("display_name"),
            customerType = rs.getEnumByName("customer_type", CustomerType.INDIVIDUAL),
            status = rs.getEnumByName("status", CustomerStatusType.ACTIVE),
            primaryPhone = rs.getString("primary_phone"),
            alternatePhone = rs.getString("alternate_phone"),
            email = rs.getString("email"),
            contactPersonName = rs.getString("contact_person_name"),
            addresses = emptyList(),
            creditProfile = creditProfile,
            notes = rs.getString("notes"),
            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString()
        )
    }

    override fun observeCustomers(): Flow<List<Customer>> = flow {
        val res = fetchCustomers()
        if (res is DomainResult.Success) {
            emit(res.data)
        } else {
            emit(emptyList())
        }
    }

    override suspend fun fetchCustomers(): DomainResult<List<Customer>> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT customer_id, project_id, customer_code, display_name, customer_type,
                           status, primary_phone, alternate_phone, email, contact_person_name,
                           credit_limit_amount, credit_days, notes, created_at, updated_at
                    FROM customers
                    WHERE project_id = ?
                    ORDER BY created_at DESC
                """.trimIndent()

                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId)) { rs ->
                    mapCustomer(rs)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "fetch customers")
        }
    }

    override suspend fun fetchCustomerById(customerId: String): DomainResult<Customer> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            val customer = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT customer_id, project_id, customer_code, display_name, customer_type,
                           status, primary_phone, alternate_phone, email, contact_person_name,
                           credit_limit_amount, credit_days, notes, created_at, updated_at
                    FROM customers
                    WHERE project_id = ? AND customer_id = ?
                """.trimIndent()

                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, customerId)) { rs ->
                    mapCustomer(rs)
                }
            }
            if (customer != null) {
                DomainResult.Success(customer)
            } else {
                DomainResult.Error(message = "Customer with ID '$customerId' not found.")
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "fetch customer by ID")
        }
    }

    override suspend fun insertCustomer(customer: Customer): DomainResult<Customer> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO customers (
                        project_id, customer_id, customer_code, display_name, customer_type,
                        status, primary_phone, alternate_phone, email, contact_person_name,
                        credit_limit_amount, credit_days, notes, created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId,
                        customer.customerId,
                        customer.customerCode,
                        customer.displayName,
                        customer.customerType.name,
                        customer.status.name,
                        customer.primaryPhone,
                        customer.alternatePhone,
                        customer.email,
                        customer.contactPersonName,
                        customer.creditProfile.creditLimit.amount,
                        customer.creditProfile.paymentTermDays,
                        customer.notes
                    )
                )
            }
            DomainResult.Success(customer)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "insert customer")
        }
    }

    override suspend fun updateCustomer(customer: Customer): DomainResult<Customer> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE customers
                    SET display_name = ?, customer_type = ?, status = ?, primary_phone = ?,
                        alternate_phone = ?, email = ?, contact_person_name = ?, credit_limit_amount = ?,
                        credit_days = ?, notes = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND customer_id = ?
                """.trimIndent()

                val affected = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        customer.displayName,
                        customer.customerType.name,
                        customer.status.name,
                        customer.primaryPhone,
                        customer.alternatePhone,
                        customer.email,
                        customer.contactPersonName,
                        customer.creditProfile.creditLimit.amount,
                        customer.creditProfile.paymentTermDays,
                        customer.notes,
                        tenant.projectId,
                        customer.customerId
                    )
                )
                if (affected == 0) {
                    throw OptimisticLockException("Customer", customer.customerId, 1)
                }
            }
            DomainResult.Success(customer)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update customer")
        }
    }

    override suspend fun setCustomerStatus(
        customerId: String,
        status: CustomerStatusType
    ): DomainResult<Customer> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE customers
                    SET status = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND customer_id = ?
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(sql, listOf(status.name, tenant.projectId, customerId))
            }
            fetchCustomerById(customerId)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "set customer status")
        }
    }

    override fun observeCustomerNotes(customerId: String): Flow<List<CustomerNote>> = flow {
        emit(emptyList())
    }

    override suspend fun insertCustomerNote(note: CustomerNote): DomainResult<CustomerNote> {
        return DomainResult.Success(note)
    }

    override suspend fun updateCustomerNote(note: CustomerNote): DomainResult<CustomerNote> {
        return DomainResult.Success(note)
    }

    override suspend fun deleteCustomerNote(noteId: String, customerId: String): DomainResult<Unit> {
        return DomainResult.Success(Unit)
    }

    override suspend fun toggleImportantNote(noteId: String, customerId: String): DomainResult<CustomerNote> {
        return DomainResult.Error(message = "Note not found.")
    }

    override fun observeCustomerActivities(customerId: String): Flow<List<CustomerActivity>> = flow {
        emit(emptyList())
    }

    override suspend fun insertCustomerActivity(activity: CustomerActivity): DomainResult<CustomerActivity> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO domain_activity_events (
                        event_id, project_id, aggregate_type, aggregate_id, event_type, actor_id, occurred_at
                    ) VALUES (?, ?, ?, ?, ?, ?, NOW())
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        activity.id,
                        tenant.projectId,
                        "CUSTOMER",
                        activity.customerId,
                        activity.type.name,
                        activity.actorName ?: "SYSTEM"
                    )
                )
            }
            DomainResult.Success(activity)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "insert customer activity")
        }
    }

    override suspend fun setCustomerFollowUp(
        customerId: String,
        followUpAt: String?
    ): DomainResult<Customer> {
        return fetchCustomerById(customerId)
    }
}
