package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Production-grade PostgreSQL DataSource for Vendor Master (Module 12 Step 01).
 * Enforces PostgreSQL Row-Level Security (RLS) and multi-tenant scoping via [TransactionManager].
 */
class PostgresVendorDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorDataSource {

    private fun mapVendor(rs: ResultSet): Vendor {
        return Vendor(
            vendorId = rs.getString("vendor_id"),
            projectId = rs.getString("project_id"),
            vendorCode = rs.getString("vendor_code"),
            vendorName = rs.getString("vendor_name"),
            legalName = rs.getString("legal_name"),
            vendorType = rs.getEnumByName("vendor_type", VendorType.SERVICE_PROVIDER),
            vendorCategory = rs.getEnumByName("vendor_category", VendorCategory.OTHER),
            status = rs.getEnumByName("status", VendorStatus.ACTIVE),
            primaryContactName = rs.getString("primary_contact_name"),
            primaryPhone = rs.getString("primary_phone"),
            primaryEmail = rs.getString("primary_email"),
            notes = rs.getString("notes"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedBy = rs.getString("updated_by"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            version = rs.getLong("version")
        )
    }

    override fun observeVendors(projectId: String): Flow<List<Vendor>> = flow {
        val res = fetchVendors(projectId)
        if (res is DomainResult.Success) {
            emit(res.data)
        } else {
            emit(emptyList())
        }
    }

    override suspend fun fetchVendors(
        projectId: String,
        type: VendorType?,
        category: VendorCategory?,
        status: VendorStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<Vendor>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sqlBuilder = StringBuilder("""
                    SELECT vendor_id, project_id, vendor_code, vendor_name, legal_name,
                           vendor_type, vendor_category, status, primary_contact_name,
                           primary_phone, primary_email, notes, created_by, updated_by,
                           created_at, updated_at, version
                    FROM vendors
                    WHERE project_id = ?
                """.trimIndent())

                val params = mutableListOf<Any>(tenant.projectId)

                if (type != null) {
                    sqlBuilder.append(" AND vendor_type = ?")
                    params.add(type.name)
                }
                if (category != null) {
                    sqlBuilder.append(" AND vendor_category = ?")
                    params.add(category.name)
                }
                if (status != null) {
                    sqlBuilder.append(" AND status = ?")
                    params.add(status.name)
                }

                sqlBuilder.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?")
                params.add(limit)
                params.add(offset)

                ctx.sqlExecutor.queryList(sqlBuilder.toString(), params) { rs ->
                    mapVendor(rs)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "fetch vendors")
        }
    }

    override suspend fun fetchVendorById(projectId: String, vendorId: String): DomainResult<Vendor> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val vendor = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT vendor_id, project_id, vendor_code, vendor_name, legal_name,
                           vendor_type, vendor_category, status, primary_contact_name,
                           primary_phone, primary_email, notes, created_by, updated_by,
                           created_at, updated_at, version
                    FROM vendors
                    WHERE project_id = ? AND vendor_id = ?
                """.trimIndent()

                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, vendorId)) { rs ->
                    mapVendor(rs)
                }
            }
            if (vendor != null) {
                DomainResult.Success(vendor)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor not found with ID '$vendorId' in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "fetch vendor by ID")
        }
    }

    override suspend fun fetchVendorByCode(projectId: String, vendorCode: String): DomainResult<Vendor> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val vendor = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT vendor_id, project_id, vendor_code, vendor_name, legal_name,
                           vendor_type, vendor_category, status, primary_contact_name,
                           primary_phone, primary_email, notes, created_by, updated_by,
                           created_at, updated_at, version
                    FROM vendors
                    WHERE project_id = ? AND vendor_code = ?
                """.trimIndent()

                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, vendorCode)) { rs ->
                    mapVendor(rs)
                }
            }
            if (vendor != null) {
                DomainResult.Success(vendor)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor not found with code '$vendorCode' in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "fetch vendor by code")
        }
    }

    override suspend fun existsByCode(projectId: String, vendorCode: String, excludeVendorId: String?): Boolean {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            transactionManager.inReadOnly(tenant) { ctx ->
                val sql = if (excludeVendorId != null) {
                    "SELECT 1 FROM vendors WHERE project_id = ? AND vendor_code = ? AND vendor_id != ?"
                } else {
                    "SELECT 1 FROM vendors WHERE project_id = ? AND vendor_code = ?"
                }
                val params = if (excludeVendorId != null) {
                    listOf(tenant.projectId, vendorCode, excludeVendorId)
                } else {
                    listOf(tenant.projectId, vendorCode)
                }
                val count = ctx.sqlExecutor.queryList(sql, params) { 1 }
                count.isNotEmpty()
            }
        } catch (e: Throwable) {
            false
        }
    }

    override suspend fun insertVendor(vendor: Vendor): DomainResult<Vendor> {
        val tenant = TenantContext(vendor.projectId.ifBlank { defaultTenantId })
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO vendors (
                        vendor_id, project_id, vendor_code, vendor_name, legal_name,
                        vendor_type, vendor_category, status, primary_contact_name,
                        primary_phone, primary_email, notes, created_by, updated_by,
                        created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                val now = Timestamp(vendor.createdAt)
                val params = listOf(
                    vendor.vendorId,
                    tenant.projectId,
                    vendor.vendorCode,
                    vendor.vendorName,
                    vendor.legalName,
                    vendor.vendorType.name,
                    vendor.vendorCategory.name,
                    vendor.status.name,
                    vendor.primaryContactName,
                    vendor.primaryPhone,
                    vendor.primaryEmail,
                    vendor.notes,
                    vendor.createdBy,
                    vendor.updatedBy,
                    now,
                    now,
                    vendor.version
                )

                ctx.sqlExecutor.executeUpdate(sql, params)
                vendor
            }
            DomainResult.Success(vendor)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "insert vendor")
        }
    }

    override suspend fun updateVendor(vendor: Vendor): DomainResult<Vendor> {
        val tenant = TenantContext(vendor.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE vendors SET
                        vendor_name = ?,
                        legal_name = ?,
                        vendor_type = ?,
                        vendor_category = ?,
                        status = ?,
                        primary_contact_name = ?,
                        primary_phone = ?,
                        primary_email = ?,
                        notes = ?,
                        updated_by = ?,
                        updated_at = ?,
                        version = version + 1
                    WHERE project_id = ? AND vendor_id = ? AND version = ?
                """.trimIndent()

                val now = Timestamp(System.currentTimeMillis())
                val params = listOf(
                    vendor.vendorName,
                    vendor.legalName,
                    vendor.vendorType.name,
                    vendor.vendorCategory.name,
                    vendor.status.name,
                    vendor.primaryContactName,
                    vendor.primaryPhone,
                    vendor.primaryEmail,
                    vendor.notes,
                    vendor.updatedBy ?: "system",
                    now,
                    tenant.projectId,
                    vendor.vendorId,
                    vendor.version
                )

                val rows = ctx.sqlExecutor.executeUpdate(sql, params)
                if (rows == 0) {
                    throw IllegalStateException("Optimistic lock failure or vendor not found: '${vendor.vendorId}'.")
                }
                vendor.copy(updatedAt = now.time, version = vendor.version + 1L)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor")
        }
    }

    override suspend fun updateVendorStatus(
        projectId: String,
        vendorId: String,
        status: VendorStatus,
        updatedBy: String
    ): DomainResult<Vendor> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE vendors SET
                        status = ?,
                        updated_by = ?,
                        updated_at = ?,
                        version = version + 1
                    WHERE project_id = ? AND vendor_id = ?
                    RETURNING vendor_id, project_id, vendor_code, vendor_name, legal_name,
                              vendor_type, vendor_category, status, primary_contact_name,
                              primary_phone, primary_email, notes, created_by, updated_by,
                              created_at, updated_at, version
                """.trimIndent()

                val now = Timestamp(System.currentTimeMillis())
                val params = listOf(
                    status.name,
                    updatedBy.ifBlank { "system" },
                    now,
                    tenant.projectId,
                    vendorId
                )

                val list = ctx.sqlExecutor.queryList(sql, params) { rs -> mapVendor(rs) }
                if (list.isEmpty()) {
                    throw NoSuchElementException("Vendor not found: '$vendorId' in project '${tenant.projectId}'.")
                }
                list.first()
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor status")
        }
    }
}
