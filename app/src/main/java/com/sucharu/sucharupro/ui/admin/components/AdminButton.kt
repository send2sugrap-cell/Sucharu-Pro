package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Button variant styles for Admin Panel actions.
 */
enum class AdminButtonStyle {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT
}

/**
 * Reusable Admin Button component with accessibility min touch size >= 48dp, loading indicator, and icon support.
 *
 * @param text Button label text.
 * @param onClick Click event handler.
 * @param modifier Optional modifier.
 * @param style Button style variant.
 * @param icon Optional leading icon.
 * @param enabled Enabled state.
 * @param isLoading Displays loading spinner and disables clicks when true.
 */
@Composable
fun AdminButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AdminButtonStyle = AdminButtonStyle.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val buttonShape = RoundedCornerShape(12.dp)
    val minModifier = modifier.defaultMinSize(minHeight = AdminTheme.spacing.buttonMinHeight)

    when (style) {
        AdminButtonStyle.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled && !isLoading,
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminTheme.colors.accentPrimary,
                    contentColor = AdminTheme.colors.background,
                    disabledContainerColor = AdminTheme.colors.elevatedSurface,
                    disabledContentColor = AdminTheme.colors.mutedText
                )
            ) {
                ButtonContent(text = text, icon = icon, isLoading = isLoading, contentColor = AdminTheme.colors.background)
            }
        }

        AdminButtonStyle.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled && !isLoading,
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminTheme.colors.elevatedSurface,
                    contentColor = AdminTheme.colors.primaryText,
                    disabledContainerColor = AdminTheme.colors.surface,
                    disabledContentColor = AdminTheme.colors.mutedText
                )
            ) {
                ButtonContent(text = text, icon = icon, isLoading = isLoading, contentColor = AdminTheme.colors.primaryText)
            }
        }

        AdminButtonStyle.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled && !isLoading,
                shape = buttonShape,
                border = BorderStroke(1.dp, if (enabled) AdminTheme.colors.border else AdminTheme.colors.surface),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AdminTheme.colors.accentPrimary,
                    disabledContentColor = AdminTheme.colors.mutedText
                )
            ) {
                ButtonContent(text = text, icon = icon, isLoading = isLoading, contentColor = AdminTheme.colors.accentPrimary)
            }
        }

        AdminButtonStyle.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = minModifier,
                enabled = enabled && !isLoading,
                shape = buttonShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AdminTheme.colors.accentPrimary,
                    disabledContentColor = AdminTheme.colors.mutedText
                )
            ) {
                ButtonContent(text = text, icon = icon, isLoading = isLoading, contentColor = AdminTheme.colors.accentPrimary)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
    contentColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(AdminTheme.spacing.sm))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(AdminTheme.spacing.sm))
        }
        Text(
            text = text,
            style = AdminTheme.typography.buttonText,
            color = contentColor
        )
    }
}
