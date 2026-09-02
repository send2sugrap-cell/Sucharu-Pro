package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalAccessContextDto

/**
 * Vendor Portal Access Context Card / Header component (Module 13 Step 01).
 *
 * Displays the active vendor identity, portal role, project scope, and permitted capabilities.
 */
@Composable
fun VendorPortalAccessContextView(
    context: VendorPortalAccessContextDto,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = context.vendorName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Vendor Code: ${context.vendorCode} • Scope: ${context.projectScope}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7))
                ) {
                    Text(
                        text = context.roleDisplayName,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = Color(0xFF1E293B))

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Authorized Portal Capabilities:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                context.allowedFeatures.forEach { feature ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = feature.replace("_", " "),
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93000A).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Sign Out", fontSize = 12.sp)
                }
            }
        }
    }
}
