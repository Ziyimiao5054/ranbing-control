@file:OptIn(ExperimentalMaterial3Api::class)

package com.anemoventi.ranbing.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anemoventi.ranbing.data.AppSettings
import com.anemoventi.ranbing.data.SettingsState
import com.anemoventi.ranbing.data.ThemeMode
import com.anemoventi.ranbing.data.UiStyle
import com.anemoventi.ranbing.ui.navigation.LocalNavigator
import com.anemoventi.ranbing.ui.theme.LocalUseMiuix
import com.anemoventi.ranbing.ui.widgets.SkinCard
import com.anemoventi.ranbing.ui.widgets.SkinChoiceRow
import com.anemoventi.ranbing.ui.widgets.SkinScaffold
import com.anemoventi.ranbing.ui.widgets.SkinSectionTitle
import com.anemoventi.ranbing.ui.widgets.SkinSwitchRow
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

private val AccentOptions = listOf(
    0xFF287AF9,
    0xFF00C2B8,
    0xFF36D167,
    0xFF8B5CF6,
    0xFFF59E0B,
    0xFFEC4899,
)

@Composable
fun ThemePage(
    prefs: SettingsState,
    settings: AppSettings,
) {
    val navigator = LocalNavigator.current
    SkinScaffold(title = "主题设置", onBack = { navigator.pop() }) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkinSectionTitle("主题风格")
            SkinChoiceRow(
                title = "跟随系统",
                summary = "HyperOS / MIUI 使用 MIUIx，其它系统使用 Material 3",
                selected = prefs.uiStyle == UiStyle.AUTO,
                onClick = { settings.setUiStyle(UiStyle.AUTO) },
            )
            SkinChoiceRow(
                title = "Google Material 3",
                summary = "标准 Material Design 界面",
                selected = prefs.uiStyle == UiStyle.MATERIAL3,
                onClick = { settings.setUiStyle(UiStyle.MATERIAL3) },
            )
            SkinChoiceRow(
                title = "MIUIx",
                summary = "类 HyperOS 风格界面",
                selected = prefs.uiStyle == UiStyle.MIUIX,
                onClick = { settings.setUiStyle(UiStyle.MIUIX) },
            )

            SkinSectionTitle("深色模式")
            SkinChoiceRow(
                title = "跟随系统",
                selected = prefs.themeMode == ThemeMode.SYSTEM,
                onClick = { settings.setThemeMode(ThemeMode.SYSTEM) },
            )
            SkinChoiceRow(
                title = "浅色",
                selected = prefs.themeMode == ThemeMode.LIGHT,
                onClick = { settings.setThemeMode(ThemeMode.LIGHT) },
            )
            SkinChoiceRow(
                title = "深色",
                selected = prefs.themeMode == ThemeMode.DARK,
                onClick = { settings.setThemeMode(ThemeMode.DARK) },
            )

            SkinSectionTitle("主题色")
            SkinSwitchRow(
                title = "动态取色",
                summary = "跟随壁纸取色",
                checked = prefs.dynamicColor,
                onCheckedChange = { settings.setDynamicColor(it) },
            )
            SkinCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AccentOptions.forEach { color ->
                        AccentSwatch(
                            color = color,
                            selected = prefs.accentColor == color,
                            onClick = {
                                settings.setAccentColor(color)
                                if (prefs.dynamicColor) settings.setDynamicColor(false)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(color: Long, selected: Boolean, onClick: () -> Unit) {
    val ring = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.onSurface
    } else {
        M3MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = ring.copy(alpha = if (selected) 1f else 0.25f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            if (LocalUseMiuix.current) {
                MiuixIcon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White,
                )
            } else {
                M3Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White,
                )
            }
        }
    }
}
