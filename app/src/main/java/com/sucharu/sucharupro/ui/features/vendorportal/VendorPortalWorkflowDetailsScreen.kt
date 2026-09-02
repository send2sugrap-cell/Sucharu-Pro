package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sucharu.sucharupro.data.api.model.VendorWorkflowDto
import com.sucharu.sucharupro.data.api.model.VendorWorkflowSlaProjectionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalWorkflowDetailsScreen(
    workflow: VendorWorkflowDto,
    slaProjection: VendorWorkflowSlaProjectionDto? = null,
    onViewTimelineClick: () -> Unit = {},
    onViewExceptionsClick: () -> Unit = {},
    onViewActionsClick: () -> Unit = {},
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
                        text = workflow.workflowTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF020617)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(bgGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status & Stage Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Stage", fontSize = 14.sp, color = Color(0xFF94A3B8))
                            Surface(
                                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = workflow.currentStage.replace("_", " "),
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status: ${workflow.status}", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                "SLA: ${workflow.slaStatus}",
                                color = if (workflow.slaStatus == "OVERDUE") Color(0xFFEF4444) else Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Cross-Module Entities Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Linked Canonical & Portal Entities",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        EntityLinkRow("Purchase Order", workflow.purchaseOrderId ?: "N/A")
                        EntityLinkRow("Work Order", workflow.workOrderId ?: "N/A")
                        EntityLinkRow("Delivery Notice (ASN)", workflow.deliveryNoticeId ?: "N/A")
                        EntityLinkRow("Commercial Invoice", workflow.invoiceId ?: "N/A")
                        EntityLinkRow("Quality Case", workflow.qualityCaseId ?: "N/A")
                        EntityLinkRow("Settlement", workflow.settlementId ?: "N/A")
                    }
                }
            }

            // SLA Details
            if (slaProjection != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SLA & Fulfillment Target",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = slaProjection.milestoneTitle, color = Color(0xFFCBD5E1), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            val daysRemaining = Math.max(0L, slaProjection.timeRemainingMs / 86400000L)
                            Text(
                                text = if (slaProjection.isBreached) "BREACHED" else "$daysRemaining days remaining",
                                color = if (slaProjection.isBreached) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onViewTimelineClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Timeline", color = Color.White)
                    }
                    Button(
                        onClick = onViewActionsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Actions", color = Color.White)
                    }
                    Button(
                        onClick = onViewExceptionsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exceptions", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun EntityLinkRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
