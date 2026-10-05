package com.sucharu.sucharupro.shared.ui.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/**
 * Complete Multiplatform Commercial Printing Calculator Engine Workspace.
 * Substrate Selection, Job Specifications, Pre-Press CTP, Offset Machine Rates,
 * Post-Press Finishing Operations (Lamination, Die-Cut, Spot UV, Folding, Binding),
 * Material Optimization Display (Items/Sheet, Cut Direction, Paper Weight Kg),
 * and Module 03 Quotation Handoff Contract Integration.
 */
@Composable
fun SharedPrintingCalculatorWorkspace(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onConfirmQuotation: (calculationId: String) -> Unit = {}
) {
    var selectedSectorIndex by remember { mutableIntStateOf(0) }

    // 1. Substrate / Paper State
    var selectedPaperStock by remember { mutableStateOf("Art Paper (Gloss / Matt)") }
    var selectedSheetSize by remember { mutableStateOf("20\" x 30\" (Demy)") }
    var selectedGsm by remember { mutableStateOf("150 GSM") }
    var reamPriceInput by remember { mutableStateOf("3200") }

    // 2. Job Specifications State
    var quantityInput by remember { mutableStateOf("1000") }
    var selectedCutSize by remember { mutableStateOf("A4 (8.27\" x 11.69\")") }
    var pagesInput by remember { mutableStateOf("2") }
    var selectedColorMode by remember { mutableStateOf("CMYK 4-Color (4/0)") }

    // 3. Pre-Press & Machine State
    var selectedPlateType by remember { mutableStateOf("CTP Thermal (৳250/plate)") }
    var selectedMachineType by remember { mutableStateOf("Offset 4-Color Speedmaster") }
    var wasteSheetsInput by remember { mutableStateOf("100") }

    // 4. Post-Press Finishing State
    var selectedLamination by remember { mutableStateOf("Thermal Gloss (৳0.80/sq.ft)") }
    var dieCuttingEnabled by remember { mutableStateOf(false) }
    var spotUvEnabled by remember { mutableStateOf(false) }
    var selectedFolding by remember { mutableStateOf("None") }
    var selectedBinding by remember { mutableStateOf("None") }

    // 5. Financials
    var marginPercentInput by remember { mutableStateOf("20") }

    val sectorTabs = listOf(
        "১. অফসেট প্রিন্টিং",
        "২. ওয়েডিং ও ইনভিটেশন",
        "৩. ডিজিটাল ফাস্ট প্রিন্টিং",
        "৪. বই ও ক্যাটালগ",
        "৫. ডায়েরি ও নোটবুক",
        "৬. গিফট ও কর্পোরেট",
        "৭. রাবার স্ট্যাম্প",
        "৮. প্যাকেজিং ও কার্টন"
    )

    // Calculation Engine Math
    val quantity = quantityInput.toDoubleOrNull() ?: 1000.0
    val pages = pagesInput.toDoubleOrNull() ?: 2.0
    val reamPrice = reamPriceInput.toDoubleOrNull() ?: 3200.0
    val wasteSheets = wasteSheetsInput.toDoubleOrNull() ?: 100.0
    val marginPercent = marginPercentInput.toDoubleOrNull() ?: 20.0
    val gsmVal = selectedGsm.replace(" GSM", "").toDoubleOrNull() ?: 150.0

    // Sheet Dimensions in inches
    val (sheetWidthIn, sheetHeightIn) = when {
        selectedSheetSize.contains("23") -> Pair(23.0, 36.0)
        selectedSheetSize.contains("25") -> Pair(25.0, 37.0)
        else -> Pair(20.0, 30.0)
    }

    // Cut items per full sheet logic
    val itemsPerSheet = when {
        selectedCutSize.contains("A4") -> 6
        selectedCutSize.contains("A5") -> 12
        selectedCutSize.contains("1/8") -> 8
        selectedCutSize.contains("1/4") -> 4
        selectedCutSize.contains("Flyer") -> 16
        else -> 6
    }
    val cutDirection = "Grid Layout ($itemsPerSheet up)"

    val totalCutItems = quantity * (pages / 2.0)
    val productiveSheets = ceil(totalCutItems / itemsPerSheet)
    val totalFullSheets = productiveSheets + wasteSheets
    val reamsRequired = totalFullSheets / 500.0

    // Paper Weight in KG
    val sheetAreaSqM = (sheetWidthIn * sheetHeightIn) * 0.00064516
    val paperWeightKg = (sheetAreaSqM * gsmVal * totalFullSheets) / 1000.0

    // Paper Cost
    val paperCost = reamsRequired * reamPrice

    // CTP Plate Cost & Passes
    val platesPerSet = when {
        selectedColorMode.contains("CMYK 4-Color Double") -> 8
        selectedColorMode.contains("CMYK 4-Color") -> 4
        selectedColorMode.contains("2-Color") -> 2
        else -> 1
    }
    val plateUnitPrice = when {
        selectedPlateType.contains("CTP Thermal") -> 250.0
        selectedPlateType.contains("CTP Violet") -> 200.0
        else -> 50.0
    }
    val plateCost = platesPerSet * plateUnitPrice

    // Printing Cost
    val impressionRate = 180.0 // per 1000 impressions
    val setupCharge = 600.0
    val totalImpressions = (totalFullSheets * platesPerSet).toLong()
    val printingCost = setupCharge + (ceil(totalImpressions / 1000.0) * impressionRate)

    // Finishing Costs
    val laminationUnitPrice = when {
        selectedLamination.contains("Thermal Gloss") -> 0.80
        selectedLamination.contains("Thermal Matt") -> 0.95
        selectedLamination.contains("Cold Gloss") -> 0.50
        else -> 0.0
    }
    val laminationCost = totalFullSheets * ((sheetWidthIn * sheetHeightIn) / 144.0) * laminationUnitPrice
    val dieCuttingCost = if (dieCuttingEnabled) (800.0 + (quantity * 0.20)) else 0.0
    val spotUvCost = if (spotUvEnabled) (1200.0 + (quantity * 0.50)) else 0.0
    val foldingUnitPrice = when {
        selectedFolding.contains("Half-Fold") -> 0.20
        selectedFolding.contains("Tri-Fold") -> 0.40
        selectedFolding.contains("Z-Fold") -> 0.50
        else -> 0.0
    }
    val foldingCost = quantity * foldingUnitPrice
    val bindingUnitRate = when {
        selectedBinding.contains("Center Saddle Stitch") -> 1.20
        selectedBinding.contains("Perfect Glue Bind") -> 8.50
        selectedBinding.contains("Wire-O Binding") -> 15.00
        else -> 0.0
    }
    val bindingCost = quantity * bindingUnitRate
    val finishingCost = laminationCost + dieCuttingCost + spotUvCost + foldingCost + bindingCost

    // Financial Summary
    val netProductionCost = paperCost + plateCost + printingCost + finishingCost
    val marginAmount = netProductionCost * (marginPercent / 100.0)
    val grandTotalEstimate = netProductionCost + marginAmount
    val unitCost = if (quantity > 0) grandTotalEstimate / quantity else 0.0

    val generatedCalculationId = remember(grandTotalEstimate) { "CALC-2026-${(1000..9999).random()}" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
            .padding(16.dp)
    ) {
        // Header Bar
        Surface(
            color = Color(0xFF0A1224),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.3f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onClose() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "বাণিজ্যিক প্রিন্টিং ক্যালকুলেটর ইঞ্জিন",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "৮টি বাণিজ্যিক সেক্টর • রিয়েল-টাইম অটোমেটেড কোটেশন ব্রেকডাউন",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF10B981),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "লাইভ হিসাব সক্রিয়",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sector Tabs Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sectorTabs.take(4).forEachIndexed { idx, title ->
                SectorTabTile(
                    title = title,
                    isSelected = selectedSectorIndex == idx,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSectorIndex = idx }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sectorTabs.drop(4).forEachIndexed { idx, title ->
                val actualIdx = idx + 4
                SectorTabTile(
                    title = title,
                    isSelected = selectedSectorIndex == actualIdx,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSectorIndex = actualIdx }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Form & Calculation Layout
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Column: Substrate & Job Specs Form
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "১. সাবস্ট্রেট ও পেপার স্পেসিফিকেশন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CalcDropdownSelector(
                        label = "কাগজের ধরন (Paper Stock)",
                        options = listOf(
                            "Art Paper (Gloss / Matt)",
                            "Art Card",
                            "Offset Paper (White / Cream)",
                            "Kraft Paper / Board",
                            "Box Board (Grey Back)",
                            "Sticker Paper",
                            "Duplex Board"
                        ),
                        selectedOption = selectedPaperStock,
                        onOptionSelected = { selectedPaperStock = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CalcDropdownSelector(
                            label = "শিট সাইজ (Full Sheet)",
                            options = listOf("20\" x 30\" (Demy)", "23\" x 36\" (Crown)", "25\" x 37\" (Royal)"),
                            selectedOption = selectedSheetSize,
                            onOptionSelected = { selectedSheetSize = it },
                            modifier = Modifier.weight(1f)
                        )

                        CalcDropdownSelector(
                            label = "ওয়েট (GSM)",
                            options = listOf("80 GSM", "100 GSM", "120 GSM", "150 GSM", "250 GSM", "300 GSM"),
                            selectedOption = selectedGsm,
                            onOptionSelected = { selectedGsm = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CalcInputField(
                        label = "রিম মূল্য (BDT / Ream Price)",
                        value = reamPriceInput,
                        onValueChange = { reamPriceInput = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "২. জব সাইজ ও কোয়ান্টিটি",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CalcInputField(
                            label = "পরিমাণ (Quantity pcs)",
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            modifier = Modifier.weight(1f)
                        )

                        CalcInputField(
                            label = "পৃষ্ঠা সংখ্যা (Pages)",
                            value = pagesInput,
                            onValueChange = { pagesInput = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CalcDropdownSelector(
                        label = "ফাইনাল ক্যাট/কাটিং সাইজ",
                        options = listOf(
                            "A4 (8.27\" x 11.69\")",
                            "A5 (5.83\" x 8.27\")",
                            "1/8 Size (7.5\" x 10\")",
                            "1/4 Size (10\" x 15\")",
                            "Flyer (4\" x 9\")"
                        ),
                        selectedOption = selectedCutSize,
                        onOptionSelected = { selectedCutSize = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CalcDropdownSelector(
                        label = "কালার মোড (Color Mode)",
                        options = listOf("CMYK 4-Color (4/0)", "CMYK 4-Color Double Side (4/4)", "2-Color (2/2)", "Monochrome (1/1)"),
                        selectedOption = selectedColorMode,
                        onOptionSelected = { selectedColorMode = it }
                    )
                }
            }

            // Middle Column: Pre-Press, Machine & Finishing Options
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "৩. প্রি-প্রেস ও মেশিন রানিং",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CalcDropdownSelector(
                        label = "সিটিপি/প্লেট অপশন (CTP Plate)",
                        options = listOf("CTP Thermal (৳250/plate)", "CTP Violet (৳200/plate)", "Master Plate (৳50/plate)"),
                        selectedOption = selectedPlateType,
                        onOptionSelected = { selectedPlateType = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CalcDropdownSelector(
                        label = "প্রিন্টিং প্রেস মেশিন",
                        options = listOf("Offset 4-Color Speedmaster", "Offset 2-Color Press", "Single Color Press", "Digital Color Press"),
                        selectedOption = selectedMachineType,
                        onOptionSelected = { selectedMachineType = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CalcInputField(
                        label = "মেক-রেডি ও ওয়েস্টেজ শিট (Spoilage)",
                        value = wasteSheetsInput,
                        onValueChange = { wasteSheetsInput = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "৪. পোস্ট-প্রেস ফিনিশিং ও বাইন্ডিং",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CalcDropdownSelector(
                        label = "লেমিনেশন কোটিং",
                        options = listOf("None", "Thermal Gloss (৳0.80/sq.ft)", "Thermal Matt (৳0.95/sq.ft)", "Cold Gloss (৳0.50/sq.ft)"),
                        selectedOption = selectedLamination,
                        onOptionSelected = { selectedLamination = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = dieCuttingEnabled,
                                onCheckedChange = { dieCuttingEnabled = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00F0FF))
                            )
                            Text(text = "ডাই-কাটিং (Die-Cut)", fontSize = 12.sp, color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = spotUvEnabled,
                                onCheckedChange = { spotUvEnabled = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00F0FF))
                            )
                            Text(text = "স্পট ইউভি (Spot UV)", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CalcDropdownSelector(
                        label = "ফোল্ডিং ও ক্রিজিং (Folding)",
                        options = listOf("None", "Half-Fold (৳0.20)", "Tri-Fold (৳0.40)", "Z-Fold (৳0.50)"),
                        selectedOption = selectedFolding,
                        onOptionSelected = { selectedFolding = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CalcDropdownSelector(
                        label = "বাইন্ডিং (Binding)",
                        options = listOf("None", "Center Saddle Stitch (৳1.20)", "Perfect Glue Bind (৳8.50)", "Wire-O Binding (৳15.00)"),
                        selectedOption = selectedBinding,
                        onOptionSelected = { selectedBinding = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CalcInputField(
                        label = "মুনাফা মার্জিন (Profit Margin %)",
                        value = marginPercentInput,
                        onValueChange = { marginPercentInput = it }
                    )
                }
            }

            // Right Column: Material Optimization & Quotation Breakdown
            Card(
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A2E)),
                border = BorderStroke(1.5.dp, Color(0xFF00F0FF))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "প্রিন্টিং কস্ট এস্টিমেট ও ম্যাটেরিয়াল অপ্টিমাইজেশন",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "সেক্টর: ${sectorTabs[selectedSectorIndex]} • আইডি: $generatedCalculationId",
                        fontSize = 11.sp,
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Material Optimization Box
                    Surface(
                        color = Color(0xFF060D1A),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "মেটেরিয়াল ও শিট কাটিং অপ্টিমাইজেশন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "• শিট প্রতি কাট: $itemsPerSheet পিস ($cutDirection)", fontSize = 11.sp, color = Color.White)
                            Text(text = "• প্রয়োজনীয় মোট কাগজ: ${reamsRequired.formatDec()} রিম (${totalFullSheets.toInt()} শিট)", fontSize = 11.sp, color = Color.White)
                            Text(text = "• প্রোডাক্টিভ শিট: ${productiveSheets.toInt()} | ওয়েস্টেজ/মেক-রেডি: ${wasteSheets.toInt()}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(text = "• আনুমানিক মোট কাগজের ওজন: ${paperWeightKg.formatDec()} কেজি", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CostBreakdownRow("কাগজ খরচ (Paper Cost)", "৳ ${paperCost.toInt()}", "${reamsRequired.formatDec()} রিম (${totalFullSheets.toInt()} শিট)")
                    CostBreakdownRow("প্লেট/সিটিপি খরচ (CTP Plate)", "৳ ${plateCost.toInt()}", "$platesPerSet টি CTP প্লেট ($totalImpressions ইমপ্রেশন)")
                    CostBreakdownRow("প্রিন্টিং ছাপাই খরচ (Press)", "৳ ${printingCost.toInt()}", "ছাপাই পাস + সেটআপ চার্জ")
                    CostBreakdownRow("ফিনিশিং ও বাইন্ডিং খরচ", "৳ ${finishingCost.toInt()}", "লেমিনেশন/ডাই/বাইন্ডিং")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(Color(0xFF1E293B))
                    )

                    CostBreakdownRow("প্রোডাকশন সাবটোটাল", "৳ ${netProductionCost.toInt()}", "উৎপাদন নিট খরচ")
                    CostBreakdownRow("মার্জিন (${marginPercent.toInt()}%)", "৳ ${marginAmount.toInt()}", "প্রফিট মার্জিন")

                    Spacer(modifier = Modifier.height(10.dp))

                    // Final Total Box
                    Surface(
                        color = Color(0xFF040914),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "সর্বমোট আনুমানিক কোটেশন মূল্য", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "৳ ${grandTotalEstimate.toInt()}.০০",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "প্রতি পিস ৳ ${unitCost.formatDec()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00F0FF)
                                    )
                                    Text(text = "ভ্যাট মুক্ত", fontSize = 9.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quotation Handoff CTA Button (Module 03 Integration)
                    Surface(
                        color = Color(0xFF0284C7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onConfirmQuotation(generatedCalculationId)
                                onClose()
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "কোটেশন রিকোয়েস্ট পাঠান (Handoff to Module 03)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorTabTile(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0A1224),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00F0FF) else Color(0xFF1E293B)),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CalcInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00F0FF),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedContainerColor = Color(0xFF040914),
                unfocusedContainerColor = Color(0xFF040914),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
private fun CalcDropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF040914),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selectedOption, fontSize = 11.sp, color = Color.White, maxLines = 1)
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF00F0FF),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (expanded) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF132038),
                border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    options.forEach { opt ->
                        Text(
                            text = opt,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected(opt)
                                    expanded = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CostBreakdownRow(label: String, amount: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = detail, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
        Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF00F0FF))
    }
}

private fun Double.formatDec(): String {
    val i = (this * 100).toInt()
    return "${i / 100}.${(i % 100).let { if (it < 10) "0$it" else "$it" }}"
}
