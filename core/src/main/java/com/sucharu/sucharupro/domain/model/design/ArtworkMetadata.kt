package com.sucharu.sucharupro.domain.model.design

/**
 * Printing and prepress specification metadata for an Artwork Version.
 *
 * NOTE: [isPrintReady] is strictly informational and does NOT imply customer approval,
 * final sign-off, or automated production handoff.
 */
data class ArtworkMetadata(
    val width: Double? = null,
    val height: Double? = null,
    val unit: String = "in",
    val orientation: String = "PORTRAIT",
    val colorMode: String = "CMYK",
    val colorProfile: String? = null,
    val bleedMarginMm: Double? = null,
    val trimWidthMm: Double? = null,
    val trimHeightMm: Double? = null,
    val safeAreaMarginMm: Double? = null,
    val pageCount: Int = 1,
    val resolutionDpi: Int? = 300,
    val isPrintReady: Boolean = false,
    val notes: String? = null
) {
    init {
        require(pageCount >= 1) { "Page count must be at least 1." }
        if (width != null) require(width > 0) { "Width must be positive." }
        if (height != null) require(height > 0) { "Height must be positive." }
        if (resolutionDpi != null) require(resolutionDpi > 0) { "Resolution DPI must be positive." }
    }

    /** Formatted dimensions string, e.g. "8.5 x 11.0 in". */
    val formattedDimensions: String?
        get() = if (width != null && height != null) {
            String.format("%.2f x %.2f %s", width, height, unit)
        } else null
}
