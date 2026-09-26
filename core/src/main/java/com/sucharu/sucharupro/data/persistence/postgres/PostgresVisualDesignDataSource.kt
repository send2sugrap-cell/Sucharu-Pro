package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.design.*
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Form 02 Visual Design Studio configurations.
 */
class PostgresVisualDesignDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) {

    private fun mapVisualDesign(rs: ResultSet): VisualDesignConfiguration {
        return VisualDesignConfiguration(
            designId = rs.getString("design_id"),
            designName = rs.getString("design_name"),
            targetType = rs.getEnumByName("target_type", DesignTargetType.PRODUCT_GALLERY_CARD),
            targetId = rs.getString("target_id"),
            versionNumber = rs.getInt("version_number"),
            status = rs.getEnumByName("status", DesignPublishStatus.DRAFT),
            isActivePublished = rs.getBoolean("is_active_published"),
            displayOrder = rs.getInt("display_order"),

            cardWidthDp = rs.getInt("card_width_dp"),
            cardHeightDp = rs.getInt("card_height_dp"),
            layoutType = rs.getEnumByName("layout_type", LayoutType.GRID),
            contentPosition = rs.getEnumByName("content_position", ContentPosition.BOTTOM),
            imagePosition = rs.getEnumByName("image_position", ImagePosition.TOP),
            imageRatio = rs.getEnumByName("image_ratio", ImageRatio.RATIO_4_3),
            alignment = rs.getEnumByName("alignment", Alignment.CENTER),
            columnCount = rs.getInt("column_count"),

            backgroundColorHex = rs.getString("background_color_hex") ?: "#1E293B",
            gradientEnable = rs.getBoolean("gradient_enable"),
            gradientStartColorHex = rs.getString("gradient_start_color_hex"),
            gradientEndColorHex = rs.getString("gradient_end_color_hex"),
            gradientDirection = rs.getEnumByName("gradient_direction", GradientDirection.TOP_TO_BOTTOM),
            backgroundOpacity = rs.getFloat("background_opacity"),

            borderEnable = rs.getBoolean("border_enable"),
            borderColorHex = rs.getString("border_color_hex") ?: "#334155",
            borderWidthDp = rs.getInt("border_width_dp"),
            borderRadiusDp = rs.getInt("border_radius_dp"),

            shadowEnable = rs.getBoolean("shadow_enable"),
            shadowColorHex = rs.getString("shadow_color_hex") ?: "#000000",
            shadowBlurDp = rs.getInt("shadow_blur_dp"),
            shadowOffsetYDp = rs.getInt("shadow_offset_y_dp"),
            shadowOpacity = rs.getFloat("shadow_opacity"),

            paddingTopDp = rs.getInt("padding_top_dp"),
            paddingBottomDp = rs.getInt("padding_bottom_dp"),
            paddingLeftDp = rs.getInt("padding_left_dp"),
            paddingRightDp = rs.getInt("padding_right_dp"),
            elementGapDp = rs.getInt("element_gap_dp"),

            fontFamily = rs.getString("font_family") ?: "SOLAIMANLIPI",
            fontSizeSp = rs.getInt("font_size_sp"),
            fontWeight = rs.getString("font_weight") ?: "BOLD",
            textColorHex = rs.getString("text_color_hex") ?: "#FFFFFF",
            textOpacity = rs.getFloat("text_opacity"),

            showImage = rs.getBoolean("show_image"),
            showTitle = rs.getBoolean("show_title"),
            showSubtitle = rs.getBoolean("show_subtitle"),
            showDescription = rs.getBoolean("show_description"),
            showBadge = rs.getBoolean("show_badge"),
            showSpecs = rs.getBoolean("show_specs"),
            showPrice = rs.getBoolean("show_price"),
            showCta = rs.getBoolean("show_cta"),
            showRating = rs.getBoolean("show_rating"),
            showFavoriteButton = rs.getBoolean("show_favorite_button"),
            showShareButton = rs.getBoolean("show_share_button"),

            ctaButtonText = rs.getString("cta_button_text") ?: "অর্ডার করুন",
            ctaButtonStyle = rs.getEnumByName("cta_button_style", ButtonStyle.FILLED),
            ctaButtonColorHex = rs.getString("cta_button_color_hex") ?: "#EA580C",
            ctaTextColorHex = rs.getString("cta_text_color_hex") ?: "#FFFFFF",
            ctaBorderRadiusDp = rs.getInt("cta_border_radius_dp"),

            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            createdBy = rs.getString("created_by") ?: "SYSTEM",
            updatedBy = rs.getString("updated_by")
        )
    }

    suspend fun insertVisualDesign(config: VisualDesignConfiguration, tenantId: String = defaultTenantId): VisualDesignConfiguration {
        val tenant = TenantContext(tenantId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO visual_design_configurations (
                    project_id, design_id, design_name, target_type, target_id, version_number, status, is_active_published, display_order,
                    card_width_dp, card_height_dp, layout_type, content_position, image_position, image_ratio, alignment, column_count,
                    background_color_hex, gradient_enable, gradient_start_color_hex, gradient_end_color_hex, gradient_direction, background_opacity,
                    border_enable, border_color_hex, border_width_dp, border_radius_dp,
                    shadow_enable, shadow_color_hex, shadow_blur_dp, shadow_offset_y_dp, shadow_opacity,
                    padding_top_dp, padding_bottom_dp, padding_left_dp, padding_right_dp, element_gap_dp,
                    font_family, font_size_sp, font_weight, text_color_hex, text_opacity,
                    show_image, show_title, show_subtitle, show_description, show_badge, show_specs, show_price, show_cta,
                    show_rating, show_favorite_button, show_share_button,
                    cta_button_text, cta_button_style, cta_button_color_hex, cta_text_color_hex, cta_border_radius_dp,
                    created_at, updated_at, created_by, updated_by, version
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    NOW(), NOW(), ?, ?, 1
                )
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    tenant.projectId, config.designId, config.designName, config.targetType.name, config.targetId, config.versionNumber, config.status.name, config.isActivePublished, config.displayOrder,
                    config.cardWidthDp, config.cardHeightDp, config.layoutType.name, config.contentPosition.name, config.imagePosition.name, config.imageRatio.name, config.alignment.name, config.columnCount,
                    config.backgroundColorHex, config.gradientEnable, config.gradientStartColorHex, config.gradientEndColorHex, config.gradientDirection.name, config.backgroundOpacity,
                    config.borderEnable, config.borderColorHex, config.borderWidthDp, config.borderRadiusDp,
                    config.shadowEnable, config.shadowColorHex, config.shadowBlurDp, config.shadowOffsetYDp, config.shadowOpacity,
                    config.paddingTopDp, config.paddingBottomDp, config.paddingLeftDp, config.paddingRightDp, config.elementGapDp,
                    config.fontFamily, config.fontSizeSp, config.fontWeight, config.textColorHex, config.textOpacity,
                    config.showImage, config.showTitle, config.showSubtitle, config.showDescription, config.showBadge, config.showSpecs, config.showPrice, config.showCta,
                    config.showRating, config.showFavoriteButton, config.showShareButton,
                    config.ctaButtonText, config.ctaButtonStyle.name, config.ctaButtonColorHex, config.ctaTextColorHex, config.ctaBorderRadiusDp,
                    config.createdBy, config.updatedBy
                )
            )
        }
        return config
    }

    suspend fun listAllVisualDesigns(tenantId: String = defaultTenantId): List<VisualDesignConfiguration> {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM visual_design_configurations
                WHERE project_id = ?
                ORDER BY display_order ASC, created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId)) { rs ->
                mapVisualDesign(rs)
            }
        }
    }

    suspend fun getVisualDesignById(designId: String, tenantId: String = defaultTenantId): VisualDesignConfiguration? {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM visual_design_configurations
                WHERE project_id = ? AND design_id = ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, designId)) { rs ->
                mapVisualDesign(rs)
            }.firstOrNull()
        }
    }

    suspend fun updateVisualDesign(config: VisualDesignConfiguration, tenantId: String = defaultTenantId): VisualDesignConfiguration {
        val tenant = TenantContext(tenantId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE visual_design_configurations
                SET design_name = ?, target_type = ?, target_id = ?, version_number = ?, status = ?, is_active_published = ?, display_order = ?,
                    card_width_dp = ?, card_height_dp = ?, layout_type = ?, content_position = ?, image_position = ?, image_ratio = ?, alignment = ?, column_count = ?,
                    background_color_hex = ?, gradient_enable = ?, gradient_start_color_hex = ?, gradient_end_color_hex = ?, gradient_direction = ?, background_opacity = ?,
                    border_enable = ?, border_color_hex = ?, border_width_dp = ?, border_radius_dp = ?,
                    shadow_enable = ?, shadow_color_hex = ?, shadow_blur_dp = ?, shadow_offset_y_dp = ?, shadow_opacity = ?,
                    padding_top_dp = ?, padding_bottom_dp = ?, padding_left_dp = ?, padding_right_dp = ?, element_gap_dp = ?,
                    font_family = ?, font_size_sp = ?, font_weight = ?, text_color_hex = ?, text_opacity = ?,
                    show_image = ?, show_title = ?, show_subtitle = ?, show_description = ?, show_badge = ?, show_specs = ?, show_price = ?, show_cta = ?,
                    show_rating = ?, show_favorite_button = ?, show_share_button = ?,
                    cta_button_text = ?, cta_button_style = ?, cta_button_color_hex = ?, cta_text_color_hex = ?, cta_border_radius_dp = ?,
                    updated_at = NOW(), updated_by = ?, version = version + 1
                WHERE project_id = ? AND design_id = ?
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    config.designName, config.targetType.name, config.targetId, config.versionNumber, config.status.name, config.isActivePublished, config.displayOrder,
                    config.cardWidthDp, config.cardHeightDp, config.layoutType.name, config.contentPosition.name, config.imagePosition.name, config.imageRatio.name, config.alignment.name, config.columnCount,
                    config.backgroundColorHex, config.gradientEnable, config.gradientStartColorHex, config.gradientEndColorHex, config.gradientDirection.name, config.backgroundOpacity,
                    config.borderEnable, config.borderColorHex, config.borderWidthDp, config.borderRadiusDp,
                    config.shadowEnable, config.shadowColorHex, config.shadowBlurDp, config.shadowOffsetYDp, config.shadowOpacity,
                    config.paddingTopDp, config.paddingBottomDp, config.paddingLeftDp, config.paddingRightDp, config.elementGapDp,
                    config.fontFamily, config.fontSizeSp, config.fontWeight, config.textColorHex, config.textOpacity,
                    config.showImage, config.showTitle, config.showSubtitle, config.showDescription, config.showBadge, config.showSpecs, config.showPrice, config.showCta,
                    config.showRating, config.showFavoriteButton, config.showShareButton,
                    config.ctaButtonText, config.ctaButtonStyle.name, config.ctaButtonColorHex, config.ctaTextColorHex, config.ctaBorderRadiusDp,
                    config.updatedBy, tenant.projectId, config.designId
                )
            )
        }
        return config
    }
}
