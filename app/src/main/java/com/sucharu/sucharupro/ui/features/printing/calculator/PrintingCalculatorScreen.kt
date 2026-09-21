package com.sucharu.sucharupro.ui.features.printing.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto
import com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto
import com.sucharu.sucharupro.data.api.model.printingcalculator.ValidationResponseDto
import com.sucharu.sucharupro.ui.features.printing.calculator.components.DashboardGrid
import com.sucharu.sucharupro.ui.features.printing.calculator.sections.BookSection
import com.sucharu.sucharupro.ui.features.printing.calculator.sections.DiarySection
import com.sucharu.sucharupro.ui.features.printing.calculator.sections.DigitalSection
import com.sucharu.sucharupro.ui.features.printing.calculator.sections.GiftSection
import com.sucharu.sucharupro.ui.features.printing.calculator.sections.OffsetSection
import com.sucharu.sucharupro.ui.features.printing.calculator.sections.PackagingSection
import com.sucharu.sucharupro.ui.features.printing.calculator.sections.StampSection
import com.sucharu.sucharupro.ui.features.printing.calculator.sections.WeddingSection

/**
 * Authoritative 8-Sector Commercial Printing Calculator Hub matching E:\App\Calculetor project.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintingCalculatorScreen(
    onCalculate: (PrintingCalculationRequestDto) -> Unit = {},
    calculationResult: PrintingCalculationResponseDto? = null,
    validationResult: ValidationResponseDto? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    initialSectorId: Int = 0
) {
    var selectedSectorId by remember { mutableIntStateOf(initialSectorId) }

    val sectorTabs = listOf(
        "হোম হাব" to Icons.Default.Dashboard,
        "১. অফসেট" to Icons.Default.Print,
        "২. ওয়েডিং" to Icons.Default.CardGiftcard,
        "৩. ডিজিটাল" to Icons.Default.DisplaySettings,
        "৪. বই/ক্যাটালগ" to Icons.Default.Book,
        "৫. ডায়েরি" to Icons.Default.MenuBook,
        "৬. গিফট" to Icons.Default.Redeem,
        "৭. স্ট্যাম্প" to Icons.Default.Approval,
        "৮. প্যাকেজিং" to Icons.Default.Inventory2
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedSectorId != 0) {
                        IconButton(onClick = { selectedSectorId = 0 }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Hub",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "স্মার্ট প্রিন্টিং ক্যালকুলেটর (Commercial Estimator)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (selectedSectorId == 0) "৮টি বাণিজ্যিক সেক্টর নির্বাচন করুন" else sectorTabs.getOrNull(selectedSectorId)?.first ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                if (selectedSectorId != 0) {
                    TextButton(onClick = { selectedSectorId = 0 }) {
                        Text("সব সেক্টর ⊞", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Scrollable Sector Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedSectorId,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            sectorTabs.forEachIndexed { index, (label, icon) ->
                Tab(
                    selected = selectedSectorId == index,
                    onClick = { selectedSectorId = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = label, fontSize = 12.sp, fontWeight = if (selectedSectorId == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }
        }

        // Sector Active Screen View
        Box(modifier = Modifier.weight(1f)) {
            when (selectedSectorId) {
                0 -> DashboardGrid(onSectorSelected = { sectorId -> selectedSectorId = sectorId })
                1 -> OffsetSection()
                2 -> WeddingSection()
                3 -> DigitalSection()
                4 -> BookSection()
                5 -> DiarySection()
                6 -> GiftSection()
                7 -> StampSection()
                8 -> PackagingSection()
                else -> DashboardGrid(onSectorSelected = { sectorId -> selectedSectorId = sectorId })
            }
        }
    }
}
