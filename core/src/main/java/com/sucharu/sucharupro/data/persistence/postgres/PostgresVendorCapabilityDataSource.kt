package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorCapabilityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityStatus
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.VendorCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorCapabilityDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorCapabilityDataSource {

    private val capabilityFlows = mutableMapOf<String, MutableStateFlow<List<VendorCapability>>>()

    private fun mapRow(rs: ResultSet): VendorCapability {
        return VendorCapability(
            capabilityId = rs.getString("capability_id"),
            vendorId = rs.getString("vendor_id"),
            projectId = rs.getString("project_id"),
            capabilityType = CapabilityType.valueOf(rs.getString("capability_type")),
            displayName = rs.getString("display_name"),
            status = CapabilityStatus.valueOf(rs.getString("status")),
            notes = rs.getString("notes"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    override fun observeCapabilities(projectId: String, vendorId: String): Flow<List<VendorCapability>> {
        val key = "$projectId:$vendorId"
        return synchronized(capabilityFlows) {
            capabilityFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findById(projectId: String, capabilityId: String): DomainResult<VendorCapability> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val capability = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_capabilities WHERE project_id = ? AND capability_id = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, capabilityId)) { rs ->
                    mapRow(rs)
                }
            }
            if (capability != null) {
                DomainResult.Success(capability)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor capability '$capabilityId' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor capability")
        }
    }

    override suspend fun findByVendorAndType(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<VendorCapability> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val capability = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_capabilities WHERE project_id = ? AND vendor_id = ? AND capability_type = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, vendorId, capabilityType.name)) { rs ->
                    mapRow(rs)
                }
            }
            if (capability != null) {
                DomainResult.Success(capability)
            } else {
                DomainResult.Error(NoSuchElementException("Capability '${capabilityType.name}' not found for vendor '$vendorId'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor capability by type")
        }
    }

    override suspend fun existsByVendorAndType(projectId: String, vendorId: String, capabilityType: CapabilityType): Boolean {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT 1 FROM vendor_capabilities WHERE project_id = ? AND vendor_id = ? AND capability_type = ?"
                val res = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, vendorId, capabilityType.name)) { rs ->
                    rs.getInt(1)
                }
                res != null
            }
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, status: CapabilityStatus?): DomainResult<List<VendorCapability>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = if (status != null) {
                    "SELECT * FROM vendor_capabilities WHERE project_id = ? AND vendor_id = ? AND status = ? ORDER BY display_name ASC"
                } else {
                    "SELECT * FROM vendor_capabilities WHERE project_id = ? AND vendor_id = ? ORDER BY display_name ASC"
                }
                val params = if (status != null) listOf(tenant.projectId, vendorId, status.name) else listOf(tenant.projectId, vendorId)
                ctx.sqlExecutor.queryList(sql, params) { rs ->
                    mapRow(rs)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor capabilities")
        }
    }

    override suspend fun listVendorsByCapability(projectId: String, capabilityType: CapabilityType, status: CapabilityStatus?): DomainResult<List<String>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val vendorIds = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = if (status != null) {
                    "SELECT DISTINCT vendor_id FROM vendor_capabilities WHERE project_id = ? AND capability_type = ? AND status = ?"
                } else {
                    "SELECT DISTINCT vendor_id FROM vendor_capabilities WHERE project_id = ? AND capability_type = ?"
                }
                val params = if (status != null) listOf(tenant.projectId, capabilityType.name, status.name) else listOf(tenant.projectId, capabilityType.name)
                ctx.sqlExecutor.queryList(sql, params) { rs ->
                    rs.getString("vendor_id")
                }
            }
            DomainResult.Success(vendorIds)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendors by capability")
        }
    }

    override suspend fun createCapability(capability: VendorCapability): DomainResult<VendorCapability> {
        val tenant = TenantContext(capability.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val insertSql = """
                    INSERT INTO vendor_capabilities (
                        project_id, capability_id, vendor_id, capability_type, display_name,
                        status, notes, created_at, updated_at, created_by,
                        updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    capability.capabilityId,
                    capability.vendorId,
                    capability.capabilityType.name,
                    capability.displayName,
                    capability.status.name,
                    capability.notes,
                    now,
                    now,
                    capability.createdBy ?: "system",
                    capability.updatedBy ?: "system"
                )

                ctx.sqlExecutor.executeUpdate(insertSql, params)
                capability.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor capability")
        }
    }

    override suspend fun updateCapability(capability: VendorCapability): DomainResult<VendorCapability> {
        val tenant = TenantContext(capability.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_capabilities SET
                        display_name = ?,
                        status = ?,
                        notes = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND capability_id = ? AND version = ?
                """.trimIndent()

                val params = listOf(
                    capability.displayName,
                    capability.status.name,
                    capability.notes,
                    now,
                    capability.updatedBy ?: "system",
                    tenant.projectId,
                    capability.capabilityId,
                    capability.version
                )

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw IllegalStateException("Optimistic lock failure or capability not found: '${capability.capabilityId}'.")
                }
                capability.copy(version = capability.version + 1L, updatedAt = now.time)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor capability")
        }
    }

    override suspend fun updateStatus(projectId: String, capabilityId: String, status: CapabilityStatus, updatedBy: String): DomainResult<VendorCapability> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_capabilities SET
                        status = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND capability_id = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, listOf(status.name, now, updatedBy, tenant.projectId, capabilityId))
                if (rows == 0) {
                    throw NoSuchElementException("Vendor capability '$capabilityId' not found.")
                }

                val findSql = "SELECT * FROM vendor_capabilities WHERE project_id = ? AND capability_id = ?"
                ctx.sqlExecutor.querySingleOrNull(findSql, listOf(tenant.projectId, capabilityId)) { rs ->
                    mapRow(rs)
                } ?: throw NoSuchElementException("Failed to retrieve updated capability '$capabilityId'.")
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor capability status")
        }
    }
}
