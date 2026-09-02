package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorContactDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.ContactType
import com.sucharu.sucharupro.domain.model.vendor.VendorContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorContactDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorContactDataSource {

    private val contactFlows = mutableMapOf<String, MutableStateFlow<List<VendorContact>>>()

    private fun mapRow(rs: ResultSet): VendorContact {
        return VendorContact(
            contactId = rs.getString("contact_id"),
            vendorId = rs.getString("vendor_id"),
            projectId = rs.getString("project_id"),
            contactType = runCatching { ContactType.valueOf(rs.getString("contact_type")) }.getOrDefault(ContactType.PRIMARY),
            name = rs.getString("name"),
            designation = rs.getString("designation"),
            phone = rs.getString("phone"),
            alternatePhone = rs.getString("alternate_phone"),
            email = rs.getString("email"),
            notes = rs.getString("notes"),
            isPrimary = rs.getBoolean("is_primary"),
            active = rs.getBoolean("active"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    override fun observeContacts(projectId: String, vendorId: String): Flow<List<VendorContact>> {
        val key = "$projectId:$vendorId"
        return synchronized(contactFlows) {
            contactFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findById(projectId: String, contactId: String): DomainResult<VendorContact> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val contact = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_contacts WHERE project_id = ? AND contact_id = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, contactId)) { rs ->
                    mapRow(rs)
                }
            }
            if (contact != null) {
                DomainResult.Success(contact)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor contact '$contactId' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor contact")
        }
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, activeOnly: Boolean): DomainResult<List<VendorContact>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = if (activeOnly) {
                    "SELECT * FROM vendor_contacts WHERE project_id = ? AND vendor_id = ? AND active = true ORDER BY is_primary DESC, name ASC"
                } else {
                    "SELECT * FROM vendor_contacts WHERE project_id = ? AND vendor_id = ? ORDER BY is_primary DESC, name ASC"
                }
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, vendorId)) { rs ->
                    mapRow(rs)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor contacts")
        }
    }

    override suspend fun createContact(contact: VendorContact): DomainResult<VendorContact> {
        val tenant = TenantContext(contact.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val insertSql = """
                    INSERT INTO vendor_contacts (
                        project_id, contact_id, vendor_id, contact_type, name,
                        designation, phone, alternate_phone, email, notes,
                        is_primary, active, created_at, updated_at, created_by,
                        updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    contact.contactId,
                    contact.vendorId,
                    contact.contactType.name,
                    contact.name,
                    contact.designation,
                    contact.phone,
                    contact.alternatePhone,
                    contact.email,
                    contact.notes,
                    contact.isPrimary,
                    contact.active,
                    now,
                    now,
                    contact.createdBy ?: "system",
                    contact.updatedBy ?: "system"
                )

                ctx.sqlExecutor.executeUpdate(insertSql, params)
                contact.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor contact")
        }
    }

    override suspend fun updateContact(contact: VendorContact): DomainResult<VendorContact> {
        val tenant = TenantContext(contact.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_contacts SET
                        contact_type = ?,
                        name = ?,
                        designation = ?,
                        phone = ?,
                        alternate_phone = ?,
                        email = ?,
                        notes = ?,
                        is_primary = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND contact_id = ? AND version = ?
                """.trimIndent()

                val params = listOf(
                    contact.contactType.name,
                    contact.name,
                    contact.designation,
                    contact.phone,
                    contact.alternatePhone,
                    contact.email,
                    contact.notes,
                    contact.isPrimary,
                    now,
                    contact.updatedBy ?: "system",
                    tenant.projectId,
                    contact.contactId,
                    contact.version
                )

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw IllegalStateException("Optimistic lock failure or contact not found: '${contact.contactId}'.")
                }
                contact.copy(version = contact.version + 1L, updatedAt = now.time)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor contact")
        }
    }

    override suspend fun updateStatus(projectId: String, contactId: String, active: Boolean, updatedBy: String): DomainResult<VendorContact> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_contacts SET
                        active = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND contact_id = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, listOf(active, now, updatedBy, tenant.projectId, contactId))
                if (rows == 0) {
                    throw NoSuchElementException("Vendor contact '$contactId' not found.")
                }

                val findSql = "SELECT * FROM vendor_contacts WHERE project_id = ? AND contact_id = ?"
                ctx.sqlExecutor.querySingleOrNull(findSql, listOf(tenant.projectId, contactId)) { rs ->
                    mapRow(rs)
                } ?: throw NoSuchElementException("Failed to retrieve updated contact '$contactId'.")
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor contact status")
        }
    }
}
