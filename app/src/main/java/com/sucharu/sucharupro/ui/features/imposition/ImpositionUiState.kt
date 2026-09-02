package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.data.api.model.imposition.ImpositionCandidateDto
import com.sucharu.sucharupro.data.api.model.imposition.ImpositionSpecificationResponseDto
import java.math.BigDecimal

/**
 * UI State for Dynamic Imposition Command Center Screen.
 * Module 18 Step 01.
 */
data class ImpositionUiState(
    val isLoading: Boolean = false,
    val isCalculating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    
    // Inputs
    val jobId: String = "JOB-2026-001",
    val orderId: String = "ORD-2026-901",
    val orderItemId: String = "ITEM-01",
    val calculationId: String = "CALC-001",
    val productName: String = "Brochure A4 Full Color",
    val itemWidthMm: String = "210.0000",
    val itemHeightMm: String = "297.0000",
    val sheetWidthMm: String = "635.0000",
    val sheetHeightMm: String = "914.4000",
    val marginTopMm: String = "10.0000",
    val marginBottomMm: String = "10.0000",
    val marginLeftMm: String = "10.0000",
    val marginRightMm: String = "10.0000",
    val bleedMm: String = "3.0000",
    val horizontalGutterMm: String = "4.0000",
    val verticalGutterMm: String = "4.0000",
    val orientationPolicy: String = "AUTO_OPTIMAL",
    val requiredQuantity: String = "1000",
    val notes: String = "Single-job imposition optimized for 25x36 parent sheet",
    
    // Results
    val currentSpecification: ImpositionSpecificationResponseDto? = null,
    val specificationsList: List<ImpositionSpecificationResponseDto> = emptyList(),
    val candidateBreakdown: List<ImpositionCandidateDto> = emptyList(),
    val selectedTab: Int = 0 // 0: Visual Layout, 1: Specifications History, 2: Prepress Parameters
)
