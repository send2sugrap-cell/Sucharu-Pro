package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sucharu.sucharupro.data.api.model.VendorPortalCorrectiveActionSummaryDto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalCorrectiveActionDetailsScreen(
    action: VendorPortalCorrectiveActionSummaryDto,
    onProgressUpdateClick: () -> Unit = {},
    onRequestCompletionClick: () -> Unit = {},
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
                        text = "CAPA Action Details",
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
                actions = {
                    Button(
                        onClick = onProgressUpdateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Update Progress", color = Color.White, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onRequestCompletionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Request Verification", color = Color.White, fontSize = 13.sp)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CAPA #${action.actionId.take(12)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = action.status,
                                color = if (action.isOverdue) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Due Date: ${dateFormat.format(Date(action.dueDate))} • Priority: ${action.priority}",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )

                        Divider(color = Color(0xFF334155))

                        Text("Issue Description:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(action.issueDescription, color = Color.White, fontSize = 14.sp)

                        val rootCause = action.rootCause
                        if (!rootCause.isNullOrBlank()) {
                            Text("Root Cause:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(rootCause, color = Color(0xFFCBD5E1), fontSize = 14.sp)
                        }

                        Text("Action Plan:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(action.actionPlan, color = Color(0xFFCBD5E1), fontSize = 14.sp)

                        val latestResponse = action.latestVendorResponse
                        if (!latestResponse.isNullOrBlank()) {
                            Divider(color = Color(0xFF334155))
                            Text("Latest Vendor Response:", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(latestResponse, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
