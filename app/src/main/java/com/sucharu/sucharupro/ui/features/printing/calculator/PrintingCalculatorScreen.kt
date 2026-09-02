package com.sucharu.sucharupro.ui.features.printing.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.printingcalculator.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintingCalculatorScreen(
    onCalculate: (PrintingCalculationRequestDto) -> Unit = {},
    calculationResult: PrintingCalculationResponseDto? = null,
    validationResult: ValidationResponseDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var jobTitle by remember { mutableStateOf("Standard Brochure 1000 Pcs") }
    var productType by remember { mutableStateOf("PRINTING_JOB") }
    var quantityStr by remember { mutableStateOf("1000") }
    var quantityUnit by remember { mutableStateOf("PIECES") }
    var finishedWidthStr by remember { mutableStateOf("210") }
    var finishedHeightStr by remember { mutableStateOf("297") }
    var dimensionUnit by remember { mutableStateOf("MILLIMETERS") }

    var materialName by remember { mutableStateOf("Art Paper Gloss") }
    var stockType by remember { mutableStateOf("ART_PAPER") }
    var gsmStr by remember { mutableStateOf("150") }
    var sheetWidthStr by remember { mutableStateOf("635") } // 25 inch = 635mm
    var sheetHeightStr by remember { mutableStateOf("914") } // 36 inch = 914mm
    var materialUnitPriceStr by remember { mutableStateOf("8.5000") }

    var processType by remember { mutableStateOf("OFFSET") }
    var sides by remember { mutableStateOf("DOUBLE_SIDED_SAME") }
    var colorMode by remember { mutableStateOf("CMYK_FOUR_COLOR") }
    var frontColorsStr by remember { mutableStateOf("4") }
    var backColorsStr by remember { mutableStateOf("4") }
    var spotColorsStr by remember { mutableStateOf("0") }

    var setupSheetsStr by remember { mutableStateOf("100") }
    var runningWastePctStr by remember { mutableStateOf("3.0000") }
    var finishingWastePctStr by remember { mutableStateOf("2.0000") }

    val buildRequest = {
        PrintingCalculationRequestDto(
            jobTitle = jobTitle,
            productType = productType,
            quantity = quantityStr.toLongOrNull() ?: 1000L,
            quantityUnit = quantityUnit,
            finishedWidth = finishedWidthStr,
            finishedHeight = finishedHeightStr,
            dimensionUnit = dimensionUnit,
            materialName = materialName,
            stockType = stockType,
            gsm = gsmStr.takeIf { it.isNotBlank() },
            sheetWidth = sheetWidthStr.takeIf { it.isNotBlank() },
            sheetHeight = sheetHeightStr.takeIf { it.isNotBlank() },
            sheetDimensionUnit = "MILLIMETERS",
            materialUnitPricePerSheet = materialUnitPriceStr.takeIf { it.isNotBlank() },
            processType = processType,
            sides = sides,
            colorMode = colorMode,
            frontColorsCount = frontColorsStr.toIntOrNull() ?: 4,
            backColorsCount = backColorsStr.toIntOrNull() ?: 0,
            spotColorsCount = spotColorsStr.toIntOrNull() ?: 0,
            setupSheets = setupSheetsStr.toLongOrNull() ?: 0L,
            runningWastePercentage = runningWastePctStr,
            finishingWastePercentage = finishingWastePctStr,
            currency = "BDT"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Smart Printing Calculator", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Canonical Commercial Print Estimation Engine • Module 17", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF0284C7).copy(alpha = 0.2f), Color(0xFF0F172A))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF0284C7),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("ESTIMATE ONLY", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Deterministic Quotation Support", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Physical requirement & cost estimation without creating secondary financial ledgers or mutating production authorities.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }

            // Input Section: 1. Job & Dimensions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1. Job & Dimension Specifications", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 15.sp)

                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text("Job Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = { quantityStr = it },
                                label = { Text("Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = quantityUnit,
                                onValueChange = { quantityUnit = it },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = finishedWidthStr,
                                onValueChange = { finishedWidthStr = it },
                                label = { Text("Finished Width") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = finishedHeightStr,
                                onValueChange = { finishedHeightStr = it },
                                label = { Text("Finished Height") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dimensionUnit,
                                onValueChange = { dimensionUnit = it },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Input Section: 2. Paper / Substrate Specification
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("2. Paper / Substrate Specification", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 15.sp)

                        OutlinedTextField(
                            value = materialName,
                            onValueChange = { materialName = it },
                            label = { Text("Material Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = stockType,
                                onValueChange = { stockType = it },
                                label = { Text("Stock Type") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = gsmStr,
                                onValueChange = { gsmStr = it },
                                label = { Text("GSM / Weight") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = sheetWidthStr,
                                onValueChange = { sheetWidthStr = it },
                                label = { Text("Sheet Width (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sheetHeightStr,
                                onValueChange = { sheetHeightStr = it },
                                label = { Text("Sheet Height (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = materialUnitPriceStr,
                            onValueChange = { materialUnitPriceStr = it },
                            label = { Text("Paper Unit Price Per Sheet (BDT)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Input Section: 3. Printing Process & Color
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("3. Press Process & Color Setup", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 15.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = processType,
                                onValueChange = { processType = it },
                                label = { Text("Process Type") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sides,
                                onValueChange = { sides = it },
                                label = { Text("Sides") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = frontColorsStr,
                                onValueChange = { frontColorsStr = it },
                                label = { Text("Front Colors") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = backColorsStr,
                                onValueChange = { backColorsStr = it },
                                label = { Text("Back Colors") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = spotColorsStr,
                                onValueChange = { spotColorsStr = it },
                                label = { Text("Spot Colors") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Input Section: 4. Waste & Spoilage Allowance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("4. Waste & Spoilage Allowance", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 15.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = setupSheetsStr,
                                onValueChange = { setupSheetsStr = it },
                                label = { Text("Setup Sheets (Fix)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = runningWastePctStr,
                                onValueChange = { runningWastePctStr = it },
                                label = { Text("Run Waste %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = finishingWastePctStr,
                                onValueChange = { finishingWastePctStr = it },
                                label = { Text("Finish Waste %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Action Button
            item {
                Button(
                    onClick = { onCalculate(buildRequest()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculate Printing Estimate", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFCA5A5))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorMessage, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Calculation Results
            calculationResult?.let { res ->
                // Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ESTIMATED CALCULATION RESULT", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF94A3B8))
                                Surface(
                                    color = if (res.status == "SUCCESSFUL") Color(0xFF166534) else Color(0xFF854D0E),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(res.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Estimated Total Cost", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    Text(
                                        res.totalEstimatedCost?.let { "$it ${res.currency}" } ?: "Not available",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = if (res.totalEstimatedCost != null) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Estimated Unit Cost", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    Text(
                                        res.estimatedUnitCost?.let { "$it ${res.currency}" } ?: "Not available",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                            }
                        }
                    }
                }

                // Physical Material & Imposition Requirement
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Material & Imposition Requirements", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 14.sp)
                            HorizontalDivider(color = Color(0xFF334155))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Items per Parent Sheet:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("${res.materialRequirement.finishedItemsPerSheet} (${res.materialRequirement.cutDirection})", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Productive Sheets:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("${res.materialRequirement.productiveSheetsRequired} Sheets", color = Color.White, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Waste Allowance Sheets:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("${res.materialRequirement.wasteSheetsRequired} Sheets", color = Color(0xFFFBBF24), fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Gross Sheets Required:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("${res.materialRequirement.totalSheetsRequired} Sheets", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reams Required (500 sh/rm):", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("${res.materialRequirement.totalReamsRequired} Reams", color = Color.White, fontSize = 13.sp)
                            }
                            res.materialRequirement.totalWeightKg?.let { wt ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estimated Total Paper Weight:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                    Text("$wt kg", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Press & Impressions Requirement
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Press & Impression Requirements", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 14.sp)
                            HorizontalDivider(color = Color(0xFF334155))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Press Impressions:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("${res.printingRequirement.totalImpressions} Imp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Passes:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("${res.printingRequirement.totalPasses} Pass", color = Color.White, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CTP Offset Plates Required:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("${res.printingRequirement.plateCount} Plates", color = Color(0xFF38BDF8), fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Cost Breakdown Table
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Cost Component Breakdown", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 14.sp)
                            HorizontalDivider(color = Color(0xFF334155))

                            res.breakdownItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                                        Text("Qty: ${item.quantity} ${item.unit} ${item.unitRate?.let { "• Rate: $it" } ?: ""}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    }
                                    Text(
                                        item.calculatedAmount?.let { "$it ${res.currency}" } ?: "Not available",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (item.calculatedAmount != null) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                // Diagnostics
                if (res.diagnostics.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Calculation Diagnostics & Notices", fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), fontSize = 14.sp)
                                HorizontalDivider(color = Color(0xFF334155))

                                res.diagnostics.forEach { diag ->
                                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                        Icon(
                                            when (diag.severity) {
                                                "ERROR" -> Icons.Default.Cancel
                                                "WARNING" -> Icons.Default.Warning
                                                else -> Icons.Default.Info
                                            },
                                            contentDescription = null,
                                            tint = when (diag.severity) {
                                                "ERROR" -> Color(0xFFEF4444)
                                                "WARNING" -> Color(0xFFF59E0B)
                                                else -> Color(0xFF38BDF8)
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(diag.message, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                            diag.suggestedRemediation?.let {
                                                Text("Tip: $it", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Cryptographic Integrity Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("AUDIT & INTEGRITY FINGERPRINT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Request Fingerprint: ${res.requestFingerprint}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("Result Integrity Hash: ${res.integrityHash}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}
