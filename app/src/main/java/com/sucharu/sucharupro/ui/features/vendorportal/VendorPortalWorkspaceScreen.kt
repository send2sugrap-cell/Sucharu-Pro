package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.VendorPortalWorkspaceDto

/**
 * Production Jetpack Compose Vendor Portal Workspace & Navigation Hub Screen (Module 13 Step 02).
 */
@Composable
fun VendorPortalWorkspaceScreen(
    workspace: VendorPortalWorkspaceDto?,
    isLoading: Boolean,
    errorMessage: String? = null,
    onNavigate: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage, color = Color(0xFFF87171))
            }
        } else if (workspace != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Vendor Workspace",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${workspace.profile.vendorName} • Role: ${workspace.portalRole}",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Text("Workspace Modules", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                items(workspace.navigationItems) { nav ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(nav.route) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(nav.label, color = Color.White, fontWeight = FontWeight.Medium)
                                Text(nav.category, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                            }
                            if (nav.badgeCount > 0) {
                                Surface(
                                    color = Color(0xFF0284C7),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "${nav.badgeCount}",
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
