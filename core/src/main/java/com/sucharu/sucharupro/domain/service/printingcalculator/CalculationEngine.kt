package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.domain.model.printingcalculator.*
import kotlin.math.ceil
import kotlin.math.max

object CalculationEngine {

    fun calculateItemsPerSheet(paperW: Double, paperH: Double, itemW: Double, itemH: Double): Int {
        if (paperW <= 0.0 || paperH <= 0.0 || itemW <= 0.0 || itemH <= 0.0) return 0
        val fitNormal = (paperW / itemW).toInt() * (paperH / itemH).toInt()
        val fitRotated = (paperW / itemH).toInt() * (paperH / itemW).toInt()
        return max(fitNormal, fitRotated)
    }

    fun getColorMultiplier(colorType: String): Int {
        return when (colorType) {
            "1 Color" -> 1
            "2 Color" -> 2
            "3 Color" -> 3
            "4 Color", "4 Color (CMYK)" -> 4
            "4+Spot Color" -> 5
            else -> 1
        }
    }

    // Sector 1. General Offset Calculation
    fun calculateOffsetCost(
        itemWidth: Double,
        itemHeight: Double,
        paperWidth: Double,
        paperHeight: Double,
        paperRatePerReam: Double,
        itemQuantity: Int,
        cutsPerMainSheet: Int,
        colorType: String,
        isJuri: Boolean,
        isBackToBack: Boolean,
        ratePerPlate: Double,
        printingRate1k: Double,
        printingMinCharge: Double,
        isLaminationEnabled: Boolean,
        laminationSides: String, // "Single" or "Both"
        laminationRatePerSqIn: Double,
        laminationMinCharge: Double,
        dieBlockCost: Double,
        dieCuttingRate1k: Double,
        foilBlockCost: Double,
        foilRatePerPc: Double,
        pastingRate1k: Double,
        pastingMinCharge: Double,
        numberingRate1k: Double,
        perforationRate1k: Double,
        pagesPerBook: Int,
        bindingRatePerPc: Double,
        bindingMinCharge: Double,
        itemsPerPacket: Int,
        costPerPacket: Double,
        packagingRate1k: Double,
        packagingMinCharge: Double,
        designCharge: Double,
        shippingEstimate: Double,
        profitPercent: Double
    ): OffsetCostResult {
        val itemsPerSheet = calculateItemsPerSheet(paperWidth, paperHeight, itemWidth, itemHeight)
        val mainSheets = if (itemsPerSheet > 0) ceil(itemQuantity.toDouble() / itemsPerSheet).toInt() else 0
        val machineSheets = mainSheets * max(1, cutsPerMainSheet)
        val paperCost = (mainSheets / 500.0) * paperRatePerReam

        val baseColors = getColorMultiplier(colorType)
        val plateMultiplier = if (isJuri) 2 else 1
        val totalPlates = baseColors * plateMultiplier
        val plateCost = totalPlates * ratePerPlate

        val impressionMultiplier = if (isJuri || isBackToBack) 2 else 1
        val totalImpressions = machineSheets * baseColors * impressionMultiplier
        val impressionCost = if (machineSheets > 0) {
            max((totalImpressions / 1000.0) * printingRate1k, printingMinCharge)
        } else 0.0

        val lamSqInches = paperWidth * paperHeight * mainSheets * (if (laminationSides == "Both") 2.0 else 1.0)
        val laminationCost = if (isLaminationEnabled && mainSheets > 0) {
            max(lamSqInches * laminationRatePerSqIn, laminationMinCharge)
        } else 0.0

        val dieCuttingCost = (itemQuantity / 1000.0) * dieCuttingRate1k
        val totalDieCost = dieBlockCost + dieCuttingCost
        val totalFoilCost = foilBlockCost + (itemQuantity * foilRatePerPc)
        val pastingCost = if (pastingRate1k > 0 && machineSheets > 0) {
            max((machineSheets / 1000.0) * pastingRate1k, pastingMinCharge)
        } else 0.0
        val numberingCost = (itemQuantity / 1000.0) * numberingRate1k
        val perforationCost = (machineSheets / 1000.0) * perforationRate1k

        val packageCount = if (itemsPerPacket > 0) ceil(itemQuantity.toDouble() / itemsPerPacket).toInt() else 0
        val packagingCost = if (packageCount > 0 && costPerPacket > 0) {
            packageCount * costPerPacket
        } else if (itemQuantity > 0) {
            max((itemQuantity / 1000.0) * packagingRate1k, packagingMinCharge)
        } else 0.0

        val bookCount = if (pagesPerBook > 0) ceil(itemQuantity.toDouble() / pagesPerBook).toInt() else itemQuantity
        val totalBindingCost = if (bindingRatePerPc > 0 && bookCount > 0) {
            max(bookCount * bindingRatePerPc, bindingMinCharge)
        } else 0.0

        val totalFinishing = totalDieCost + totalFoilCost + pastingCost + numberingCost + perforationCost + packagingCost + designCharge
        val subTotal = paperCost + plateCost + impressionCost + laminationCost + totalFinishing + totalBindingCost + shippingEstimate
        val profitAmount = (subTotal - shippingEstimate) * (profitPercent / 100.0)
        val grandTotal = subTotal + profitAmount
        val perUnitCost = if (itemQuantity > 0) grandTotal / itemQuantity else 0.0

        return OffsetCostResult(
            mainSheets = mainSheets,
            machineSheets = machineSheets,
            paperCost = paperCost,
            totalPlates = totalPlates,
            plateCost = plateCost,
            totalImpressions = totalImpressions,
            impressionCost = impressionCost,
            laminationSqInches = lamSqInches,
            laminationCost = laminationCost,
            dieCuttingCost = dieCuttingCost,
            totalDieCost = totalDieCost,
            totalFoilCost = totalFoilCost,
            pastingCost = pastingCost,
            numberingCost = numberingCost,
            perforationCost = perforationCost,
            packagingCost = packagingCost,
            totalBindingCost = totalBindingCost,
            totalFinishing = totalFinishing,
            subTotal = subTotal,
            profitAmount = profitAmount,
            grandTotal = grandTotal,
            perUnitCost = perUnitCost
        )
    }

    // Sector 2. Wedding & Invitation Card Calculation
    fun calculateWeddingCardCost(
        quantity: Int,
        cardW: Double,
        cardH: Double,
        paperW: Double,
        paperH: Double,
        paperReamRate: Double,
        insertCardCount: Int,
        insertW: Double,
        insertH: Double,
        envelopeFlatW: Double,
        envelopeFlatH: Double,
        envPaperRate: Double,
        envMakingLabourPerPc: Double,
        isLamEnabled: Boolean,
        lamBothSides: Boolean,
        lamRateSqIn: Double,
        lamMin: Double,
        foilBlockCost: Double,
        foilPerPcRate: Double,
        embossBlockCost: Double,
        embossPerPcRate: Double,
        dieMakingCharge: Double,
        diePunchPerPcRate: Double,
        assemblyLabourPerSet: Double,
        colorType: String,
        isJuri: Boolean,
        plateRate: Double,
        printRate1k: Double,
        printMin: Double,
        designCharge: Double,
        shippingEstimate: Double,
        profitPercent: Double
    ): WeddingCostResult {
        val itemsPerMainCardSheet = calculateItemsPerSheet(paperW, paperH, cardW, cardH)
        val mainCardSheets = if (itemsPerMainCardSheet > 0) ceil(quantity.toDouble() / itemsPerMainCardSheet) else 0.0

        val itemsPerInsertSheet = calculateItemsPerSheet(paperW, paperH, insertW, insertH)
        val insertSheetsTotal = if (itemsPerInsertSheet > 0) insertCardCount * ceil(quantity.toDouble() / itemsPerInsertSheet) else 0.0

        val cardPaperCost = ((mainCardSheets + insertSheetsTotal) / 500.0) * paperReamRate

        val itemsPerEnvSheet = calculateItemsPerSheet(paperW, paperH, envelopeFlatW, envelopeFlatH)
        val envSheets = if (itemsPerEnvSheet > 0) ceil(quantity.toDouble() / itemsPerEnvSheet) else 0.0
        val envelopeCost = ((envSheets / 500.0) * envPaperRate) + (envMakingLabourPerPc * quantity)

        val weddingLamSqIn = paperW * paperH * mainCardSheets * (if (lamBothSides) 2.0 else 1.0)
        val laminationCost = if (isLamEnabled && mainCardSheets > 0) max(weddingLamSqIn * lamRateSqIn, lamMin) else 0.0

        val foilRunningCost = quantity * foilPerPcRate
        val embossRunningCost = quantity * embossPerPcRate
        val diePunchingCost = quantity * diePunchPerPcRate
        val assemblyLaborCost = quantity * assemblyLabourPerSet

        val totalPlates = getColorMultiplier(colorType) * (if (isJuri) 2 else 1)
        val plateCost = totalPlates * plateRate
        val totalImpressions = ((mainCardSheets + insertSheetsTotal) * getColorMultiplier(colorType)).toInt()
        val printCost = if (mainCardSheets > 0) max((totalImpressions / 1000.0) * printRate1k, printMin) else 0.0
        val platesAndPrintCost = plateCost + printCost

        val totalFinishingAndLabor = foilBlockCost + foilRunningCost + embossBlockCost + embossRunningCost + dieMakingCharge + diePunchingCost + assemblyLaborCost
        val subTotal = cardPaperCost + envelopeCost + laminationCost + platesAndPrintCost + totalFinishingAndLabor + designCharge + shippingEstimate
        val profitAmount = (subTotal - shippingEstimate) * (profitPercent / 100.0)
        val grandTotal = subTotal + profitAmount
        val perUnitCost = if (quantity > 0) grandTotal / quantity else 0.0

        return WeddingCostResult(
            mainCardSheets = mainCardSheets,
            insertSheetsTotal = insertSheetsTotal,
            cardPaperCost = cardPaperCost,
            envSheets = envSheets,
            envelopeCost = envelopeCost,
            laminationSqIn = weddingLamSqIn,
            laminationCost = laminationCost,
            foilBlockCost = foilBlockCost,
            foilRunningCost = foilRunningCost,
            embossBlockCost = embossBlockCost,
            embossRunningCost = embossRunningCost,
            dieMakingCharge = dieMakingCharge,
            diePunchingCost = diePunchingCost,
            assemblyLaborCost = assemblyLaborCost,
            platesAndPrintCost = platesAndPrintCost,
            totalFinishingAndLabor = totalFinishingAndLabor,
            subTotal = subTotal,
            profitAmount = profitAmount,
            grandTotal = grandTotal,
            perUnitCost = perUnitCost
        )
    }

    // Sector 3. Banner & Festoon / Digital Large Format
    fun calculateDigitalCost(
        widthFeet: Double,
        heightFeet: Double,
        quantity: Int,
        materialRatePerSqFt: Double,
        isDigitalLamEnabled: Boolean,
        lamRatePerSqFt: Double,
        digitalLamMinCharge: Double,
        grommetsPerUnit: Int,
        ratePerGrommet: Double,
        isHemmingEnabled: Boolean,
        hemmingRatePerFoot: Double,
        isBoardMountEnabled: Boolean,
        boardMountRatePerSqFt: Double,
        standUnitPrice: Double,
        designCharge: Double,
        profitPercent: Double
    ): DigitalCostResult {
        val totalSqFtPerUnit = widthFeet * heightFeet
        val totalSqFtAll = totalSqFtPerUnit * quantity

        val printCost = totalSqFtAll * materialRatePerSqFt

        val digitalLamCost = if (isDigitalLamEnabled && totalSqFtAll > 0) {
            max(totalSqFtAll * lamRatePerSqFt, digitalLamMinCharge)
        } else 0.0

        val grommetCost = (grommetsPerUnit * quantity) * ratePerGrommet
        val perimeterFeet = 2.0 * (widthFeet + heightFeet) * quantity
        val hemmingCost = if (isHemmingEnabled) perimeterFeet * hemmingRatePerFoot else 0.0
        val boardMountCost = if (isBoardMountEnabled) totalSqFtAll * boardMountRatePerSqFt else 0.0
        val standCost = standUnitPrice * quantity

        val subTotal = printCost + digitalLamCost + grommetCost + hemmingCost + boardMountCost + standCost + designCharge
        val profitAmount = subTotal * (profitPercent / 100.0)
        val grandTotal = subTotal + profitAmount
        val perUnitCost = if (quantity > 0) grandTotal / quantity else 0.0

        return DigitalCostResult(
            totalSqFtPerUnit = totalSqFtPerUnit,
            totalSqFtAll = totalSqFtAll,
            printCost = printCost,
            digitalLamCost = digitalLamCost,
            grommetCost = grommetCost,
            perimeterFeet = perimeterFeet,
            hemmingCost = hemmingCost,
            boardMountCost = boardMountCost,
            standCost = standCost,
            subTotal = subTotal,
            profitAmount = profitAmount,
            grandTotal = grandTotal,
            perUnitCost = perUnitCost
        )
    }

    // Sector 4. Book Calculation
    fun calculateBookCost(
        bookWidth: Double,
        bookHeight: Double,
        totalPages: Int,
        bookQuantity: Int,
        pagesPerForme: Int,
        formasPerSheet: Int,
        innerPaperWidth: Double,
        innerPaperHeight: Double,
        innerPaperRate: Double,
        formeGroups: List<InnerFormeGroupState>,
        includeEndpaper: Boolean,
        epPaperWidth: Double,
        epPaperHeight: Double,
        epPaperRate: Double,
        epGtoCuts: Int,
        epColorType: String,
        epPlatePrice: Double,
        epIsJuri: Boolean,
        epIsBackToBack: Boolean,
        epPrintRate1k: Double,
        epPrintMin: Double,
        isEpLamEnabled: Boolean,
        epLamRate: Double,
        epLamMinCharge: Double,
        includeCover: Boolean,
        paperCaliper: Double,
        coverFlap: Double,
        cvPaperWidth: Double,
        cvPaperHeight: Double,
        cvPaperRate: Double,
        cvCutsPerSheet: Int,
        cvColorType: String,
        cvPlatePrice: Double,
        cvIsJuri: Boolean,
        cvIsBackToBack: Boolean,
        cvPrintRate1k: Double,
        cvPrintMin: Double,
        cvLamEnabled: Boolean,
        cvLamRatePerInch: Double,
        cvLamMinCharge: Double,
        cvLamBothSides: Boolean,
        includeExtraFinishing: Boolean,
        cvSandiRate: Double,
        cvSandiMin: Double,
        cvSpotUVRate: Double,
        cvSpotUVMin: Double,
        cvFoilBlockCost: Double,
        cvFoilRatePerPiece: Double,
        cvFoilMin: Double,
        cvEmbushBlockCost: Double,
        cvEmbushRatePerPiece: Double,
        cvEmbushMin: Double,
        bindingChargePerBook: Double,
        bindingMinCharge: Double,
        itemsPerPacket: Int,
        costPerPacket: Double,
        packagingRate1k: Double,
        packagingMinCharge: Double,
        designCharge: Double,
        shippingEstimate: Double,
        profitPercent: Double
    ): BookCostResult {
        val totalFormas = if (pagesPerForme > 0) ceil(totalPages.toDouble() / pagesPerForme) else 0.0
        val totalRequiredMainSheets = if (formasPerSheet > 0) (totalFormas * bookQuantity) / formasPerSheet else 0.0
        val totalReams = totalRequiredMainSheets / 500.0
        val innerPaperCost = totalReams * innerPaperRate

        var totalInnerPlatesCost = 0.0
        var totalInnerPrintCost = 0.0
        var totalInnerLamCost = 0.0
        val formeGroupResults = mutableListOf<InnerFormeGroupResult>()

        for (group in formeGroups) {
            val colorMultiplier = getColorMultiplier(group.printingColor)
            val calculatedPlates = (group.formaCount * colorMultiplier * (if (group.isJuri) 2 else 1))
            val groupPlateCost = calculatedPlates * group.plateRate

            val fullSheetsForGroup = if (formasPerSheet > 0) (bookQuantity.toDouble() * group.formaCount) / formasPerSheet else 0.0
            val machineSheets = fullSheetsForGroup * group.cutsPerSheet
            val calculatedImpressions = (machineSheets * colorMultiplier * (if (group.isJuri || group.isBackToBack) 2 else 1)).toInt()
            val groupPrintCost = if (fullSheetsForGroup > 0) {
                max((calculatedImpressions / 1000.0) * group.printingRate1k, group.printingMinCharge)
            } else 0.0

            val lamAreaSqIn = innerPaperWidth * innerPaperHeight * fullSheetsForGroup * (if (group.lamBothSides) 2.0 else 1.0)
            val groupLamCost = if (group.isLamEnabled && fullSheetsForGroup > 0) {
                max(lamAreaSqIn * group.lamRate, group.lamMin)
            } else 0.0

            val groupTotal = groupPlateCost + groupPrintCost + groupLamCost

            totalInnerPlatesCost += groupPlateCost
            totalInnerPrintCost += groupPrintCost
            totalInnerLamCost += groupLamCost

            formeGroupResults.add(
                InnerFormeGroupResult(
                    calculatedPlates = calculatedPlates,
                    groupPlateCost = groupPlateCost,
                    fullSheetsForGroup = fullSheetsForGroup,
                    machineSheets = machineSheets,
                    calculatedImpressions = calculatedImpressions,
                    groupPrintCost = groupPrintCost,
                    lamAreaSqIn = lamAreaSqIn,
                    groupLamCost = groupLamCost,
                    groupTotal = groupTotal
                )
            )
        }

        val epQuantity = bookQuantity * 2
        val epWidth = bookHeight
        val epHeight = bookWidth * 2.0
        val epItemsPerSheet = calculateItemsPerSheet(epPaperWidth, epPaperHeight, epWidth, epHeight)
        val epFullSheetQty = if (epItemsPerSheet > 0) ceil(epQuantity.toDouble() / epItemsPerSheet) else 0.0
        val epPaperCost = if (includeEndpaper) (epFullSheetQty / 500.0) * epPaperRate else 0.0

        val epColorMultiplier = getColorMultiplier(epColorType)
        val epPlates = epColorMultiplier * (if (epIsJuri) 2 else 1)
        val epPlateCost = if (includeEndpaper) epPlates * epPlatePrice else 0.0

        val machineSizeEp = epFullSheetQty * epGtoCuts
        val epImpressions = (machineSizeEp * epColorMultiplier * (if (epIsJuri || epIsBackToBack) 2 else 1)).toInt()
        val epPrintCost = if (includeEndpaper && epFullSheetQty > 0) {
            max((epImpressions / 1000.0) * epPrintRate1k, epPrintMin)
        } else 0.0

        val epTotalAreaSqIn = epPaperWidth * epPaperHeight * epFullSheetQty
        val epLamCost = if (includeEndpaper && isEpLamEnabled && epFullSheetQty > 0) {
            max(epTotalAreaSqIn * epLamRate, epLamMinCharge)
        } else 0.0

        val calculatedSpine = totalPages * paperCaliper
        val coverWidth = (bookWidth * 2) + calculatedSpine + coverFlap
        val coverHeight = bookHeight

        val cvItemsPerSheet = calculateItemsPerSheet(cvPaperWidth, cvPaperHeight, coverWidth, coverHeight)
        val cvFullSheetQty = if (cvItemsPerSheet > 0) ceil(bookQuantity.toDouble() / cvItemsPerSheet) else 0.0
        val cvPaperCost = if (includeCover) (cvFullSheetQty / 500.0) * cvPaperRate else 0.0

        val cvColorMultiplier = getColorMultiplier(cvColorType)
        val cvPlates = cvColorMultiplier * (if (cvIsJuri) 2 else 1)
        val cvPlateCost = if (includeCover) cvPlates * cvPlatePrice else 0.0

        val machineSizeCv = cvFullSheetQty * cvCutsPerSheet
        val cvImpressions = (machineSizeCv * cvColorMultiplier * (if (cvIsJuri || cvIsBackToBack) 2 else 1)).toInt()
        val cvPrintCost = if (includeCover && cvFullSheetQty > 0) {
            max((cvImpressions / 1000.0) * cvPrintRate1k, cvPrintMin)
        } else 0.0

        val cvItemAreaTotal = coverWidth * coverHeight * bookQuantity
        val cvLamTotalSqIn = cvItemAreaTotal * (if (cvLamBothSides) 2.0 else 1.0)
        val cvLamCost = if (includeCover && cvLamEnabled && bookQuantity > 0) {
            max(cvLamTotalSqIn * cvLamRatePerInch, cvLamMinCharge)
        } else 0.0

        val sandiCost = if (includeCover && includeExtraFinishing && bookQuantity > 0) {
            max(cvItemAreaTotal * cvSandiRate, cvSandiMin)
        } else 0.0
        val spotUvCost = if (includeCover && includeExtraFinishing && bookQuantity > 0) {
            max(cvItemAreaTotal * cvSpotUVRate, cvSpotUVMin)
        } else 0.0
        val foilCost = if (includeCover && includeExtraFinishing && bookQuantity > 0) {
            cvFoilBlockCost + max(bookQuantity * cvFoilRatePerPiece, cvFoilMin)
        } else 0.0
        val embossCost = if (includeCover && includeExtraFinishing && bookQuantity > 0) {
            cvEmbushBlockCost + max(bookQuantity * cvEmbushRatePerPiece, cvEmbushMin)
        } else 0.0
        val totalExtraFinishing = sandiCost + spotUvCost + foilCost + embossCost

        val totalBindingCost = if (bookQuantity > 0 && bindingChargePerBook > 0) {
            max(bookQuantity * bindingChargePerBook, bindingMinCharge)
        } else 0.0

        val packageCount = if (itemsPerPacket > 0) ceil(bookQuantity.toDouble() / itemsPerPacket).toInt() else 0
        val totalPackaging = if (packageCount > 0 && costPerPacket > 0) {
            packageCount * costPerPacket
        } else if (bookQuantity > 0) {
            max((bookQuantity / 1000.0) * packagingRate1k, packagingMinCharge)
        } else 0.0

        val totalPaper = innerPaperCost + epPaperCost + cvPaperCost
        val totalPlate = totalInnerPlatesCost + epPlateCost + cvPlateCost
        val totalPrint = totalInnerPrintCost + epPrintCost + cvPrintCost
        val totalLamination = totalInnerLamCost + epLamCost + cvLamCost

        val subTotal = totalPaper + totalPlate + totalPrint + totalLamination + totalExtraFinishing + totalBindingCost + totalPackaging + designCharge + shippingEstimate
        val profitAmount = (subTotal - shippingEstimate) * (profitPercent / 100.0)
        val grandTotal = subTotal + profitAmount
        val perBookCost = if (bookQuantity > 0) grandTotal / bookQuantity else 0.0

        return BookCostResult(
            totalFormas = totalFormas,
            totalRequiredMainSheets = totalRequiredMainSheets,
            totalReams = totalReams,
            innerPaperCost = innerPaperCost,
            formeGroupResults = formeGroupResults,
            totalInnerPlatesCost = totalInnerPlatesCost,
            totalInnerPrintCost = totalInnerPrintCost,
            totalInnerLamCost = totalInnerLamCost,
            epFullSheetQty = epFullSheetQty,
            epPaperCost = epPaperCost,
            epPlateCost = epPlateCost,
            epPrintCost = epPrintCost,
            epLamCost = epLamCost,
            calculatedSpine = calculatedSpine,
            coverWidth = coverWidth,
            coverHeight = coverHeight,
            cvFullSheetQty = cvFullSheetQty,
            cvPaperCost = cvPaperCost,
            cvPlateCost = cvPlateCost,
            cvPrintCost = cvPrintCost,
            cvLamCost = cvLamCost,
            sandiCost = sandiCost,
            spotUvCost = spotUvCost,
            foilCost = foilCost,
            embossCost = embossCost,
            totalExtraFinishing = totalExtraFinishing,
            totalBindingCost = totalBindingCost,
            totalPackaging = totalPackaging,
            subTotal = subTotal,
            profitAmount = profitAmount,
            grandTotal = grandTotal,
            perBookCost = perBookCost
        )
    }

    // Sector 5. Diary Calculation
    fun calculateDiaryCost(
        diaryCategory: DiaryCoverCategory,
        diaryWidth: Double,
        diaryHeight: Double,
        totalPages: Int,
        diaryQuantity: Int,
        paperCaliper: Double,
        boardRatePerSqFt: Double,
        boardMakingLaborRatePerPc: Double,
        wrapPaperWidth: Double,
        wrapPaperHeight: Double,
        wrapPaperRate: Double,
        wrapCuts: Int,
        wrapColorType: String,
        wrapPlateRate: Double,
        wrapIsJuri: Boolean,
        wrapIsBackToBack: Boolean,
        wrapPrintRate1k: Double,
        wrapPrintMin: Double,
        wrapLamEnabled: Boolean,
        wrapLamRate: Double,
        wrapLamMin: Double,
        rexineRatePerSqFt: Double,
        rexineWastagePercent: Double,
        rexineFoilBlockCost: Double,
        rexineFoilRatePerPc: Double,
        rexineBindingLaborRatePerPc: Double,
        foamRatePerPc: Double,
        puLeatherRatePerSqFt: Double,
        puWastagePercent: Double,
        stitchingRatePerPc: Double,
        debossBlockCost: Double,
        debossHitRatePerPc: Double,
        ribbonRatePerPc: Double,
        elasticMaterialRate: Double,
        elasticRivetingRate: Double,
        penLoopRatePerPc: Double,
        metalCornerRatePerPc: Double,
        presentationBoxRatePerPc: Double,
        pagesPerForme: Int,
        formasPerSheet: Int,
        innerPaperWidth: Double,
        innerPaperHeight: Double,
        innerPaperRate: Double,
        formeGroups: List<InnerFormeGroupState>,
        includeEndpaper: Boolean,
        epPaperWidth: Double,
        epPaperHeight: Double,
        epPaperRate: Double,
        epGtoCuts: Int,
        epColorType: String,
        epPlatePrice: Double,
        epIsJuri: Boolean,
        epIsBackToBack: Boolean,
        epPrintRate1k: Double,
        epPrintMin: Double,
        isEpLamEnabled: Boolean,
        epLamRate: Double,
        epLamMinCharge: Double,
        bindingChargePerBook: Double,
        bindingMinCharge: Double,
        itemsPerPacket: Int,
        costPerPacket: Double,
        packagingRate1k: Double,
        packagingMinCharge: Double,
        designCharge: Double,
        shippingEstimate: Double,
        profitPercent: Double
    ): DiaryCostResult {
        val spineWidth = totalPages * paperCaliper

        val bookBaseResult = calculateBookCost(
            bookWidth = diaryWidth,
            bookHeight = diaryHeight,
            totalPages = totalPages,
            bookQuantity = diaryQuantity,
            pagesPerForme = pagesPerForme,
            formasPerSheet = formasPerSheet,
            innerPaperWidth = innerPaperWidth,
            innerPaperHeight = innerPaperHeight,
            innerPaperRate = innerPaperRate,
            formeGroups = formeGroups,
            includeEndpaper = includeEndpaper,
            epPaperWidth = epPaperWidth,
            epPaperHeight = epPaperHeight,
            epPaperRate = epPaperRate,
            epGtoCuts = epGtoCuts,
            epColorType = epColorType,
            epPlatePrice = epPlatePrice,
            epIsJuri = epIsJuri,
            epIsBackToBack = epIsBackToBack,
            epPrintRate1k = epPrintRate1k,
            epPrintMin = epPrintMin,
            isEpLamEnabled = isEpLamEnabled,
            epLamRate = epLamRate,
            epLamMinCharge = epLamMinCharge,
            includeCover = false,
            paperCaliper = paperCaliper,
            coverFlap = 0.0,
            cvPaperWidth = 0.0,
            cvPaperHeight = 0.0,
            cvPaperRate = 0.0,
            cvCutsPerSheet = 1,
            cvColorType = "4 Color",
            cvPlatePrice = 0.0,
            cvIsJuri = false,
            cvIsBackToBack = false,
            cvPrintRate1k = 0.0,
            cvPrintMin = 0.0,
            cvLamEnabled = false,
            cvLamRatePerInch = 0.0,
            cvLamMinCharge = 0.0,
            cvLamBothSides = false,
            includeExtraFinishing = false,
            cvSandiRate = 0.0,
            cvSandiMin = 0.0,
            cvSpotUVRate = 0.0,
            cvSpotUVMin = 0.0,
            cvFoilBlockCost = 0.0,
            cvFoilRatePerPiece = 0.0,
            cvFoilMin = 0.0,
            cvEmbushBlockCost = 0.0,
            cvEmbushRatePerPiece = 0.0,
            cvEmbushMin = 0.0,
            bindingChargePerBook = bindingChargePerBook,
            bindingMinCharge = bindingMinCharge,
            itemsPerPacket = itemsPerPacket,
            costPerPacket = costPerPacket,
            packagingRate1k = packagingRate1k,
            packagingMinCharge = packagingMinCharge,
            designCharge = 0.0,
            shippingEstimate = 0.0,
            profitPercent = 0.0
        )

        val boardAreaSqFt = (((diaryWidth * diaryHeight * 2.0) + (spineWidth * diaryHeight)) / 144.0) * diaryQuantity
        var boardCost = 0.0
        var wrappingPaperCost = 0.0
        var wrappingPlateCost = 0.0
        var wrappingPrintCost = 0.0
        var wrappingLamCost = 0.0
        var boardPastingLaborCost = 0.0
        var rexineCost = 0.0
        var rexineFoilCost = 0.0
        var rexineBindingLaborCost = 0.0
        var foamPaddingCost = 0.0
        var puLeatherCost = 0.0
        var stitchingCost = 0.0
        var debossCost = 0.0

        when (diaryCategory) {
            DiaryCoverCategory.SOFT_COVER -> {}
            DiaryCoverCategory.HARD_COVER -> {
                boardCost = boardAreaSqFt * boardRatePerSqFt

                val wrapPaperW = (diaryWidth * 2.0) + spineWidth + 1.5
                val wrapPaperH = diaryHeight + 1.5
                val wrapItemsPerSheet = calculateItemsPerSheet(wrapPaperWidth, wrapPaperHeight, wrapPaperW, wrapPaperH)
                val wrapSheets = if (wrapItemsPerSheet > 0) ceil(diaryQuantity.toDouble() / wrapItemsPerSheet) else 0.0
                wrappingPaperCost = (wrapSheets / 500.0) * wrapPaperRate

                val wrapColorMult = getColorMultiplier(wrapColorType)
                wrappingPlateCost = wrapColorMult * (if (wrapIsJuri) 2 else 1) * wrapPlateRate

                val machineSizeWrap = wrapSheets * wrapCuts
                val wrapImpressions = (machineSizeWrap * wrapColorMult * (if (wrapIsJuri || wrapIsBackToBack) 2 else 1)).toInt()
                wrappingPrintCost = if (wrapSheets > 0) max((wrapImpressions / 1000.0) * wrapPrintRate1k, wrapPrintMin) else 0.0

                val wrapAreaSqIn = wrapPaperWidth * wrapPaperHeight * wrapSheets
                wrappingLamCost = if (wrapLamEnabled && wrapSheets > 0) max(wrapAreaSqIn * wrapLamRate, wrapLamMin) else 0.0

                boardPastingLaborCost = diaryQuantity * boardMakingLaborRatePerPc
            }
            DiaryCoverCategory.REXINE_HARDBOUND -> {
                boardCost = boardAreaSqFt * boardRatePerSqFt

                val rexineW = (diaryWidth * 2.0) + spineWidth + 1.5
                val rexineH = diaryHeight + 1.5
                val rexineAreaSqFt = ((rexineW * rexineH) / 144.0) * diaryQuantity * (1.0 + (rexineWastagePercent / 100.0))
                rexineCost = rexineAreaSqFt * rexineRatePerSqFt

                rexineFoilCost = rexineFoilBlockCost + (diaryQuantity * rexineFoilRatePerPc)
                rexineBindingLaborCost = diaryQuantity * rexineBindingLaborRatePerPc
            }
            DiaryCoverCategory.PREMIUM_PU_LEATHER -> {
                boardCost = boardAreaSqFt * boardRatePerSqFt
                foamPaddingCost = diaryQuantity * foamRatePerPc

                val puW = (diaryWidth * 2.0) + spineWidth + 1.5
                val puH = diaryHeight + 1.5
                val puAreaSqFt = ((puW * puH) / 144.0) * diaryQuantity * (1.0 + (puWastagePercent / 100.0))
                puLeatherCost = puAreaSqFt * puLeatherRatePerSqFt

                stitchingCost = diaryQuantity * stitchingRatePerPc
                debossCost = debossBlockCost + (diaryQuantity * debossHitRatePerPc)
            }
        }

        val ribbonCost = diaryQuantity * ribbonRatePerPc
        val elasticCost = diaryQuantity * (elasticMaterialRate + elasticRivetingRate)
        val penLoopCost = diaryQuantity * penLoopRatePerPc
        val metalCornerCost = diaryQuantity * 4.0 * metalCornerRatePerPc
        val boxCost = diaryQuantity * presentationBoxRatePerPc

        val diaryCoverTotal = boardCost + wrappingPaperCost + wrappingPlateCost + wrappingPrintCost + wrappingLamCost +
                boardPastingLaborCost + rexineCost + rexineFoilCost + rexineBindingLaborCost + foamPaddingCost +
                puLeatherCost + stitchingCost + debossCost

        val accessoriesTotal = ribbonCost + elasticCost + penLoopCost + metalCornerCost + boxCost

        val subTotal = bookBaseResult.subTotal + diaryCoverTotal + accessoriesTotal + designCharge + shippingEstimate
        val profitAmount = (subTotal - shippingEstimate) * (profitPercent / 100.0)
        val grandTotal = subTotal + profitAmount
        val perDiaryCost = if (diaryQuantity > 0) grandTotal / diaryQuantity else 0.0

        return DiaryCostResult(
            bookBaseResult = bookBaseResult,
            boardCost = boardCost,
            wrappingPaperCost = wrappingPaperCost,
            wrappingPlateCost = wrappingPlateCost,
            wrappingPrintCost = wrappingPrintCost,
            wrappingLamCost = wrappingLamCost,
            boardPastingLaborCost = boardPastingLaborCost,
            rexineCost = rexineCost,
            rexineFoilCost = rexineFoilCost,
            rexineBindingLaborCost = rexineBindingLaborCost,
            foamPaddingCost = foamPaddingCost,
            puLeatherCost = puLeatherCost,
            stitchingCost = stitchingCost,
            debossCost = debossCost,
            ribbonCost = ribbonCost,
            elasticCost = elasticCost,
            penLoopCost = penLoopCost,
            metalCornerCost = metalCornerCost,
            boxCost = boxCost,
            diaryCoverTotal = diaryCoverTotal,
            accessoriesTotal = accessoriesTotal,
            subTotal = subTotal,
            profitAmount = profitAmount,
            grandTotal = grandTotal,
            perDiaryCost = perDiaryCost
        )
    }

    // Sector 6. Gift Items & Sublimation Calculation
    fun calculateGiftCost(
        quantity: Int,
        blankItemPurchaseRate: Double,
        customMethod: String,
        screenMakingCostPerColor: Double,
        colorCount: Int,
        printRunRatePerPc: Double,
        totalPrintAreaSqIn: Double,
        dtfRatePerSqFt: Double,
        sublimationPaperAndInkRate: Double,
        heatPressLabourPerPc: Double,
        uvRatePerSqIn: Double,
        engraveRatePerMinute: Double,
        timeInMinutes: Double,
        packagingBoxRate: Double,
        designSetupFee: Double,
        profitPercent: Double
    ): GiftCostResult {
        val baseBlankCost = quantity * blankItemPurchaseRate
        val printMethodCost = when (customMethod) {
            "Screen Print", "স্ক্রিন প্রিন্টিং (Screen Print)" -> (screenMakingCostPerColor * colorCount) + (quantity * printRunRatePerPc)
            "DTF", "ডিটিএফ প্রিন্ট (DTF Printing)" -> ((totalPrintAreaSqIn / 144.0) * dtfRatePerSqFt * quantity)
            "Sublimation", "সাবলিমেশন ప్రింట్ (Sublimation)" -> quantity * (sublimationPaperAndInkRate + heatPressLabourPerPc)
            "UV Flatbed", "ইউভি ফ্ল্যাটবেড (UV Flatbed)" -> (totalPrintAreaSqIn * uvRatePerSqIn * quantity)
            "Laser Engrave", "লেজার এনগ্রেভিং (Laser Engrave)" -> quantity * engraveRatePerMinute * timeInMinutes
            else -> quantity * printRunRatePerPc
        }
        val packagingCost = quantity * packagingBoxRate
        val subTotal = baseBlankCost + printMethodCost + packagingCost + designSetupFee
        val profitAmount = subTotal * (profitPercent / 100.0)
        val grandTotal = subTotal + profitAmount
        val perUnitCost = if (quantity > 0) grandTotal / quantity else 0.0

        return GiftCostResult(
            baseBlankCost = baseBlankCost,
            printMethodCost = printMethodCost,
            packagingCost = packagingCost,
            subTotal = subTotal,
            profitAmount = profitAmount,
            grandTotal = grandTotal,
            perUnitCost = perUnitCost
        )
    }

    // Sector 7. Seal & Stamp Calculation
    fun calculateStampCost(
        quantity: Int,
        casingPriceByModel: Double,
        isBulkSameDesign: Boolean,
        plateMakingRateBySize: Double,
        isSelfInking: Boolean,
        inkPadRate: Double,
        manualInkPadPrice: Double,
        designCharge: Double,
        profitPercent: Double
    ): StampCostResult {
        val baseCasingCost = casingPriceByModel * quantity
        val plateMakingCost = if (isBulkSameDesign) {
            plateMakingRateBySize
        } else {
            plateMakingRateBySize * quantity
        }
        val inkPadCost = (if (isSelfInking) inkPadRate else manualInkPadPrice) * quantity
        val subTotal = baseCasingCost + plateMakingCost + inkPadCost + designCharge
        val profitAmount = subTotal * (profitPercent / 100.0)
        val grandTotal = subTotal + profitAmount
        val perUnitCost = if (quantity > 0) grandTotal / quantity else 0.0

        return StampCostResult(
            baseCasingCost = baseCasingCost,
            plateMakingCost = plateMakingCost,
            inkPadCost = inkPadCost,
            subTotal = subTotal,
            profitAmount = profitAmount,
            grandTotal = grandTotal,
            perUnitCost = perUnitCost
        )
    }

    // Sector 8. Packaging & Carton Box Calculation
    fun calculatePackagingCost(
        length: Double,
        width: Double,
        height: Double,
        glueFlapAllowance: Double,
        topFlapAllowance: Double,
        boxQuantity: Int,
        wastagePercent: Double,
        paperW: Double,
        paperH: Double,
        paperReamRate: Double,
        cutsPerSheet: Int,
        colorType: String,
        isJuri: Boolean,
        isBackToBack: Boolean,
        plateRate: Double,
        printRate1k: Double,
        printMin: Double,
        isLamEnabled: Boolean,
        lamRateSqIn: Double,
        lamMin: Double,
        isExistingDie: Boolean,
        dieBlockCost: Double,
        dieCuttingRate1k: Double,
        hasWindow: Boolean,
        windowFilmRate: Double,
        windowPastingLabour: Double,
        sidePastingLabourPerPc: Double,
        isShoppingBag: Boolean,
        handleRatePerPc: Double,
        designCharge: Double,
        shippingEstimate: Double,
        profitPercent: Double
    ): PackagingCostResult {
        val flatSheetW = (length * 2.0) + (width * 2.0) + glueFlapAllowance
        val flatSheetH = height + (width * 2.0) + topFlapAllowance

        val itemsPerMachineSheet = calculateItemsPerSheet(paperW, paperH, flatSheetW, flatSheetH)
        val totalMachineSheets = if (itemsPerMachineSheet > 0) {
            ceil(boxQuantity.toDouble() / itemsPerMachineSheet) * (1.0 + (wastagePercent / 100.0))
        } else 0.0
        val boxMaterialCost = (totalMachineSheets / 500.0) * paperReamRate

        val colorMult = getColorMultiplier(colorType)
        val boxPlates = colorMult * (if (isJuri) 2 else 1)
        val plateCost = boxPlates * plateRate
        val boxImpressions = (totalMachineSheets * colorMult * (if (isJuri || isBackToBack) 2 else 1)).toInt()
        val printCost = if (totalMachineSheets > 0) max((boxImpressions / 1000.0) * printRate1k, printMin) else 0.0

        val cuts = max(1, cutsPerSheet)
        val boxLamSqIn = paperW * paperH * (totalMachineSheets / cuts)
        val laminationCost = if (isLamEnabled && totalMachineSheets > 0) max(boxLamSqIn * lamRateSqIn, lamMin) else 0.0

        val dieBlockFinalCost = if (isExistingDie) 0.0 else dieBlockCost
        val dieRunCost = (totalMachineSheets / 1000.0) * dieCuttingRate1k
        val windowCost = if (hasWindow) boxQuantity * (windowFilmRate + windowPastingLabour) else 0.0
        val pastingCost = boxQuantity * sidePastingLabourPerPc
        val handleCost = if (isShoppingBag) boxQuantity * handleRatePerPc else 0.0

        val subTotal = boxMaterialCost + plateCost + printCost + laminationCost + dieBlockFinalCost + dieRunCost + windowCost + pastingCost + handleCost + designCharge + shippingEstimate
        val profitAmount = (subTotal - shippingEstimate) * (profitPercent / 100.0)
        val grandTotal = subTotal + profitAmount
        val perUnitCost = if (boxQuantity > 0) grandTotal / boxQuantity else 0.0

        return PackagingCostResult(
            flatSheetW = flatSheetW,
            flatSheetH = flatSheetH,
            itemsPerMachineSheet = itemsPerMachineSheet,
            totalMachineSheets = totalMachineSheets,
            boxMaterialCost = boxMaterialCost,
            plateCost = plateCost,
            printCost = printCost,
            laminationSqIn = boxLamSqIn,
            laminationCost = laminationCost,
            dieBlockFinalCost = dieBlockFinalCost,
            dieRunCost = dieRunCost,
            windowCost = windowCost,
            pastingCost = pastingCost,
            handleCost = handleCost,
            subTotal = subTotal,
            profitAmount = profitAmount,
            grandTotal = grandTotal,
            perUnitCost = perUnitCost
        )
    }
}
