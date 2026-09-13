package com.sucharu.sucharupro.ui.admin.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Reusable Breadcrumb path component for page navigation context.
 *
 * @param items List of breadcrumb labels in hierarchical order (e.g. ["Production", "Job Cards", "Job #123"]).
 * @param modifier Optional modifier.
 * @param onItemClick Optional click handler when a parent breadcrumb item is clicked.
 */
@Composable
fun AdminBreadcrumb(
    items: List<String>,
    modifier: Modifier = Modifier,
    onItemClick: ((index: Int) -> Unit)? = null
) {
    if (items.isEmpty()) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            val textColor = if (isLast) AdminTheme.colors.primaryText else AdminTheme.colors.secondaryText

            Text(
                text = item,
                style = AdminTheme.typography.caption,
                color = textColor,
                modifier = if (!isLast && onItemClick != null) Modifier.clickable { onItemClick(index) } else Modifier
            )

            if (!isLast) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AdminTheme.colors.mutedText,
                    modifier = Modifier.width(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
