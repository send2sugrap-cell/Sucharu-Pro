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
import com.sucharu.sucharupro.data.api.model.VendorPortalNotificationPreferenceDto
import com.sucharu.sucharupro.data.api.model.VendorPortalUpdateNotificationPreferencesRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalNotificationPreferencesScreen(
    preferences: VendorPortalNotificationPreferenceDto,
    onSavePreferences: (VendorPortalUpdateNotificationPreferencesRequest) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var emailEnabled by remember { mutableStateOf(preferences.emailEnabled) }
    var inAppEnabled by remember { mutableStateOf(preferences.inAppEnabled) }
    var pushEnabled by remember { mutableStateOf(preferences.pushEnabled) }
    var importantOnlyMode by remember { mutableStateOf(preferences.importantOnlyMode) }
    var minSeverity by remember { mutableStateOf(preferences.minSeverity) }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notification Preferences",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Delivery Channels",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Email Notifications", color = Color.White)
                            Switch(checked = emailEnabled, onCheckedChange = { emailEnabled = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "In-App Notification Center", color = Color.White)
                            Switch(checked = inAppEnabled, onCheckedChange = { inAppEnabled = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Push Notifications", color = Color.White)
                            Switch(checked = pushEnabled, onCheckedChange = { pushEnabled = it })
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Filtering & Severity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Important Only Mode", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Only receive Critical and Urgent priority alerts",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Switch(checked = importantOnlyMode, onCheckedChange = { importantOnlyMode = it })
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Minimum Severity: $minSeverity", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onSavePreferences(
                            VendorPortalUpdateNotificationPreferencesRequest(
                                emailEnabled = emailEnabled,
                                inAppEnabled = inAppEnabled,
                                pushEnabled = pushEnabled,
                                importantOnlyMode = importantOnlyMode,
                                disabledCategories = preferences.disabledCategories,
                                minSeverity = minSeverity
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(text = "Save Preferences", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
