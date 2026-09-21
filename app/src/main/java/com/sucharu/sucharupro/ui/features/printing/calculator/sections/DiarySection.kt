package com.sucharu.sucharupro.ui.features.printing.calculator.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.printingcalculator.ColorTypeOption
import com.sucharu.sucharupro.domain.model.printingcalculator.DiaryCoverCategory
import com.sucharu.sucharupro.domain.model.printingcalculator.InnerFormeGroupState
import com.sucharu.sucharupro.domain.service.printingcalculator.CalculationEngine
import com.sucharu.sucharupro.ui.features.printing.calculator.components.*

@Composable
fun DiarySection(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    // 1. Basic Specs
    var diaryWidthStr by remember { mutableStateOf("6.0") }
    var diaryHeightStr by remember { mutableStateOf("8.5") }
    var totalPagesStr by remember { mutableStateOf("200") }
    var diaryQuantityStr by remember { mutableStateOf("500") }
    var paperCaliperStr by remember { mutableStateOf("0.0035") }

    // 2. Cover Category Selection
    var selectedCategoryCode by remember { mutableStateOf(DiaryCoverCategory.HARD_COVER.categoryName) }

    // Board Specs (Categories B, C, D)
    var boardRatePerSqFtStr by remember { mutableStateOf("25.0") }
    var boardMakingLaborRatePerPcStr by remember { mutableStateOf("15.0") }

    // Category B: Hardcover Wrapping Paper
    var wrapPaperWidthStr by remember { mutableStateOf("20.0") }
    var wrapPaperHeightStr by remember { mutableStateOf("30.0") }
    var wrapPaperRateStr by remember { mutableStateOf("2200.0") }
    var wrapCutsStr by remember { mutableStateOf("1") }
    var wrapColorType by remember { mutableStateOf(ColorTypeOption.FOUR_COLOR.displayName) }
    var wrapPlateRateStr by remember { mutableStateOf("250.0") }
    var wrapIsJuri by remember { mutableStateOf(false) }
    var wrapIsBackToBack by remember { mutableStateOf(false) }
    var wrapPrintRate1kStr by remember { mutableStateOf("180.0") }
    var wrapPrintMinStr by remember { mutableStateOf("350.0") }
    var wrapLamEnabled by remember { mutableStateOf(true) }
    var wrapLamRateStr by remember { mutableStateOf("0.002") }
    var wrapLamMinStr by remember { mutableStateOf("150.0") }

    // Category C: Rexine
    var rexineRatePerSqFtStr by remember { mutableStateOf("45.0") }
    var rexineWastagePercentStr by remember { mutableStateOf("10.0") }
    var rexineFoilBlockCostStr by remember { mutableStateOf("400.0") }
    var rexineFoilRatePerPcStr by remember { mutableStateOf("5.0") }
    var rexineBindingLaborRatePerPcStr by remember { mutableStateOf("25.0") }

    // Category D: PU Leather
    var foamRatePerPcStr by remember { mutableStateOf("12.0") }
    var puLeatherRatePerSqFtStr by remember { mutableStateOf("90.0") }
    var puWastagePercentStr by remember { mutableStateOf("15.0") }
    var stitchingRatePerPcStr by remember { mutableStateOf("15.0") }
    var debossBlockCostStr by remember { mutableStateOf("600.0") }
    var debossHitRatePerPcStr by remember { mutableStateOf("8.0") }

    // Accessories & Assembly
    var includeRibbon by remember { mutableStateOf(true) }
    var ribbonRatePerPcStr by remember { mutableStateOf("4.0") }
    var includeElastic by remember { mutableStateOf(true) }
    var elasticMaterialRateStr by remember { mutableStateOf("8.0") }
    var elasticRivetingRateStr by remember { mutableStateOf("4.0") }
    var includePenLoop by remember { mutableStateOf(true) }
    var penLoopRatePerPcStr by remember { mutableStateOf("6.0") }
    var includeMetalCorners by remember { mutableStateOf(true) }
    var metalCornerRatePerPcStr by remember { mutableStateOf("2.5") } // per corner (4 per diary)
    var includeBox by remember { mutableStateOf(false) }
    var presentationBoxRatePerPcStr by remember { mutableStateOf("35.0") }

    // Inner Pages Specs (Book base)
    var pagesPerFormeStr by remember { mutableStateOf("16") }
    var formasPerSheetStr by remember { mutableStateOf("2") }
    var innerPaperWidthStr by remember { mutableStateOf("20.0") }
    var innerPaperHeightStr by remember { mutableStateOf("30.0") }
    var innerPaperRateStr by remember { mutableStateOf("1800.0") }

    var formeGroups by remember {
        mutableStateOf(
            listOf(
                InnerFormeGroupState(
                    formaCount = 10,
                    printingColor = ColorTypeOption.ONE_COLOR.displayName,
                    plateRate = 250.0,
                    printingRate1k = 120.0,
                    printingMinCharge = 250.0
                ),
                InnerFormeGroupState(
                    formaCount = 2,
                    printingColor = ColorTypeOption.FOUR_COLOR.displayName,
                    plateRate = 250.0,
                    printingRate1k = 180.0,
                    printingMinCharge = 350.0
                )
            )
        )
    }

    var includeEndpaper by remember { mutableStateOf(true) }
    var epPaperWidthStr by remember { mutableStateOf("20.0") }
    var epPaperHeightStr by remember { mutableStateOf("30.0") }
    var epPaperRateStr by remember { mutableStateOf("1800.0") }
    var epGtoCutsStr by remember { mutableStateOf("1") }
    var epColorType by remember { mutableStateOf(ColorTypeOption.ONE_COLOR.displayName) }
    var epPlatePriceStr by remember { mutableStateOf("250.0") }
    var epIsJuri by remember { mutableStateOf(false) }
    var epIsBackToBack by remember { mutableStateOf(false) }
    var epPrintRate1kStr by remember { mutableStateOf("120.0") }
    var epPrintMinStr by remember { mutableStateOf("250.0") }
    var isEpLamEnabled by remember { mutableStateOf(false) }
    var epLamRateStr by remember { mutableStateOf("0.002") }
    var epLamMinChargeStr by remember { mutableStateOf("100.0") }

    var bindingChargePerBookStr by remember { mutableStateOf("20.0") }
    var bindingMinChargeStr by remember { mutableStateOf("500.0") }
    var itemsPerPacketStr by remember { mutableStateOf("20") }
    var costPerPacketStr by remember { mutableStateOf("0.0") }
    var packagingRate1kStr by remember { mutableStateOf("100.0") }
    var packagingMinChargeStr by remember { mutableStateOf("100.0") }
    var designChargeStr by remember { mutableStateOf("600.0") }
    var shippingEstimateStr by remember { mutableStateOf("300.0") }
    var profitPercentStr by remember { mutableStateOf("20.0") }

    val categoryEnum = remember(selectedCategoryCode) {
        DiaryCoverCategory.entries.find { it.categoryName == selectedCategoryCode } ?: DiaryCoverCategory.HARD_COVER
    }

    // Calculation Result
    val result = remember(
        categoryEnum, diaryWidthStr, diaryHeightStr, totalPagesStr, diaryQuantityStr, paperCaliperStr,
        boardRatePerSqFtStr, boardMakingLaborRatePerPcStr, wrapPaperWidthStr, wrapPaperHeightStr, wrapPaperRateStr, wrapCutsStr, wrapColorType, wrapPlateRateStr, wrapIsJuri, wrapIsBackToBack, wrapPrintRate1kStr, wrapPrintMinStr, wrapLamEnabled, wrapLamRateStr, wrapLamMinStr,
        rexineRatePerSqFtStr, rexineWastagePercentStr, rexineFoilBlockCostStr, rexineFoilRatePerPcStr, rexineBindingLaborRatePerPcStr,
        foamRatePerPcStr, puLeatherRatePerSqFtStr, puWastagePercentStr, stitchingRatePerPcStr, debossBlockCostStr, debossHitRatePerPcStr,
        includeRibbon, ribbonRatePerPcStr, includeElastic, elasticMaterialRateStr, elasticRivetingRateStr, includePenLoop, penLoopRatePerPcStr, includeMetalCorners, metalCornerRatePerPcStr, includeBox, presentationBoxRatePerPcStr,
        pagesPerFormeStr, formasPerSheetStr, innerPaperWidthStr, innerPaperHeightStr, innerPaperRateStr, formeGroups, includeEndpaper, epPaperWidthStr, epPaperHeightStr, epPaperRateStr, epGtoCutsStr, epColorType, epPlatePriceStr, epIsJuri, epIsBackToBack, epPrintRate1kStr, epPrintMinStr, isEpLamEnabled, epLamRateStr, epLamMinChargeStr,
        bindingChargePerBookStr, bindingMinChargeStr, itemsPerPacketStr, costPerPacketStr, packagingRate1kStr, packagingMinChargeStr, designChargeStr, shippingEstimateStr, profitPercentStr
    ) {
        val dW = diaryWidthStr.toDoubleOrNull() ?: 0.0
        val dH = diaryHeightStr.toDoubleOrNull() ?: 0.0
        val pages = totalPagesStr.toIntOrNull() ?: 0
        val qty = diaryQuantityStr.toIntOrNull() ?: 0
        val caliper = paperCaliperStr.toDoubleOrNull() ?: 0.0035

        val boardRate = boardRatePerSqFtStr.toDoubleOrNull() ?: 0.0
        val boardLabor = boardMakingLaborRatePerPcStr.toDoubleOrNull() ?: 0.0

        val wrapW = wrapPaperWidthStr.toDoubleOrNull() ?: 0.0
        val wrapH = wrapPaperHeightStr.toDoubleOrNull() ?: 0.0
        val wrapRate = wrapPaperRateStr.toDoubleOrNull() ?: 0.0
        val wrapCuts = wrapCutsStr.toIntOrNull() ?: 1
        val wrapPlate = wrapPlateRateStr.toDoubleOrNull() ?: 0.0
        val wrapPrint1k = wrapPrintRate1kStr.toDoubleOrNull() ?: 0.0
        val wrapPrintMin = wrapPrintMinStr.toDoubleOrNull() ?: 0.0
        val wrapLamRate = wrapLamRateStr.toDoubleOrNull() ?: 0.0
        val wrapLamMin = wrapLamMinStr.toDoubleOrNull() ?: 0.0

        val rexineRate = rexineRatePerSqFtStr.toDoubleOrNull() ?: 0.0
        val rexineWastage = rexineWastagePercentStr.toDoubleOrNull() ?: 0.0
        val rexineBlock = rexineFoilBlockCostStr.toDoubleOrNull() ?: 0.0
        val rexineFoilRate = rexineFoilRatePerPcStr.toDoubleOrNull() ?: 0.0
        val rexineLabor = rexineBindingLaborRatePerPcStr.toDoubleOrNull() ?: 0.0

        val foamRate = foamRatePerPcStr.toDoubleOrNull() ?: 0.0
        val puRate = puLeatherRatePerSqFtStr.toDoubleOrNull() ?: 0.0
        val puWastage = puWastagePercentStr.toDoubleOrNull() ?: 0.0
        val stitchingRate = stitchingRatePerPcStr.toDoubleOrNull() ?: 0.0
        val debossBlock = debossBlockCostStr.toDoubleOrNull() ?: 0.0
        val debossHit = debossHitRatePerPcStr.toDoubleOrNull() ?: 0.0

        val ribbonRate = if (includeRibbon) ribbonRatePerPcStr.toDoubleOrNull() ?: 0.0 else 0.0
        val elasticMat = if (includeElastic) elasticMaterialRateStr.toDoubleOrNull() ?: 0.0 else 0.0
        val elasticRiv = if (includeElastic) elasticRivetingRateStr.toDoubleOrNull() ?: 0.0 else 0.0
        val penLoop = if (includePenLoop) penLoopRatePerPcStr.toDoubleOrNull() ?: 0.0 else 0.0
        val metalCorner = if (includeMetalCorners) metalCornerRatePerPcStr.toDoubleOrNull() ?: 0.0 else 0.0
        val boxRate = if (includeBox) presentationBoxRatePerPcStr.toDoubleOrNull() ?: 0.0 else 0.0

        val ppForme = pagesPerFormeStr.toIntOrNull() ?: 16
        val fPerSheet = formasPerSheetStr.toIntOrNull() ?: 2
        val inPaperW = innerPaperWidthStr.toDoubleOrNull() ?: 0.0
        val inPaperH = innerPaperHeightStr.toDoubleOrNull() ?: 0.0
        val inPaperRate = innerPaperRateStr.toDoubleOrNull() ?: 0.0

        val epPaperW = epPaperWidthStr.toDoubleOrNull() ?: 0.0
        val epPaperH = epPaperHeightStr.toDoubleOrNull() ?: 0.0
        val epPaperRate = epPaperRateStr.toDoubleOrNull() ?: 0.0
        val epCuts = epGtoCutsStr.toIntOrNull() ?: 1
        val epPlatePrice = epPlatePriceStr.toDoubleOrNull() ?: 0.0
        val epPrint1k = epPrintRate1kStr.toDoubleOrNull() ?: 0.0
        val epPrintMin = epPrintMinStr.toDoubleOrNull() ?: 0.0
        val epLamRate = epLamRateStr.toDoubleOrNull() ?: 0.0
        val epLamMin = epLamMinChargeStr.toDoubleOrNull() ?: 0.0

        val bindingCharge = bindingChargePerBookStr.toDoubleOrNull() ?: 0.0
        val bindingMin = bindingMinChargeStr.toDoubleOrNull() ?: 0.0
        val itemsPkt = itemsPerPacketStr.toIntOrNull() ?: 20
        val costPkt = costPerPacketStr.toDoubleOrNull() ?: 0.0
        val pack1k = packagingRate1kStr.toDoubleOrNull() ?: 0.0
        val packMin = packagingMinChargeStr.toDoubleOrNull() ?: 0.0
        val design = designChargeStr.toDoubleOrNull() ?: 0.0
        val shipping = shippingEstimateStr.toDoubleOrNull() ?: 0.0
        val profit = profitPercentStr.toDoubleOrNull() ?: 0.0

        CalculationEngine.calculateDiaryCost(
            diaryCategory = categoryEnum,
            diaryWidth = dW,
            diaryHeight = dH,
            totalPages = pages,
            diaryQuantity = qty,
            paperCaliper = caliper,
            boardRatePerSqFt = boardRate,
            boardMakingLaborRatePerPc = boardLabor,
            wrapPaperWidth = wrapW,
            wrapPaperHeight = wrapH,
            wrapPaperRate = wrapRate,
            wrapCuts = wrapCuts,
            wrapColorType = wrapColorType,
            wrapPlateRate = wrapPlate,
            wrapIsJuri = wrapIsJuri,
            wrapIsBackToBack = wrapIsBackToBack,
            wrapPrintRate1k = wrapPrint1k,
            wrapPrintMin = wrapPrintMin,
            wrapLamEnabled = wrapLamEnabled,
            wrapLamRate = wrapLamRate,
            wrapLamMin = wrapLamMin,
            rexineRatePerSqFt = rexineRate,
            rexineWastagePercent = rexineWastage,
            rexineFoilBlockCost = rexineBlock,
            rexineFoilRatePerPc = rexineFoilRate,
            rexineBindingLaborRatePerPc = rexineLabor,
            foamRatePerPc = foamRate,
            puLeatherRatePerSqFt = puRate,
            puWastagePercent = puWastage,
            stitchingRatePerPc = stitchingRate,
            debossBlockCost = debossBlock,
            debossHitRatePerPc = debossHit,
            ribbonRatePerPc = ribbonRate,
            elasticMaterialRate = elasticMat,
            elasticRivetingRate = elasticRiv,
            penLoopRatePerPc = penLoop,
            metalCornerRatePerPc = metalCorner,
            presentationBoxRatePerPc = boxRate,
            pagesPerForme = ppForme,
            formasPerSheet = fPerSheet,
            innerPaperWidth = inPaperW,
            innerPaperHeight = inPaperH,
            innerPaperRate = inPaperRate,
            formeGroups = formeGroups,
            includeEndpaper = includeEndpaper,
            epPaperWidth = epPaperW,
            epPaperHeight = epPaperH,
            epPaperRate = epPaperRate,
            epGtoCuts = epCuts,
            epColorType = epColorType,
            epPlatePrice = epPlatePrice,
            epIsJuri = epIsJuri,
            epIsBackToBack = epIsBackToBack,
            epPrintRate1k = epPrint1k,
            epPrintMin = epPrintMin,
            isEpLamEnabled = isEpLamEnabled,
            epLamRate = epLamRate,
            epLamMinCharge = epLamMin,
            bindingChargePerBook = bindingCharge,
            bindingMinCharge = bindingMin,
            itemsPerPacket = itemsPkt,
            costPerPacket = costPkt,
            packagingRate1k = pack1k,
            packagingMinCharge = packMin,
            designCharge = design,
            shippingEstimate = shipping,
            profitPercent = profit
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Sticky Cost Summary Header
        CostSummaryCard(
            grandTotal = result.grandTotal,
            perUnitCost = result.perDiaryCost,
            subTotal = result.subTotal,
            profitAmount = result.profitAmount,
            profitPercent = profitPercentStr.toDoubleOrNull() ?: 0.0,
            itemsQuantity = diaryQuantityStr.toIntOrNull() ?: 0,
            breakdownItems = listOf(
                "ইনার পেপার খরচ" to result.bookBaseResult.innerPaperCost,
                "ইনার ফর্মা প্লেট ও প্রিন্টিং" to (result.bookBaseResult.totalInnerPlatesCost + result.bookBaseResult.totalInnerPrintCost),
                "পুস্তানি (Endpaper) খরচ" to (result.bookBaseResult.epPaperCost + result.bookBaseResult.epPlateCost + result.bookBaseResult.epPrintCost),
                "ডায়েরি কভার বোর্ড/মেটেরিয়াল খরচ" to (result.boardCost + result.wrappingPaperCost + result.rexineCost + result.puLeatherCost + result.foamPaddingCost),
                "কভার মেকিং, প্রিন্টিং ও লেবার" to (result.wrappingPlateCost + result.wrappingPrintCost + result.wrappingLamCost + result.boardPastingLaborCost + result.rexineFoilCost + result.rexineBindingLaborCost + result.stitchingCost + result.debossCost),
                "ডায়েরি আনুষঙ্গিক (Accessories & Box)" to result.accessoriesTotal,
                "বাইন্ডিং খরচ" to result.bookBaseResult.totalBindingCost,
                "প্যাকেজিং খরচ" to result.bookBaseResult.totalPackaging,
                "ডিজাইন চার্জ" to (designChargeStr.toDoubleOrNull() ?: 0.0),
                "শিপিং/ডেলিভারি খরচ" to (shippingEstimateStr.toDoubleOrNull() ?: 0.0)
            ),
            initialExpanded = false
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 1. Basic Diary Specs
        SectionHeaderCard(
            title = "১. ডায়েরির বেসিক স্পেসিফিকেশন",
            subtitle = "ডায়েরির মাপ, পৃষ্ঠা ও কপি সংখ্যা"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = diaryWidthStr,
                    onValueChange = { diaryWidthStr = it },
                    label = "ডায়েরির চওড়া (Width)",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = diaryHeightStr,
                    onValueChange = { diaryHeightStr = it },
                    label = "ডায়েরির উচ্চতা (Height)",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = totalPagesStr,
                    onValueChange = { totalPagesStr = it },
                    label = "মোট পৃষ্ঠা (Pages)",
                    isInteger = true,
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = diaryQuantityStr,
                    onValueChange = { diaryQuantityStr = it },
                    label = "ডায়েরির পরিমাণ (Qty)",
                    suffix = "pcs",
                    isInteger = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Specialized Diary Cover Category
        SectionHeaderCard(
            title = "২. ডায়েরি কভার টাইপ (Specialized Cover Category)",
            subtitle = "সফটকভার, পেস্ট-আপ হার্ডকভার, রেক্সিন বা পিইউ লেদার"
        ) {
            DropdownMenuField(
                selectedOption = selectedCategoryCode,
                options = DiaryCoverCategory.entries.map { it.categoryName },
                onOptionSelected = { selectedCategoryCode = it },
                label = "কভার ক্যাটাগরি"
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (categoryEnum) {
                DiaryCoverCategory.SOFT_COVER -> {
                    // Soft cover handled in standard cover
                }
                DiaryCoverCategory.HARD_COVER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = boardRatePerSqFtStr,
                            onValueChange = { boardRatePerSqFtStr = it },
                            label = "বোর্ড দর/বর্গফুট",
                            suffix = "৳/sq.ft",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = boardMakingLaborRatePerPcStr,
                            onValueChange = { boardMakingLaborRatePerPcStr = it },
                            label = "বোর্ড পেস্টিং/মেকিং লেবার",
                            suffix = "৳/pc",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = wrapPaperWidthStr,
                            onValueChange = { wrapPaperWidthStr = it },
                            label = "র‌্যাপিং পেপার W",
                            suffix = "in",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = wrapPaperHeightStr,
                            onValueChange = { wrapPaperHeightStr = it },
                            label = "র‌্যাপিং পেপার H",
                            suffix = "in",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = wrapPaperRateStr,
                            onValueChange = { wrapPaperRateStr = it },
                            label = "র‌্যাপিং পেপার রিম দর",
                            suffix = "৳/ream",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = wrapPlateRateStr,
                            onValueChange = { wrapPlateRateStr = it },
                            label = "প্লেট দর",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                DiaryCoverCategory.REXINE_HARDBOUND -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = boardRatePerSqFtStr,
                            onValueChange = { boardRatePerSqFtStr = it },
                            label = "বোর্ড দর/বর্গফুট",
                            suffix = "৳/sq.ft",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = rexineRatePerSqFtStr,
                            onValueChange = { rexineRatePerSqFtStr = it },
                            label = "রেক্সিন দর/বর্গফুট",
                            suffix = "৳/sq.ft",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = rexineFoilBlockCostStr,
                            onValueChange = { rexineFoilBlockCostStr = it },
                            label = "রেক্সিন ফয়েল ব্লক মেকিং",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = rexineFoilRatePerPcStr,
                            onValueChange = { rexineFoilRatePerPcStr = it },
                            label = "ফয়েল স্ট্যাম্পিং লেবার/পিস",
                            suffix = "৳/pc",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    NumberInputField(
                        value = rexineBindingLaborRatePerPcStr,
                        onValueChange = { rexineBindingLaborRatePerPcStr = it },
                        label = "রেক্সিন বাইন্ডিং লেবার/পিস",
                        suffix = "৳/pc"
                    )
                }
                DiaryCoverCategory.PREMIUM_PU_LEATHER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = foamRatePerPcStr,
                            onValueChange = { foamRatePerPcStr = it },
                            label = "ফোম/স্পঞ্জ প্যাডিং দর",
                            suffix = "৳/pc",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = puLeatherRatePerSqFtStr,
                            onValueChange = { puLeatherRatePerSqFtStr = it },
                            label = "পিইউ লেদার দর/বর্গফুট",
                            suffix = "৳/sq.ft",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = stitchingRatePerPcStr,
                            onValueChange = { stitchingRatePerPcStr = it },
                            label = "সীমানা সেলাই/স্টিচিং দর",
                            suffix = "৳/pc",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = debossBlockCostStr,
                            onValueChange = { debossBlockCostStr = it },
                            label = "এমবসিং ব্লক তৈরি খরচ",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Diary Accessories
        SectionHeaderCard(
            title = "৩. ডায়েরি আনুষঙ্গিক উপাদান (Accessories)",
            subtitle = "রিবন, ইলাস্টিক ব্যান্ড, পেন লুপ, মেটাল কর্নার ও বক্স"
        ) {
            SwitchRowField(
                title = "রিবন মার্কার (Bookmark Ribbon)",
                checked = includeRibbon,
                onCheckedChange = { includeRibbon = it }
            )
            if (includeRibbon) {
                Spacer(modifier = Modifier.height(4.dp))
                NumberInputField(
                    value = ribbonRatePerPcStr,
                    onValueChange = { ribbonRatePerPcStr = it },
                    label = "রিবন দর/পিস",
                    suffix = "৳/pc"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRowField(
                title = "ইলাস্টিক ক্লোজার ব্যান্ড (Elastic Band)",
                checked = includeElastic,
                onCheckedChange = { includeElastic = it }
            )
            if (includeElastic) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = elasticMaterialRateStr,
                        onValueChange = { elasticMaterialRateStr = it },
                        label = "ইলাস্টিক মেটেরিয়াল",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = elasticRivetingRateStr,
                        onValueChange = { elasticRivetingRateStr = it },
                        label = "রিভেটিং চার্জ",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRowField(
                title = "পেন লুপ (Pen Holder Loop)",
                checked = includePenLoop,
                onCheckedChange = { includePenLoop = it }
            )
            if (includePenLoop) {
                Spacer(modifier = Modifier.height(4.dp))
                NumberInputField(
                    value = penLoopRatePerPcStr,
                    onValueChange = { penLoopRatePerPcStr = it },
                    label = "পেন লুপ দর/পিস",
                    suffix = "৳/pc"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRowField(
                title = "মেটাল কর্নার ক্লিপ (Metal Corner Protectors)",
                checked = includeMetalCorners,
                onCheckedChange = { includeMetalCorners = it }
            )
            if (includeMetalCorners) {
                Spacer(modifier = Modifier.height(4.dp))
                NumberInputField(
                    value = metalCornerRatePerPcStr,
                    onValueChange = { metalCornerRatePerPcStr = it },
                    label = "প্রতি কর্নার ক্লিপ দর (৪টি/ডায়েরি)",
                    suffix = "৳/clip"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRowField(
                title = "ব্যক্তিগত প্রেজেন্টেশন বক্স (Individual Box)",
                checked = includeBox,
                onCheckedChange = { includeBox = it }
            )
            if (includeBox) {
                Spacer(modifier = Modifier.height(4.dp))
                NumberInputField(
                    value = presentationBoxRatePerPcStr,
                    onValueChange = { presentationBoxRatePerPcStr = it },
                    label = "বক্স মেকিং দর/পিস",
                    suffix = "৳/pc"
                )
            }
        }

        // 4. Inner Paper Specs
        SectionHeaderCard(
            title = "৪. ইনার কাগজ ও ফর্মা সেটিংস",
            subtitle = "ডায়েরির ভিতরের পাতার কাগজ ও প্রিন্টিং"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = innerPaperWidthStr,
                    onValueChange = { innerPaperWidthStr = it },
                    label = "ইনার পেপার W",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = innerPaperHeightStr,
                    onValueChange = { innerPaperHeightStr = it },
                    label = "ইনার পেপার H",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(
                value = innerPaperRateStr,
                onValueChange = { innerPaperRateStr = it },
                label = "ইনার পেপার রিম দর",
                suffix = "৳/ream"
            )
        }

        // 5. Binding & Profit
        SectionHeaderCard(
            title = "৫. বাইন্ডিং, প্যাকেজিং ও প্রফিট মার্জিন",
            subtitle = "বাইন্ডিং, ডিজাইন চার্জ, শিপিং ও লাভ"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = bindingChargePerBookStr,
                    onValueChange = { bindingChargePerBookStr = it },
                    label = "বাইন্ডিং চার্জ/পিস",
                    suffix = "৳/pc",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = bindingMinChargeStr,
                    onValueChange = { bindingMinChargeStr = it },
                    label = "মিনিমাম বাইন্ডিং চার্জ",
                    suffix = "৳",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = designChargeStr,
                    onValueChange = { designChargeStr = it },
                    label = "ডিজাইন চার্জ",
                    suffix = "৳",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = shippingEstimateStr,
                    onValueChange = { shippingEstimateStr = it },
                    label = "শিপিং/ডেলিভারি খরচ",
                    suffix = "৳",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(
                value = profitPercentStr,
                onValueChange = { profitPercentStr = it },
                label = "লাভের শতাংশ (Profit Percent)",
                suffix = "%"
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
