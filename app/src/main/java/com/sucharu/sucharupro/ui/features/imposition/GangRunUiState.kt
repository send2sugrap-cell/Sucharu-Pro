package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.data.api.model.imposition.GangRunCandidateItemDto
import com.sucharu.sucharupro.data.api.model.imposition.GangRunSpecificationResponseDto
import java.math.BigDecimal

/**
 * UI State for Multi-Job Gang-Run Batching & Clustering Command Center.
 * Module 18 Step 02.
 */
data class GangRunUiState(
    val selectedTab: Int = 0, // 0: Visual Gang Form, 1: Candidate Pool & Clustering, 2: Batch History
    val batchName: String = "Gang Form - Commercial Flyers",
    val parentSheetWidthMm: String = "635.0000",
    val parentSheetHeightMm: String = "914.4000",
    val marginTopMm: String = "10.0000",
    val marginBottomMm: String = "10.0000",
    val marginLeftMm: String = "10.0000",
    val marginRightMm: String = "10.0000",
    val bleedMm: String = "3.0000",
    val horizontalGutterMm: String = "4.0000",
    val verticalGutterMm: String = "4.0000",
    val clusteringPolicy: String = "STRICT_IDENTICAL_SUBSTRATE",

    // Candidate Items in pool
    val candidatePool: List<GangRunCandidateItemDto> = listOf(
        GangRunCandidateItemDto(
            jobId = "JOB-FLYER-01",
            orderId = "ORD-101",
            orderItemId = "ITEM-01",
            productName = "Corporate Promo A5 Flyer",
            finishedWidthMm = BigDecimal("148.0000"),
            finishedHeightMm = BigDecimal("210.0000"),
            requiredQuantity = 2000L,
            paperStockType = "ART_CARD",
            gsm = BigDecimal("300.0000")
        ),
        GangRunCandidateItemDto(
            jobId = "JOB-MENU-02",
            orderId = "ORD-102",
            orderItemId = "ITEM-02",
            productName = "Restaurant Cafe Table Menu A5",
            finishedWidthMm = BigDecimal("148.0000"),
            finishedHeightMm = BigDecimal("210.0000"),
            requiredQuantity = 1000L,
            paperStockType = "ART_CARD",
            gsm = BigDecimal("300.0000")
        ),
        GangRunCandidateItemDto(
            jobId = "JOB-LEAF-03",
            orderId = "ORD-103",
            orderItemId = "ITEM-03",
            productName = "Product Showcase A5 Handout",
            finishedWidthMm = BigDecimal("148.0000"),
            finishedHeightMm = BigDecimal("210.0000"),
            requiredQuantity = 1500L,
            paperStockType = "ART_CARD",
            gsm = BigDecimal("300.0000")
        )
    ),

    // Active specification outcome
    val currentSpecification: GangRunSpecificationResponseDto? = null,
    val specificationsList: List<GangRunSpecificationResponseDto> = emptyList(),

    val isOptimizing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
