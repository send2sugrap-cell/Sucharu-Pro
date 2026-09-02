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
import com.sucharu.sucharupro.data.api.model.RespondQualityRequestDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalQualityResponseScreen(
    inspectionId: String,
    rejectionId: String? = null,
    onSubmitResponse: (RespondQualityRequestDto) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var responseType by remember { mutableStateOf("PROPOSE_CORRECTIVE_ACTION") }
    var comment by remember { mutableStateOf("") }
    var correctiveActionPlan by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Submit Quality Response",
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Text("Response Action", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("PROPOSE_CORRECTIVE_ACTION", "COMMIT_REPLACEMENT", "REQUEST_DISPUTE").forEach { type ->
                                FilterChip(
                                    selected = responseType == type,
                                    onClick = { responseType = type },
                                    label = { Text(type.replace("_", " "), fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2563EB),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Vendor Comments / Explanation") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }

                    if (responseType == "PROPOSE_CORRECTIVE_ACTION") {
                        item {
                            OutlinedTextField(
                                value = correctiveActionPlan,
                                onValueChange = { correctiveActionPlan = it },
                                label = { Text("Corrective & Preventive Action (CAPA) Plan") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Button(
                        onClick = {
                            val req = RespondQualityRequestDto(
                                rejectionId = rejectionId,
                                responseType = responseType,
                                comment = comment,
                                correctiveActionPlan = correctiveActionPlan.takeIf { it.isNotBlank() },
                                promisedReplacementDate = if (responseType == "COMMIT_REPLACEMENT") System.currentTimeMillis() + 86400000L * 7 else null
                            )
                            onSubmitResponse(req)
                        },
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
