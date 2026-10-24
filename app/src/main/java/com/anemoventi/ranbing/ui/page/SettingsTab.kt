@file:OptIn(ExperimentalMaterial3Api::class)

package com.anemoventi.ranbing.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anemoventi.ranbing.BuildConfig
import com.anemoventi.ranbing.ui.navigation.LocalNavigator
import com.anemoventi.ranbing.ui.navigation.Route
import com.anemoventi.ranbing.ui.theme.LocalUseMiuix
import com.anemoventi.ranbing.ui.widgets.SkinNavRow
import com.anemoventi.ranbing.ui.widgets.SkinSectionTitle

@Composable
fun SettingsTab(outerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = outerPadding.calculateBottomPadding() + 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(
            if (LocalUseMiuix.current) 12.dp else 4.dp,
        ),
    ) {
        SkinSectionTitle("个性化")
        SkinNavRow(
            title = "外观",
            summary = "界面风格与主题模式",
            icon = Icons.Rounded.Palette,
            onClick = { navigator.push(Route.Theme) },
        )

        SkinSectionTitle("通用")
        SkinNavRow(
            title = "关于",
            summary = "版本 ${BuildConfig.VERSION_NAME} 与开源声明",
            icon = Icons.Rounded.Info,
            onClick = { navigator.push(Route.About) },
        )
    }
}
