package com.teumta.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaShapes
import com.teumta.core.designsystem.theme.TeumtaTheme

// Figma: SearchBar (State=Default). 홈에서 누르면 장소 검색(S-03)으로 가요
@Composable
fun HomeSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "들를 곳 검색",
) {
    val colors = TeumtaTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TeumtaDimens.SizeSearch)
            .semantics { role = Role.Button },
        shape = TeumtaShapes.Md,
        color = colors.bgField,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TeumtaDimens.Space12),
            horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(TeumtaIcons.Search, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(TeumtaDimens.SizeIcon20))
            Text(placeholder, style = TeumtaTheme.typography.body, color = colors.textTertiary)
        }
    }
}
