package com.sucharu.sucharupro.ui.features.communication.vendor.document

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.vendor.document.*

private val BgColor = Color(0xFF0F172A)
private val AccentColor = Color(0xFF38BDF8)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)

@Composable
fun VendorDocumentRequestListScreen(
    viewModel: VendorDocumentRequestListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToNewRequest: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(
            title = "Document Requests",
            onBack = onNavigateBack,
            actions = {
                IconButton(onClick = onNavigateToNewRequest) {
                    Icon(Icons.Default.Add, contentDescription = "New Request", tint = AccentColor)
                }
            }
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        if (state.requests.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Inbox, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                    Text("No document requests found", color = TextSecondary, fontSize = 14.sp)
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.requests) { request ->
                VendorDocumentRequestListItem(
                    request = request,
                    onClick = { onNavigateToDetails(request.requestId) }
                )
            }
        }
    }
}

@Composable
fun VendorDocumentRequestDetailsScreen(
    requestId: String,
    viewModel: VendorDocumentRequestDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubmit: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(requestId) { viewModel.load(requestId) }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(title = "Request Details", onBack = onNavigateBack)

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        val request = state.request
        if (request == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Request not found", color = TextSecondary)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(request.title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(request.documentType.defaultLabel, color = AccentColor, fontSize = 13.sp)
                        if (request.description.isNotBlank()) {
                            Text(request.description, color = TextSecondary, fontSize = 13.sp)
                        }
                        HorizontalDivider(color = Color(0xFF334155))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Status", color = TextSecondary, fontSize = 12.sp)
                            Text(request.status.defaultLabel, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Required", color = TextSecondary, fontSize = 12.sp)
                            Text(if (request.required) "Yes" else "No", color = TextPrimary, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Request No.", color = TextSecondary, fontSize = 12.sp)
                            Text(request.requestNo, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            item {
                if (!request.status.isTerminal) {
                    Button(
                        onClick = { onNavigateToSubmit(request.requestId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Submit Document", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
