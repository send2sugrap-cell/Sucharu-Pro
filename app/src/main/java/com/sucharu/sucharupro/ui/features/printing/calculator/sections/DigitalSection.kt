package com.sucharu.sucharupro.ui.features.printing.calculator.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.printingcalculator.DigitalLaminationType
import com.sucharu.sucharupro.domain.model.printingcalculator.DigitalMaterial
import com.sucharu.sucharupro.domain.service.printingcalculator.CalculationEngine
import com.sucharu.sucharupro.ui.features.printing.calculator.components.*

@Composable
fun DigitalSection(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    // 1. Material & Dimension Inputs
    var selectedMaterialName by remember { mutableStateOf(DigitalMaterial.FLEX.displayName) }
    var widthFeetStr by remember { mutableStateOf("10.0") }
    var heightFeetStr by remember { mutableStateOf("5.0") }
    var quantityStr by remember { mutableStateOf("1") }
    var materialRateStr by remember { mutableStateOf("18.0") }

    // Synchronize default rate when material changes
    LaunchedEffect(selectedMaterialName) {
        val mat = DigitalMaterial.entries.find { it.displayName == selectedMaterialName }
        if (mat != null) {
            materialRateStr = mat.defaultRatePerSqFt.toString()
        }
    }

    // 2. Digital Lamination Inputs (Sq Ft)
    var isDigitalLamEnabled by remember { mutableStateOf(false) }
    var selectedDigitalLamType by remember { mutableStateOf(DigitalLaminationType.COLD_MATT.displayName) }
    var lamRatePerSqFtStr by remember { mutableStateOf("12.0") }
    var digitalLamMinChargeStr by remember { mutableStateOf("100.0") }

    // 3. Finishing & Accessories Inputs
    var grommetsPerUnitStr by remember { mutableStateOf("4") }
    var ratePerGrommetStr by remember { mutableStateOf("5.0") }

    var isHemmingEnabled by remember { mutableStateOf(true) }
    var hemmingRatePerFootStr by remember { mutableStateOf("3.0") }

    var isBoardMountEnabled by remember { mutableStateOf(false) }
    var boardMountRatePerSqFtStr by remember { mutableStateOf("45.0") }

    var standUnitPriceStr by remember { mutableStateOf("0.0") } // e.g. Roll-up or X-Stand

    // 4. Design & Profit
    var designChargeStr by remember { mutableStateOf("200.0") }
    var profitPercentStr by remember { mutableStateOf("20.0") }

    // Real-time Calculation
    val result = remember(
        widthFeetStr, heightFeetStr, quantityStr, materialRateStr,
        isDigitalLamEnabled, lamRatePerSqFtStr, digitalLamMinChargeStr,
        grommetsPerUnitStr, ratePerGrommetStr,
        isHemmingEnabled, hemmingRatePerFootStr,
        isBoardMountEnabled, boardMountRatePerSqFtStr,
        standUnitPriceStr, designChargeStr, profitPercentStr
    ) {
        val wFt = widthFeetStr.toDoubleOrNull() ?: 0.0
        val hFt = heightFeetStr.toDoubleOrNull() ?: 0.0
        val qty = quantityStr.toIntOrNull() ?: 0
        val matRate = materialRateStr.toDoubleOrNull() ?: 0.0

        val lamRate = lamRatePerSqFtStr.toDoubleOrNull() ?: 0.0
        val lamMin = digitalLamMinChargeStr.toDoubleOrNull() ?: 0.0

        val grommets = grommetsPerUnitStr.toIntOrNull() ?: 0
        val grommetRate = ratePerGrommetStr.toDoubleOrNull() ?: 0.0

        val hemmingRate = hemmingRatePerFootStr.toDoubleOrNull() ?: 0.0
        val boardRate = boardMountRatePerSqFtStr.toDoubleOrNull() ?: 0.0
        val standPrice = standUnitPriceStr.toDoubleOrNull() ?: 0.0

        val design = designChargeStr.toDoubleOrNull() ?: 0.0
        val profit = profitPercentStr.toDoubleOrNull() ?: 0.0

        CalculationEngine.calculateDigitalCost(
            widthFeet = wFt,
            heightFeet = hFt,
            quantity = qty,
            materialRatePerSqFt = matRate,
            isDigitalLamEnabled = isDigitalLamEnabled,
            lamRatePerSqFt = lamRate,
            digitalLamMinCharge = lamMin,
            grommetsPerUnit = grommets,
            ratePerGrommet = grommetRate,
            isHemmingEnabled = isHemmingEnabled,
            hemmingRatePerFoot = hemmingRate,
            isBoardMountEnabled = isBoardMountEnabled,
            boardMountRatePerSqFt = boardRate,
            standUnitPrice = standPrice,
            designCharge = design,
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
            itemsQuantity = quantityStr.toIntOrNull() ?: 0,
            breakdownItems = listOf(
                "মেটেরিয়াল প্রিন্ট খরচ (${formatMoney(result.totalSqFtAll)} sq.ft)" to result.printCost,
                "ডিজিটাল ল্যামিনেশন খরচ (স্কয়ার ফুট ভিত্তিক)" to result.digitalLamCost,
                "গ্রোমেট/আইলেট খরচ" to result.grommetCost,
                "সাইড হেমিং/সিলিং খরচ (${formatMoney(result.perimeterFeet)} ft)" to result.hemmingCost,
                "মাউন্টিং বোর্ড খরচ (PVC Foam/Acrylic)" to result.boardMountCost,
                "স্ট্যান্ড মেকানিজম (Roll-up / X-Stand)" to result.standCost,
                "ডিজাইন চার্জ" to (designChargeStr.toDoubleOrNull() ?: 0.0)
            ),
            initialExpanded = false
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 1. Material & Size
        SectionHeaderCard(
            title = "১. মেটেরিয়াল ও সাইন পরিমাপ (Dimensions in Feet)",
            subtitle = "ডিজিটাল/ব্যানার মেটেরিয়াল, দৈর্ঘ্য ও প্রস্থ (ফুট হিসেবে)"
        ) {
            DropdownMenuField(
                selectedOption = selectedMaterialName,
                options = DigitalMaterial.entries.map { it.displayName },
                onOptionSelected = { selectedMaterialName = it },
                label = "মেটেরিয়াল টাইপ (Material Type)"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = widthFeetStr,
                    onValueChange = { widthFeetStr = it },
                    label = "দৈর্ঘ্য/চওড়া (Width)",
                    suffix = "ft",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = heightFeetStr,
                    onValueChange = { heightFeetStr = it },
                    label = "প্রস্থ/উচ্চতা (Height)",
                    suffix = "ft",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = materialRateStr,
                    onValueChange = { materialRateStr = it },
                    label = "মেটেরিয়াল দর (Rate/sq.ft)",
                    suffix = "৳/sq.ft",
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = "পরিমাণ (Quantity)",
                    suffix = "pcs",
                    isInteger = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "মোট ক্ষেত্রফল: ${formatMoney(result.totalSqFtPerUnit)} বর্গফুট/পিস | সর্বমোট: ${formatMoney(result.totalSqFtAll)} বর্গফুট",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 2. Square Foot Lamination
        SectionHeaderCard(
            title = "২. ডিজিটাল ল্যামিনেশন (Square Foot Lamination)",
            subtitle = "কোল্ড ম্যাট, কোল্ড গ্লসি বা ফ্লোর ল্যামিনেশন"
        ) {
            SwitchRowField(
                title = "ডিজিটাল ল্যামিনেশন যোগ করুন",
                checked = isDigitalLamEnabled,
                onCheckedChange = { isDigitalLamEnabled = it }
            )
            if (isDigitalLamEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                DropdownMenuField(
                    selectedOption = selectedDigitalLamType,
                    options = DigitalLaminationType.entries.map { it.displayName },
                    onOptionSelected = { selectedDigitalLamType = it },
                    label = "ল্যামিনেশন টাইপ"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = lamRatePerSqFtStr,
                        onValueChange = { lamRatePerSqFtStr = it },
                        label = "দর প্রতি বর্গফুট",
                        suffix = "৳/sq.ft",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = digitalLamMinChargeStr,
                        onValueChange = { digitalLamMinChargeStr = it },
                        label = "মিনিমাম ল্যামিনেশন চার্জ",
                        suffix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Finishing & Accessories
        SectionHeaderCard(
            title = "৩. ফিনিশিং, এক্সেসরিজ ও মাউন্টিং",
            subtitle = "গ্রোমেট, সাইড হেমিং, ফোম বোর্ড মাউন্টিং ও স্ট্যান্ড"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = grommetsPerUnitStr,
                    onValueChange = { grommetsPerUnitStr = it },
                    label = "গ্রোমেট/আইলেট সংখ্যা (পিস/ইউনিট)",
                    isInteger = true,
                    modifier = Modifier.weight(1f)
                )
                NumberInputField(
                    value = ratePerGrommetStr,
                    onValueChange = { ratePerGrommetStr = it },
                    label = "প্রতি গ্রোমেট দর",
                    suffix = "৳/pc",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRowField(
                title = "সাইড হেমিং/সিলিং (Side Hemming)",
                checked = isHemmingEnabled,
                onCheckedChange = { isHemmingEnabled = it }
            )
            if (isHemmingEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                NumberInputField(
                    value = hemmingRatePerFootStr,
                    onValueChange = { hemmingRatePerFootStr = it },
                    label = "হেমিং রেট প্রতি রানিং ফুট",
                    suffix = "৳/ft"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchRowField(
                title = "মাউন্টিং বোর্ড (PVC Foam / Acrylic Sheet)",
                checked = isBoardMountEnabled,
                onCheckedChange = { isBoardMountEnabled = it }
            )
            if (isBoardMountEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                NumberInputField(
                    value = boardMountRatePerSqFtStr,
                    onValueChange = { boardMountRatePerSqFtStr = it },
                    label = "বোর্ড মাউন্টিং দর/বর্গফুট",
                    suffix = "৳/sq.ft"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(
                value = standUnitPriceStr,
                onValueChange = { standUnitPriceStr = it },
                label = "স্ট্যান্ড মেকানিজম (Roll-up / X-Stand) দর/পিস",
                suffix = "৳/pc"
            )
        }

        // 4. Design & Profit
        SectionHeaderCard(
            title = "৪. ডিজাইন চার্জ ও প্রফিট মার্জিন",
            subtitle = "ডিজাইন ফি ও লাভের শতাংশ"
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
                    value = profitPercentStr,
                    onValueChange = { profitPercentStr = it },
                    label = "লাভের শতাংশ (Profit Percent)",
                    suffix = "%",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
