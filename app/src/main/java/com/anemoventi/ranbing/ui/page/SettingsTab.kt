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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anemoventi.ranbing.BuildConfig
import com.anemoventi.ranbing.data.AppSettings
import com.anemoventi.ranbing.ui.navigation.LocalNavigator
import com.anemoventi.ranbing.ui.navigation.Route
import com.anemoventi.ranbing.ui.theme.LocalUseMiuix
import com.anemoventi.ranbing.ui.widgets.SkinAlertDialog
import com.anemoventi.ranbing.ui.widgets.SkinNavRow
import com.anemoventi.ranbing.ui.widgets.SkinSectionTitle
import com.anemoventi.ranbing.ui.widgets.SkinSwitchRow

@Composable
fun SettingsTab(outerPadding: PaddingValues, settings: AppSettings) {
    val navigator = LocalNavigator.current
    val prefs by settings.state.collectAsStateWithLifecycle()
    var showWarning by remember { mutableStateOf(false) }
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

        SkinSectionTitle("实验性功能")
        SkinSwitchRow(
            title = "超频 Max（14.7 V）",
            summary = "解锁 14.7 V 超频，峰值功率约 60 W",
            checked = prefs.experimentalOverclock,
            onCheckedChange = { on ->
                if (on) showWarning = true else settings.setExperimentalOverclock(false)
            },
        )
    }

    SkinAlertDialog(
        show = showWarning,
        title = "高风险操作",
        message = "14.7 V 超频高于官方对 V1.3 的 14 V 限制，峰值功率约 60 W。\n\n" +
            "可能导致制冷片过热、充电头过载、电池或主板加速老化，甚至硬件损坏。\n\n" +
            "请确认散热和电源余量充足再开启，造成的损坏需自行承担。",
        confirmText = "我已了解，开启",
        onConfirm = {
            settings.setExperimentalOverclock(true)
            showWarning = false
        },
        onDismiss = { showWarning = false },
    )
}
