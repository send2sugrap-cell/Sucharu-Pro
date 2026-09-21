package com.sucharu.sucharupro.ui.features.printing.calculator.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.printingcalculator.ColorTypeOption
import com.sucharu.sucharupro.domain.model.printingcalculator.WeddingCardArchitecture
import com.sucharu.sucharupro.domain.service.printingcalculator.CalculationEngine
import com.sucharu.sucharupro.ui.features.printing.calculator.components.*

@Composable
fun WeddingSection(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    // 1. Card Architecture & Dimensions
    var selectedArchitecture by remember { mutableStateOf(WeddingCardArchitecture.BI_FOLD.displayName) }
    var quantityStr by remember { mutableStateOf("300") }

    var cardWidthStr by remember { mutableStateOf("7.0") }
    var cardHeightStr by remember { mutableStateOf("10.0") }
    var paperWidthStr by remember { mutableStateOf("22.0") }
    var paperHeightStr by remember { mutableStateOf("28.0") }
    var paperRateStr by remember { mutableStateOf("3500.0") } // Premium Board/Paper

    // Inserts
    var insertCardCountStr by remember { mutableStateOf("2") }
    var insertWidthStr by remember { mutableStateOf("6.5") }
    var insertHeightStr by remember { mutableStateOf("9.5") }

    // Envelope
    var envelopeFlatWStr by remember { mutableStateOf("15.0") }
    var envelopeFlatHStr by remember { mutableStateOf("11.0") }
    var envPaperRateStr by remember { mutableStateOf("2500.0") }
    var envMakingLabourPerPcStr by remember { mutableStateOf("3.5") }

    // Lamination (Sq In)
    var isLamEnabled by remember { mutableStateOf(true) }
    var lamBothSides by remember { mutableStateOf(false) }
    var lamRateSqInStr by remember { mutableStateOf("0.0025") }
    var lamMinStr by remember { mutableStateOf("150.0") }

    // Premium Finishing
    var foilBlockCostStr by remember { mutableStateOf("500.0") }
    var foilPerPcRateStr by remember { mutableStateOf("3.0") }
    var embossBlockCostStr by remember { mutableStateOf("400.0") }
    var embossPerPcRateStr by remember { mutableStateOf("2.0") }
    var dieMakingChargeStr by remember { mutableStateOf("600.0") }
    var diePunchPerPcRateStr by remember { mutableStateOf("2.5") }
    var assemblyLabourPerSetStr by remember { mutableStateOf("5.0") }

    // Plate & Printing
    var selectedColorType by remember { mutableStateOf(ColorTypeOption.TWO_COLOR.displayName) }
    var isJuri by remember { mutableStateOf(false) }
    var plateRateStr by remember { mutableStateOf("250.0") }
    var printRate1kStr by remember { mutableStateOf("200.0") }
    var printMinStr by remember { mutableStateOf("300.0") }

    // Design & Profit
    var designChargeStr by remember { mutableStateOf("500.0") }
    var shippingEstimateStr by remember { mutableStateOf("150.0") }
    var profitPercentStr by remember { mutableStateOf("20.0") }

    val result = remember(
        selectedArchitecture, quantityStr, cardWidthStr, cardHeightStr, paperWidthStr, paperHeightStr, paperRateStr,
        insertCardCountStr, insertWidthStr, insertHeightStr, envelopeFlatWStr, envelopeFlatHStr, envPaperRateStr, envMakingLabourPerPcStr,
        isLamEnabled, lamBothSides, lamRateSqInStr, lamMinStr, foilBlockCostStr, foilPerPcRateStr, embossBlockCostStr, embossPerPcRateStr,
        dieMakingChargeStr, diePunchPerPcRateStr, assemblyLabourPerSetStr, selectedColorType, isJuri, plateRateStr, printRate1kStr, printMinStr,
        designChargeStr, shippingEstimateStr, profitPercentStr
    ) {
        val qty = quantityStr.toIntOrNull() ?: 0
        val cW = cardWidthStr.toDoubleOrNull() ?: 0.0
        val cH = cardHeightStr.toDoubleOrNull() ?: 0.0
        val pW = paperWidthStr.toDoubleOrNull() ?: 0.0
        val pH = paperHeightStr.toDoubleOrNull() ?: 0.0
        val pRate = paperRateStr.toDoubleOrNull() ?: 0.0

        val insCount = insertCardCountStr.toIntOrNull() ?: 0
        val insW = insertWidthStr.toDoubleOrNull() ?: 0.0
        val insH = insertHeightStr.toDoubleOrNull() ?: 0.0

        val envW = envelopeFlatWStr.toDoubleOrNull() ?: 0.0
        val envH = envelopeFlatHStr.toDoubleOrNull() ?: 0.0
        val envRate = envPaperRateStr.toDoubleOrNull() ?: 0.0
        val envLabour = envMakingLabourPerPcStr.toDoubleOrNull() ?: 0.0

        val lamRate = lamRateSqInStr.toDoubleOrNull() ?: 0.0
        val lamMin = lamMinStr.toDoubleOrNull() ?: 0.0

        val foilBlock = foilBlockCostStr.toDoubleOrNull() ?: 0.0
        val foilRate = foilPerPcRateStr.toDoubleOrNull() ?: 0.0
        val embossBlock = embossBlockCostStr.toDoubleOrNull() ?: 0.0
        val embossRate = embossPerPcRateStr.toDoubleOrNull() ?: 0.0
        val dieBlock = dieMakingChargeStr.toDoubleOrNull() ?: 0.0
        val diePunch = diePunchPerPcRateStr.toDoubleOrNull() ?: 0.0
        val assembly = assemblyLabourPerSetStr.toDoubleOrNull() ?: 0.0

        val plateRate = plateRateStr.toDoubleOrNull() ?: 0.0
        val printRate = printRate1kStr.toDoubleOrNull() ?: 0.0
        val printMin = printMinStr.toDoubleOrNull() ?: 0.0

        val design = designChargeStr.toDoubleOrNull() ?: 0.0
        val shipping = shippingEstimateStr.toDoubleOrNull() ?: 0.0
        val profit = profitPercentStr.toDoubleOrNull() ?: 0.0

        CalculationEngine.calculateWeddingCardCost(
            quantity = qty,
            cardW = cW,
            cardH = cH,
            paperW = pW,
            paperH = pH,
            paperReamRate = pRate,
            insertCardCount = insCount,
            insertW = insW,
            insertH = insH,
            envelopeFlatW = envW,
            envelopeFlatH = envH,
            envPaperRate = envRate,
            envMakingLabourPerPc = envLabour,
            isLamEnabled = isLamEnabled,
            lamBothSides = lamBothSides,
            lamRateSqIn = lamRate,
            lamMin = lamMin,
            foilBlockCost = foilBlock,
            foilPerPcRate = foilRate,
            embossBlockCost = embossBlock,
            embossPerPcRate = embossRate,
            dieMakingCharge = dieBlock,
            diePunchPerPcRate = diePunch,
            assemblyLabourPerSet = assembly,
            colorType = selectedColorType,
            isJuri = isJuri,
            plateRate = plateRate,
            printRate1k = printRate,
            printMin = printMin,
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
            // 1. Architecture & Quantity
            SectionHeaderCard(
                title = "১. কার্ড আর্কিটেকচার ও পরিমাণ",
                subtitle = "ইনভিটেশন কার্ডের ধরণ, সাইজ ও পরিমাণ"
            ) {
                DropdownMenuField(
                    selectedOption = selectedArchitecture,
                    options = WeddingCardArchitecture.entries.map { it.displayName },
                    onOptionSelected = { selectedArchitecture = it },
                    label = "কার্ডের স্টাইল / টাইপ"
                )
                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = "কার্ডের পরিমাণ (Quantity)",
                    suffix = "sets",
                    isInteger = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = cardWidthStr,
                        onValueChange = { cardWidthStr = it },
                        label = "মূল কার্ড W",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = cardHeightStr,
                        onValueChange = { cardHeightStr = it },
                        label = "মূল কার্ড H",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = paperWidthStr,
                        onValueChange = { paperWidthStr = it },
                        label = "বোর্ড/কাগজ W",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = paperHeightStr,
                        onValueChange = { paperHeightStr = it },
                        label = "বোর্ড/কাগজ H",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField(
                    value = paperRateStr,
                    onValueChange = { paperRateStr = it },
                    label = "কার্ড পেপার/বোর্ড রিম দর",
                    suffix = "৳/ream"
                )
            }

            // 2. Inserts & Envelope
            SectionHeaderCard(
                title = "২. ইনসার্ট পাতা ও খাম (Inserts & Envelope)",
                subtitle = "ভেতরের ইনসার্ট পাতা ও খামের পরিমাপ"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = insertCardCountStr,
                        onValueChange = { insertCardCountStr = it },
                        label = "ইনসার্ট সংখ্যা",
                        isInteger = true,
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = insertWidthStr,
                        onValueChange = { insertWidthStr = it },
                        label = "ইনসার্ট W",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = insertHeightStr,
                        onValueChange = { insertHeightStr = it },
                        label = "ইনসার্ট H",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = envelopeFlatWStr,
                        onValueChange = { envelopeFlatWStr = it },
                        label = "খাম ফ্ল্যাট W",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = envelopeFlatHStr,
                        onValueChange = { envelopeFlatHStr = it },
                        label = "খাম ফ্ল্যাট H",
                        suffix = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = envPaperRateStr,
                        onValueChange = { envPaperRateStr = it },
                        label = "খামের পেপার রিম দর",
                        suffix = "৳/ream",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = envMakingLabourPerPcStr,
                        onValueChange = { envMakingLabourPerPcStr = it },
                        label = "খাম মেকিং লেবার/পিস",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Premium Finishing (Foil, Emboss, Die-Cut)
            SectionHeaderCard(
                title = "৩. প্রিমিয়াম ফিনিশিং (Foil, Emboss & Die-Cut)",
                subtitle = "এককালীন মেটাল ব্লক মেকিং ও রানিং প্রসেসিং চার্জ"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = foilBlockCostStr,
                        onValueChange = { foilBlockCostStr = it },
                        label = "ফয়েল ব্লক তৈরি (Fixed)",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = foilPerPcRateStr,
                        onValueChange = { foilPerPcRateStr = it },
                        label = "ফয়েল স্ট্যাম্পিং/পিস",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = embossBlockCostStr,
                        onValueChange = { embossBlockCostStr = it },
                        label = "এম্বস ব্লক তৈরি (Fixed)",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = embossPerPcRateStr,
                        onValueChange = { embossPerPcRateStr = it },
                        label = "এম্বসিং প্রেস/পিস",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = dieMakingChargeStr,
                        onValueChange = { dieMakingChargeStr = it },
                        label = "কাস্টম ডাই মেকিং (Fixed)",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = diePunchPerPcRateStr,
                        onValueChange = { diePunchPerPcRateStr = it },
                        label = "পাঞ্চিং চার্জ/পিস",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField(
                    value = assemblyLabourPerSetStr,
                    onValueChange = { assemblyLabourPerSetStr = it },
                    label = "অ্যাসেম্বলি ও ফিতা/ট্যাসেল ফিটিং লেবার/সেট",
                    suffix = "৳/set"
                )
            }

            // 4. Plate, Print & Design
            SectionHeaderCard(
                title = "৪. প্লেট, প্রিন্টিং ও ডিজাইন",
                subtitle = "প্রিন্ট কালার ও প্রফিট মার্জিন"
            ) {
                DropdownMenuField(
                    selectedOption = selectedColorType,
                    options = ColorTypeOption.entries.map { it.displayName },
                    onOptionSelected = { selectedColorType = it },
                    label = "কালার টাইপ"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = plateRateStr,
                        onValueChange = { plateRateStr = it },
                        label = "প্লেট দর",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = printRate1kStr,
                        onValueChange = { printRate1kStr = it },
                        label = "প্রিন্ট রেট/১০০০",
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
                        value = profitPercentStr,
                        onValueChange = { profitPercentStr = it },
                        label = "লাভের শতাংশ",
                        suffix = "%",
                        modifier = Modifier.weight(1f)
                    )
                }
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
            itemsQuantity = quantityStr.toIntOrNull() ?: 0,
            breakdownItems = listOf(
                "মূল কার্ড ও ইনসার্ট কাগজ খরচ (${formatMoney(result.mainCardSheets + result.insertSheetsTotal)} শিট)" to result.cardPaperCost,
                "খাম (Envelope) পেপার ও তৈরি লেবার" to result.envelopeCost,
                "ল্যামিনেশন খরচ (স্কয়ার ইঞ্চি)" to result.laminationCost,
                "প্লেট ও প্রিন্টিং খরচ" to result.platesAndPrintCost,
                "ফয়েল, এম্বস, ডাই-কাট ব্লক ও প্রসেসিং" to result.totalFinishingAndLabor,
                "ডিজাইন ফি" to (designChargeStr.toDoubleOrNull() ?: 0.0),
                "শিপিং/ডেলিভারি খরচ" to (shippingEstimateStr.toDoubleOrNull() ?: 0.0)
            ),
            initialExpanded = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
