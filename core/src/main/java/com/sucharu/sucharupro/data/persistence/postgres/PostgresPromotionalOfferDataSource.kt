package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.offer.OfferType
import com.sucharu.sucharupro.domain.model.offer.PromotionalOffer
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Form 03 Promotional Offers & Audience Eligibility.
 */
class PostgresPromotionalOfferDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) {

    private fun mapPromotionalOffer(rs: ResultSet): PromotionalOffer {
        return PromotionalOffer(
            offerId = rs.getString("offer_id"),
            offerName = rs.getString("offer_name"),
            offerCode = rs.getString("offer_code"),
            offerType = rs.getEnumByName("offer_type", OfferType.PROMOTIONAL_DISCOUNT),
            description = rs.getString("description"),
            badgeText = rs.getString("badge_text"),
            termsAndConditions = rs.getString("terms_and_conditions"),

            isEveryoneEligible = rs.getBoolean("is_everyone_eligible"),
            isGuestEligible = rs.getBoolean("is_guest_eligible"),
            isCustomerEligible = rs.getBoolean("is_customer_eligible"),
            isAffiliateEligible = rs.getBoolean("is_affiliate_eligible"),

            isActive = rs.getBoolean("is_active"),
            startAt = rs.getTimestamp("start_at")?.toInstant()?.toString(),
            endAt = rs.getTimestamp("end_at")?.toInstant()?.toString(),
            maxRedemption = rs.getInt("max_redemption"),
            perCustomerLimit = rs.getInt("per_customer_limit"),
            minOrderQuantity = rs.getInt("min_order_quantity"),
            minOrderValue = rs.getDouble("min_order_value"),

            visualDesignId = rs.getString("visual_design_id"),
            promotionalText = rs.getString("promotional_text"),
            ctaText = rs.getString("cta_text") ?: "অর্ডার করুন",
            displayPriority = rs.getInt("display_priority"),

            publicGuestWallVisible = rs.getBoolean("public_guest_wall_visible"),
            customerWallVisible = rs.getBoolean("customer_wall_visible"),
            affiliateWallVisible = rs.getBoolean("affiliate_wall_visible"),

            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            createdBy = rs.getString("created_by") ?: "SYSTEM",
            updatedBy = rs.getString("updated_by")
        )
    }

    suspend fun insertOffer(offer: PromotionalOffer, tenantId: String = defaultTenantId): PromotionalOffer {
        val tenant = TenantContext(tenantId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO promotional_offers (
                    project_id, offer_id, offer_name, offer_code, offer_type, description, badge_text, terms_and_conditions,
                    is_everyone_eligible, is_guest_eligible, is_customer_eligible, is_affiliate_eligible,
                    is_active, start_at, end_at, max_redemption, per_customer_limit, min_order_quantity, min_order_value,
                    visual_design_id, promotional_text, cta_text, display_priority,
                    public_guest_wall_visible, customer_wall_visible, affiliate_wall_visible,
                    created_at, updated_at, created_by, updated_by, version
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?,
                    NOW(), NOW(), ?, ?, 1
                )
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    tenant.projectId, offer.offerId, offer.offerName, offer.offerCode, offer.offerType.name, offer.description, offer.badgeText, offer.termsAndConditions,
                    offer.isEveryoneEligible, offer.isGuestEligible, offer.isCustomerEligible, offer.isAffiliateEligible,
                    offer.isActive, offer.startAt, offer.endAt, offer.maxRedemption, offer.perCustomerLimit, offer.minOrderQuantity, offer.minOrderValue,
                    offer.visualDesignId, offer.promotionalText, offer.ctaText, offer.displayPriority,
                    offer.publicGuestWallVisible, offer.customerWallVisible, offer.affiliateWallVisible,
                    offer.createdBy, offer.updatedBy
                )
            )
        }
        return offer
    }

    suspend fun listAllOffers(tenantId: String = defaultTenantId): List<PromotionalOffer> {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM promotional_offers
                WHERE project_id = ?
                ORDER BY display_priority ASC, created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId)) { rs ->
                mapPromotionalOffer(rs)
            }
        }
    }

    suspend fun getOfferById(offerId: String, tenantId: String = defaultTenantId): PromotionalOffer? {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM promotional_offers
                WHERE project_id = ? AND offer_id = ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, offerId)) { rs ->
                mapPromotionalOffer(rs)
            }.firstOrNull()
        }
    }
}
