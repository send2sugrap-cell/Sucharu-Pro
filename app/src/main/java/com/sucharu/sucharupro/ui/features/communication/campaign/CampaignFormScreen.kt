package com.sucharu.sucharupro.ui.features.communication.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignPriority
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignFormScreen(
    viewModel: CampaignFormViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSuccess()
    }

    Column(modifier = Modifier.fillMaxSize().background(CampaignBg)) {
        CampaignTopBar(
            title = "New Campaign",
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Campaign Details", color = CampaignTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CampaignAccent,
                    unfocusedBorderColor = CampaignBorder,
                    focusedTextColor = CampaignTextPrimary,
                    unfocusedTextColor = CampaignTextPrimary
                )
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CampaignAccent,
                    unfocusedBorderColor = CampaignBorder,
                    focusedTextColor = CampaignTextPrimary,
                    unfocusedTextColor = CampaignTextPrimary
                )
            )

            // Campaign Type Dropdown
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.campaignType.defaultLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Campaign Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CampaignAccent,
                        unfocusedBorderColor = CampaignBorder,
                        focusedTextColor = CampaignTextPrimary,
                        unfocusedTextColor = CampaignTextPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                    modifier = Modifier.background(CampaignSurface)
                ) {
                    CampaignType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.defaultLabel, color = CampaignTextPrimary) },
                            onClick = {
                                viewModel.updateType(type)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Audience Type Dropdown
            var audienceExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = audienceExpanded,
                onExpandedChange = { audienceExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.audienceType.defaultLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Audience") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audienceExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CampaignAccent,
                        unfocusedBorderColor = CampaignBorder,
                        focusedTextColor = CampaignTextPrimary,
                        unfocusedTextColor = CampaignTextPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = audienceExpanded,
                    onDismissRequest = { audienceExpanded = false },
                    modifier = Modifier.background(CampaignSurface)
                ) {
                    CampaignAudienceType.entries.forEach { aud ->
                        DropdownMenuItem(
                            text = { Text(aud.defaultLabel, color = CampaignTextPrimary) },
                            onClick = {
                                viewModel.updateAudienceType(aud)
                                audienceExpanded = false
                            }
                        )
                    }
                }
            }

            // Priority Dropdown
            var priorityExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.priority.defaultLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CampaignAccent,
                        unfocusedBorderColor = CampaignBorder,
                        focusedTextColor = CampaignTextPrimary,
                        unfocusedTextColor = CampaignTextPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false },
                    modifier = Modifier.background(CampaignSurface)
                ) {
                    CampaignPriority.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.defaultLabel, color = CampaignTextPrimary) },
                            onClick = {
                                viewModel.updatePriority(p)
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.content,
                onValueChange = { viewModel.updateContent(it) },
                label = { Text("Content / Notification Body *") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CampaignAccent,
                    unfocusedBorderColor = CampaignBorder,
                    focusedTextColor = CampaignTextPrimary,
                    unfocusedTextColor = CampaignTextPrimary
                )
            )

            if (state.error != null) {
                Text(state.error!!, color = CampaignAccentRed, fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.submit() },
                colors = ButtonDefaults.buttonColors(containerColor = CampaignAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !state.isSubmitting
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Text("Create Campaign (Draft)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
