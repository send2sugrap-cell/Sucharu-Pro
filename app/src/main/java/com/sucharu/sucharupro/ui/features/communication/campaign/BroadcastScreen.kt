package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.campaign.BroadcastMessage
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignPriority

@Composable
fun BroadcastScreen(
    viewModel: BroadcastViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(CampaignBg)) {
        CampaignTopBar(
            title = "Instant Broadcast",
            onBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compose Instant Broadcast Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CampaignSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Podcasts, contentDescription = null, tint = CampaignAccentAmber)
                            Text("Send Urgent / Priority Broadcast", color = CampaignTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Broadcast Title") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CampaignAccent,
                                unfocusedBorderColor = CampaignBorder,
                                focusedTextColor = CampaignTextPrimary,
                                unfocusedTextColor = CampaignTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Broadcast Message") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CampaignAccent,
                                unfocusedBorderColor = CampaignBorder,
                                focusedTextColor = CampaignTextPrimary,
                                unfocusedTextColor = CampaignTextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                if (title.isNotBlank() && message.isNotBlank()) {
                                    viewModel.sendBroadcast(
                                        title = title,
                                        message = message,
                                        priority = CampaignPriority.URGENT
                                    )
                                    title = ""
                                    message = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CampaignAccentAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSending && title.isNotBlank() && message.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("Dispatch Broadcast", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Past Broadcast History
            item {
                Text("Broadcast History", color = CampaignTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            if (state.broadcasts.isEmpty()) {
                item {
                    Text("No past broadcasts found.", color = CampaignTextSecondary, fontSize = 12.sp)
                }
            } else {
                items(state.broadcasts) { brd ->
                    BroadcastCard(broadcast = brd)
                }
            }
        }
    }
}

@Composable
fun BroadcastCard(broadcast: BroadcastMessage) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CampaignSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(broadcast.broadcastNo, color = CampaignTextSecondary, fontSize = 12.sp)
                CampaignPriorityBadge(broadcast.priority)
            }
            Text(broadcast.title, color = CampaignTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(broadcast.message, color = CampaignTextSecondary, fontSize = 13.sp)
        }
    }
}
