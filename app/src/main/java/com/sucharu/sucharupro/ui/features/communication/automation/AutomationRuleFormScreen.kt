package com.sucharu.sucharupro.ui.features.communication.automation

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
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationEventType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationRuleFormScreen(
    viewModel: AutomationRuleFormViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSuccess()
    }

    Column(modifier = Modifier.fillMaxSize().background(AutoBg)) {
        AutomationTopBar(
            title = "New Automation Rule",
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Rule Parameters", color = AutoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Rule Name *") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AutoAccent,
                    unfocusedBorderColor = AutoBorder,
                    focusedTextColor = AutoTextPrimary,
                    unfocusedTextColor = AutoTextPrimary
                )
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AutoAccent,
                    unfocusedBorderColor = AutoBorder,
                    focusedTextColor = AutoTextPrimary,
                    unfocusedTextColor = AutoTextPrimary
                )
            )

            // Event Type Dropdown
            var eventExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = eventExpanded,
                onExpandedChange = { eventExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.eventType.defaultLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Triggering Event *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AutoAccent,
                        unfocusedBorderColor = AutoBorder,
                        focusedTextColor = AutoTextPrimary,
                        unfocusedTextColor = AutoTextPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = eventExpanded,
                    onDismissRequest = { eventExpanded = false },
                    modifier = Modifier.background(AutoSurface)
                ) {
                    CommunicationAutomationEventType.entries.forEach { evt ->
                        DropdownMenuItem(
                            text = { Text(evt.defaultLabel, color = AutoTextPrimary) },
                            onClick = {
                                viewModel.updateEventType(evt)
                                eventExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.titleTemplate,
                onValueChange = { viewModel.updateTitleTemplate(it) },
                label = { Text("Title Template (e.g. Order #{sourceEntityId} Update)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AutoAccent,
                    unfocusedBorderColor = AutoBorder,
                    focusedTextColor = AutoTextPrimary,
                    unfocusedTextColor = AutoTextPrimary
                )
            )

            OutlinedTextField(
                value = state.messageTemplate,
                onValueChange = { viewModel.updateMessageTemplate(it) },
                label = { Text("Message Body Template") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AutoAccent,
                    unfocusedBorderColor = AutoBorder,
                    focusedTextColor = AutoTextPrimary,
                    unfocusedTextColor = AutoTextPrimary
                )
            )

            if (state.error != null) {
                Text(state.error!!, color = AutoAccentRed, fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.submit() },
                colors = ButtonDefaults.buttonColors(containerColor = AutoAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !state.isSubmitting
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Text("Save & Activate Rule", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
