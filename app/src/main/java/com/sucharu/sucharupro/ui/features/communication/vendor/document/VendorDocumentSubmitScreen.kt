package com.sucharu.sucharupro.ui.features.communication.vendor.document

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentType

private val BgColor = Color(0xFF0F172A)
private val SurfaceColor = Color(0xFF1E293B)
private val AccentColor = Color(0xFF38BDF8)
private val AccentGreen = Color(0xFF22D3EE)
private val AccentRed = Color(0xFFF87171)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val InputBg = Color(0xFF334155)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDocumentSubmitScreen(
    requestId: String?,
    viewModel: VendorDocumentSubmitViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(requestId) {
        if (!requestId.isNullOrBlank()) {
            // Pre-fill from linked request in a real implementation
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSuccess()
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(title = "Submit Document", onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Document Details", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            // Document Type Dropdown
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.documentType.defaultLabel,
                    onValueChange = {},
                    label = { Text("Document Type", color = TextSecondary, fontSize = 12.sp) },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentColor,
                        unfocusedBorderColor = InputBg,
                        focusedLabelColor = AccentColor,
                        cursorColor = AccentColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                    modifier = Modifier.background(SurfaceColor)
                ) {
                    VendorDocumentType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.defaultLabel, color = TextPrimary) },
                            onClick = {
                                viewModel.updateDocumentType(type)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Document Title *", color = TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = InputBg,
                    focusedLabelColor = AccentColor,
                    cursorColor = AccentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description", color = TextSecondary, fontSize = 12.sp) },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = InputBg,
                    focusedLabelColor = AccentColor,
                    cursorColor = AccentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = Color(0xFF334155))

            Text("File Reference", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            // File Reference ID
            OutlinedTextField(
                value = state.fileReferenceId,
                onValueChange = viewModel::updateFileReferenceId,
                label = { Text("File Reference ID *", color = TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = InputBg,
                    focusedLabelColor = AccentColor,
                    cursorColor = AccentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.fileName,
                onValueChange = viewModel::updateFileName,
                label = { Text("File Name", color = TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = InputBg,
                    focusedLabelColor = AccentColor,
                    cursorColor = AccentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = Color(0xFF334155))

            Text("Notes", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes", color = TextSecondary, fontSize = 12.sp) },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = InputBg,
                    focusedLabelColor = AccentColor,
                    cursorColor = AccentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.error != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentRed.copy(alpha = 0.12f)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.error!!, color = AccentRed, fontSize = 12.sp)
                    }
                }
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0F172A), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Document", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
