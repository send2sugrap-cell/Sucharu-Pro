package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorProfileDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorProfileDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorProfileDataSource {

    private val profileFlows = mutableMapOf<String, MutableStateFlow<VendorProfile?>>()

    private fun mapRow(rs: ResultSet): VendorProfile {
        return VendorProfile(
            vendorId = rs.getString("vendor_id"),
            projectId = rs.getString("project_id"),
            legalName = rs.getString("legal_name"),
            displayName = rs.getString("display_name"),
            contactPerson = rs.getString("contact_person"),
            primaryPhone = rs.getString("primary_phone"),
            alternatePhone = rs.getString("alternate_phone"),
            email = rs.getString("email"),
            website = rs.getString("website"),
            taxId = rs.getString("tax_id"),
            businessRegistrationNumber = rs.getString("business_registration_number"),
            notes = rs.getString("notes"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    override fun observeProfile(projectId: String, vendorId: String): Flow<VendorProfile?> {
        val key = "$projectId:$vendorId"
        return synchronized(profileFlows) {
            profileFlows.getOrPut(key) { MutableStateFlow(null) }.asStateFlow()
        }
    }

    override suspend fun findByVendorId(projectId: String, vendorId: String): DomainResult<VendorProfile> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val profile = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT * FROM vendor_profiles
                    WHERE project_id = ? AND vendor_id = ?
                """.trimIndent()
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, vendorId)) { rs ->
                    mapRow(rs)
                }
            }
            if (profile != null) {
                DomainResult.Success(profile)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor profile not found for vendor '$vendorId'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor profile")
        }
    }

    override suspend fun saveProfile(profile: VendorProfile): DomainResult<VendorProfile> {
        val tenant = TenantContext(profile.projectId.ifBlank { defaultTenantId })
        return try {
            val saved = transactionManager.inTransaction(tenant) { ctx ->
                val checkSql = "SELECT version FROM vendor_profiles WHERE project_id = ? AND vendor_id = ?"
                val existingVersion = ctx.sqlExecutor.querySingleOrNull(checkSql, listOf(tenant.projectId, profile.vendorId)) { rs ->
                    rs.getLong("version")
                }

                val now = Timestamp(System.currentTimeMillis())
                if (existingVersion == null) {
                    val insertSql = """
                        INSERT INTO vendor_profiles (
                            project_id, vendor_id, legal_name, display_name, contact_person,
                            primary_phone, alternate_phone, email, website, tax_id,
                            business_registration_number, notes, created_at, updated_at,
                            created_by, updated_by, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """.trimIndent()

                    val params = listOf(
                        tenant.projectId,
                        profile.vendorId,
                        profile.legalName,
                        profile.displayName,
                        profile.contactPerson,
                        profile.primaryPhone,
                        profile.alternatePhone,
                        profile.email,
                        profile.website,
                        profile.taxId,
                        profile.businessRegistrationNumber,
                        profile.notes,
                        now,
                        now,
                        profile.createdBy ?: "system",
                        profile.updatedBy ?: "system"
                    )
                    ctx.sqlExecutor.executeUpdate(insertSql, params)
                    profile.copy(version = 1L, createdAt = now.time, updatedAt = now.time)
                } else {
                    val updateSql = """
                        UPDATE vendor_profiles SET
                            legal_name = ?,
                            display_name = ?,
                            contact_person = ?,
                            primary_phone = ?,
                            alternate_phone = ?,
                            email = ?,
                            website = ?,
                            tax_id = ?,
                            business_registration_number = ?,
                            notes = ?,
                            updated_at = ?,
                            updated_by = ?,
                            version = version + 1
                        WHERE project_id = ? AND vendor_id = ? AND version = ?
                    """.trimIndent()

                    val params = listOf(
                        profile.legalName,
                        profile.displayName,
                        profile.contactPerson,
                        profile.primaryPhone,
                        profile.alternatePhone,
                        profile.email,
                        profile.website,
                        profile.taxId,
                        profile.businessRegistrationNumber,
                        profile.notes,
                        now,
                        profile.updatedBy ?: "system",
                        tenant.projectId,
                        profile.vendorId,
                        profile.version
                    )
                    val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                    if (rows == 0) {
                        throw IllegalStateException("Optimistic lock failure on vendor profile: '${profile.vendorId}'.")
                    }
                    profile.copy(version = profile.version + 1L, updatedAt = now.time)
                }
            }

            val key = "${tenant.projectId}:${profile.vendorId}"
            synchronized(profileFlows) {
                profileFlows[key]?.value = saved
            }

            DomainResult.Success(saved)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "save vendor profile")
        }
    }
}
