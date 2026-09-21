package com.sucharu.sucharupro.ui.features.printing.calculator.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.printingcalculator.ColorTypeOption
import com.sucharu.sucharupro.domain.model.printingcalculator.InnerFormeGroupState
import com.sucharu.sucharupro.domain.service.printingcalculator.CalculationEngine
import com.sucharu.sucharupro.ui.features.printing.calculator.components.*

@Composable
fun BookSection(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    // 1. Basic Book Specs & Inner Paper
    var bookWidthStr by remember { mutableStateOf("5.5") }
    var bookHeightStr by remember { mutableStateOf("8.5") }
    var totalPagesStr by remember { mutableStateOf("160") }
    var bookQuantityStr by remember { mutableStateOf("1000") }

    var pagesPerFormeStr by remember { mutableStateOf("16") }
    var formasPerSheetStr by remember { mutableStateOf("2") }

    var innerPaperWidthStr by remember { mutableStateOf("20.0") }
    var innerPaperHeightStr by remember { mutableStateOf("30.0") }
    var innerPaperRateStr by remember { mutableStateOf("1800.0") }

    // 2. Dynamic Inner Forme Groups
    var formeGroups by remember {
        mutableStateOf(
            listOf(
                InnerFormeGroupState(
                    formaCount = 8,
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

    // 3. Endpaper / Pustany
    var includeEndpaper by remember { mutableStateOf(false) }
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

    // 4. Book Cover
    var includeCover by remember { mutableStateOf(true) }
    var paperCaliperStr by remember { mutableStateOf("0.0035") } // Caliper per page in inches
    var coverFlapStr by remember { mutableStateOf("2.0") }
    var cvPaperWidthStr by remember { mutableStateOf("20.0") }
    var cvPaperHeightStr by remember { mutableStateOf("30.0") }
    var cvPaperRateStr by remember { mutableStateOf("2400.0") }
    var cvCutsPerSheetStr by remember { mutableStateOf("1") }
    var cvColorType by remember { mutableStateOf(ColorTypeOption.FOUR_COLOR.displayName) }
    var cvPlatePriceStr by remember { mutableStateOf("250.0") }
    var cvIsJuri by remember { mutableStateOf(false) }
    var cvIsBackToBack by remember { mutableStateOf(false) }
    var cvPrintRate1kStr by remember { mutableStateOf("180.0") }
    var cvPrintMinStr by remember { mutableStateOf("350.0") }

    var cvLamEnabled by remember { mutableStateOf(true) }
    var cvLamRatePerInchStr by remember { mutableStateOf("0.002") }
    var cvLamMinChargeStr by remember { mutableStateOf("150.0") }
    var cvLamBothSides by remember { mutableStateOf(false) }

    var includeExtraFinishing by remember { mutableStateOf(false) }
    var cvSandiRateStr by remember { mutableStateOf("0.0") }
    var cvSpotUVRateStr by remember { mutableStateOf("0.0") }
    var cvFoilBlockCostStr by remember { mutableStateOf("0.0") }
    var cvFoilRatePerPieceStr by remember { mutableStateOf("0.0") }
    var cvEmbushBlockCostStr by remember { mutableStateOf("0.0") }
    var cvEmbushRatePerPieceStr by remember { mutableStateOf("0.0") }

    // 5. Binding, Packaging & Others
    var bindingChargePerBookStr by remember { mutableStateOf("15.0") }
    var bindingMinChargeStr by remember { mutableStateOf("500.0") }
    var itemsPerPacketStr by remember { mutableStateOf("50") }
    var costPerPacketStr by remember { mutableStateOf("0.0") }
    var packagingRate1kStr by remember { mutableStateOf("100.0") }
    var packagingMinChargeStr by remember { mutableStateOf("100.0") }
    var designChargeStr by remember { mutableStateOf("500.0") }
    var shippingEstimateStr by remember { mutableStateOf("200.0") }
    var profitPercentStr by remember { mutableStateOf("15.0") }

    // Calculation Result
    val result = remember(
        bookWidthStr, bookHeightStr, totalPagesStr, bookQuantityStr, pagesPerFormeStr, formasPerSheetStr,
        innerPaperWidthStr, innerPaperHeightStr, innerPaperRateStr, formeGroups,
        includeEndpaper, epPaperWidthStr, epPaperHeightStr, epPaperRateStr, epGtoCutsStr, epColorType, epPlatePriceStr, epIsJuri, epIsBackToBack, epPrintRate1kStr, epPrintMinStr, isEpLamEnabled, epLamRateStr, epLamMinChargeStr,
        includeCover, paperCaliperStr, coverFlapStr, cvPaperWidthStr, cvPaperHeightStr, cvPaperRateStr, cvCutsPerSheetStr, cvColorType, cvPlatePriceStr, cvIsJuri, cvIsBackToBack, cvPrintRate1kStr, cvPrintMinStr, cvLamEnabled, cvLamRatePerInchStr, cvLamMinChargeStr, cvLamBothSides, includeExtraFinishing, cvSandiRateStr, cvSpotUVRateStr, cvFoilBlockCostStr, cvFoilRatePerPieceStr, cvEmbushBlockCostStr, cvEmbushRatePerPieceStr,
        bindingChargePerBookStr, bindingMinChargeStr, itemsPerPacketStr, costPerPacketStr, packagingRate1kStr, packagingMinChargeStr, designChargeStr, shippingEstimateStr, profitPercentStr
    ) {
        val bW = bookWidthStr.toDoubleOrNull() ?: 0.0
        val bH = bookHeightStr.toDoubleOrNull() ?: 0.0
        val pages = totalPagesStr.toIntOrNull() ?: 0
        val qty = bookQuantityStr.toIntOrNull() ?: 0
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

        val caliper = paperCaliperStr.toDoubleOrNull() ?: 0.0035
        val flap = coverFlapStr.toDoubleOrNull() ?: 0.0
        val cvPaperW = cvPaperWidthStr.toDoubleOrNull() ?: 0.0
        val cvPaperH = cvPaperHeightStr.toDoubleOrNull() ?: 0.0
        val cvPaperRate = cvPaperRateStr.toDoubleOrNull() ?: 0.0
        val cvCuts = cvCutsPerSheetStr.toIntOrNull() ?: 1
        val cvPlatePrice = cvPlatePriceStr.toDoubleOrNull() ?: 0.0
        val cvPrint1k = cvPrintRate1kStr.toDoubleOrNull() ?: 0.0
        val cvPrintMin = cvPrintMinStr.toDoubleOrNull() ?: 0.0
        val cvLamRate = cvLamRatePerInchStr.toDoubleOrNull() ?: 0.0
        val cvLamMin = cvLamMinChargeStr.toDoubleOrNull() ?: 0.0

        val sandiRate = cvSandiRateStr.toDoubleOrNull() ?: 0.0
        val spotUvRate = cvSpotUVRateStr.toDoubleOrNull() ?: 0.0
        val foilBlock = cvFoilBlockCostStr.toDoubleOrNull() ?: 0.0
        val foilRate = cvFoilRatePerPieceStr.toDoubleOrNull() ?: 0.0
        val embushBlock = cvEmbushBlockCostStr.toDoubleOrNull() ?: 0.0
        val embushRate = cvEmbushRatePerPieceStr.toDoubleOrNull() ?: 0.0

        val bindingCharge = bindingChargePerBookStr.toDoubleOrNull() ?: 0.0
        val bindingMin = bindingMinChargeStr.toDoubleOrNull() ?: 0.0
        val itemsPkt = itemsPerPacketStr.toIntOrNull() ?: 50
        val costPkt = costPerPacketStr.toDoubleOrNull() ?: 0.0
        val pack1k = packagingRate1kStr.toDoubleOrNull() ?: 0.0
        val packMin = packagingMinChargeStr.toDoubleOrNull() ?: 0.0
        val design = designChargeStr.toDoubleOrNull() ?: 0.0
        val shipping = shippingEstimateStr.toDoubleOrNull() ?: 0.0
        val profit = profitPercentStr.toDoubleOrNull() ?: 0.0

        CalculationEngine.calculateBookCost(
            bookWidth = bW,
            bookHeight = bH,
            totalPages = pages,
            bookQuantity = qty,
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
            includeCover = includeCover,
            paperCaliper = caliper,
            coverFlap = flap,
            cvPaperWidth = cvPaperW,
            cvPaperHeight = cvPaperH,
            cvPaperRate = cvPaperRate,
            cvCutsPerSheet = cvCuts,
            cvColorType = cvColorType,
            cvPlatePrice = cvPlatePrice,
            cvIsJuri = cvIsJuri,
            cvIsBackToBack = cvIsBackToBack,
            cvPrintRate1k = cvPrint1k,
            cvPrintMin = cvPrintMin,
            cvLamEnabled = cvLamEnabled,
            cvLamRatePerInch = cvLamRate,
            cvLamMinCharge = cvLamMin,
            cvLamBothSides = cvLamBothSides,
            includeExtraFinishing = includeExtraFinishing,
            cvSandiRate = sandiRate,
            cvSandiMin = 0.0,
            cvSpotUVRate = spotUvRate,
            cvSpotUVMin = 0.0,
            cvFoilBlockCost = foilBlock,
            cvFoilRatePerPiece = foilRate,
            cvFoilMin = 0.0,
            cvEmbushBlockCost = embushBlock,
            cvEmbushRatePerPiece = embushRate,
            cvEmbushMin = 0.0,
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 12.dp, end = 12.dp, top = 110.dp, bottom = 24.dp)
        ) {
            // 1. Basic Specs & Inner Paper
            SectionHeaderCard(
                title = "১. বইয়ের মূল স্পেসিফিকেশন ও ইনার কাগজ",
                subtitle = "বইয়ের মাপ, মোট পৃষ্ঠা, কপি সংখ্যা ও ফর্মা সেটআপ"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = bookWidthStr,
                        onValueChange = { bookWidthStr = it },
                        label = "বইয়ের চওড়া (Width)",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = bookHeightStr,
                        onValueChange = { bookHeightStr = it },
                        label = "বইয়ের উচ্চতা (Height)",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = totalPagesStr,
                        onValueChange = { totalPagesStr = it },
                        label = "মোট পৃষ্ঠা (Total Pages)",
                        isInteger = true,
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = bookQuantityStr,
                        onValueChange = { bookQuantityStr = it },
                        label = "বইয়ের কপি সংখ্যা (Quantity)",
                        suffix = "copies",
                        isInteger = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = pagesPerFormeStr,
                        onValueChange = { pagesPerFormeStr = it },
                        label = "প্রতি ফর্মার পেজ (4/8/16/32)",
                        isInteger = true,
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = formasPerSheetStr,
                        onValueChange = { formasPerSheetStr = it },
                        label = "১ শিটে কত ফর্মা (Formes/Sheet)",
                        isInteger = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                    label = "ইনার পেপার রিম দর (Inner Paper Rate)",
                    suffix = "৳/ream"
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "হিসাবীকৃত মোট ফর্মা: ${result.totalFormas.toInt()} | মোট মেইন শিট: ${result.totalRequiredMainSheets.toInt()} | মোট রিম: ${formatMoney(result.totalReams)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Dynamic Inner Forme Groups
            SectionHeaderCard(
                title = "২. ইনার ফর্মা গ্রুপসমূহ (Dynamic Forme Groups)",
                subtitle = "ভিন্ন কালার ও প্লেট সাইজের ফর্মা গ্রুপ যোগ করুন",
                actionButton = {
                    Button(
                        onClick = {
                            formeGroups = formeGroups + InnerFormeGroupState(
                                formaCount = 1,
                                printingColor = ColorTypeOption.ONE_COLOR.displayName
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("+ ফর্মা গ্রুপ", fontSize = 12.sp)
                    }
                }
            ) {
                formeGroups.forEachIndexed { index, group ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ফর্মা গ্রুপ #${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (formeGroups.size > 1) {
                                    TextButton(onClick = {
                                        formeGroups = formeGroups.filterIndexed { i, _ -> i != index }
                                    }) {
                                        Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberInputField(
                                    value = group.formaCount.toString(),
                                    onValueChange = { valCount ->
                                        val count = valCount.toIntOrNull() ?: 0
                                        formeGroups = formeGroups.mapIndexed { i, g -> if (i == index) g.copy(formaCount = count) else g }
                                    },
                                    label = "কতটি ফর্মা",
                                    isInteger = true,
                                    modifier = Modifier.weight(1f)
                                )
                                NumberInputField(
                                    value = group.cutsPerSheet.toString(),
                                    onValueChange = { valCuts ->
                                        val cuts = valCuts.toIntOrNull() ?: 1
                                        formeGroups = formeGroups.mapIndexed { i, g -> if (i == index) g.copy(cutsPerSheet = cuts) else g }
                                    },
                                    label = "কাট/শিট",
                                    isInteger = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            DropdownMenuField(
                                selectedOption = group.printingColor,
                                options = ColorTypeOption.entries.map { it.displayName },
                                onOptionSelected = { col ->
                                    formeGroups = formeGroups.mapIndexed { i, g -> if (i == index) g.copy(printingColor = col) else g }
                                },
                                label = "কালার টাইপ"
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SwitchRowField(
                                    title = "জুরি (Juri)",
                                    checked = group.isJuri,
                                    onCheckedChange = { chk ->
                                        formeGroups = formeGroups.mapIndexed { i, g -> if (i == index) g.copy(isJuri = chk, isBackToBack = if (chk) false else g.isBackToBack) else g }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SwitchRowField(
                                    title = "এপিঠ-ওপিঠ",
                                    checked = group.isBackToBack,
                                    onCheckedChange = { chk ->
                                        formeGroups = formeGroups.mapIndexed { i, g -> if (i == index) g.copy(isBackToBack = chk, isJuri = if (chk) false else g.isJuri) else g }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberInputField(
                                    value = group.plateRate.toString(),
                                    onValueChange = { pRate ->
                                        val rate = pRate.toDoubleOrNull() ?: 0.0
                                        formeGroups = formeGroups.mapIndexed { i, g -> if (i == index) g.copy(plateRate = rate) else g }
                                    },
                                    label = "প্লেট দর",
                                    suffix = "৳",
                                    modifier = Modifier.weight(1f)
                                )
                                NumberInputField(
                                    value = group.printingRate1k.toString(),
                                    onValueChange = { prRate ->
                                        val rate = prRate.toDoubleOrNull() ?: 0.0
                                        formeGroups = formeGroups.mapIndexed { i, g -> if (i == index) g.copy(printingRate1k = rate) else g }
                                    },
                                    label = "প্রিন্ট রেট/১০০০",
                                    suffix = "৳",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Endpaper / Pustany
            SectionHeaderCard(
                title = "৩. পুস্তানি / অ্যান্ডপেপার (Endpaper / Pustany)",
                subtitle = "বই বাঁধাইয়ের জন্য ভিতরের ডাবল পাতা (Pustany)"
            ) {
                SwitchRowField(
                    title = "পুস্তানি যোগ করুন (Include Endpaper)",
                    checked = includeEndpaper,
                    onCheckedChange = { includeEndpaper = it }
                )
                if (includeEndpaper) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = epPaperWidthStr,
                            onValueChange = { epPaperWidthStr = it },
                            label = "পুস্তানি পেপার W",
                            suffix = "in",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = epPaperHeightStr,
                            onValueChange = { epPaperHeightStr = it },
                            label = "পুস্তানি পেপার H",
                            suffix = "in",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = epPaperRateStr,
                            onValueChange = { epPaperRateStr = it },
                            label = "পুস্তানি কাগজ রিম দর",
                            suffix = "৳/ream",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = epGtoCutsStr,
                            onValueChange = { epGtoCutsStr = it },
                            label = "কাট সংখ্যা",
                            isInteger = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DropdownMenuField(
                        selectedOption = epColorType,
                        options = ColorTypeOption.entries.map { it.displayName },
                        onOptionSelected = { epColorType = it },
                        label = "পুস্তানি কালার টাইপ"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = epPlatePriceStr,
                            onValueChange = { epPlatePriceStr = it },
                            label = "প্লেট দর",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = epPrintRate1kStr,
                            onValueChange = { epPrintRate1kStr = it },
                            label = "প্রিন্টিং রেট/১০০০",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Book Cover
            SectionHeaderCard(
                title = "৪. বইয়ের কভার (Book Cover)",
                subtitle = "স্পাইন ও ওপেন সাইজ অটো-ক্যালকুলেশন সহ কভার ডিজাইন"
            ) {
                SwitchRowField(
                    title = "কভার যোগ করুন (Include Book Cover)",
                    checked = includeCover,
                    onCheckedChange = { includeCover = it }
                )
                if (includeCover) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = paperCaliperStr,
                            onValueChange = { paperCaliperStr = it },
                            label = "কাগজের থিকনেস/Caliper",
                            suffix = "in",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = coverFlapStr,
                            onValueChange = { coverFlapStr = it },
                            label = "কভার ফ্ল্যাপ (Flap)",
                            suffix = "in",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "অটো হিসাবীকৃত স্পাইন: ${formatMoney(result.calculatedSpine)}\" | কভার ওপেন সাইজ: ${formatMoney(result.coverWidth)}\" x ${formatMoney(result.coverHeight)}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = cvPaperWidthStr,
                            onValueChange = { cvPaperWidthStr = it },
                            label = "কভার পেপার W",
                            suffix = "in",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = cvPaperHeightStr,
                            onValueChange = { cvPaperHeightStr = it },
                            label = "কভার পেপার H",
                            suffix = "in",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = cvPaperRateStr,
                            onValueChange = { cvPaperRateStr = it },
                            label = "কভার পেপার রিম দর",
                            suffix = "৳/ream",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = cvCutsPerSheetStr,
                            onValueChange = { cvCutsPerSheetStr = it },
                            label = "কাট/শিট",
                            isInteger = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DropdownMenuField(
                        selectedOption = cvColorType,
                        options = ColorTypeOption.entries.map { it.displayName },
                        onOptionSelected = { cvColorType = it },
                        label = "কভার কালার টাইপ"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = cvPlatePriceStr,
                            onValueChange = { cvPlatePriceStr = it },
                            label = "প্লেট দর",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = cvPrintRate1kStr,
                            onValueChange = { cvPrintRate1kStr = it },
                            label = "প্রিন্টিং রেট/১০০০",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchRowField(
                        title = "কভার ল্যামিনেশন (Sq in Lamination)",
                        checked = cvLamEnabled,
                        onCheckedChange = { cvLamEnabled = it }
                    )
                    if (cvLamEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(
                                value = cvLamRatePerInchStr,
                                onValueChange = { cvLamRatePerInchStr = it },
                                label = "প্রতি স্কয়ার ইঞ্চি দর",
                                suffix = "৳",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputField(
                                value = cvLamMinChargeStr,
                                onValueChange = { cvLamMinChargeStr = it },
                                label = "মিনিমাম ল্যামিনেশন চার্জ",
                                suffix = "৳",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchRowField(
                        title = "এক্সট্রা ফিনিশিং (Spot UV / Foil / Emboss)",
                        checked = includeExtraFinishing,
                        onCheckedChange = { includeExtraFinishing = it }
                    )
                    if (includeExtraFinishing) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(
                                value = cvSpotUVRateStr,
                                onValueChange = { cvSpotUVRateStr = it },
                                label = "স্পট UV দর/sq.in",
                                suffix = "৳",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputField(
                                value = cvSandiRateStr,
                                onValueChange = { cvSandiRateStr = it },
                                label = "স্যান্ডি UV দর/sq.in",
                                suffix = "৳",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(
                                value = cvFoilBlockCostStr,
                                onValueChange = { cvFoilBlockCostStr = it },
                                label = "ফয়েল ব্লক মেকিং",
                                suffix = "৳",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputField(
                                value = cvFoilRatePerPieceStr,
                                onValueChange = { cvFoilRatePerPieceStr = it },
                                label = "ফয়েল স্ট্যাম্পিং/পিস",
                                suffix = "৳",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Binding, Packaging & Others
            SectionHeaderCard(
                title = "৫. বই বাঁধাই, প্যাকেজিং ও প্রফিট মার্জিন",
                subtitle = "বাইন্ডিং টাইপ, প্যাকেজিং চার্জ ও কমিশন"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = bindingChargePerBookStr,
                        onValueChange = { bindingChargePerBookStr = it },
                        label = "বই বাইন্ডিং দর/পিস",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = bindingMinChargeStr,
                        onValueChange = { bindingMinChargeStr = it },
                        label = "বাইন্ডিং মিনিমাম চার্জ",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = itemsPerPacketStr,
                        onValueChange = { itemsPerPacketStr = it },
                        label = "প্যাকেট প্রতি পিস",
                        isInteger = true,
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = costPerPacketStr,
                        onValueChange = { costPerPacketStr = it },
                        label = "প্যাকেট প্রতি খরচ",
                        suffix = "৳/pkt",
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

        // Sticky Cost Summary Header
        CostSummaryCard(
            grandTotal = result.grandTotal,
            perUnitCost = result.perBookCost,
            subTotal = result.subTotal,
            profitAmount = result.profitAmount,
            profitPercent = profitPercentStr.toDoubleOrNull() ?: 0.0,
            itemsQuantity = bookQuantityStr.toIntOrNull() ?: 0,
            breakdownItems = listOf(
                "ইনার পেপার খরচ (${formatMoney(result.totalReams)} রিম)" to result.innerPaperCost,
                "ইনার ফর্মা প্লেট খরচ" to result.totalInnerPlatesCost,
                "ইনার ফর্মা প্রিন্টিং খরচ" to result.totalInnerPrintCost,
                "ইনার ফর্মা ল্যামিনেশন খরচ" to result.totalInnerLamCost,
                "পুস্তানি (Endpaper) কাগজ খরচ" to result.epPaperCost,
                "পুস্তানি প্লেট/প্রিন্টিং/ল্যামিনেশন" to (result.epPlateCost + result.epPrintCost + result.epLamCost),
                "কভার কাগজ খরচ (${formatMoney(result.cvFullSheetQty)} শিট)" to result.cvPaperCost,
                "কভার প্লেট ও প্রিন্টিং খরচ" to (result.cvPlateCost + result.cvPrintCost),
                "কভার ল্যামিনেশন খরচ (Sq in)" to result.cvLamCost,
                "কভার এক্সট্রা ফিনিশিং (Spot UV/Foil/Emboss)" to result.totalExtraFinishing,
                "বই বাঁধাই খরচ (Binding)" to result.totalBindingCost,
                "প্যাকেজিং খরচ" to result.totalPackaging,
                "ডিজাইন চার্জ" to (designChargeStr.toDoubleOrNull() ?: 0.0),
                "শিপিং/ডেলিভারি খরচ" to (shippingEstimateStr.toDoubleOrNull() ?: 0.0)
            ),
            initialExpanded = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
