package com.sucharu.sucharupro.ui.features.printing.calculator.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.printingcalculator.StampCategory
import com.sucharu.sucharupro.domain.service.printingcalculator.CalculationEngine
import com.sucharu.sucharupro.ui.features.printing.calculator.components.*

@Composable
fun StampSection(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    var selectedCategoryName by remember { mutableStateOf(StampCategory.SELF_INKING.displayName) }
    var quantityStr by remember { mutableStateOf("10") }
    var casingPriceByModelStr by remember { mutableStateOf("180.0") } // Mount/Casing price

    var isBulkSameDesign by remember { mutableStateOf(true) } // If true, plate making charge is one-time setup
    var plateMakingRateBySizeStr by remember { mutableStateOf("150.0") }

    var isSelfInking by remember { mutableStateOf(true) }
    var inkPadRateStr by remember { mutableStateOf("30.0") }
    var manualInkPadPriceStr by remember { mutableStateOf("45.0") }

    var designChargeStr by remember { mutableStateOf("100.0") }
    var profitPercentStr by remember { mutableStateOf("25.0") }

    val result = remember(
        selectedCategoryName, quantityStr, casingPriceByModelStr, isBulkSameDesign,
        plateMakingRateBySizeStr, isSelfInking, inkPadRateStr, manualInkPadPriceStr,
        designChargeStr, profitPercentStr
    ) {
        val qty = quantityStr.toIntOrNull() ?: 0
        val casing = casingPriceByModelStr.toDoubleOrNull() ?: 0.0
        val plateRate = plateMakingRateBySizeStr.toDoubleOrNull() ?: 0.0
        val inkPad = inkPadRateStr.toDoubleOrNull() ?: 0.0
        val manualPad = manualInkPadPriceStr.toDoubleOrNull() ?: 0.0
        val design = designChargeStr.toDoubleOrNull() ?: 0.0
        val profit = profitPercentStr.toDoubleOrNull() ?: 0.0

        CalculationEngine.calculateStampCost(
            quantity = qty,
            casingPriceByModel = casing,
            isBulkSameDesign = isBulkSameDesign,
            plateMakingRateBySize = plateRate,
            isSelfInking = isSelfInking,
            inkPadRate = inkPad,
            manualInkPadPrice = manualPad,
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
                "কেসিং/মাউন্ট ক্রয় খরচ ($quantityStr পিস)" to result.baseCasingCost,
                "লেজার ডাই/প্লেট মেকিং খরচ" to result.plateMakingCost,
                "ইঙ্ক প্যাড/কালি খরচ" to result.inkPadCost,
                "ডিজাইন চার্জ" to (designChargeStr.toDoubleOrNull() ?: 0.0)
            ),
            initialExpanded = false
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 1. Stamp Model & Quantity
        SectionHeaderCard(
            title = "১. স্ট্যাম্প ক্যাটাগরি ও মাউন্ট কেসিং",
            subtitle = "সিল স্ট্যাম্প টাইপ, মডেল ও মাউন্ট দর"
        ) {
            DropdownMenuField(
                selectedOption = selectedCategoryName,
                options = StampCategory.entries.map { it.displayName },
                onOptionSelected = {
                    selectedCategoryName = it
                    isSelfInking = it.contains("Self") || it.contains("সেলফ")
                },
                label = "স্ট্যাম্প ক্যাটাগরি"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputField(
                    value = casingPriceByModelStr,
                    onValueChange = { casingPriceByModelStr = it },
                    label = "কেসিং/মাউন্ট দর",
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

        // 2. Design & Plate Making
        SectionHeaderCard(
            title = "২. রাবার/ফ্ল্যাশ প্লেট তৈরি ও কালি",
            subtitle = "একই ডিজাইনে বাল্ক নাকি ভিন্ন নাম/সিল"
        ) {
            SwitchRowField(
                title = "সবগুলো স্ট্যাম্প একই ডিজাইনের (Same Design)",
                checked = isBulkSameDesign,
                onCheckedChange = { isBulkSameDesign = it },
                subtitle = if (isBulkSameDesign) "প্লেট তৈরির খরচ ১ বারই যুক্ত হবে" else "প্রতি স্ট্যাম্পের জন্য আলাদা প্লেট তৈরির চার্জ লাগবে"
            )
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(
                value = plateMakingRateBySizeStr,
                onValueChange = { plateMakingRateBySizeStr = it },
                label = "প্লেট/ডাই তৈরি খরচ",
                suffix = "৳"
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isSelfInking) {
                NumberInputField(
                    value = inkPadRateStr,
                    onValueChange = { inkPadRateStr = it },
                    label = "রিফিল ইঙ্ক প্যাড দর",
                    suffix = "৳/pc"
                )
            } else {
                NumberInputField(
                    value = manualInkPadPriceStr,
                    onValueChange = { manualInkPadPriceStr = it },
                    label = "ম্যানুয়াল স্ট্যাম্প প্যাড দর",
                    suffix = "৳/pc"
                )
            }
        }

        // 3. Design & Profit
        SectionHeaderCard(
            title = "৩. ডিজাইন ফি ও লাভ মার্জিন",
            subtitle = "মনোগ্রাম/ডিজাইন চার্জ ও প্রফিট"
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
                    label = "লাভের শতাংশ",
                    suffix = "%",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
