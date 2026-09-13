package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Container box for rendering icons with semantic background tint, border, and size controls.
 *
 * @param icon ImageVector icon to render.
 * @param modifier Optional modifier.
 * @param iconTint Color tint applied to the icon.
 * @param containerColor Background color box tint.
 * @param borderColor Border stroke color.
 * @param boxSize Overall size of the icon container box.
 * @param iconSize Size of the inner icon.
 */
@Composable
fun AdminIconContainer(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = AdminTheme.colors.accentPrimary,
    containerColor: Color = AdminTheme.colors.accentContainer,
    borderColor: Color = AdminTheme.colors.border,
    boxSize: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    Surface(
        modifier = modifier.size(boxSize),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
