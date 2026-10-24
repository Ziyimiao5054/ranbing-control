@file:OptIn(ExperimentalMaterial3Api::class)

package com.anemoventi.ranbing.ui.page

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anemoventi.ranbing.ble.ConnectionState
import com.anemoventi.ranbing.ble.DiscoveredDevice
import com.anemoventi.ranbing.data.AppSettings
import com.anemoventi.ranbing.data.SettingsState
import com.anemoventi.ranbing.device.DeviceRepository
import com.anemoventi.ranbing.device.DeviceState
import com.anemoventi.ranbing.device.configFloat
import com.anemoventi.ranbing.device.configInt
import com.anemoventi.ranbing.device.float
import com.anemoventi.ranbing.device.int
import com.anemoventi.ranbing.device.long
import com.anemoventi.ranbing.protocol.DeviceModel
import com.anemoventi.ranbing.ui.theme.LocalUseMiuix
import com.anemoventi.ranbing.overlay.OverlayService
import com.anemoventi.ranbing.ui.widgets.SkinIconAction
import com.anemoventi.ranbing.ui.widgets.SkinButton
import com.anemoventi.ranbing.ui.widgets.SkinCard
import com.anemoventi.ranbing.ui.widgets.SkinInfoRow
import com.anemoventi.ranbing.ui.widgets.SkinLabeledSlider
import com.anemoventi.ranbing.ui.widgets.SkinMonoText
import com.anemoventi.ranbing.ui.widgets.SkinNavRow
import com.anemoventi.ranbing.ui.widgets.SkinNumberField
import com.anemoventi.ranbing.ui.widgets.SkinPillRow
import com.anemoventi.ranbing.ui.widgets.SkinPromptDialog
import com.anemoventi.ranbing.ui.widgets.SkinSectionTitle
import com.anemoventi.ranbing.ui.widgets.SkinSlider
import com.anemoventi.ranbing.ui.widgets.SkinStatCard
import com.anemoventi.ranbing.ui.widgets.SkinStatusCard
import com.anemoventi.ranbing.ui.widgets.SkinStatusTone
import com.anemoventi.ranbing.ui.widgets.SkinSwitchRow
import com.anemoventi.ranbing.ui.widgets.SkinText
import com.anemoventi.ranbing.ui.widgets.SkinTextKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

private val OverclockRed = Color(0xFFD70014)

@Composable
fun ControlTab(
    device: DeviceRepository,
    prefs: SettingsState,
    settings: AppSettings,
    outerPadding: PaddingValues,
) {
    val state by device.state.collectAsStateWithLifecycle()
    val devices by device.ble.devices.collectAsStateWithLifecycle()
    val scanning by device.ble.scanning.collectAsStateWithLifecycle()

    var granted by remember { mutableStateOf(device.ble.hasPermissions()) }
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        granted = device.ble.hasPermissions()
        if (granted) device.scan()
    }

    // Reconnect to the last device on cold start (the radiator has no pairing concept).
    LaunchedEffect(granted) {
        val last = prefs.lastDeviceId
        if (last != null && granted && device.ble.isBluetoothEnabled &&
            device.state.value.connection is ConnectionState.Idle
        ) {
            device.connect(
                DiscoveredDevice(
                    address = last,
                    name = prefs.lastDeviceName ?: "散热器",
                    rssi = 0,
                    model = prefs.lastDeviceModel.orEmpty(),
                    firmwareVersion = prefs.lastDeviceFirmware.orEmpty(),
                ),
            )
        }
    }

    // Persist the model once it has been resolved from the status block.
    LaunchedEffect(state.model) {
        if (state.model != DeviceModel.UNKNOWN && prefs.lastDeviceModel != state.model.modelName) {
            settings.setLastDevice(
                prefs.lastDeviceId,
                prefs.lastDeviceName,
                state.model.modelName,
                prefs.lastDeviceFirmware,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
        when (val connection = state.connection) {
            is ConnectionState.Connected -> ControlPanel(state, device, prefs.experimentalOverclock)
            is ConnectionState.Connecting -> ConnectingPanel(device)
            else -> ScanPanel(
                devices = devices,
                scanning = scanning,
                granted = granted,
                prefs = prefs,
                settings = settings,
                device = device,
                connection = connection,
                onRequestPermission = { permissionLauncher.launch(permissions) },
            )
        }
    }
    }
}

@Composable
private fun ScanPanel(
    devices: List<DiscoveredDevice>,
    scanning: Boolean,
    granted: Boolean,
    prefs: SettingsState,
    settings: AppSettings,
    device: DeviceRepository,
    connection: ConnectionState,
    onRequestPermission: () -> Unit,
) {
    val failed = connection is ConnectionState.Failed
    SkinStatusCard(
        title = if (failed) "连接失败" else "未连接散热器",
        subtitle = when {
            !granted -> "需要「附近的设备」权限"
            !device.ble.isBluetoothEnabled -> "蓝牙未开启"
            scanning -> "正在扫描附近设备…"
            failed -> (connection as ConnectionState.Failed).message
            else -> "点击开始扫描并连接"
        },
        detail = null,
        icon = Icons.Rounded.Close,
        tone = SkinStatusTone.ERROR,
        onClick = if (granted && device.ble.isBluetoothEnabled) {
            { if (scanning) device.stopScan() else device.scan() }
        } else {
            null
        },
    )

    SkinSectionTitle("连接散热器")

    when {
        !granted -> SkinCard {
            SkinText("需要蓝牙权限", SkinTextKind.TITLE)
            SkinText("扫描与连接散热器需要「附近的设备」权限。", SkinTextKind.CAPTION)
            SkinButton("授予权限", onClick = onRequestPermission)
        }

        !device.ble.isBluetoothEnabled -> SkinCard {
            SkinText("蓝牙未开启", SkinTextKind.TITLE)
            SkinText("请先在系统设置中打开蓝牙。", SkinTextKind.CAPTION)
        }

        else -> SkinButton(
            text = if (scanning) "停止扫描" else "开始扫描",
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
            onClick = { if (scanning) device.stopScan() else device.scan() },
        )
    }

    (connection as? ConnectionState.Failed)?.let {
        SkinText(
            "连接失败：${it.message}",
            SkinTextKind.CAPTION,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
    (connection as? ConnectionState.Disconnected)?.reason?.let {
        SkinText(
            "已断开：$it",
            SkinTextKind.CAPTION,
            modifier = Modifier.padding(start = 12.dp),
        )
    }

    val lastAddress = prefs.lastDeviceId
    if (!scanning && lastAddress != null && devices.none { it.address == lastAddress }) {
        SkinSectionTitle("上次连接")
        SkinCard(
            onClick = {
                device.connect(
                    DiscoveredDevice(
                        address = lastAddress,
                        name = prefs.lastDeviceName ?: "上次设备",
                        rssi = 0,
                    ),
                )
            },
        ) {
            SkinText(prefs.lastDeviceName ?: "上次设备", SkinTextKind.TITLE)
            SkinMonoText(lastAddress)
        }
    }

    if (devices.isNotEmpty()) {
        SkinSectionTitle("发现 ${devices.size} 台设备")
        devices.forEach { discovered ->
            SkinCard(
                onClick = {
                    settings.setLastDevice(
                        discovered.address,
                        discovered.name,
                        discovered.model,
                        discovered.firmwareVersion,
                    )
                    device.connect(discovered)
                },
            ) {
                SkinText(discovered.name.ifBlank { "未知设备" }, SkinTextKind.TITLE)
                val details = listOfNotNull(
                    discovered.model.ifBlank { null },
                    discovered.firmwareVersion.ifBlank { null },
                    "${discovered.rssi} dBm",
                    discovered.advertisedMac.ifBlank { null },
                ).joinToString(" · ")
                SkinText(details, SkinTextKind.CAPTION)
            }
        }
    }
}

@Composable
private fun ConnectingPanel(device: DeviceRepository) {
    SkinSectionTitle("连接中")
    SkinCard {
        SkinText("正在连接散热器…", SkinTextKind.TITLE)
        Spacer(Modifier.height(6.dp))
        SkinButton("取消", onClick = { device.disconnect() })
    }
}

@Composable
private fun ControlPanel(
    state: DeviceState,
    device: DeviceRepository,
    experimental: Boolean,
) {
    val scope = rememberCoroutineScope()
    val powerOn = state.int("device_power_on", state.configInt("device_power_on", 1)) != 0
    val fanLed = state.int("FAN_LED", state.configInt("FAN_LED", 0)) != 0
    val grade = state.int("key_grade", state.configInt("key_grade", 0))
    val tempControl = state.int("temp_control_enable", state.configInt("temp_control_enable", 0)) != 0
    val voltage = state.float("slider_voltage", 5f)
    val overclock = voltage >= 14f
    var preOverclockVoltage by remember { mutableFloatStateOf(5f) }
    var prePowerVoltage by remember { mutableFloatStateOf(5f) }
    var maxRampJob by remember { mutableStateOf<Job?>(null) }
    var maxRamping by remember { mutableStateOf(false) }
    fun stopMaxRamp(restore: Boolean) {
        maxRampJob?.cancel()
        maxRampJob = null
        maxRamping = false
        if (restore) {
            scope.launch {
                device.writeRegister("slider_voltage", preOverclockVoltage)
                device.writeRegister("key_grade", 3)
                device.writeRegister("temp_control_enable", 0)
            }
        }
    }
    val powerTarget by device.powerTarget.collectAsStateWithLifecycle()
    val powerMode = powerTarget != null
    var powerSlider by remember { mutableFloatStateOf(powerTarget ?: 0f) }
    var powerInput by remember { mutableStateOf("") }
    LaunchedEffect(powerTarget) {
        if (powerTarget != null) {
            powerSlider = powerTarget!!.coerceIn(0f, 40f)
        }
    }

    val firmwareLabel = state.firmware.ifBlank { "固件未知" }
    val gradeLabel = when {
        overclock -> "超频"
        tempControl -> "温控"
        grade in 1..3 -> "$grade 档"
        else -> "—"
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val infoBlock = state.config["upgrade_info_block"] as? ByteArray
        val configuredName = remember(infoBlock) { decodeDeviceName(infoBlock) }
        val displayName = configuredName ?: state.deviceName.ifBlank { "散热器" }
        var showRename by remember { mutableStateOf(false) }
        var renameValue by remember { mutableStateOf("") }
        var renameError by remember { mutableStateOf<String?>(null) }
        SkinStatusCard(
            title = "已连接 $displayName",
            subtitle = "$firmwareLabel · $gradeLabel",
            detail = null,
            icon = Icons.Rounded.CheckCircleOutline,
            tone = SkinStatusTone.ACTIVE,
            onClick = {
                renameValue = displayName
                renameError = null
                showRename = true
            },
        )

        SkinPromptDialog(
            show = showRename,
            title = "修改设备名称",
            value = renameValue,
            onValueChange = {
                renameValue = it
                renameError = null
            },
            confirmText = "保存",
            error = renameError,
            onConfirm = {
                scope.launch {
                    device.renameDevice(renameValue)
                        .onSuccess { showRename = false }
                        .onFailure { renameError = it.message ?: "保存失败" }
                }
            },
            onDismiss = { showRename = false },
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkinStatCard(
                label = "冷端 · °C",
                value = formatTemperature(state.float("l_temp", 25f)),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            SkinStatCard(
                label = "热端 · °C",
                value = formatTemperature(state.float("r_temp", 25f)),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkinStatCard(
                label = "功率 · W",
                value = String.format(Locale.US, "%.1f", state.float("power")),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            SkinStatCard(
                label = "输入 · V",
                value = String.format(Locale.US, "%.1f", state.float("input_voltage")),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            SkinStatCard(
                label = "电流 · A",
                value = String.format(Locale.US, "%.2f", state.float("current")),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }

    if (state.model != DeviceModel.X8U) {
        SkinSectionTitle("档位")
        SkinCard {
            SkinPillRow(
                options = listOf("1 档", "2 档", "3 档", "温控"),
                selectedIndex = when {
                    overclock -> -1
                    tempControl -> 3
                    grade in 1..3 -> grade - 1
                    else -> -1
                },
                center = true,
                enabled = !powerMode && powerOn,
            ) { index ->
                device.setPowerTarget(null)
                scope.launch {
                    if (overclock) device.writeRegister("slider_voltage", preOverclockVoltage)
                    if (index < 3) {
                        device.writeRegister("key_grade", index + 1)
                        device.writeRegister("temp_control_enable", 0)
                    } else {
                        device.writeRegister("temp_control_enable", 1)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val isMaxOverclock = voltage >= 14.5f
            val applyOverclock: (Float) -> Unit = { target ->
                stopMaxRamp(restore = false)
                device.setPowerTarget(null)
                scope.launch {
                    when {
                        overclock && abs(voltage - target) < 0.6f -> {
                            device.writeRegister("slider_voltage", preOverclockVoltage)
                            device.writeRegister("key_grade", 3)
                            device.writeRegister("temp_control_enable", 0)
                        }
                        overclock -> device.writeRegister("slider_voltage", target)
                        else -> {
                            preOverclockVoltage = voltage.coerceIn(2f, 12f)
                            device.writeRegister("slider_voltage", target)
                        }
                    }
                }
            }
            if (experimental) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkinButton(
                        text = if (overclock && !isMaxOverclock) "退出超频" else "超频",
                        modifier = Modifier.weight(1f).height(52.dp),
                        containerColor = OverclockRed,
                        enabled = !powerMode && powerOn && !isMaxOverclock && !maxRamping,
                        onClick = { applyOverclock(overclockVoltage(state.firmware)) },
                    )
                    SkinButton(
                        text = if (isMaxOverclock || maxRamping) "退出超频" else "超频Max(60w)",
                        modifier = Modifier.weight(1f).height(52.dp),
                        containerColor = Color(0xFF8B0000),
                        enabled = !powerMode && powerOn && !(overclock && !isMaxOverclock),
                        onClick = {
                            if (maxRamping || isMaxOverclock) {
                                stopMaxRamp(restore = true)
                            } else {
                                device.setPowerTarget(null)
                                preOverclockVoltage = voltage.coerceIn(2f, 12f)
                                maxRamping = true
                                maxRampJob = scope.launch {
                                    var next = voltage.coerceIn(2f, 13.5f)
                                    while (isActive && next < 14.7f) {
                                        next = (next + 0.2f).coerceAtMost(14.7f)
                                        val step = (next * 10f).roundToInt() / 10f
                                        device.writeRegister("slider_voltage", step)
                                        delay(250)
                                    }
                                    maxRamping = false
                                }
                            }
                        },
                    )
                }
            } else {
                SkinButton(
                    text = if (overclock) "退出超频" else "超频",
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    containerColor = OverclockRed,
                    enabled = !powerMode && powerOn,
                    onClick = { applyOverclock(overclockVoltage(state.firmware)) },
                )
            }
        }
    }

    SkinSectionTitle("制冷功率")
    SkinCard {
        if (tempControl) {
            val storedTemp = state.float("temp_target")
            var tempDraft by remember { mutableFloatStateOf(storedTemp) }
            var tempDragging by remember { mutableStateOf(false) }
            LaunchedEffect(storedTemp) {
                if (!tempDragging) tempDraft = storedTemp
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SkinText("目标温度", SkinTextKind.TITLE)
                SkinText(
                    String.format(Locale.US, "%.1f °C", tempDraft),
                    SkinTextKind.TITLE,
                )
            }
            SkinSlider(
                value = tempDraft,
                onValueChange = {
                    tempDragging = true
                    tempDraft = it
                },
                valueRange = -25f..25f,
                steps = 499,
                enabled = powerOn,
                onValueChangeFinished = {
                    tempDragging = false
                    scope.launch { device.writeRegister("temp_target", tempDraft) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val miuix = LocalUseMiuix.current
            var powerDragging by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SkinText("目标功率", SkinTextKind.TITLE)
                SkinText(
                    when {
                        powerTarget != null -> String.format(Locale.US, "%.0f W", powerSlider)
                        powerDragging -> String.format(Locale.US, "%.0f W", powerSlider)
                        else -> "未启用"
                    },
                    SkinTextKind.TITLE,
                )
            }
            SkinSlider(
                value = powerSlider,
                onValueChange = {
                    powerDragging = true
                    powerSlider = it
                },
                valueRange = 0f..40f,
                steps = 39,
                enabled = powerOn,
                onValueChangeFinished = {
                    powerDragging = false
                    stopMaxRamp(restore = false)
                    if (powerTarget == null) prePowerVoltage = voltage
                    device.setPowerTarget(powerSlider)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (miuix) 10.dp else 0.dp),
            )
            if (!miuix) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    SkinText("W", SkinTextKind.CAPTION)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!miuix) {
                    SkinNumberField(
                        value = powerInput,
                        onValueChange = { text ->
                            powerInput = text.filter { it.isDigit() || it == '.' }.take(4)
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = "0–40",
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                SkinButton("应用", enabled = powerOn) {
                    powerInput.toFloatOrNull()?.let {
                        stopMaxRamp(restore = false)
                        if (powerTarget == null) prePowerVoltage = voltage
                        device.setPowerTarget(it)
                    }
                }
                if (powerTarget != null) {
                    SkinButton("关闭", enabled = powerOn) {
                        device.setPowerTarget(null)
                        scope.launch { device.writeRegister("slider_voltage", prePowerVoltage) }
                    }
                }
            }
            if (powerTarget != null) {
                SkinText(
                    "实测 %.1f W · 电压 %.1f V".format(
                        state.float("power"),
                        state.float("slider_voltage", 5f),
                    ),
                    SkinTextKind.CAPTION,
                )
            }
        }
    }

    SkinSectionTitle("控制")
    SkinCard(contentPadding = 0.dp) {
        SkinSwitchRow(
            title = "电源",
            summary = if (powerOn) "已开机" else "已关机",
            checked = powerOn,
            card = false,
        ) { on ->
            if (!on) {
                device.setPowerTarget(null)
                stopMaxRamp(restore = false)
            }
            scope.launch { device.writeRegister("device_power_on", if (on) 1 else 0) }
        }
        SkinSwitchRow(
            title = "风扇灯",
            summary = "FAN_LED",
            checked = fanLed,
            card = false,
            enabled = powerOn,
        ) { on ->
            if (powerOn) {
                scope.launch { device.writeRegister("FAN_LED", if (on) 1 else 0) }
            }
        }
    }
    SkinNavRow(
        title = "断开连接",
        summary = "停止轮询并断开蓝牙",
        icon = Icons.Rounded.BluetoothDisabled,
        onClick = { device.disconnect() },
    )

    SkinSectionTitle("运行信息")
    SkinCard {
        SkinInfoRow("设备状态", if (powerOn) "运行中" else "已关机")
        SkinInfoRow("当前档位", when {
            overclock -> "超频"
            tempControl -> "温控"
            grade in 1..3 -> "$grade 档"
            else -> "—"
        })
    }

    if (state.model == DeviceModel.X8U) {
        SkinSectionTitle("风冷 / 制冷")
        SkinCard {
            SyncedSlider(
                title = "风扇 PWM",
                statusValue = state.configInt("fan_pwm").toFloat(),
                range = 0f..100f,
                steps = 99,
                format = { String.format(Locale.US, "%.0f", it) },
            ) { value -> scope.launch { device.writeRegister("fan_pwm", value.toInt()) } }
            SyncedSlider(
                title = "制冷 PWM",
                statusValue = state.configInt("pn_pwm").toFloat(),
                range = 0f..100f,
                steps = 99,
                format = { String.format(Locale.US, "%.0f", it) },
            ) { value -> scope.launch { device.writeRegister("pn_pwm", value.toInt()) } }
            SkinSwitchRow(
                title = "灯效",
                summary = "light_show_flag",
                checked = state.configInt("light_show_flag", 1) != 0,
            ) { on ->
                scope.launch { device.writeRegister("light_show_flag", if (on) 1 else 0) }
            }
            SyncedSlider(
                title = "过热保护",
                statusValue = state.configFloat("overheat_temp", 65f),
                range = 50f..85f,
                format = { String.format(Locale.US, "%.0f °C", it) },
            ) { value -> scope.launch { device.writeRegister("overheat_temp", value) } }
        }
    }
}

@Composable
private fun SyncedSlider(
    title: String,
    statusValue: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String,
    onCommit: (Float) -> Unit,
) {
    var value by remember { mutableFloatStateOf(statusValue) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(statusValue) {
        if (!dragging) value = statusValue.coerceIn(range.start, range.endInclusive)
    }
    SkinLabeledSlider(
        title = title,
        valueText = format(value),
        value = value,
        valueRange = range,
        steps = steps,
        onValueChange = {
            dragging = true
            value = it
        },
        onValueChangeFinished = {
            dragging = false
            onCommit(value)
        },
    )
}

/** The X8 stores its name at bytes 16..44 of the 256-byte info block, NUL padded. */
private fun decodeDeviceName(block: ByteArray?): String? {
    if (block == null || block.size < 45) return null
    val raw = block.copyOfRange(16, 45)
    val end = raw.indexOfFirst { it == 0.toByte() }.let { if (it < 0) raw.size else it }
    if (end == 0) return null
    return String(raw, 0, end, Charsets.UTF_8).trim().ifEmpty { null }
}

/** The mini program uses 14 V for firmware 1.03, 15 V otherwise (S(firmwareVersion)). */
private fun overclockVoltage(firmware: String): Float {
    val match = Regex("""(\d+)\.(\d+)""").find(firmware) ?: return 15f
    val code = match.groupValues[1].toInt() * 100 + match.groupValues[2].toInt()
    return if (code == 103) 14f else 15f
}

/** Unconnected NTC channels report ~-273.15 °C; show a dash instead of a bogus value. */
private fun formatTemperature(value: Float): String =
    if (value < -100f) "--" else String.format(Locale.US, "%.1f", value)
