package com.teumta.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaShapes
import com.teumta.core.designsystem.theme.TeumtaTheme

// Figma 컴포넌트 TextField (State=Default / Focused / Error / Disabled)
// 라벨은 상자 밖 위에 둬서 입력해도 사라지지 않아요. 오류는 빨강 2 테두리 + 아래 한 줄.
@Composable
fun TeumtaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
    errorText: String? = null,
    helperText: String? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val isError = errorText != null
    val border = when {
        isError -> colors.borderDanger
        focused -> colors.borderFocus
        else -> Color.Transparent
    }
    val container = if (focused || isError) colors.bgSurface else colors.bgField

    Column(modifier) {
        Text(
            text = label,
            style = type.label,
            color = if (enabled) colors.textSecondary else colors.textTertiary,
            modifier = Modifier.padding(start = TeumtaDimens.Space4),
        )
        Spacer(Modifier.height(TeumtaDimens.Space8))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = type.body.copy(
                color = if (enabled) colors.textPrimary else colors.textTertiary,
            ),
            cursorBrush = SolidColor(colors.borderFocus),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (errorText != null) error(errorText) },  // TalkBack이 오류를 함께 읽어요
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .heightIn(min = TeumtaDimens.SizeInput)
                        .clip(TeumtaShapes.Md)
                        .background(container)
                        .border(TeumtaDimens.StrokeThick, border, TeumtaShapes.Md)
                        .padding(start = TeumtaDimens.Space16, end = TeumtaDimens.Space4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = if (isError) colors.textDanger else colors.textSecondary,
                            modifier = Modifier.size(TeumtaDimens.SizeIcon24),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = type.body, color = colors.textTertiary)
                        }
                        innerTextField()
                    }
                    if (isPassword) {
                        // IconButton은 48dp 터치 영역을 지켜요
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) TeumtaIcons.EyeOff else TeumtaIcons.Eye,
                                contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                                tint = colors.textSecondary,
                            )
                        }
                    }
                }
            },
        )
        val bottomText = errorText ?: helperText
        if (bottomText != null) {
            Spacer(Modifier.height(TeumtaDimens.Space8))
            Row(
                modifier = Modifier.padding(start = TeumtaDimens.Space4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8),
            ) {
                if (isError) {
                    Icon(
                        imageVector = TeumtaIcons.Info,
                        contentDescription = null,
                        tint = colors.textDanger,
                        modifier = Modifier.size(TeumtaDimens.SizeIcon16),
                    )
                }
                Text(
                    text = bottomText,
                    style = type.caption,
                    color = if (isError) colors.textDanger else colors.textTertiary,
                )
            }
        }
    }
}
