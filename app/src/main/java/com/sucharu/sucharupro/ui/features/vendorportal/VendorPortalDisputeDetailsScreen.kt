package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalDisputeSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalDisputeDetailsScreen(
    dispute: VendorPortalDisputeSummaryDto,
    onRespondClick: () -> Unit = {},
    onAcceptProposalClick: () -> Unit = {},
    onRejectProposalClick: () -> Unit = {},
    onViewEvidenceClick: () -> Unit = {},
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
                        text = "Dispute: ${dispute.disputeReference}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dispute.subject,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = dispute.status,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dispute.description, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Disputed Qty", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("${dispute.disputedQuantity}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Disputed Amount", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("$${dispute.disputedAmount}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Requested Resolution", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(dispute.requestedResolution, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Resolution Proposal Section
            dispute.resolutionProposal?.let { proposal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Formal Resolution Proposal", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(proposal, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onAcceptProposalClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Accept Proposal", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onRejectProposalClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reject Proposal", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Vendor Response
            dispute.vendorResponse?.let { resp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Vendor Clarification / Response", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(resp, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                    }
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRespondClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Submit Dispute Response / Clarification", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onViewEvidenceClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View & Upload Dispute Evidence", color = Color(0xFF38BDF8))
                }
            }
        }
    }
}
