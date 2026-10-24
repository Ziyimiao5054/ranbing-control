@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.anemoventi.ranbing.ui.page

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.anemoventi.ranbing.data.AppSettings
import com.anemoventi.ranbing.data.SettingsState
import com.anemoventi.ranbing.device.DeviceRepository
import com.anemoventi.ranbing.overlay.OverlayService
import com.anemoventi.ranbing.ui.navigation.LocalNavigator
import com.anemoventi.ranbing.ui.navigation.Route
import com.anemoventi.ranbing.ui.navigation.rememberMainPagerState
import com.anemoventi.ranbing.ui.widgets.SkinIconAction
import com.anemoventi.ranbing.ui.widgets.SkinMainScaffold
import com.anemoventi.ranbing.ui.widgets.SkinNavigationBar
import com.anemoventi.ranbing.ui.widgets.SkinTab
import com.anemoventi.ranbing.ui.widgets.SkinTabPage

private val Tabs = listOf(
    SkinTab("控制", Icons.Rounded.DeviceThermostat),
    SkinTab("调试", Icons.Rounded.BugReport),
    SkinTab("设置", Icons.Rounded.Settings),
)

@Composable
fun MainPage(
    prefs: SettingsState,
    settings: AppSettings,
    device: DeviceRepository,
) {
    val pagerState = rememberPagerState(pageCount = { Tabs.size })
    val mainPagerState = rememberMainPagerState(pagerState)

    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    // Back on a non-first tab returns to the control tab, exactly like the reference shell.
    val navigator = LocalNavigator.current
    val atRoot = navigator.current() is Route.Main && navigator.backStackSize() == 1
    BackHandler(enabled = atRoot && mainPagerState.selectedPage != 0) {
        mainPagerState.animateToPage(0)
    }

    SkinMainScaffold(
        bottomBar = {
            SkinNavigationBar(
                tabs = Tabs,
                selectedIndex = mainPagerState.selectedPage,
                onSelect = { mainPagerState.animateToPage(it) },
            )
        },
    ) { outerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            beyondViewportPageCount = 1,
            overscrollEffect = null,
        ) { page ->
            SkinTabPage(
                title = Tabs[page].label,
                outerPadding = outerPadding,
                actions = {
                    if (page == 0) {
                        val context = LocalContext.current
                        var overlayRunning by remember { mutableStateOf(OverlayService.isRunning) }
                        SkinIconAction(
                            icon = Icons.Rounded.Layers,
                            contentDescription = if (overlayRunning) "关闭悬浮窗" else "开启悬浮窗",
                            onClick = {
                                if (!Settings.canDrawOverlays(context)) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}"),
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                } else if (OverlayService.isRunning) {
                                    context.stopService(Intent(context, OverlayService::class.java))
                                    overlayRunning = false
                                } else {
                                    context.startService(Intent(context, OverlayService::class.java))
                                    overlayRunning = true
                                }
                            },
                        )
                    }
                },
            ) {
                when (page) {
                    0 -> ControlTab(device = device, prefs = prefs, settings = settings, outerPadding = outerPadding)
                    1 -> DebugTab(device = device, outerPadding = outerPadding)
                    else -> SettingsTab(outerPadding = outerPadding, settings = settings)
                }
            }
        }
    }
}
