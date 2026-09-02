package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.data.api.model.imposition.SignatureImpositionSpecificationResponseDto

/**
 * UI State for Multi-Page Signature Imposition Command Center.
 * Module 18 Step 04.
 */
data class SignatureUiState(
    val selectedTab: Int = 0, // 0: Signature Sheet Visualizer (Front & Back), 1: Folding & Creep Parameters, 2: Run Length & Wastage Analytics, 3: History & Handoff
    val name: String = "Annual Report 16pp Signature Run",
    val jobId: String = "JOB-PUB-2026-001",
    val orderId: String = "ORD-401",
    val orderItemId: String = "ITEM-01",
    val productName: String = "Corporate Annual Report 2026",
    val totalPages: String = "64",
    val signaturePageCount: String = "16",
    val bindingMethod: String = "SADDLE_STITCH",
    val sheetTurningMethod: String = "SHEETWISE",
    val foldingScheme: String = "RIGHT_ANGLE_16PP",
    val pageWidthMm: String = "210.0000",
    val pageHeightMm: String = "297.0000",
    val parentSheetWidthMm: String = "635.0000",
    val parentSheetHeightMm: String = "914.4000",
    val requiredQuantity: String = "2500",
    val paperStockType: String = "ART_PAPER",
    val gsm: String = "150.0000",
    val customCaliperMm: String = "",
    val marginTopMm: String = "10.0000",
    val marginBottomMm: String = "10.0000",
    val marginLeftMm: String = "10.0000",
    val marginRightMm: String = "10.0000",
    val spineGutterMm: String = "4.0000",
    val headGutterMm: String = "6.0000",
    val footGutterMm: String = "6.0000",
    val faceTrimMm: String = "4.0000",
    val bleedMm: String = "3.0000",
    val enableCreepCompensation: Boolean = true,

    // Selected signature form for visualizer
    val selectedSignatureIndex: Int = 0,
    val selectedFormSideIndex: Int = 0, // 0: FRONT_SIDE_OUTER, 1: BACK_SIDE_INNER

    // Active specification outcome
    val currentSpecification: SignatureImpositionSpecificationResponseDto? = null,
    val isOptimizing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // History specifications
    val historySpecifications: List<SignatureImpositionSpecificationResponseDto> = emptyList(),
    val handoffExportedJson: String? = null
)
