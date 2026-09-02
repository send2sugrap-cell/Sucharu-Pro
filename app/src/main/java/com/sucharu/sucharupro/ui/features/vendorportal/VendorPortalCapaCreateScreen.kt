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
import com.sucharu.sucharupro.data.api.model.VendorPortalCapaPlanCreateRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalCapaCreateScreen(
    caseId: String? = null,
    inspectionId: String? = null,
    rejectionId: String? = null,
    onSubmit: (VendorPortalCapaPlanCreateRequest) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    var title by remember { mutableStateOf("") }
    var rootCause by remember { mutableStateOf("") }
    var correctiveAction by remember { mutableStateOf("") }
    var preventiveAction by remember { mutableStateOf("") }
    var responsiblePerson by remember { mutableStateOf("") }
    var targetDays by remember { mutableStateOf("14") }
    var priority by remember { mutableStateOf("MEDIUM") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Draft CAPA Plan",
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CAPA Details", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("CAPA Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rootCause,
                        onValueChange = { rootCause = it },
                        label = { Text("Root Cause Analysis (5 Whys / Ishikawa)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    OutlinedTextField(
                        value = correctiveAction,
                        onValueChange = { correctiveAction = it },
                        label = { Text("Immediate Corrective Action") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = preventiveAction,
                        onValueChange = { preventiveAction = it },
                        label = { Text("Long-Term Preventive Action") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = responsiblePerson,
                        onValueChange = { responsiblePerson = it },
                        label = { Text("Responsible Person / Quality Lead") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetDays,
                        onValueChange = { targetDays = it },
                        label = { Text("Target Completion (Days from now)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                        val days = targetDays.toLongOrNull() ?: 14L
                        val targetDate = System.currentTimeMillis() + (days * 86400000L)
                        onSubmit(
                            VendorPortalCapaPlanCreateRequest(
                                caseId = caseId,
                                inspectionId = inspectionId,
                                rejectionId = rejectionId,
                                title = title,
                                rootCause = rootCause,
                                correctiveAction = correctiveAction,
                                preventiveAction = preventiveAction,
                                responsiblePerson = responsiblePerson,
                                targetCompletionDate = targetDate,
                                priority = priority
                            )
                        )
                    },
                    enabled = title.isNotBlank() && rootCause.isNotBlank() && correctiveAction.isNotBlank() && preventiveAction.isNotBlank() && responsiblePerson.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create CAPA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
