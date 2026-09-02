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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.vendor.document.*
import java.text.SimpleDateFormat
import java.util.*

private val BgColor = Color(0xFF0F172A)
private val SurfaceColor = Color(0xFF1E293B)
private val AccentColor = Color(0xFF38BDF8)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val AccentRed = Color(0xFFF87171)

private fun Long.toDateStr() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))

@Composable
fun VendorDocumentListScreen(
    viewModel: VendorDocumentListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(title = "All Documents", onBack = onNavigateBack)

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        if (state.documents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                    Text("No documents found", color = TextSecondary, fontSize = 14.sp)
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.documents) { doc ->
                VendorDocumentListItem(document = doc, onClick = { onNavigateToDetails(doc.documentId) })
            }
        }
    }
}

@Composable
fun VendorDocumentDetailsScreen(
    documentId: String,
    viewModel: VendorDocumentDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: (String) -> Unit,
    onNavigateToVersionHistory: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(documentId) { viewModel.load(documentId) }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(
            title = "Document Details",
            onBack = onNavigateBack,
            actions = {
                if (state.document != null) {
                    IconButton(onClick = { onNavigateToVersionHistory(documentId) }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = AccentColor)
                    }
                }
            }
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            return@Column
        }

        val doc = state.document
        if (doc == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Document not found", color = TextSecondary)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(doc.title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            VendorDocStatusBadge(status = doc.status)
                        }
                        Text(doc.documentType.defaultLabel, color = AccentColor, fontSize = 13.sp)
                        if (doc.description.isNotBlank()) {
                            Text(doc.description, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Document Info", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider(color = Color(0xFF334155))
                        InfoRow("Document No.", doc.documentNo)
                        InfoRow("Vendor", doc.vendorId)
                        InfoRow("Version", "v${doc.documentVersion}")
                        InfoRow("File", doc.fileName)
                        doc.issueDate?.let { InfoRow("Issue Date", it.toDateStr()) }
                        doc.expiryDate?.let { InfoRow("Expiry Date", it.toDateStr()) }
                        InfoRow("Submitted", doc.submittedAt?.toDateStr() ?: "—")
                        doc.approvedAt?.let { InfoRow("Approved", it.toDateStr()) }
                    }
                }
            }

            if (state.reviews.isNotEmpty()) {
                item {
                    Text("Review History", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                items(state.reviews) { review ->
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoRow("Reviewer", review.reviewedBy)
                            InfoRow("Status", review.reviewStatus.name)
                            if (review.remarks.isNotBlank()) {
                                Text("Remarks: ${review.remarks}", color = TextSecondary, fontSize = 12.sp)
                            }
                            review.rejectionReason?.let {
                                Text("Rejection: $it", color = AccentRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (doc.status == VendorDocumentStatus.SUBMITTED || doc.status == VendorDocumentStatus.UNDER_REVIEW) {
                item {
                    Button(
                        onClick = { onNavigateToReview(documentId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Review Document", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
