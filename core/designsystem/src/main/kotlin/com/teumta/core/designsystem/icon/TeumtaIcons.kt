package com.teumta.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.teumta.core.designsystem.R

// Figma 아이콘 SVG → Vector Asset (res/drawable/ic_*.xml)
object TeumtaIcons {
    val Search      : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_search)
    val Back        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_back)
    val Close       : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_close)
    val ChevronRight: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_chevron_right)
    val ChevronDown : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_chevron_down)
    val Home        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_home)
    val Sliders     : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sliders)
    val Clock       : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_clock)
    val Info        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_info)
    val Help        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_help)
    val Refresh     : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_refresh)
    val Flag        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_flag)
    val Walk        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_walk)
    val Train       : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_train)
    val Shield      : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_shield)
    val File        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_file)
    val Swap        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_swap)
    val Coffee      : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_coffee)
    val Check       : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_check)
    val Mail        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_mail)
    val Lock        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_lock)
    val Eye         : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_eye)
    val EyeOff      : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_eye_off)
    val User        : ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_user)
}
