@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.anemoventi.ranbing.ui.page

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.anemoventi.ranbing.data.AppSettings
import com.anemoventi.ranbing.data.SettingsState
import com.anemoventi.ranbing.device.DeviceRepository
import com.anemoventi.ranbing.ui.navigation.LocalNavigator
import com.anemoventi.ranbing.ui.navigation.Route
import com.anemoventi.ranbing.ui.navigation.rememberMainPagerState
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
            SkinTabPage(title = Tabs[page].label, outerPadding = outerPadding) {
                when (page) {
                    0 -> ControlTab(device = device, prefs = prefs, settings = settings, outerPadding = outerPadding)
                    1 -> DebugTab(device = device, outerPadding = outerPadding)
                    else -> SettingsTab(outerPadding = outerPadding, settings = settings)
                }
            }
        }
    }
}
