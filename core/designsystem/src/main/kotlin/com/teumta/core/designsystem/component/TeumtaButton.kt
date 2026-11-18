package com.teumta.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaShapes
import com.teumta.core.designsystem.theme.TeumtaTheme

enum class TeumtaButtonStyle { Filled, Outlined }

// Figma: Button (Style=Filled / Outlined, State=Default / Disabled)
@Composable
fun TeumtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TeumtaButtonStyle = TeumtaButtonStyle.Filled,
    enabled: Boolean = true,
) {
    val colors = TeumtaTheme.colors
    val filled = style == TeumtaButtonStyle.Filled
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = TeumtaDimens.SizeButton),
        enabled = enabled,
        shape = TeumtaShapes.Md,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) colors.actionPrimary else colors.bgSurface,
            contentColor = if (filled) colors.textOnAccent else colors.textPrimary,
            disabledContainerColor = colors.actionDisabled,
            disabledContentColor = colors.textTertiary,
        ),
        border = if (!filled && enabled) BorderStroke(TeumtaDimens.StrokeHairline, colors.borderDefault) else null,
        contentPadding = PaddingValues(horizontal = TeumtaDimens.Space24),
    ) {
        Text(text = text, style = TeumtaTheme.typography.titleS)
    }
}
