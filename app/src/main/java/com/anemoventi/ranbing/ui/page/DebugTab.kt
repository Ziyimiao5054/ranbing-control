@file:OptIn(ExperimentalMaterial3Api::class)

package com.anemoventi.ranbing.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anemoventi.ranbing.device.DeviceRepository
import com.anemoventi.ranbing.device.FrameDirection
import com.anemoventi.ranbing.device.FrameLogEntry
import com.anemoventi.ranbing.ui.theme.LocalUseMiuix
import com.anemoventi.ranbing.ui.widgets.SkinCard
import com.anemoventi.ranbing.ui.widgets.SkinMonoText
import com.anemoventi.ranbing.ui.widgets.SkinSectionTitle
import com.anemoventi.ranbing.ui.widgets.SkinText
import com.anemoventi.ranbing.ui.widgets.SkinTextKind

@Composable
fun DebugTab(
    device: DeviceRepository,
    outerPadding: PaddingValues,
) {
    val frames by device.frames.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            if (LocalUseMiuix.current) 12.dp else 4.dp,
        ),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = outerPadding.calculateBottomPadding() + 8.dp,
        ),
    ) {
        item { SkinSectionTitle("收发帧 · ${frames.size}") }
        items(frames) { entry -> FrameRow(entry) }
    }
}

@Composable
private fun FrameRow(entry: FrameLogEntry) {
    SkinCard {
        val prefix = when (entry.direction) {
            FrameDirection.TX -> "→ 发送"
            FrameDirection.RX -> "← 接收"
            FrameDirection.INFO -> "· 信息"
        }
        SkinText(prefix, SkinTextKind.CAPTION)
        if (entry.cmd >= 0) {
            SkinMonoText(
                "cmd=0x%02X  addr=0x%04X  len=%d".format(
                    entry.cmd,
                    entry.addr,
                    entry.hex.split(" ").size,
                ),
            )
        }
        if (entry.hex.isNotBlank()) SkinMonoText(entry.hex)
        if (entry.note.isNotBlank()) SkinText(entry.note, SkinTextKind.CAPTION)
    }
}
