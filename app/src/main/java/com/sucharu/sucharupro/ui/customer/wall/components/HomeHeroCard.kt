package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.customer.components.StatusChip
import com.sucharu.sucharupro.ui.customer.components.SucharuWallCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Hero / Quick Overview Card for Redesigned Home / Sucharu Wall.
 */
@Composable
fun HomeHeroCard(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    activeOrderNumber: String? = "ORD-000001",
    activeOrderStatus: String? = "PRINTING",
    onTrackOrderClick: () -> Unit = {},
    onNewOrderClick: () -> Unit = {}
) {
    SucharuWallCard(
        modifier = modifier,
        containerColor = CustomerTheme.colors.surface,
        borderColor = CustomerTheme.colors.border,
        cornerRadius = CustomerTheme.spacing.offerCardCornerRadius
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${principal?.username ?: "Customer"}",
                        style = CustomerTheme.typography.sectionHeader.copy(fontWeight = FontWeight.Bold),
                        color = CustomerTheme.colors.primaryText
                    )
                    Text(
                        text = "Welcome to Sucharu Pro Printing Services",
                        style = CustomerTheme.typography.caption,
                        color = CustomerTheme.colors.secondaryText
                    )
                }

                if (!activeOrderStatus.isNullOrBlank()) {
                    StatusChip(
                        label = activeOrderStatus,
                        dotColor = CustomerTheme.colors.accentPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))

            if (!activeOrderNumber.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Order Progress",
                            style = CustomerTheme.typography.caption,
                            color = CustomerTheme.colors.secondaryText
                        )
                        Text(
                            text = activeOrderNumber,
                            style = CustomerTheme.typography.title,
                            color = CustomerTheme.colors.primaryText
                        )
                    }

                    Button(
                        onClick = onTrackOrderClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CustomerTheme.colors.accentPrimary,
                            contentColor = CustomerTheme.colors.surface
                        )
                    ) {
                        Text(
                            text = "Track Order",
                            style = CustomerTheme.typography.button
                        )
                    }
                }
            } else {
                Button(
                    onClick = onNewOrderClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CustomerTheme.colors.accentPrimary,
                        contentColor = CustomerTheme.colors.surface
                    )
                ) {
                    Text(
                        text = "New Order Quotation",
                        style = CustomerTheme.typography.button
                    )
                }
            }
        }
    }
}
