@file:OptIn(ExperimentalMaterial3Api::class)

package com.anemoventi.ranbing.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anemoventi.ranbing.BuildConfig
import com.anemoventi.ranbing.ui.navigation.LocalNavigator
import com.anemoventi.ranbing.ui.theme.LocalUseMiuix
import com.anemoventi.ranbing.ui.widgets.SkinCard
import com.anemoventi.ranbing.ui.widgets.SkinScaffold
import com.anemoventi.ranbing.ui.widgets.SkinSectionTitle
import com.anemoventi.ranbing.ui.widgets.SkinText
import com.anemoventi.ranbing.ui.widgets.SkinTextKind

@Composable
fun AboutPage() {
    val navigator = LocalNavigator.current
    SkinScaffold(title = "关于", onBack = { navigator.pop() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!LocalUseMiuix.current) {
                SkinSectionTitle("应用")
            }
            SkinCard {
                SkinText("燃冰散热器控制", SkinTextKind.TITLE)
                SkinText("版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", SkinTextKind.CAPTION)
                SkinText(
                    "用于管理 X8 / X8U 系列半导体制冷散热器的 Android 客户端。",
                    SkinTextKind.BODY,
                )
            }

        }
    }
}
