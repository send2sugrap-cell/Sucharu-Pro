package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Structured Admin Section container with title, subtitle, optional action slot, and content spacing.
 *
 * @param title Section header title.
 * @param subtitle Optional descriptive subtitle.
 * @param modifier Optional modifier.
 * @param actionSlot Optional trailing action control (e.g. "View All" button or filter dropdown).
 * @param content Section body content.
 */
@Composable
fun AdminSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionSlot: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AdminTheme.spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AdminTheme.typography.sectionTitle,
                    color = AdminTheme.colors.primaryText
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = AdminTheme.typography.caption,
                        color = AdminTheme.colors.secondaryText
                    )
                }
            }
            if (actionSlot != null) {
                actionSlot()
            }
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
        content()
    }
}
