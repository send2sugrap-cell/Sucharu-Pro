package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.sucharu.sucharupro.data.api.model.VendorPortalNotificationDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalNotificationDetailsScreen(
    notification: VendorPortalNotificationDto,
    onNavigateDeepLink: (String) -> Unit = {},
    onArchiveClick: () -> Unit = {},
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
                        text = "Notification Details",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Badge(containerColor = Color(0xFF3B82F6)) {
                            Text(
                                text = notification.category,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Badge(
                            containerColor = when (notification.severity) {
                                "CRITICAL", "URGENT" -> Color(0xFFEF4444)
                                "HIGH" -> Color(0xFFF59E0B)
                                else -> Color(0xFF10B981)
                            }
                        ) {
                            Text(
                                text = notification.severity,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = notification.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = notification.message,
                        fontSize = 15.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 22.sp
                    )

                    if (notification.relatedEntityType != null && notification.relatedEntityId != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Related Entity: ${notification.relatedEntityType} (${notification.relatedEntityId})",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            val deepLink = notification.deepLinkTarget
            if (deepLink != null) {
                Button(
                    onClick = { onNavigateDeepLink(deepLink) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(text = "Go to Associated Document / Workspace", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onArchiveClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text(text = "Archive Notification")
            }
        }
    }
}
