package com.sucharu.sucharupro.shared.ui.calculator

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared Multiplatform 8-Sector Commercial Printing Calculator Hub.
 */
@Composable
fun SharedPrintingCalculatorWorkspace(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    var selectedSectorIndex by remember { mutableIntStateOf(0) }
    var quantityInput by remember { mutableStateOf("1000") }
    var paperGsmInput by remember { mutableStateOf("300") }
    var calculatedTotal by remember { mutableStateOf(4800.0) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Header
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "বাণিজ্যিক প্রিন্টিং ক্যালকুলেটর হাব",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "৮টি বাণিজ্যিক সেক্টর • অটোমেটেড কোটেশন জেনারেটর",
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
                        text = "লাইভ ক্যালকুলেটর",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sector Tabs Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sectorTabs.take(4).forEachIndexed { idx, title ->
                SectorTabButton(
                    title = title,
                    isSelected = selectedSectorIndex == idx,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSectorIndex = idx }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sectorTabs.drop(4).forEachIndexed { idx, title ->
                val actualIdx = idx + 4
                SectorTabButton(
                    title = title,
                    isSelected = selectedSectorIndex == actualIdx,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSectorIndex = actualIdx }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Active Sector Form & Quotation Generator
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "সেক্টর ইনপুট প্যারামিটার - ${sectorTabs[selectedSectorIndex]}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "পরিমাণ (Quantity)", fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = {
                                    quantityInput = it
                                    val qty = it.toDoubleOrNull() ?: 1.0
                                    val gsm = paperGsmInput.toDoubleOrNull() ?: 100.0
                                    calculatedTotal = (qty * (gsm / 100.0) * 1.6) + 1200.0
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "কাগজের জিএসএম (Paper GSM)", fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = paperGsmInput,
                                onValueChange = {
                                    paperGsmInput = it
                                    val qty = quantityInput.toDoubleOrNull() ?: 1.0
                                    val gsm = it.toDoubleOrNull() ?: 100.0
                                    calculatedTotal = (qty * (gsm / 100.0) * 1.6) + 1200.0
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calculation Result Box
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "আনুমানিক কোটেশন মূল্য", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "৳ ${calculatedTotal.toInt()}.০০",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "কাগজ খরচ + প্লেট + ছাপাই + লেমিনেশন অন্তর্ভুক্ত",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "অর্ডার নিশ্চিত করুন",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorTabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
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
