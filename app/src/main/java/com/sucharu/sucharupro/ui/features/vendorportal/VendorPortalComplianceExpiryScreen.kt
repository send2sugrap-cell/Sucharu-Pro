package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalCertificationExpiryAlertDto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalComplianceExpiryScreen(
    expiries: List<VendorPortalCertificationExpiryAlertDto>,
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compliance Expiry & Renewal Alerts",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Back", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (expiries.isEmpty()) {
                item {
                    EmptySectionPlaceholder(message = "No expiring certifications or compliance items.")
                }
            } else {
                items(expiries) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (exp.alertLevel) {
                                "EXPIRED" -> Color(0x33EF4444)
                                "CRITICAL_7_DAYS" -> Color(0x33F97316)
                                "WARNING_30_DAYS" -> Color(0x22F59E0B)
                                else -> Color(0xFF1E293B)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exp.certificationName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = exp.alertLevel,
                                    color = when (exp.alertLevel) {
                                        "EXPIRED" -> Color(0xFFEF4444)
                                        "CRITICAL_7_DAYS" -> Color(0xFFF97316)
                                        "WARNING_30_DAYS" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Code: ${exp.requirementCode} • Expiry: ${dateFormat.format(Date(exp.expiryDate))}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (exp.daysRemaining < 0) "Expired ${-exp.daysRemaining} days ago" else "${exp.daysRemaining} days remaining for renewal",
                                color = if (exp.daysRemaining <= 7) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
