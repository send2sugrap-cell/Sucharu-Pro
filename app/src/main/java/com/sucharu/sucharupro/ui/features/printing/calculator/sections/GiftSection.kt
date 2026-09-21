package com.sucharu.sucharupro.ui.features.printing.calculator.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.printingcalculator.GiftCategoryItem
import com.sucharu.sucharupro.domain.model.printingcalculator.GiftCustomizationMethod
import com.sucharu.sucharupro.domain.service.printingcalculator.CalculationEngine
import com.sucharu.sucharupro.ui.features.printing.calculator.components.*

@Composable
fun GiftSection(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    var selectedItemName by remember { mutableStateOf(GiftCategoryItem.MUG.displayName) }
    var quantityStr by remember { mutableStateOf("100") }
    var blankPurchaseRateStr by remember { mutableStateOf("65.0") }

    var selectedMethodName by remember { mutableStateOf(GiftCustomizationMethod.SUBLIMATION.displayName) }

    // Screen print params
    var screenMakingCostPerColorStr by remember { mutableStateOf("300.0") }
    var colorCountStr by remember { mutableStateOf("1") }
    var printRunRatePerPcStr by remember { mutableStateOf("15.0") }

    // DTF & UV params
    var totalPrintAreaSqInStr by remember { mutableStateOf("24.0") } // e.g. 4" x 6"
    var dtfRatePerSqFtStr by remember { mutableStateOf("120.0") }
    var uvRatePerSqInStr by remember { mutableStateOf("1.5") }

    // Sublimation params
    var sublimationPaperAndInkRateStr by remember { mutableStateOf("8.0") }
    var heatPressLabourPerPcStr by remember { mutableStateOf("12.0") }

    // Laser Engraving params
    var engraveRatePerMinuteStr by remember { mutableStateOf("10.0") }
    var timeInMinutesStr by remember { mutableStateOf("2.0") }

    // Packaging & Profit
    var packagingBoxRateStr by remember { mutableStateOf("15.0") }
    var designSetupFeeStr by remember { mutableStateOf("200.0") }
    var profitPercentStr by remember { mutableStateOf("25.0") }

    val result = remember(
        selectedItemName, quantityStr, blankPurchaseRateStr, selectedMethodName,
        screenMakingCostPerColorStr, colorCountStr, printRunRatePerPcStr,
        totalPrintAreaSqInStr, dtfRatePerSqFtStr, uvRatePerSqInStr,
        sublimationPaperAndInkRateStr, heatPressLabourPerPcStr,
        engraveRatePerMinuteStr, timeInMinutesStr, packagingBoxRateStr, designSetupFeeStr, profitPercentStr
    ) {
        val qty = quantityStr.toIntOrNull() ?: 0
        val blankRate = blankPurchaseRateStr.toDoubleOrNull() ?: 0.0

        val screenCost = screenMakingCostPerColorStr.toDoubleOrNull() ?: 0.0
        val colors = colorCountStr.toIntOrNull() ?: 1
        val printRun = printRunRatePerPcStr.toDoubleOrNull() ?: 0.0

        val areaSqIn = totalPrintAreaSqInStr.toDoubleOrNull() ?: 0.0
        val dtfRate = dtfRatePerSqFtStr.toDoubleOrNull() ?: 0.0
        val uvRate = uvRatePerSqInStr.toDoubleOrNull() ?: 0.0

        val subRate = sublimationPaperAndInkRateStr.toDoubleOrNull() ?: 0.0
        val heatPress = heatPressLabourPerPcStr.toDoubleOrNull() ?: 0.0

        val engraveRate = engraveRatePerMinuteStr.toDoubleOrNull() ?: 0.0
        val engraveTime = timeInMinutesStr.toDoubleOrNull() ?: 0.0

        val boxRate = packagingBoxRateStr.toDoubleOrNull() ?: 0.0
        val design = designSetupFeeStr.toDoubleOrNull() ?: 0.0
        val profit = profitPercentStr.toDoubleOrNull() ?: 0.0

        CalculationEngine.calculateGiftCost(
            quantity = qty,
            blankItemPurchaseRate = blankRate,
            customMethod = selectedMethodName,
            screenMakingCostPerColor = screenCost,
            colorCount = colors,
            printRunRatePerPc = printRun,
            totalPrintAreaSqIn = areaSqIn,
            dtfRatePerSqFt = dtfRate,
            sublimationPaperAndInkRate = subRate,
            heatPressLabourPerPc = heatPress,
            uvRatePerSqIn = uvRate,
            engraveRatePerMinute = engraveRate,
            timeInMinutes = engraveTime,
            packagingBoxRate = boxRate,
            designSetupFee = design,
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
            // 1. Item Selection & Quantity
            SectionHeaderCard(
                title = "১. গিফট আইটেম ও বডি ক্রয়মূল্য",
                subtitle = "আইটেমের নাম, কাস্টমাইজেশন টাইপ ও ব্ল্যাংক বডি রেট"
            ) {
                DropdownMenuField(
                    selectedOption = selectedItemName,
                    options = GiftCategoryItem.entries.map { it.displayName },
                    onOptionSelected = { selectedItemName = it },
                    label = "গিফট ক্যাটাগরি আইটেম"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = blankPurchaseRateStr,
                        onValueChange = { blankPurchaseRateStr = it },
                        label = "ব্ল্যাংক আইটেম ক্রয়মূল্য",
                        suffix = "৳/pc",
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
            }

            // 2. Customization Method
            SectionHeaderCard(
                title = "২. কাস্টমাইজেশন মেথড ও প্রিন্টিং রেট",
                subtitle = "সাবলিমেশন, ডিটিএফ, স্ক্রিন প্রিন্ট বা ইউভি"
            ) {
                DropdownMenuField(
                    selectedOption = selectedMethodName,
                    options = GiftCustomizationMethod.entries.map { it.displayName },
                    onOptionSelected = { selectedMethodName = it },
                    label = "প্রিন্ট/কাস্টমাইজেশন পদ্ধতি"
                )
                Spacer(modifier = Modifier.height(10.dp))

                when {
                    selectedMethodName.contains("Sublimation") || selectedMethodName.contains("সাবলিমেশন") -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(
                                value = sublimationPaperAndInkRateStr,
                                onValueChange = { sublimationPaperAndInkRateStr = it },
                                label = "কাগজ ও ইনক কালি খরচ",
                                suffix = "৳/pc",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputField(
                                value = heatPressLabourPerPcStr,
                                onValueChange = { heatPressLabourPerPcStr = it },
                                label = "হিট প্রেস লেবার চার্জ",
                                suffix = "৳/pc",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    selectedMethodName.contains("Screen") || selectedMethodName.contains("স্ক্রিন") -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(
                                value = screenMakingCostPerColorStr,
                                onValueChange = { screenMakingCostPerColorStr = it },
                                label = "স্ক্রিন মেকিং ফি (Fixed)",
                                suffix = "৳/color",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputField(
                                value = colorCountStr,
                                onValueChange = { colorCountStr = it },
                                label = "রঙের সংখ্যা",
                                isInteger = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        NumberInputField(
                            value = printRunRatePerPcStr,
                            onValueChange = { printRunRatePerPcStr = it },
                            label = "প্রিন্ট ড্রাগিং/রানিং লেবার",
                            suffix = "৳/pc"
                        )
                    }
                    selectedMethodName.contains("DTF") || selectedMethodName.contains("ডিটিএফ") -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(
                                value = totalPrintAreaSqInStr,
                                onValueChange = { totalPrintAreaSqInStr = it },
                                label = "প্রিন্ট এরিয়া",
                                suffix = "sq.in",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputField(
                                value = dtfRatePerSqFtStr,
                                onValueChange = { dtfRatePerSqFtStr = it },
                                label = "DTF দর/বর্গফুট",
                                suffix = "৳/sq.ft",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    selectedMethodName.contains("UV") || selectedMethodName.contains("ইউভি") -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(
                                value = totalPrintAreaSqInStr,
                                onValueChange = { totalPrintAreaSqInStr = it },
                                label = "প্রিন্ট এরিয়া",
                                suffix = "sq.in",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputField(
                                value = uvRatePerSqInStr,
                                onValueChange = { uvRatePerSqInStr = it },
                                label = "UV ফ্ল্যাটবেড দর/sq.in",
                                suffix = "৳",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    selectedMethodName.contains("Laser") || selectedMethodName.contains("লেজার") -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(
                                value = timeInMinutesStr,
                                onValueChange = { timeInMinutesStr = it },
                                label = "এনগ্রেভিং সময় (মিনিট)",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputField(
                                value = engraveRatePerMinuteStr,
                                onValueChange = { engraveRatePerMinuteStr = it },
                                label = "মেশিন রেট/মিনিট",
                                suffix = "৳/min",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Packaging & Profit
            SectionHeaderCard(
                title = "৩. প্যাকেজিং ও প্রফিট মার্জিন",
                subtitle = "বক্স প্যাকেজিং, ডিজাইন ও লাভ"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(
                        value = packagingBoxRateStr,
                        onValueChange = { packagingBoxRateStr = it },
                        label = "ব্যক্তিগত বক্স/ভেলভেট বক্স",
                        suffix = "৳/pc",
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        value = designSetupFeeStr,
                        onValueChange = { designSetupFeeStr = it },
                        label = "ডিজাইন ও সেটআপ ফি",
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
            itemsQuantity = quantityStr.toIntOrNull() ?: 0,
            breakdownItems = listOf(
                "ব্ল্যাংক আইটেম ক্রয় খরচ ($quantityStr পিস)" to result.baseBlankCost,
                "কাস্টমাইজেশন প্রিন্ট প্রসেসিং খরচ ($selectedMethodName)" to result.printMethodCost,
                "বক্স/প্যাকেজিং খরচ" to result.packagingCost,
                "ডিজাইন ও সেটআপ ফি" to (designSetupFeeStr.toDoubleOrNull() ?: 0.0)
            ),
            initialExpanded = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
