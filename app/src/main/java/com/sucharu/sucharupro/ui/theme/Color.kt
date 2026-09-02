package com.sucharu.sucharupro.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================================
// Brand Color Palette - Primary (Process Ink Blue / Deep Cyan)
// ============================================================================
val PrimaryLight = Color(0xFF0061A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD1E4FF)
val OnPrimaryContainerLight = Color(0xFF001D36)

val PrimaryDark = Color(0xFF9ECAFF)
val OnPrimaryDark = Color(0xFF003258)
val PrimaryContainerDark = Color(0xFF00497D)
val OnPrimaryContainerDark = Color(0xFFD1E4FF)

// ============================================================================
// Secondary Palette (Steel Workshop / Cool Slate)
// ============================================================================
val SecondaryLight = Color(0xFF4F6070)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD3E4F7)
val OnSecondaryContainerLight = Color(0xFF0B1D2B)

val SecondaryDark = Color(0xFFB7C8D8)
val OnSecondaryDark = Color(0xFF21323F)
val SecondaryContainerDark = Color(0xFF384957)
val OnSecondaryContainerDark = Color(0xFFD3E4F7)

// ============================================================================
// Tertiary Palette (Warm Amber / Ochre Craft)
// ============================================================================
val TertiaryLight = Color(0xFF934B00)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDCC2)
val OnTertiaryContainerLight = Color(0xFF301400)

val TertiaryDark = Color(0xFFFFB77C)
val OnTertiaryDark = Color(0xFF4F2500)
val TertiaryContainerDark = Color(0xFF703800)
val OnTertiaryContainerDark = Color(0xFFFFDCC2)

// ============================================================================
// Error & Alert Palette
// ============================================================================
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ============================================================================
// Neutral Surfaces & Backgrounds
// ============================================================================
val BackgroundLight = Color(0xFFF8FAFC)
val OnBackgroundLight = Color(0xFF191C1E)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF191C1E)
val SurfaceVariantLight = Color(0xFFDFE2EB)
val OnSurfaceVariantLight = Color(0xFF43474E)
val OutlineLight = Color(0xFF73777F)
val OutlineVariantLight = Color(0xFFC3C6CF)

val BackgroundDark = Color(0xFF0B131E)
val OnBackgroundDark = Color(0xFFE1E2E5)
val SurfaceDark = Color(0xFF121C28)
val OnSurfaceDark = Color(0xFFE1E2E5)
val SurfaceVariantDark = Color(0xFF43474E)
val OnSurfaceVariantDark = Color(0xFFC3C6CF)
val OutlineDark = Color(0xFF8D9199)
val OutlineVariantDark = Color(0xFF43474E)

// ============================================================================
// Status & Workflow Color Token
// ============================================================================
@Immutable
data class StatusColor(
    val container: Color,
    val content: Color,
    val border: Color = Color.Transparent
)

// ============================================================================
// Sucharu Status Color System
// Covers: Order Status, Production Stages, Payment States, Stock Levels
// ============================================================================
@Immutable
data class SucharuStatusColors(
    // ---- Commercial Order Lifecycle States ----
    val orderPending: StatusColor,
    val orderConfirmed: StatusColor,
    val orderInProduction: StatusColor,
    val orderReady: StatusColor,
    val orderDelivered: StatusColor,
    val orderOnHold: StatusColor,
    val orderCancelled: StatusColor,

    // ---- Production Stage Colors (13-stage canonical workflow) ----
    val stageDesign: StatusColor,
    val stageApproval: StatusColor,
    val stageQc: StatusColor,
    val stageItemApproval: StatusColor,
    val stageCtp: StatusColor,
    val stagePrinting: StatusColor,
    val stageLamination: StatusColor,
    val stageFolding: StatusColor,
    val stageBinding: StatusColor,
    val stageFinalQc: StatusColor,
    val stagePackaging: StatusColor,
    val stageReady: StatusColor,
    val stageDelivered: StatusColor,

    // ---- Payment States ----
    val paymentPaid: StatusColor,
    val paymentPartial: StatusColor,
    val paymentUnpaid: StatusColor,
    val paymentOverdue: StatusColor,

    // ---- Finished Product Stock Levels ----
    val stockInStock: StatusColor,
    val stockLow: StatusColor,
    val stockOut: StatusColor
) {
    // ---- Legacy aliases for backward compatibility during migration ----
    // These map old OrderStatusType-named properties to new equivalents.
    // Remove after all usages are migrated.
    @Deprecated("Use orderPending instead", ReplaceWith("orderPending"))
    val statusPending: StatusColor get() = orderPending

    @Deprecated("Use stageDesign instead", ReplaceWith("stageDesign"))
    val statusDesigning: StatusColor get() = stageDesign

    @Deprecated("Use stageApproval instead", ReplaceWith("stageApproval"))
    val statusProofApproved: StatusColor get() = stageApproval

    @Deprecated("Use stagePrinting instead", ReplaceWith("stagePrinting"))
    val statusPrinting: StatusColor get() = stagePrinting

    @Deprecated("Use stageLamination/stageFolding/stageBinding instead", ReplaceWith("stageLamination"))
    val statusFinishing: StatusColor get() = stageLamination

    @Deprecated("Use orderReady instead", ReplaceWith("orderReady"))
    val statusReady: StatusColor get() = orderReady

    @Deprecated("Use orderDelivered instead", ReplaceWith("orderDelivered"))
    val statusDelivered: StatusColor get() = orderDelivered

    @Deprecated("Use orderCancelled instead", ReplaceWith("orderCancelled"))
    val statusCancelled: StatusColor get() = orderCancelled
}

val LightStatusColors = SucharuStatusColors(
    // Order states
    orderPending    = StatusColor(container = Color(0xFFF1F5F9), content = Color(0xFF475569), border = Color(0xFFCBD5E1)),
    orderConfirmed  = StatusColor(container = Color(0xFFE0F2FE), content = Color(0xFF0284C7), border = Color(0xFFBAE6FD)),
    orderInProduction = StatusColor(container = Color(0xFFDBEAFE), content = Color(0xFF1D4ED8), border = Color(0xFFBFDBFE)),
    orderReady      = StatusColor(container = Color(0xFFD1FAE5), content = Color(0xFF047857), border = Color(0xFFA7F3D0)),
    orderDelivered  = StatusColor(container = Color(0xFFCCFBF1), content = Color(0xFF0F766E), border = Color(0xFF99F6E4)),
    orderOnHold     = StatusColor(container = Color(0xFFFEF3C7), content = Color(0xFFB45309), border = Color(0xFFFDE68A)),
    orderCancelled  = StatusColor(container = Color(0xFFFEE2E2), content = Color(0xFFB91C1C), border = Color(0xFFFECACA)),

    // Production stages
    stageDesign       = StatusColor(container = Color(0xFFF3E8FF), content = Color(0xFF7C3AED), border = Color(0xFFDDD6FE)),
    stageApproval     = StatusColor(container = Color(0xFFEDE9FE), content = Color(0xFF6D28D9), border = Color(0xFFC4B5FD)),
    stageQc           = StatusColor(container = Color(0xFFFEF9C3), content = Color(0xFF854D0E), border = Color(0xFFFEF08A)),
    stageItemApproval = StatusColor(container = Color(0xFFE0F2FE), content = Color(0xFF0369A1), border = Color(0xFFBAE6FD)),
    stageCtp          = StatusColor(container = Color(0xFFE8F5E9), content = Color(0xFF2E7D32), border = Color(0xFFA5D6A7)),
    stagePrinting     = StatusColor(container = Color(0xFFDBEAFE), content = Color(0xFF1D4ED8), border = Color(0xFFBFDBFE)),
    stageLamination   = StatusColor(container = Color(0xFFFCE7F3), content = Color(0xFF9D174D), border = Color(0xFFFBCFE8)),
    stageFolding      = StatusColor(container = Color(0xFFFFF7ED), content = Color(0xFFC2410C), border = Color(0xFFFED7AA)),
    stageBinding      = StatusColor(container = Color(0xFFFEF3C7), content = Color(0xFFB45309), border = Color(0xFFFDE68A)),
    stageFinalQc      = StatusColor(container = Color(0xFFFEF9C3), content = Color(0xFF713F12), border = Color(0xFFFDE68A)),
    stagePackaging    = StatusColor(container = Color(0xFFE0F2FE), content = Color(0xFF075985), border = Color(0xFF7DD3FC)),
    stageReady        = StatusColor(container = Color(0xFFD1FAE5), content = Color(0xFF047857), border = Color(0xFFA7F3D0)),
    stageDelivered    = StatusColor(container = Color(0xFFCCFBF1), content = Color(0xFF0F766E), border = Color(0xFF99F6E4)),

    // Payment
    paymentPaid    = StatusColor(container = Color(0xFFDCFCE7), content = Color(0xFF15803D), border = Color(0xFFBBF7D0)),
    paymentPartial = StatusColor(container = Color(0xFFFEF3C7), content = Color(0xFFB45309), border = Color(0xFFFDE68A)),
    paymentUnpaid  = StatusColor(container = Color(0xFFFEE2E2), content = Color(0xFFB91C1C), border = Color(0xFFFECACA)),
    paymentOverdue = StatusColor(container = Color(0xFFFFE4E6), content = Color(0xFF9F1239), border = Color(0xFFFECDD3)),

    // Stock
    stockInStock = StatusColor(container = Color(0xFFDCFCE7), content = Color(0xFF166534)),
    stockLow     = StatusColor(container = Color(0xFFFEF3C7), content = Color(0xFF92400E)),
    stockOut     = StatusColor(container = Color(0xFFFEE2E2), content = Color(0xFF991B1B))
)

val DarkStatusColors = SucharuStatusColors(
    // Order states
    orderPending    = StatusColor(container = Color(0xFF1E293B), content = Color(0xFF94A3B8), border = Color(0xFF334155)),
    orderConfirmed  = StatusColor(container = Color(0xFF0C4A6E), content = Color(0xFF7DD3FC), border = Color(0xFF0369A1)),
    orderInProduction = StatusColor(container = Color(0xFF172554), content = Color(0xFF93C5FD), border = Color(0xFF1E40AF)),
    orderReady      = StatusColor(container = Color(0xFF064E3B), content = Color(0xFF6EE7B7), border = Color(0xFF047857)),
    orderDelivered  = StatusColor(container = Color(0xFF134E4A), content = Color(0xFF5EEAD4), border = Color(0xFF115E59)),
    orderOnHold     = StatusColor(container = Color(0xFF451A03), content = Color(0xFFFCD34D), border = Color(0xFF78350F)),
    orderCancelled  = StatusColor(container = Color(0xFF450A0A), content = Color(0xFFFCA5A5), border = Color(0xFF7F1D1D)),

    // Production stages
    stageDesign       = StatusColor(container = Color(0xFF3B1864), content = Color(0xFFC4B5FD), border = Color(0xFF5B21B6)),
    stageApproval     = StatusColor(container = Color(0xFF2E1065), content = Color(0xFFA78BFA), border = Color(0xFF4C1D95)),
    stageQc           = StatusColor(container = Color(0xFF422006), content = Color(0xFFFCD34D), border = Color(0xFF78350F)),
    stageItemApproval = StatusColor(container = Color(0xFF0C4A6E), content = Color(0xFF7DD3FC), border = Color(0xFF0369A1)),
    stageCtp          = StatusColor(container = Color(0xFF14532D), content = Color(0xFF86EFAC), border = Color(0xFF166534)),
    stagePrinting     = StatusColor(container = Color(0xFF172554), content = Color(0xFF93C5FD), border = Color(0xFF1E40AF)),
    stageLamination   = StatusColor(container = Color(0xFF500724), content = Color(0xFFF9A8D4), border = Color(0xFF831843)),
    stageFolding      = StatusColor(container = Color(0xFF431407), content = Color(0xFFFDBA74), border = Color(0xFF7C2D12)),
    stageBinding      = StatusColor(container = Color(0xFF451A03), content = Color(0xFFFDE68A), border = Color(0xFF78350F)),
    stageFinalQc      = StatusColor(container = Color(0xFF422006), content = Color(0xFFFDE68A), border = Color(0xFF78350F)),
    stagePackaging    = StatusColor(container = Color(0xFF0C4A6E), content = Color(0xFF38BDF8), border = Color(0xFF075985)),
    stageReady        = StatusColor(container = Color(0xFF064E3B), content = Color(0xFF6EE7B7), border = Color(0xFF047857)),
    stageDelivered    = StatusColor(container = Color(0xFF134E4A), content = Color(0xFF5EEAD4), border = Color(0xFF115E59)),

    // Payment
    paymentPaid    = StatusColor(container = Color(0xFF052E16), content = Color(0xFF86EFAC), border = Color(0xFF14532D)),
    paymentPartial = StatusColor(container = Color(0xFF451A03), content = Color(0xFFFDE68A), border = Color(0xFF78350F)),
    paymentUnpaid  = StatusColor(container = Color(0xFF450A0A), content = Color(0xFFFCA5A5), border = Color(0xFF7F1D1D)),
    paymentOverdue = StatusColor(container = Color(0xFF4C0519), content = Color(0xFFFDA4AF), border = Color(0xFF881337)),

    // Stock
    stockInStock = StatusColor(container = Color(0xFF052E16), content = Color(0xFF86EFAC)),
    stockLow     = StatusColor(container = Color(0xFF451A03), content = Color(0xFFFDE68A)),
    stockOut     = StatusColor(container = Color(0xFF450A0A), content = Color(0xFFFCA5A5))
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }