package com.sucharu.sucharupro.ui.features.customerfinancial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.CustomerFinancialDocumentDeliveryDto
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.*

/**
 * Customer Financial Document Center, Secure Access & Delivery Management Screen (Module 14 Step 11).
 */
@Composable
fun CustomerFinancialDocumentCenterScreen(
    deliveries: List<CustomerFinancialDocumentDeliveryDto>,
    isStaffOrAdmin: Boolean = false,
    onDownloadClick: (CustomerFinancialDocumentDeliveryDto) -> Unit = {},
    onNotifyClick: (CustomerFinancialDocumentDeliveryDto) -> Unit = {},
    onRevokeClick: (CustomerFinancialDocumentDeliveryDto) -> Unit = {},
    onViewAuditClick: (CustomerFinancialDocumentDeliveryDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            DocumentCenterHeader(
                totalDocuments = deliveries.size,
                readyCount = deliveries.count { it.deliveryStatus == "READY" || it.deliveryStatus == "NOTIFIED" }
            )
        }

        if (deliveries.isEmpty()) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                        Text(
                            text = "No financial documents found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(deliveries, key = { it.deliveryId }) { doc ->
                DocumentDeliveryCard(
                    document = doc,
                    isStaffOrAdmin = isStaffOrAdmin,
                    onDownloadClick = { onDownloadClick(doc) },
                    onNotifyClick = { onNotifyClick(doc) },
                    onRevokeClick = { onRevokeClick(doc) },
                    onViewAuditClick = { onViewAuditClick(doc) }
                )
            }
        }
    }
}

@Composable
private fun DocumentCenterHeader(
    totalDocuments: Int,
    readyCount: Int
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Customer Financial Document Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Secure document delivery, auditable client access & instant notification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                DeliveryStatBox(
                    label = "Total Documents",
                    value = totalDocuments.toString(),
                    modifier = Modifier.weight(1f)
                )
                DeliveryStatBox(
                    label = "Available Downloads",
                    value = readyCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DeliveryStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DocumentDeliveryCard(
    document: CustomerFinancialDocumentDeliveryDto,
    isStaffOrAdmin: Boolean,
    onDownloadClick: () -> Unit,
    onNotifyClick: () -> Unit,
    onRevokeClick: () -> Unit,
    onViewAuditClick: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (document.documentFormat == "CSV") Icons.Default.TableChart else Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Column {
                        Text(
                            text = document.documentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Type: ${document.documentType} • Size: ${document.fileSize} B • Access: ${document.accessCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DeliveryStatusChip(status = document.deliveryStatus, isRevoked = document.isRevoked, isExpired = document.isExpired)
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(document.createdAt))
            Text(
                text = "Created: $dateStr • Checksum: ${document.checksum.take(16)}...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (document.revocationReason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Revocation Reason: ${document.revocationReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                if (document.isDownloadable) {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }

                if (isStaffOrAdmin) {
                    if (document.isDownloadable) {
                        OutlinedButton(
                            onClick = onNotifyClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Notify")
                        }
                    }

                    if (!document.isRevoked) {
                        OutlinedButton(
                            onClick = onRevokeClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revoke")
                        }
                    }

                    OutlinedButton(
                        onClick = onViewAuditClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Audit")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryStatusChip(
    status: String,
    isRevoked: Boolean,
    isExpired: Boolean
) {
    val (label, bg, fg) = when {
        isRevoked -> Triple("REVOKED", Color(0xFFFFEBEE), Color(0xFFC62828))
        isExpired -> Triple("EXPIRED", Color(0xFFFFF3E0), Color(0xFFE65100))
        status == "ACCESSED" -> Triple("ACCESSED", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        status == "NOTIFIED" -> Triple("NOTIFIED", Color(0xFFE3F2FD), Color(0xFF1565C0))
        status == "READY" -> Triple("READY", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        status == "FAILED" -> Triple("FAILED", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Triple(status, Color(0xFFF5F5F5), Color(0xFF616161))
    }

    Box(
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
