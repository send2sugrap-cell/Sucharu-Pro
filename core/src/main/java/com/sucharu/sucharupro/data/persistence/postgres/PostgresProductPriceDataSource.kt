package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.pricing.*
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Form 04 Pricing & Commercial Rules.
 */
class PostgresProductPriceDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) {

    private fun mapPriceConfiguration(rs: ResultSet): ProductPriceConfiguration {
        return ProductPriceConfiguration(
            priceConfigId = rs.getString("price_config_id"),
            productId = rs.getString("product_id"),
            priceVersion = rs.getInt("price_version"),
            isActive = rs.getBoolean("is_active"),
            currency = rs.getString("currency") ?: "BDT",

            basePrice = rs.getDouble("base_price"),
            baseQuantity = rs.getInt("base_quantity"),
            unit = rs.getEnumByName("unit", InventoryUnit.PCS),
            minOrderQuantity = rs.getInt("min_order_quantity"),
            maxOrderQuantity = rs.getInt("max_order_quantity"),

            pricingModel = rs.getEnumByName("pricing_model", PricingModel.BREAK_PRICE),
            discountType = rs.getEnumByName("discount_type", CommercialDiscountType.NONE),
            discountValue = rs.getDouble("discount_value"),

            surchargeAmount = rs.getDouble("surcharge_amount"),
            insideDhakaDeliveryCharge = rs.getDouble("inside_dhaka_delivery_charge"),
            outsideDhakaDeliveryCharge = rs.getDouble("outside_dhaka_delivery_charge"),
            taxPercentage = rs.getDouble("tax_percentage"),
            roundingRule = rs.getEnumByName("rounding_rule", RoundingRule.NEAREST),

            validFrom = rs.getTimestamp("valid_from")?.toInstant()?.toString(),
            validUntil = rs.getTimestamp("valid_until")?.toInstant()?.toString(),

            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            createdBy = rs.getString("created_by") ?: "SYSTEM",
            updatedBy = rs.getString("updated_by")
        )
    }

    suspend fun insertPriceConfiguration(config: ProductPriceConfiguration, tenantId: String = defaultTenantId): ProductPriceConfiguration {
        val tenant = TenantContext(tenantId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO product_price_configurations (
                    project_id, price_config_id, product_id, price_version, is_active, currency,
                    base_price, base_quantity, unit, min_order_quantity, max_order_quantity,
                    pricing_model, discount_type, discount_value,
                    surcharge_amount, inside_dhaka_delivery_charge, outside_dhaka_delivery_charge, tax_percentage, rounding_rule,
                    valid_from, valid_until, created_at, updated_at, created_by, updated_by, version
                ) VALUES (
                    ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, NOW(), NOW(), ?, ?, 1
                )
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    tenant.projectId, config.priceConfigId, config.productId, config.priceVersion, config.isActive, config.currency,
                    config.basePrice, config.baseQuantity, config.unit.name, config.minOrderQuantity, config.maxOrderQuantity,
                    config.pricingModel.name, config.discountType.name, config.discountValue,
                    config.surchargeAmount, config.insideDhakaDeliveryCharge, config.outsideDhakaDeliveryCharge, config.taxPercentage, config.roundingRule.name,
                    config.validFrom, config.validUntil, config.createdBy, config.updatedBy
                )
            )
        }
        return config
    }

    suspend fun getActivePriceConfigByProduct(productId: String, tenantId: String = defaultTenantId): ProductPriceConfiguration? {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM product_price_configurations
                WHERE project_id = ? AND product_id = ? AND is_active = TRUE
                ORDER BY price_version DESC
                LIMIT 1
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, productId)) { rs ->
                mapPriceConfiguration(rs)
            }.firstOrNull()
        }
    }
}
