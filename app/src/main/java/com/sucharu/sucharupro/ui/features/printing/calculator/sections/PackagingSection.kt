package com.sucharu.sucharupro.ui.features.printing.calculator.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.printingcalculator.BoxCategory
import com.sucharu.sucharupro.domain.model.printingcalculator.ColorTypeOption
import com.sucharu.sucharupro.domain.service.printingcalculator.CalculationEngine
import com.sucharu.sucharupro.ui.features.printing.calculator.components.*

@Composable
fun PackagingSection(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    var selectedBoxCategory by remember { mutableStateOf(BoxCategory.FOLDING_CARTON.displayName) }

    // Dieline dimensions
    var lengthStr by remember { mutableStateOf("6.0") } // L in inches
    var widthStr by remember { mutableStateOf("4.0") }  // W in inches
    var heightStr by remember { mutableStateOf("2.0") } // H in inches
    var glueFlapAllowanceStr by remember { mutableStateOf("0.75") }
    var topFlapAllowanceStr by remember { mutableStateOf("1.5") }

    var boxQuantityStr by remember { mutableStateOf("2000") }
    var wastagePercentStr by remember { mutableStateOf("5.0") }

    // Paper & Print Specs
    var paperWidthStr by remember { mutableStateOf("22.0") }
    var paperHeightStr by remember { mutableStateOf("28.0") }
    var paperRateStr by remember { mutableStateOf("3200.0") } // Duplex board / Art card
    var cutsPerSheetStr by remember { mutableStateOf("1") }

    var selectedColorType by remember { mutableStateOf(ColorTypeOption.FOUR_COLOR.displayName) }
    var isJuri by remember { mutableStateOf(false) }
    var isBackToBack by remember { mutableStateOf(false) }
    var plateRateStr by remember { mutableStateOf("250.0") }
    var printRate1kStr by remember { mutableStateOf("200.0") }
    var printMinStr by remember { mutableStateOf("350.0") }

    // Lamination (Sq In)
    var isLamEnabled by remember { mutableStateOf(true) }
    var lamRateSqInStr by remember { mutableStateOf("0.002") }
    var lamMinStr by remember { mutableStateOf("150.0") }

    // Dieline & Die-Cutting
    var isExistingDie by remember { mutableStateOf(false) } // If existing die = true, dieBlockCost = 0
    var dieBlockCostStr by remember { mutableStateOf("1200.0") } // Fixed setup cost
    var dieCuttingRate1kStr by remember { mutableStateOf("250.0") }

    // Window Patching & Pasting
    var hasWindow by remember { mutableStateOf(false) }
    var windowFilmRateStr by remember { mutableStateOf("1.5") }
    var windowPastingLabourStr by remember { mutableStateOf("1.0") }

    var sidePastingLabourPerPcStr by remember { mutableStateOf("0.80") }

    var isShoppingBag by remember { mutableStateOf(false) }
    var handleRatePerPcStr by remember { mutableStateOf("3.0") }

    var designChargeStr by remember { mutableStateOf("500.0") }
    var shippingEstimateStr by remember { mutableStateOf("250.0") }
    var profitPercentStr by remember { mutableStateOf("18.0") }

    val result = remember(
        selectedBoxCategory, lengthStr, widthStr, heightStr, glueFlapAllowanceStr, topFlapAllowanceStr, boxQuantityStr, wastagePercentStr,
        paperWidthStr, paperHeightStr, paperRateStr, cutsPerSheetStr, selectedColorType, isJuri, isBackToBack, plateRateStr, printRate1kStr, printMinStr,
        isLamEnabled, lamRateSqInStr, lamMinStr, isExistingDie, dieBlockCostStr, dieCuttingRate1kStr,
        hasWindow, windowFilmRateStr, windowPastingLabourStr, sidePastingLabourPerPcStr, isShoppingBag, handleRatePerPcStr,
        designChargeStr, shippingEstimateStr, profitPercentStr
    ) {
        val L = lengthStr.toDoubleOrNull() ?: 0.0
        val W = widthStr.toDoubleOrNull() ?: 0.0
        val H = heightStr.toDoubleOrNull() ?: 0.0
        val glueFlap = glueFlapAllowanceStr.toDoubleOrNull() ?: 0.75
        val topFlap = topFlapAllowanceStr.toDoubleOrNull() ?: 1.5

        val qty = boxQuantityStr.toIntOrNull() ?: 0
        val wastage = wastagePercentStr.toDoubleOrNull() ?: 0.0

        val pW = paperWidthStr.toDoubleOrNull() ?: 0.0
        val pH = paperHeightStr.toDoubleOrNull() ?: 0.0
        val pRate = paperRateStr.toDoubleOrNull() ?: 0.0
        val cuts = cutsPerSheetStr.toIntOrNull() ?: 1

        val plateRate = plateRateStr.toDoubleOrNull() ?: 0.0
        val printRate = printRate1kStr.toDoubleOrNull() ?: 0.0
        val printMin = printMinStr.toDoubleOrNull() ?: 0.0

        val lamRate = lamRateSqInStr.toDoubleOrNull() ?: 0.0
        val lamMin = lamMinStr.toDoubleOrNull() ?: 0.0

        val dieBlock = dieBlockCostStr.toDoubleOrNull() ?: 0.0
        val dieRun = dieCuttingRate1kStr.toDoubleOrNull() ?: 0.0

        val winFilm = windowFilmRateStr.toDoubleOrNull() ?: 0.0
        val winLabour = windowPastingLabourStr.toDoubleOrNull() ?: 0.0
        val sideLabour = sidePastingLabourPerPcStr.toDoubleOrNull() ?: 0.0
        val handleRate = handleRatePerPcStr.toDoubleOrNull() ?: 0.0

        val design = designChargeStr.toDoubleOrNull() ?: 0.0
        val shipping = shippingEstimateStr.toDoubleOrNull() ?: 0.0
        val profit = profitPercentStr.toDoubleOrNull() ?: 0.0

        CalculationEngine.calculatePackagingCost(
            length = L,
            width = W,
            height = H,
            glueFlapAllowance = glueFlap,
            topFlapAllowance = topFlap,
            boxQuantity = qty,
            wastagePercent = wastage,
            paperW = pW,
            paperH = pH,
            paperReamRate = pRate,
            cutsPerSheet = cuts,
            colorType = selectedColorType,
            isJuri = isJuri,
            isBackToBack = isBackToBack,
            plateRate = plateRate,
            printRate1k = printRate,
            printMin = printMin,
            isLamEnabled = isLamEnabled,
            lamRateSqIn = lamRate,
            lamMin = lamMin,
            isExistingDie = isExistingDie,
            dieBlockCost = dieBlock,
            dieCuttingRate1k = dieRun,
            hasWindow = hasWindow,
            windowFilmRate = winFilm,
            windowPastingLabour = winLabour,
            sidePastingLabourPerPc = sideLabour,
            isShoppingBag = isShoppingBag,
            handleRatePerPc = handleRate,
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
            perUnitCost = result.perUnitCost,
            subTotal = result.subTotal,
            profitAmount = result.profitAmount,
            profitPercent = profitPercentStr.toDoubleOrNull() ?: 0.0,
            itemsQuantity = boxQuantityStr.toIntOrNull() ?: 0,
            breakdownItems = listOf(
                "কার্টন বোর্ড/মেটেরিয়াল খরচ (${formatMoney(result.totalMachineSheets)} শিট)" to result.boxMaterialCost,
                "প্লেট ও অফসেট প্রিন্টিং খরচ" to (result.plateCost + result.printCost),
                "ল্যামিনেশন খরচ (স্কয়ার ইঞ্চি)" to result.laminationCost,
                "ডাই ব্লক তৈরি খরচ (Fixed)" to result.dieBlockFinalCost,
                "ডাই কাটিং রানিং প্রসেসিং" to result.dieRunCost,
                "উইন্ডো ফিল্ম ও পেস্টিং" to result.windowCost,
                "সাইড পেস্টিং লেবার" to result.pastingCost,
                "শপিং ব্যাগ হ্যান্ডেল রসি" to result.handleCost,
                "ডিজাইন চার্জ" to (designChargeStr.toDoubleOrNull() ?: 0.0),
                "শিপিং/ডেলিভারি খরচ" to (shippingEstimateStr.toDoubleOrNull() ?: 0.0)
            ),
            initialExpanded = false
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 1. Box Dieline Dimensions
        SectionHeaderCard(
            title = "১. কার্টন ডাইলাইন ও বক্সের পরিমাপ",
            subtitle = "বক্সের দৈর্ঘ্য (L), প্রস্থ (W), উচ্চতা (H) থেকে ফ্ল্যাট শিট নির্ণয়"
        ) {
            DropdownMenuField(
                selectedOption = selectedBoxCategory,
                options = BoxCategory.entries.map { it.displayName },
                onOptionSelected = { selectedBoxCategory = it },
                label = "কার্টন ক্যাটাগরি"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = lengthStr,
                    onValueChange = { lengthStr = it },
                    label = "দৈর্ঘ্য (L)",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = widthStr,
                    onValueChange = { widthStr = it },
                    label = "প্রস্থ (W)",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = heightStr,
                    onValueChange = { heightStr = it },
                    label = "উচ্চতা (H)",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "হিসাবীকৃত ফ্ল্যাট শিট সাইজ: ${formatMoney(result.flatSheetW)}\" x ${formatMoney(result.flatSheetH)}\" | ১ শিটে হয়: ${result.itemsPerMachineSheet} টি বক্স",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = boxQuantityStr,
                    onValueChange = { boxQuantityStr = it },
                    label = "বক্সের পরিমাণ (Qty)",
                    suffix = "boxes",
                    isInteger = true,
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = wastagePercentStr,
                    onValueChange = { wastagePercentStr = it },
                    label = "ওয়েস্টেজ %",
                    suffix = "%",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Paper, Board & Printing
        SectionHeaderCard(
            title = "২. পেপার/বোর্ড ও প্রিন্টিং স্পেক্স",
            subtitle = "আর্ট কার্ড/ডুপ্লেক্স বোর্ড, রিম দর ও অফসেট কালার"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = paperWidthStr,
                    onValueChange = { paperWidthStr = it },
                    label = "মেইন কাগজ W",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = paperHeightStr,
                    onValueChange = { paperHeightStr = it },
                    label = "মেইন কাগজ H",
                    suffix = "in",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = paperRateStr,
                    onValueChange = { paperRateStr = it },
                    label = "বোর্ড রিম দর",
                    suffix = "৳/ream",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = cutsPerSheetStr,
                    onValueChange = { cutsPerSheetStr = it },
                    label = "মেশিন কাট/শিট",
                    isInteger = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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
        }

        // 3. Lamination & Die-Cutting
        SectionHeaderCard(
            title = "৩. ল্যামিনেশন, ডাই ব্লক ও প্রসেসিং",
            subtitle = "ডাই তৈরি (এককালীন) বনাম বিদ্যমান ডাই ব্যবহার"
        ) {
            SwitchRowField(
                title = "স্কয়ার ইঞ্চি ল্যামিনেশন",
                checked = isLamEnabled,
                onCheckedChange = { isLamEnabled = it }
            )
            if (isLamEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                NumberInputField(
                    value = lamRateSqInStr,
                    onValueChange = { lamRateSqInStr = it },
                    label = "প্রতি স্কয়ার ইঞ্চি দর",
                    suffix = "৳"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRowField(
                title = "ডাই আগে থেকেই তৈরি আছে (Existing Die)",
                checked = isExistingDie,
                onCheckedChange = { isExistingDie = it },
                subtitle = if (isExistingDie) "নতুন ডাই ব্লক মেকিং চার্জ যুক্ত হবে না" else "নতুন ডাই তৈরির খরচ সাবটোটালে যোগ হবে"
            )
            if (!isExistingDie) {
                Spacer(modifier = Modifier.height(6.dp))
                NumberInputField(
                    value = dieBlockCostStr,
                    onValueChange = { dieBlockCostStr = it },
                    label = "নতুন ডাই ব্লক তৈরির খরচ (Fixed)",
                    suffix = "৳"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(
                value = dieCuttingRate1kStr,
                onValueChange = { dieCuttingRate1kStr = it },
                label = "ডাই কাটিং রানিং রেট (প্রতি ১০০০ শিট)",
                suffix = "৳/1k"
            )
        }

        // 4. Window, Pasting & Handles
        SectionHeaderCard(
            title = "৪. উইন্ডো প্যাচিং, পেস্টিং ও অন্যান্য",
            subtitle = "স্বচ্ছ জানালা, সাইড পেস্টিং ও শপিং ব্যাগ রসি"
        ) {
            SwitchRowField(
                title = "স্বচ্ছ পিভিসি জানালা (Window Patching)",
                checked = hasWindow,
                onCheckedChange = { hasWindow = it }
            )
            if (hasWindow) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = windowFilmRateStr,
                        onValueChange = { windowFilmRateStr = it },
                        label = "প্লাস্টিক ফিল্ম দর",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = windowPastingLabourStr,
                        onValueChange = { windowPastingLabourStr = it },
                        label = "প্যাচিং লেবার",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(
                value = sidePastingLabourPerPcStr,
                onValueChange = { sidePastingLabourPerPcStr = it },
                label = "সাইড পেস্টিং ও ফোল্ডিং লেবার/পিস",
                suffix = "৳/pc"
            )
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRowField(
                title = "শপিং ব্যাগ হলে দড়ি/সাটিন রসি (Handles)",
                checked = isShoppingBag,
                onCheckedChange = { isShoppingBag = it }
            )
            if (isShoppingBag) {
                Spacer(modifier = Modifier.height(6.dp))
                NumberInputField(
                    value = handleRatePerPcStr,
                    onValueChange = { handleRatePerPcStr = it },
                    label = "হ্যান্ডেল রসি দর/পিস",
                    suffix = "৳/pc"
                )
            }
        }

        // 5. Profit & Shipping
        SectionHeaderCard(
            title = "৫. শিপিং, ডিজাইন ও প্রফিট মার্জিন",
            subtitle = "ডেলিভারি চার্জ ও লাভ"
        ) {
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
                label = "লাভের শতাংশ",
                suffix = "%"
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
