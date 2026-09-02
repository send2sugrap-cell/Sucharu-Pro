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
import com.sucharu.sucharupro.data.api.model.VendorPortalRejectionSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalRejectionDetailsScreen(
    rejection: VendorPortalRejectionSummaryDto,
    onRaiseDisputeClick: () -> Unit = {},
    onCreateCapaClick: () -> Unit = {},
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
                        text = "Rejection: ${rejection.rejectionReference}",
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
            // Rejection Overview Card
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
                            text = rejection.rejectionType,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = rejection.status,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Reason: ${rejection.rejectionReason}", color = Color(0xFFE2E8F0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Rejected Qty", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("${rejection.rejectedQuantity}", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text("Rejected Value", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("$${rejection.rejectedValue}", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text("Disposition", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(rejection.disposition, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Expectations & Requirements Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vendor Expectations", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text("• Replacement Required: ${if (rejection.replacementRequired) "YES" else "NO"}", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Text("• Return Required: ${if (rejection.returnRequired) "YES" else "NO"}", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Text("• Credit Note Required: ${if (rejection.creditRequired) "YES" else "NO"}", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                }
            }

            // Vendor Response Section
            rejection.vendorResponse?.let { resp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Vendor Response", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(resp, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                    }
                }
            }

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onCreateCapaClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create CAPA Plan", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRaiseDisputeClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Raise Dispute", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
