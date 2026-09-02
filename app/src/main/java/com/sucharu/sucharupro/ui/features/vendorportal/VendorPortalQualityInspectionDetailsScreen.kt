package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalQualityInspectionSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalQualityInspectionDetailsScreen(
    inspection: VendorPortalQualityInspectionSummaryDto,
    onAcknowledgeClick: () -> Unit = {},
    onRespondClick: () -> Unit = {},
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
                        text = "Inspection: ${inspection.inspectionNumber}",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Overall Result: ${inspection.overallResult}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Inspected: ${inspection.inspectedQuantity}", color = Color.White, fontSize = 13.sp)
                            Text("Accepted: ${inspection.acceptedQuantity}", color = Color(0xFF34D399), fontSize = 13.sp)
                            Text("Rejected: ${inspection.rejectedQuantity}", color = Color(0xFFF87171), fontSize = 13.sp)
                        }
                        if (inspection.rejectionReason != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Rejection: ${inspection.rejectionReason}", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                        }
                        if (inspection.disposition != null) {
                            Text("Disposition: ${inspection.disposition}", color = Color(0xFFFBBF24), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Defect Findings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (inspection.defects.isEmpty()) {
                        item {
                            Text("No specific defect items recorded.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    } else {
                        items(inspection.defects) { defect ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(defect.defectCode, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(defect.severity, color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(defect.description, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("Affected Qty: ${defect.affectedQuantity}", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onAcknowledgeClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Acknowledge", color = Color(0xFF38BDF8))
                    }
                    Button(
                        onClick = onRespondClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Submit Response", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
