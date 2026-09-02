package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalCorrectiveActionResponseRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalCapaResponseScreen(
    actionId: String,
    onSubmitResponse: (VendorPortalCorrectiveActionResponseRequest) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var remediationNotes by remember { mutableStateOf("") }
    var rootCauseExplanation by remember { mutableStateOf("") }
    var progressPercentage by remember { mutableStateOf(50f) }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Update CAPA Progress",
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "CAPA Progress for #${actionId.take(12)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Progress: ${progressPercentage.toInt()}%",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = progressPercentage,
                            onValueChange = { progressPercentage = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF0284C7),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )

                        OutlinedTextField(
                            value = rootCauseExplanation,
                            onValueChange = { rootCauseExplanation = it },
                            label = { Text("Root Cause Analysis (Vendor View)", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        OutlinedTextField(
                            value = remediationNotes,
                            onValueChange = { remediationNotes = it },
                            label = { Text("Remediation Progress Notes", color = Color(0xFF94A3B8)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        Button(
                            onClick = {
                                if (remediationNotes.isNotBlank()) {
                                    onSubmitResponse(
                                        VendorPortalCorrectiveActionResponseRequest(
                                            remediationNotes = remediationNotes,
                                            rootCauseExplanation = rootCauseExplanation.ifBlank { null },
                                            progressPercentage = progressPercentage.toDouble()
                                        )
                                    )
                                }
                            },
                            enabled = remediationNotes.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Submit Progress Update", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
