package com.sucharu.sucharupro.data.model

import kotlinx.serialization.Serializable

/**
 * Dynamic Server-Driven Design Gallery Template Item Model.
 */
@Serializable
data class GalleryTemplateItem(
    val id: String = "",
    val category: String = "সব", // e.g., "পোস্টার", "ভিজিটিং কার্ড", "লিফলেট", "স্টিকার"
    val templateCode: String = "#TMPL-101",
    val title: String = "",
    val imageUrl: String = "",
    val badgeText: String = "বেস্টসেলার",
    val badgeColorHex: String = "#0088FF",
    val cardBackgroundHex: String = "#1E293B",
    val textColorHex: String = "#FFFFFF",
    val fontStyle: String = "DEFAULT", // DEFAULT, BOLD, SERIF
    val colorSpec: String = "৪ কালার (CMYK)",
    val gsmSpec: String = "১৫০ GSM আর্ট পেপার",
    val sizeSpec: String = "১৮\" × ২৩\" (Demy)",
    val finishSpec: String = "গ্লস ল্যামিনেশন",
    val subtitleText: String = "",
    val priceText: String = "৳ ৩৫০ / ১,০০০ পিস",
    val perPieceText: String = "(৳ ০.৩৫ / পিস)",
    val orderPriority: Int = 0,
    val isActive: Boolean = true
)
