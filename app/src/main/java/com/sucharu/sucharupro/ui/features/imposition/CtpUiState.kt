package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.data.api.model.imposition.CtpOutputSpecificationResponseDto
import com.sucharu.sucharupro.domain.model.imposition.CtpOutputStatus
import com.sucharu.sucharupro.domain.model.imposition.OutputResolutionDpi
import com.sucharu.sucharupro.domain.model.imposition.PlateColorSeparation
import com.sucharu.sucharupro.domain.model.imposition.ScreeningMethod
import java.math.BigDecimal

/**
 * UI State for CTP Prepress Output & Plate Package Command Center.
 * Module 18 Step 05.
 */
data class CtpUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTab: Int = 0, // 0: Plate Visualizer, 1: Color Separations, 2: Prepress Marks, 3: Production Package, 4: Handoff & History
    val activePlateIndex: Int = 0,
    val activeColorChannel: PlateColorSeparation = PlateColorSeparation.BLACK,
    val showPlateGrid: Boolean = true,
    val showMarks: Boolean = true,
    val showBleedLines: Boolean = true,
    val showGripperZone: Boolean = true,
    val showColorBars: Boolean = true,
    val currentSpecification: CtpOutputSpecificationResponseDto? = null,
    val specificationsList: List<CtpOutputSpecificationResponseDto> = emptyList(),

    // Input form parameters for live generation
    val inputJobId: String = "JOB-2026-PUB-01",
    val inputOrderId: String = "ORD-2026-0089",
    val inputOrderItemId: String = "ITEM-001",
    val inputProductName: String = "A4 Premium Catalog 16pp",
    val inputPlateWidthMm: String = "695.0000",
    val inputPlateHeightMm: String = "994.4000",
    val inputGripperMarginMm: String = "45.0000",
    val inputTailMarginMm: String = "25.0000",
    val inputSideGuideLeftMm: String = "30.0000",
    val inputSideGuideRightMm: String = "30.0000",
    val inputResolutionDpi: OutputResolutionDpi = OutputResolutionDpi.DPI_2540,
    val inputScreeningMethod: ScreeningMethod = ScreeningMethod.AM_CONVENTIONAL,
    val inputScreenRulingLpi: String = "175.0000",
    val inputIncludeRegistrationMarks: Boolean = true,
    val inputIncludeCropMarks: Boolean = true,
    val inputIncludeBleedMarks: Boolean = true,
    val inputIncludeColorBars: Boolean = true,
    val inputIncludePlateSlugs: Boolean = true,
    val inputIncludeSpotVarnish: Boolean = false,
    val inputSpotColorName: String = "Pantone 185 C",

    val handoffContractJson: String? = null
)
