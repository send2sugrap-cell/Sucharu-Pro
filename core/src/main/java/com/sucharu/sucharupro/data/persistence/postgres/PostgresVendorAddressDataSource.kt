package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorAddressDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.AddressType
import com.sucharu.sucharupro.domain.model.vendor.VendorAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorAddressDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorAddressDataSource {

    private val addressFlows = mutableMapOf<String, MutableStateFlow<List<VendorAddress>>>()

    private fun mapRow(rs: ResultSet): VendorAddress {
        return VendorAddress(
            addressId = rs.getString("address_id"),
            vendorId = rs.getString("vendor_id"),
            projectId = rs.getString("project_id"),
            addressType = runCatching { AddressType.valueOf(rs.getString("address_type")) }.getOrDefault(AddressType.OFFICE),
            addressLine1 = rs.getString("address_line1"),
            addressLine2 = rs.getString("address_line2"),
            city = rs.getString("city"),
            district = rs.getString("district"),
            postalCode = rs.getString("postal_code"),
            country = rs.getString("country"),
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

    override fun observeAddresses(projectId: String, vendorId: String): Flow<List<VendorAddress>> {
        val key = "$projectId:$vendorId"
        return synchronized(addressFlows) {
            addressFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findById(projectId: String, addressId: String): DomainResult<VendorAddress> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val address = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_addresses WHERE project_id = ? AND address_id = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, addressId)) { rs ->
                    mapRow(rs)
                }
            }
            if (address != null) {
                DomainResult.Success(address)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor address '$addressId' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor address")
        }
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, activeOnly: Boolean): DomainResult<List<VendorAddress>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = if (activeOnly) {
                    "SELECT * FROM vendor_addresses WHERE project_id = ? AND vendor_id = ? AND active = true ORDER BY is_primary DESC, address_type ASC"
                } else {
                    "SELECT * FROM vendor_addresses WHERE project_id = ? AND vendor_id = ? ORDER BY is_primary DESC, address_type ASC"
                }
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, vendorId)) { rs ->
                    mapRow(rs)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor addresses")
        }
    }

    override suspend fun createAddress(address: VendorAddress): DomainResult<VendorAddress> {
        val tenant = TenantContext(address.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val insertSql = """
                    INSERT INTO vendor_addresses (
                        project_id, address_id, vendor_id, address_type, address_line1,
                        address_line2, city, district, postal_code, country,
                        notes, is_primary, active, created_at, updated_at,
                        created_by, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    address.addressId,
                    address.vendorId,
                    address.addressType.name,
                    address.addressLine1,
                    address.addressLine2,
                    address.city,
                    address.district,
                    address.postalCode,
                    address.country,
                    address.notes,
                    address.isPrimary,
                    address.active,
                    now,
                    now,
                    address.createdBy ?: "system",
                    address.updatedBy ?: "system"
                )

                ctx.sqlExecutor.executeUpdate(insertSql, params)
                address.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor address")
        }
    }

    override suspend fun updateAddress(address: VendorAddress): DomainResult<VendorAddress> {
        val tenant = TenantContext(address.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_addresses SET
                        address_type = ?,
                        address_line1 = ?,
                        address_line2 = ?,
                        city = ?,
                        district = ?,
                        postal_code = ?,
                        country = ?,
                        notes = ?,
                        is_primary = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND address_id = ? AND version = ?
                """.trimIndent()

                val params = listOf(
                    address.addressType.name,
                    address.addressLine1,
                    address.addressLine2,
                    address.city,
                    address.district,
                    address.postalCode,
                    address.country,
                    address.notes,
                    address.isPrimary,
                    now,
                    address.updatedBy ?: "system",
                    tenant.projectId,
                    address.addressId,
                    address.version
                )

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw IllegalStateException("Optimistic lock failure or address not found: '${address.addressId}'.")
                }
                address.copy(version = address.version + 1L, updatedAt = now.time)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor address")
        }
    }

    override suspend fun updateStatus(projectId: String, addressId: String, active: Boolean, updatedBy: String): DomainResult<VendorAddress> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_addresses SET
                        active = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND address_id = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, listOf(active, now, updatedBy, tenant.projectId, addressId))
                if (rows == 0) {
                    throw NoSuchElementException("Vendor address '$addressId' not found.")
                }

                val findSql = "SELECT * FROM vendor_addresses WHERE project_id = ? AND address_id = ?"
                ctx.sqlExecutor.querySingleOrNull(findSql, listOf(tenant.projectId, addressId)) { rs ->
                    mapRow(rs)
                } ?: throw NoSuchElementException("Failed to retrieve updated address '$addressId'.")
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor address status")
        }
    }
}
