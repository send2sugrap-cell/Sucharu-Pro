package com.sucharu.sucharupro.ui.features.printing.calculator.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.printingcalculator.ColorTypeOption
import com.sucharu.sucharupro.domain.model.printingcalculator.LaminationType
import com.sucharu.sucharupro.domain.model.printingcalculator.PlateSizeOption
import com.sucharu.sucharupro.domain.service.printingcalculator.CalculationEngine
import com.sucharu.sucharupro.ui.features.printing.calculator.components.*

@Composable
fun OffsetSection(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    // 1. Job & Size Inputs
    var itemWidthStr by remember { mutableStateOf("5.5") }
    var itemHeightStr by remember { mutableStateOf("8.5") }
    var paperWidthStr by remember { mutableStateOf("20.0") }
    var paperHeightStr by remember { mutableStateOf("30.0") }
    var paperRateStr by remember { mutableStateOf("2200.0") }
    var itemQuantityStr by remember { mutableStateOf("1000") }
    var cutsPerMainSheetStr by remember { mutableStateOf("1") }

    // 2. Plate & Printing Inputs
    var selectedPlateSize by remember { mutableStateOf(PlateSizeOption.GTO.displayName) }
    var selectedColorType by remember { mutableStateOf(ColorTypeOption.FOUR_COLOR.displayName) }
    var isJuri by remember { mutableStateOf(false) }
    var isBackToBack by remember { mutableStateOf(false) }
    var plateRateStr by remember { mutableStateOf("250.0") }
    var printingRate1kStr by remember { mutableStateOf("150.0") }
    var printingMinChargeStr by remember { mutableStateOf("300.0") }

    // 3. Lamination Inputs (Square Inch)
    var isLaminationEnabled by remember { mutableStateOf(true) }
    var selectedLamType by remember { mutableStateOf(LaminationType.GLOSSY.displayName) }
    var laminationSides by remember { mutableStateOf("Single") } // "Single" or "Both"
    var laminationRateStr by remember { mutableStateOf("0.002") }
    var laminationMinChargeStr by remember { mutableStateOf("150.0") }

    // 4. Finishing & Die-Cutting Inputs
    var dieBlockCostStr by remember { mutableStateOf("0.0") }
    var dieCuttingRate1kStr by remember { mutableStateOf("0.0") }
    var foilBlockCostStr by remember { mutableStateOf("0.0") }
    var foilRatePerPcStr by remember { mutableStateOf("0.0") }
    var pastingRate1kStr by remember { mutableStateOf("0.0") }
    var pastingMinChargeStr by remember { mutableStateOf("0.0") }
    var numberingRate1kStr by remember { mutableStateOf("0.0") }
    var perforationRate1kStr by remember { mutableStateOf("0.0") }

    // 5. Binding, Packaging & Profit
    var pagesPerBookStr by remember { mutableStateOf("1") }
    var bindingRatePerPcStr by remember { mutableStateOf("0.0") }
    var bindingMinChargeStr by remember { mutableStateOf("0.0") }
    var itemsPerPacketStr by remember { mutableStateOf("100") }
    var costPerPacketStr by remember { mutableStateOf("0.0") }
    var packagingRate1kStr by remember { mutableStateOf("50.0") }
    var packagingMinChargeStr by remember { mutableStateOf("50.0") }
    var designChargeStr by remember { mutableStateOf("300.0") }
    var shippingEstimateStr by remember { mutableStateOf("100.0") }
    var profitPercentStr by remember { mutableStateOf("15.0") }

    // Real-time calculation
    val result = remember(
        itemWidthStr, itemHeightStr, paperWidthStr, paperHeightStr, paperRateStr, itemQuantityStr, cutsPerMainSheetStr,
        selectedColorType, isJuri, isBackToBack, plateRateStr, printingRate1kStr, printingMinChargeStr,
        isLaminationEnabled, laminationSides, laminationRateStr, laminationMinChargeStr,
        dieBlockCostStr, dieCuttingRate1kStr, foilBlockCostStr, foilRatePerPcStr, pastingRate1kStr, pastingMinChargeStr,
        numberingRate1kStr, perforationRate1kStr, pagesPerBookStr, bindingRatePerPcStr, bindingMinChargeStr,
        itemsPerPacketStr, costPerPacketStr, packagingRate1kStr, packagingMinChargeStr, designChargeStr, shippingEstimateStr, profitPercentStr
    ) {
        val itemWidth = itemWidthStr.toDoubleOrNull() ?: 0.0
        val itemHeight = itemHeightStr.toDoubleOrNull() ?: 0.0
        val paperWidth = paperWidthStr.toDoubleOrNull() ?: 0.0
        val paperHeight = paperHeightStr.toDoubleOrNull() ?: 0.0
        val paperRate = paperRateStr.toDoubleOrNull() ?: 0.0
        val itemQuantity = itemQuantityStr.toIntOrNull() ?: 0
        val cuts = cutsPerMainSheetStr.toIntOrNull() ?: 1

        val plateRate = plateRateStr.toDoubleOrNull() ?: 0.0
        val printRate1k = printingRate1kStr.toDoubleOrNull() ?: 0.0
        val printMin = printingMinChargeStr.toDoubleOrNull() ?: 0.0

        val lamRate = laminationRateStr.toDoubleOrNull() ?: 0.0
        val lamMin = laminationMinChargeStr.toDoubleOrNull() ?: 0.0

        val dieBlock = dieBlockCostStr.toDoubleOrNull() ?: 0.0
        val dieRate1k = dieCuttingRate1kStr.toDoubleOrNull() ?: 0.0
        val foilBlock = foilBlockCostStr.toDoubleOrNull() ?: 0.0
        val foilRate = foilRatePerPcStr.toDoubleOrNull() ?: 0.0
        val pastingRate1k = pastingRate1kStr.toDoubleOrNull() ?: 0.0
        val pastingMin = pastingMinChargeStr.toDoubleOrNull() ?: 0.0
        val numberingRate1k = numberingRate1kStr.toDoubleOrNull() ?: 0.0
        val perforationRate1k = perforationRate1kStr.toDoubleOrNull() ?: 0.0

        val pagesPerBook = pagesPerBookStr.toIntOrNull() ?: 1
        val bindingRate = bindingRatePerPcStr.toDoubleOrNull() ?: 0.0
        val bindingMin = bindingMinChargeStr.toDoubleOrNull() ?: 0.0
        val itemsPerPacket = itemsPerPacketStr.toIntOrNull() ?: 100
        val costPerPacket = costPerPacketStr.toDoubleOrNull() ?: 0.0
        val packRate1k = packagingRate1kStr.toDoubleOrNull() ?: 0.0
        val packMin = packagingMinChargeStr.toDoubleOrNull() ?: 0.0
        val design = designChargeStr.toDoubleOrNull() ?: 0.0
        val shipping = shippingEstimateStr.toDoubleOrNull() ?: 0.0
        val profit = profitPercentStr.toDoubleOrNull() ?: 0.0

        CalculationEngine.calculateOffsetCost(
            itemWidth = itemWidth,
            itemHeight = itemHeight,
            paperWidth = paperWidth,
            paperHeight = paperHeight,
            paperRatePerReam = paperRate,
            itemQuantity = itemQuantity,
            cutsPerMainSheet = cuts,
            colorType = selectedColorType,
            isJuri = isJuri,
            isBackToBack = isBackToBack,
            ratePerPlate = plateRate,
            printingRate1k = printRate1k,
            printingMinCharge = printMin,
            isLaminationEnabled = isLaminationEnabled,
            laminationSides = laminationSides,
            laminationRatePerSqIn = lamRate,
            laminationMinCharge = lamMin,
            dieBlockCost = dieBlock,
            dieCuttingRate1k = dieRate1k,
            foilBlockCost = foilBlock,
            foilRatePerPc = foilRate,
            pastingRate1k = pastingRate1k,
            pastingMinCharge = pastingMin,
            numberingRate1k = numberingRate1k,
            perforationRate1k = perforationRate1k,
            pagesPerBook = pagesPerBook,
            bindingRatePerPc = bindingRate,
            bindingMinCharge = bindingMin,
            itemsPerPacket = itemsPerPacket,
            costPerPacket = costPerPacket,
            packagingRate1k = packRate1k,
            packagingMinCharge = packMin,
            designCharge = design,
            shippingEstimate = shipping,
            profitPercent = profit
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Scrollable Input List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 12.dp, end = 12.dp, top = 110.dp, bottom = 24.dp)
        ) {
            // 1. Job & Paper Dimensions
            SectionHeaderCard(
                title = "১. জব ও কাগজের সাইজ (Job & Paper Specs)",
                subtitle = "আইটেম ও কাগজের পরিমাপ দিন"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = itemWidthStr,
                        onValueChange = { itemWidthStr = it },
                        label = "আইটেমের চওড়া (Width)",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = itemHeightStr,
                        onValueChange = { itemHeightStr = it },
                        label = "আইটেমের উচ্চতা (Height)",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = paperWidthStr,
                        onValueChange = { paperWidthStr = it },
                        label = "কাগজের চওড়া (Paper W)",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = paperHeightStr,
                        onValueChange = { paperHeightStr = it },
                        label = "কাগজের উচ্চতা (Paper H)",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = paperRateStr,
                        onValueChange = { paperRateStr = it },
                        label = "কাগজের রিম দর (Paper Rate)",
                        suffix = "৳/ream",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = itemQuantityStr,
                        onValueChange = { itemQuantityStr = it },
                        label = "আইটেম সংখ্যা (Quantity)",
                        suffix = "pcs",
                        isInteger = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField(
                    value = cutsPerMainSheetStr,
                    onValueChange = { cutsPerMainSheetStr = it },
                    label = "মেইন কাগজ থেকে মেশিন সাইজ কাট (Cuts per Sheet)",
                    isInteger = true
                )
            }

            // 2. Plate & Printing Specs
            SectionHeaderCard(
                title = "২. প্লেট ও প্রিন্টিং (Plate & Impression)",
                subtitle = "প্লেট সাইজ, কালার টাইপ ও প্রিন্টিং রেট"
            ) {
                DropdownMenuField(
                    selectedOption = selectedPlateSize,
                    options = PlateSizeOption.entries.map { it.displayName },
                    onOptionSelected = { selectedPlateSize = it },
                    label = "প্লেট সাইজ (Plate Size)"
                )
                Spacer(modifier = Modifier.height(8.dp))
                DropdownMenuField(
                    selectedOption = selectedColorType,
                    options = ColorTypeOption.entries.map { it.displayName },
                    onOptionSelected = { selectedColorType = it },
                    label = "কালার টাইপ (Color Type)"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwitchRowField(
                        title = "জুরি (Work & Turn / Juri)",
                        checked = isJuri,
                        onCheckedChange = {
                            isJuri = it
                            if (it) isBackToBack = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SwitchRowField(
                        title = "এপিঠ-ওপিঠ (Back to Back)",
                        checked = isBackToBack,
                        onCheckedChange = {
                            isBackToBack = it
                            if (it) isJuri = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = plateRateStr,
                        onValueChange = { plateRateStr = it },
                        label = "প্লেট দর (Plate Rate)",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = printingRate1kStr,
                        onValueChange = { printingRate1kStr = it },
                        label = "প্রতি ১০০০ ইমপ্রেশন দর",
                        suffix = "৳/1k",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField(
                    value = printingMinChargeStr,
                    onValueChange = { printingMinChargeStr = it },
                    label = "প্রিন্টিং মিনিমাম চার্জ (Min Print Charge)",
                    suffix = "৳"
                )
            }

            // 3. Lamination Specs
            SectionHeaderCard(
                title = "৩. ল্যামিনেশন (Lamination - Square Inch)",
                subtitle = "স্কয়ার ইঞ্চি ভিত্তিক ল্যামিনেশন হিসাব"
            ) {
                SwitchRowField(
                    title = "ল্যামিনেশন যোগ করুন (Enable Lamination)",
                    checked = isLaminationEnabled,
                    onCheckedChange = { isLaminationEnabled = it }
                )
                if (isLaminationEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DropdownMenuField(
                        selectedOption = selectedLamType,
                        options = LaminationType.entries.map { it.displayName },
                        onOptionSelected = { selectedLamType = it },
                        label = "ল্যামিনেশন টাইপ"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DropdownMenuField(
                        selectedOption = if (laminationSides == "Both") "উভয় পাশ (Both Sides)" else "এক পাশ (Single Side)",
                        options = listOf("এক পাশ (Single Side)", "উভয় পাশ (Both Sides)"),
                        onOptionSelected = { laminationSides = if (it.contains("Both") || it.contains("উভয়")) "Both" else "Single" },
                        label = "ল্যামিনেশন পাশ (Sides)"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = laminationRateStr,
                            onValueChange = { laminationRateStr = it },
                            label = "প্রতি স্কয়ার ইঞ্চি দর (Rate/sq.in)",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = laminationMinChargeStr,
                            onValueChange = { laminationMinChargeStr = it },
                            label = "মিনিমাম ল্যামিনেশন চার্জ",
                            suffix = "৳",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Finishing & Die-Cutting
            SectionHeaderCard(
                title = "৪. ফিনিশিং ও প্রসেসিং (Finishing & Processing)",
                subtitle = "ডাই কাটিং, ফয়েল স্ট্যাম্পিং, পেস্টিং, নাম্বারিং"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = dieBlockCostStr,
                        onValueChange = { dieBlockCostStr = it },
                        label = "ডাই ব্লক চার্জ (Fixed)",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = dieCuttingRate1kStr,
                        onValueChange = { dieCuttingRate1kStr = it },
                        label = "ডাই কাটিং রানিং রেট",
                        suffix = "৳/1k",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = foilBlockCostStr,
                        onValueChange = { foilBlockCostStr = it },
                        label = "ফয়েল ব্লক মেকিং খরচ",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = foilRatePerPcStr,
                        onValueChange = { foilRatePerPcStr = it },
                        label = "ফয়েল স্ট্যাম্পিং দর/পিস",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = pastingRate1kStr,
                        onValueChange = { pastingRate1kStr = it },
                        label = "পেস্টিং রেট (প্রতি ১০০০ শিট)",
                        suffix = "৳/1k",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = pastingMinChargeStr,
                        onValueChange = { pastingMinChargeStr = it },
                        label = "পেস্টিং মিনিমাম চার্জ",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = numberingRate1kStr,
                        onValueChange = { numberingRate1kStr = it },
                        label = "নাম্বারিং রেট/১০০০",
                        suffix = "৳/1k",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = perforationRate1kStr,
                        onValueChange = { perforationRate1kStr = it },
                        label = "পারফোরেশন রেট/১০০০",
                        suffix = "৳/1k",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Binding, Packaging & Others
            SectionHeaderCard(
                title = "৫. বাইন্ডিং, প্যাকেজিং ও লাভ মার্জিন",
                subtitle = "প্যাকেজিং, ডিজাইন চার্জ, ডেলিভারি ও প্রফিট"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = pagesPerBookStr,
                        onValueChange = { pagesPerBookStr = it },
                        label = "বই/প্যাডের পাতা সংখ্যা",
                        isInteger = true,
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = bindingRatePerPcStr,
                        onValueChange = { bindingRatePerPcStr = it },
                        label = "বাইন্ডিং দর (প্রতি পিস)",
                        suffix = "৳/pc",
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
            perUnitCost = result.perUnitCost,
            subTotal = result.subTotal,
            profitAmount = result.profitAmount,
            profitPercent = profitPercentStr.toDoubleOrNull() ?: 0.0,
            itemsQuantity = itemQuantityStr.toIntOrNull() ?: 0,
            breakdownItems = listOf(
                "কাগজ খরচ (${result.mainSheets} মেইন শিট / ${result.machineSheets} কাট শিট)" to result.paperCost,
                "প্লেট খরচ (${result.totalPlates} প্লেট)" to result.plateCost,
                "প্রিন্টিং / ইমপ্রেশন খরচ (${result.totalImpressions} ইমপ্রেশন)" to result.impressionCost,
                "ল্যামিনেশন খরচ (স্কয়ার ইঞ্চি ভিত্তিক)" to result.laminationCost,
                "ফিনিশিং ও ডাই কাটিং (ডাই/ফয়েল/পেস্টিং)" to result.totalFinishing,
                "বাইন্ডিং খরচ" to result.totalBindingCost,
                "প্যাকেজিং খরচ" to result.packagingCost,
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
