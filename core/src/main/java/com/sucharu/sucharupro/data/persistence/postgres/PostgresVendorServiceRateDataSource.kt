package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorServiceRateDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorServiceRateDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorServiceRateDataSource {

    private val rateFlows = mutableMapOf<String, MutableStateFlow<List<VendorServiceRate>>>()

    private fun mapTierRow(rs: ResultSet): VendorServiceRateTier {
        return VendorServiceRateTier(
            tierId = rs.getString("tier_id"),
            projectId = rs.getString("project_id"),
            rateId = rs.getString("rate_id"),
            minimumQuantity = rs.getBigDecimal("minimum_quantity"),
            maximumQuantity = rs.getBigDecimal("maximum_quantity"),
            rateAmount = Money(rs.getBigDecimal("rate_amount")),
            version = rs.getLong("version")
        )
    }

    private fun mapRateRow(rs: ResultSet, tiers: List<VendorServiceRateTier> = emptyList()): VendorServiceRate {
        return VendorServiceRate(
            rateId = rs.getString("rate_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            capabilityType = CapabilityType.valueOf(rs.getString("capability_type")),
            rateCode = rs.getString("rate_code"),
            serviceName = rs.getString("service_name"),
            pricingMethod = PricingMethod.valueOf(rs.getString("pricing_method")),
            unitOfMeasure = UnitOfMeasure.valueOf(rs.getString("unit_of_measure")),
            rateAmount = Money(rs.getBigDecimal("rate_amount")),
            currency = rs.getString("currency"),
            minimumQuantity = rs.getBigDecimal("minimum_quantity"),
            maximumQuantity = rs.getBigDecimal("maximum_quantity"),
            effectiveFrom = rs.getTimestamp("effective_from").time,
            effectiveTo = rs.getTimestamp("effective_to")?.time,
            status = RateStatus.valueOf(rs.getString("status")),
            tiers = tiers,
            notes = rs.getString("notes"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    override fun observeRates(projectId: String, vendorId: String): Flow<List<VendorServiceRate>> {
        val key = "$projectId:$vendorId"
        return synchronized(rateFlows) {
            rateFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findById(projectId: String, rateId: String): DomainResult<VendorServiceRate> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val rate = transactionManager.inReadOnly(tenant) { ctx ->
                val tiersSql = "SELECT * FROM vendor_service_rate_tiers WHERE project_id = ? AND rate_id = ? ORDER BY minimum_quantity ASC"
                val tiers = ctx.sqlExecutor.queryList(tiersSql, listOf(tenant.projectId, rateId)) { rs -> mapTierRow(rs) }

                val rateSql = "SELECT * FROM vendor_service_rates WHERE project_id = ? AND rate_id = ?"
                ctx.sqlExecutor.querySingleOrNull(rateSql, listOf(tenant.projectId, rateId)) { rs ->
                    mapRateRow(rs, tiers)
                }
            }
            if (rate != null) {
                DomainResult.Success(rate)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor service rate '$rateId' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor service rate")
        }
    }

    override suspend fun findByRateCode(projectId: String, rateCode: String): DomainResult<VendorServiceRate> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val rate = transactionManager.inReadOnly(tenant) { ctx ->
                val rateSql = "SELECT * FROM vendor_service_rates WHERE project_id = ? AND rate_code = ?"
                val base = ctx.sqlExecutor.querySingleOrNull(rateSql, listOf(tenant.projectId, rateCode)) { rs ->
                    mapRateRow(rs)
                } ?: return@inReadOnly null

                val tiersSql = "SELECT * FROM vendor_service_rate_tiers WHERE project_id = ? AND rate_id = ? ORDER BY minimum_quantity ASC"
                val tiers = ctx.sqlExecutor.queryList(tiersSql, listOf(tenant.projectId, base.rateId)) { rs -> mapTierRow(rs) }
                base.copy(tiers = tiers)
            }
            if (rate != null) {
                DomainResult.Success(rate)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor service rate with code '$rateCode' not found."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor service rate by code")
        }
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, status: RateStatus?): DomainResult<List<VendorServiceRate>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = if (status != null) {
                    "SELECT * FROM vendor_service_rates WHERE project_id = ? AND vendor_id = ? AND status = ? ORDER BY effective_from DESC, service_name ASC"
                } else {
                    "SELECT * FROM vendor_service_rates WHERE project_id = ? AND vendor_id = ? ORDER BY effective_from DESC, service_name ASC"
                }
                val params = if (status != null) listOf(tenant.projectId, vendorId, status.name) else listOf(tenant.projectId, vendorId)
                ctx.sqlExecutor.queryList(sql, params) { rs ->
                    mapRateRow(rs)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor service rates")
        }
    }

    override suspend fun listByCapability(projectId: String, capabilityType: CapabilityType, status: RateStatus?): DomainResult<List<VendorServiceRate>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = if (status != null) {
                    "SELECT * FROM vendor_service_rates WHERE project_id = ? AND capability_type = ? AND status = ? ORDER BY rate_amount ASC"
                } else {
                    "SELECT * FROM vendor_service_rates WHERE project_id = ? AND capability_type = ? ORDER BY rate_amount ASC"
                }
                val params = if (status != null) listOf(tenant.projectId, capabilityType.name, status.name) else listOf(tenant.projectId, capabilityType.name)
                ctx.sqlExecutor.queryList(sql, params) { rs ->
                    mapRateRow(rs)
                }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list rates by capability")
        }
    }

    override suspend fun findApplicableRate(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        pricingMethod: PricingMethod?,
        unitOfMeasure: UnitOfMeasure?,
        effectiveDate: Long
    ): DomainResult<VendorServiceRate> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val effTimestamp = Timestamp(effectiveDate)
            val matched = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT * FROM vendor_service_rates
                    WHERE project_id = ?
                      AND vendor_id = ?
                      AND capability_type = ?
                      AND status = 'ACTIVE'
                      AND effective_from <= ?
                      AND (effective_to IS NULL OR effective_to >= ?)
                      AND (? IS NULL OR pricing_method = ?)
                      AND (? IS NULL OR unit_of_measure = ?)
                    ORDER BY effective_from DESC
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    vendorId,
                    capabilityType.name,
                    effTimestamp,
                    effTimestamp,
                    pricingMethod?.name,
                    pricingMethod?.name,
                    unitOfMeasure?.name,
                    unitOfMeasure?.name
                )

                val candidates = ctx.sqlExecutor.queryList(sql, params) { rs ->
                    mapRateRow(rs)
                }

                if (candidates.isEmpty()) return@inReadOnly null

                val selected = candidates.first()
                val tiersSql = "SELECT * FROM vendor_service_rate_tiers WHERE project_id = ? AND rate_id = ? ORDER BY minimum_quantity ASC"
                val tiers = ctx.sqlExecutor.queryList(tiersSql, listOf(tenant.projectId, selected.rateId)) { rs -> mapTierRow(rs) }
                selected.copy(tiers = tiers)
            }

            if (matched != null) {
                DomainResult.Success(matched)
            } else {
                DomainResult.Error(
                    NoSuchElementException("No active applicable rate found for vendor '$vendorId' in capability '${capabilityType.name}' on date '$effectiveDate'.")
                )
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "resolve applicable rate")
        }
    }

    override suspend fun createRate(rate: VendorServiceRate): DomainResult<VendorServiceRate> {
        val tenant = TenantContext(rate.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val insertRateSql = """
                    INSERT INTO vendor_service_rates (
                        project_id, rate_id, vendor_id, capability_type, rate_code,
                        service_name, pricing_method, unit_of_measure, rate_amount, currency,
                        minimum_quantity, maximum_quantity, effective_from, effective_to,
                        status, notes, created_at, updated_at, created_by, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    rate.rateId,
                    rate.vendorId,
                    rate.capabilityType.name,
                    rate.rateCode,
                    rate.serviceName,
                    rate.pricingMethod.name,
                    rate.unitOfMeasure.name,
                    rate.rateAmount.amount,
                    rate.currency,
                    rate.minimumQuantity,
                    rate.maximumQuantity,
                    Timestamp(rate.effectiveFrom),
                    rate.effectiveTo?.let { Timestamp(it) },
                    rate.status.name,
                    rate.notes,
                    now,
                    now,
                    rate.createdBy,
                    rate.updatedBy
                )

                ctx.sqlExecutor.executeUpdate(insertRateSql, params)

                // Insert tiers if tiered
                if (rate.tiers.isNotEmpty()) {
                    val insertTierSql = """
                        INSERT INTO vendor_service_rate_tiers (
                            project_id, tier_id, rate_id, minimum_quantity, maximum_quantity, rate_amount, version
                        ) VALUES (?, ?, ?, ?, ?, ?, 1)
                    """.trimIndent()
                    for (tier in rate.tiers) {
                        ctx.sqlExecutor.executeUpdate(
                            insertTierSql,
                            listOf(
                                tenant.projectId,
                                tier.tierId,
                                rate.rateId,
                                tier.minimumQuantity,
                                tier.maximumQuantity,
                                tier.rateAmount.amount
                            )
                        )
                    }
                }

                rate.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor service rate")
        }
    }

    override suspend fun updateRate(rate: VendorServiceRate): DomainResult<VendorServiceRate> {
        val tenant = TenantContext(rate.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_service_rates SET
                        service_name = ?,
                        minimum_quantity = ?,
                        maximum_quantity = ?,
                        effective_to = ?,
                        status = ?,
                        notes = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND rate_id = ? AND version = ?
                """.trimIndent()

                val params = listOf(
                    rate.serviceName,
                    rate.minimumQuantity,
                    rate.maximumQuantity,
                    rate.effectiveTo?.let { Timestamp(it) },
                    rate.status.name,
                    rate.notes,
                    now,
                    rate.updatedBy,
                    tenant.projectId,
                    rate.rateId,
                    rate.version
                )

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw IllegalStateException("Optimistic lock failure or rate not found: '${rate.rateId}'.")
                }
                rate.copy(version = rate.version + 1L, updatedAt = now.time)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor service rate")
        }
    }

    override suspend fun updateStatus(projectId: String, rateId: String, status: RateStatus, updatedBy: String): DomainResult<VendorServiceRate> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_service_rates SET
                        status = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND rate_id = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, listOf(status.name, now, updatedBy, tenant.projectId, rateId))
                if (rows == 0) {
                    throw NoSuchElementException("Vendor service rate '$rateId' not found.")
                }

                val findSql = "SELECT * FROM vendor_service_rates WHERE project_id = ? AND rate_id = ?"
                ctx.sqlExecutor.querySingleOrNull(findSql, listOf(tenant.projectId, rateId)) { rs ->
                    mapRateRow(rs)
                } ?: throw NoSuchElementException("Failed to retrieve updated rate '$rateId'.")
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor service rate status")
        }
    }

    override suspend fun getRateHistory(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<List<VendorServiceRate>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val history = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT * FROM vendor_service_rates
                    WHERE project_id = ? AND vendor_id = ? AND capability_type = ?
                    ORDER BY effective_from DESC, version DESC
                """.trimIndent()
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, vendorId, capabilityType.name)) { rs ->
                    mapRateRow(rs)
                }
            }
            DomainResult.Success(history)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "get rate history")
        }
    }
}
