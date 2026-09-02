package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalQualityInspectionSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalQualityInspectionListScreen(
    inspections: List<VendorPortalQualityInspectionSummaryDto>,
    onInspectionClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quality Inspections & Findings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (inspections.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No quality inspections found.", color = Color(0xFF64748B), fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(inspections) { insp ->
                            QualityInspectionCard(inspection = insp, onClick = { onInspectionClick(insp.inspectionId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QualityInspectionCard(
    inspection: VendorPortalQualityInspectionSummaryDto,
    onClick: () -> Unit
) {
    val outcomeColor = when (inspection.overallResult) {
        "PASSED" -> Color(0xFF34D399)
        "CONDITIONALLY_PASSED" -> Color(0xFFFBBF24)
        "FAILED" -> Color(0xFFF87171)
        else -> Color(0xFF94A3B8)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inspection.inspectionNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    color = outcomeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = inspection.overallResult,
                        color = outcomeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Inspected: ${inspection.inspectedQuantity}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("Accepted: ${inspection.acceptedQuantity}", color = Color(0xFF34D399), fontSize = 12.sp)
                if (inspection.rejectedQuantity > 0.0) {
                    Text("Rejected: ${inspection.rejectedQuantity}", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (inspection.rejectionReason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rejection Reason: ${inspection.rejectionReason}",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp
                )
            }
        }
    }
}
