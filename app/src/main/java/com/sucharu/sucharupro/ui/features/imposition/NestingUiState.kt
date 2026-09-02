package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.data.api.model.imposition.DynamicNestingSpecificationResponseDto
import com.sucharu.sucharupro.data.api.model.imposition.NestingCandidateItemDto
import java.math.BigDecimal

/**
 * UI State for Dynamic 2D Nesting & Wastage Optimization Command Center.
 * Module 18 Step 03.
 */
data class NestingUiState(
    val selectedTab: Int = 0, // 0: 2D Canvas Layout, 1: Candidate Pool, 2: Offcuts & Wastage KPI, 3: History
    val name: String = "Mixed-Item Dynamic Nesting Form",
    val parentSheetWidthMm: String = "635.0000",
    val parentSheetHeightMm: String = "914.4000",
    val marginTopMm: String = "10.0000",
    val marginBottomMm: String = "10.0000",
    val marginLeftMm: String = "10.0000",
    val marginRightMm: String = "10.0000",
    val bleedMm: String = "3.0000",
    val horizontalGutterMm: String = "4.0000",
    val verticalGutterMm: String = "4.0000",
    val orientationPolicy: String = "ALLOW_ROTATION",
    val placementStrategy: String = "BOTTOM_LEFT_FILL",
    val minOffcutDimensionMm: String = "100.0000",

    // Candidate Items in pool
    val candidatePool: List<NestingCandidateItemDto> = listOf(
        NestingCandidateItemDto(
            jobId = "JOB-BOOKLET-A4",
            orderId = "ORD-301",
            orderItemId = "ITEM-01",
            productName = "Product Catalog Cover (A4)",
            finishedWidthMm = BigDecimal("210.0000"),
            finishedHeightMm = BigDecimal("297.0000"),
            requiredQuantity = 1000L,
            paperStockType = "ART_CARD",
            gsm = BigDecimal("300.0000"),
            allowRotation = true,
            priorityScore = 150
        ),
        NestingCandidateItemDto(
            jobId = "JOB-BROCHURE-A5",
            orderId = "ORD-302",
            orderItemId = "ITEM-02",
            productName = "Promotional Brochure (A5)",
            finishedWidthMm = BigDecimal("148.0000"),
            finishedHeightMm = BigDecimal("210.0000"),
            requiredQuantity = 2000L,
            paperStockType = "ART_CARD",
            gsm = BigDecimal("300.0000"),
            allowRotation = true,
            priorityScore = 120
        ),
        NestingCandidateItemDto(
            jobId = "JOB-CARD-BC",
            orderId = "ORD-303",
            orderItemId = "ITEM-03",
            productName = "Executive Business Cards",
            finishedWidthMm = BigDecimal("90.0000"),
            finishedHeightMm = BigDecimal("54.0000"),
            requiredQuantity = 4000L,
            paperStockType = "ART_CARD",
            gsm = BigDecimal("300.0000"),
            allowRotation = true,
            priorityScore = 100
        )
    ),

    // Active specification outcome
    val currentSpecification: DynamicNestingSpecificationResponseDto? = null,
    val specificationsList: List<DynamicNestingSpecificationResponseDto> = emptyList(),

    val isOptimizing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
