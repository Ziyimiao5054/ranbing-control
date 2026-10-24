package com.anemoventi.ranbing.device

import android.content.Context
import com.anemoventi.ranbing.ble.BleClient
import com.anemoventi.ranbing.ble.ConnectionState
import com.anemoventi.ranbing.ble.DiscoveredDevice
import com.anemoventi.ranbing.protocol.Commands
import com.anemoventi.ranbing.protocol.DeviceModel
import com.anemoventi.ranbing.protocol.FrameCodec
import com.anemoventi.ranbing.protocol.ParsedFrame
import com.anemoventi.ranbing.protocol.RegisterDef
import com.anemoventi.ranbing.protocol.RegisterSpace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FrameDirection { TX, RX, INFO }

data class FrameLogEntry(
    val direction: FrameDirection,
    val time: Long,
    val cmd: Int = -1,
    val addr: Int = -1,
    val hex: String = "",
    val note: String = "",
)

data class DeviceState(
    val connection: ConnectionState = ConnectionState.Idle,
    val model: DeviceModel = DeviceModel.UNKNOWN,
    val deviceName: String = "",
    val status: Map<String, Any> = emptyMap(),
    val config: Map<String, Any> = emptyMap(),
    val writeCmd: Int = Commands.STATUS,
    val polling: Boolean = false,
    val reconnecting: Boolean = false,
)

fun DeviceState.int(name: String, fallback: Int = 0): Int =
    (status[name] as? Number)?.toInt() ?: fallback

fun DeviceState.float(name: String, fallback: Float = 0f): Float =
    (status[name] as? Number)?.toFloat() ?: fallback

fun DeviceState.long(name: String, fallback: Long = 0L): Long =
    (status[name] as? Number)?.toLong() ?: fallback

fun DeviceState.configInt(name: String, fallback: Int = 0): Int =
    (config[name] as? Number)?.toInt() ?: fallback

fun DeviceState.configFloat(name: String, fallback: Float = 0f): Float =
    (config[name] as? Number)?.toFloat() ?: fallback

/**
 * Owns the connected device: register snapshot, polling, writes and reconnection.
 */
class DeviceRepository(context: Context) {

    val ble = BleClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(DeviceState())
    val state: StateFlow<DeviceState> = _state.asStateFlow()

    private val _frames = MutableStateFlow<List<FrameLogEntry>>(emptyList())
    val frames: StateFlow<List<FrameLogEntry>> = _frames.asStateFlow()

    private val assembler = FrameCodec.Assembler()

    private var model: DeviceModel = DeviceModel.UNKNOWN
    private var statusValues: MutableMap<String, Any> = mutableMapOf()
    private var configValues: MutableMap<String, Any> = mutableMapOf()
    private var currentDevice: DiscoveredDevice? = null
    private var pollJob: Job? = null
    private var manualDisconnect = false
    private var reconnectAttempts = 0

    private val statusSpace: RegisterSpace get() = model.statusSpace
    private val configSpace: RegisterSpace get() = model.configSpace

    init {
        scope.launch {
            ble.notifications.collect { chunk ->
                assembler.append(chunk).forEach(::handleFrame)
            }
        }
        scope.launch {
            ble.state.collect { connection ->
                when (connection) {
                    is ConnectionState.Connected -> onConnected()
                    is ConnectionState.Disconnected -> onDisconnected(connection.reason)
                    is ConnectionState.Failed -> onDisconnected(connection.message)
                    ConnectionState.Idle -> _state.value = _state.value.copy(connection = connection)
                    ConnectionState.Connecting -> _state.value = _state.value.copy(connection = connection)
                }
            }
        }
        scope.launch {
            ble.log.collect { message -> addLog(FrameLogEntry(FrameDirection.INFO, System.currentTimeMillis(), note = message)) }
        }
    }

    fun scan() {
        if (ble.hasPermissions()) ble.startScan() else addLog(
            FrameLogEntry(FrameDirection.INFO, System.currentTimeMillis(), note = "缺少蓝牙权限"),
        )
    }

    fun stopScan() = ble.stopScan()

    fun connect(device: DiscoveredDevice) {
        currentDevice = device
        manualDisconnect = false
        reconnectAttempts = 0
        model = DeviceModel.fromName(device.model)
        resetValues()
        _state.value = DeviceState(
            connection = ConnectionState.Connecting,
            model = model,
            deviceName = device.name,
            status = statusValues.toMap(),
            config = configValues.toMap(),
            writeCmd = model.writeCmd,
        )
        ble.connect(device.address, device.name)
    }

    fun disconnect() {
        manualDisconnect = true
        pollJob?.cancel()
        ble.disconnect()
    }

    fun shutdown() {
        pollJob?.cancel()
        ble.shutdown()
        scope.cancel()
    }

    private fun onConnected() {
        reconnectAttempts = 0
        resetValuesIfEmpty()
        _state.value = _state.value.copy(
            connection = ble.state.value,
            model = model,
            status = statusValues.toMap(),
            config = configValues.toMap(),
            writeCmd = model.writeCmd,
            reconnecting = false,
        )
        addLog(FrameLogEntry(FrameDirection.INFO, System.currentTimeMillis(), note = "已连接，请求全量状态"))
        startPolling()
    }

    private fun onDisconnected(reason: String?) {
        pollJob?.cancel()
        pollJob = null
        _state.value = _state.value.copy(
            connection = ConnectionState.Disconnected(reason),
            polling = false,
        )
        addLog(FrameLogEntry(FrameDirection.INFO, System.currentTimeMillis(), note = "断开：${reason ?: "未知"}"))
        val device = currentDevice
        if (!manualDisconnect && device != null && reconnectAttempts < MAX_RECONNECT &&
            ble.state.value !is ConnectionState.Connected
        ) {
            reconnectAttempts++
            _state.value = _state.value.copy(reconnecting = true)
            scope.launch {
                delay(RECONNECT_DELAY_MS)
                if (!manualDisconnect && currentDevice === device) {
                    addLog(
                        FrameLogEntry(
                            FrameDirection.INFO,
                            System.currentTimeMillis(),
                            note = "自动重连 $reconnectAttempts/$MAX_RECONNECT",
                        ),
                    )
                    ble.connect(device.address, device.name)
                }
            }
        }
    }

    private fun resetValues() {
        statusValues = model.statusSpace.initialValues().toMutableMap()
        configValues = model.configSpace.initialValues().toMutableMap()
    }

    private fun resetValuesIfEmpty() {
        if (statusValues.isEmpty()) resetValues()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            _state.value = _state.value.copy(polling = true)
            // Immediate first read, then once per second like the mini program.
            requestAllStatus()
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                requestAllStatus()
            }
        }
    }

    suspend fun requestAllStatus() {
        // The firmware expects READ_ALL with an empty payload; sending the serialized register
        // block (what the mini program does) is silently ignored by this firmware revision.
        sendFrame(model.statusReadCmd, Commands.READ_ALL, ByteArray(0), "全量读取状态")
    }

    /** Reads the configuration block (`0x02` + READ_ALL), including the OTA info block. */
    suspend fun requestConfigAll() {
        sendFrame(model.configReadCmd, Commands.READ_ALL, ByteArray(0), "全量读取配置")
    }

    /** Debug probes used while reverse engineering the firmware. */
    suspend fun probeRead(cmd: Int, withPayload: Boolean) {
        val payload = if (withPayload) statusSpace.serializeAll(statusValues) else ByteArray(0)
        sendFrame(cmd, Commands.READ_ALL, payload, "探针 cmd=0x%02X payload=%d".format(cmd, payload.size))
    }

    fun setFixedChunk20(enabled: Boolean) = ble.setFixedChunk20(enabled)

    suspend fun sendFrame(cmd: Int, addr: Int, payload: ByteArray, note: String = "") {
        val bytes = FrameCodec.pack(cmd, addr, payload)
        addLog(FrameLogEntry(FrameDirection.TX, System.currentTimeMillis(), cmd, addr, bytes.toHex(), note))
        runCatching { ble.write(bytes) }.onFailure {
            addLog(FrameLogEntry(FrameDirection.INFO, System.currentTimeMillis(), note = "发送失败：${it.message}"))
        }
    }

    suspend fun writeRegister(name: String, value: Any) {
        val def = configSpace.def(name) ?: run {
            addLog(FrameLogEntry(FrameDirection.INFO, System.currentTimeMillis(), note = "未知寄存器 $name"))
            return
        }
        val payload = configSpace.serialize(def, value)
        sendFrame(model.writeCmd, def.addr, payload, "写 $name=$value")
        configValues[name] = value
        _state.value = _state.value.copy(config = configValues.toMap())
    }

    /** Sends an arbitrary frame typed in the debug screen. */
    suspend fun sendRawHex(hex: String): Boolean {
        val clean = hex.replace(" ", "").replace("\n", "").replace("\t", "")
        if (clean.length % 2 != 0) return false
        val bytes = runCatching {
            ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        }.getOrNull() ?: return false
        addLog(FrameLogEntry(FrameDirection.TX, System.currentTimeMillis(), hex = bytes.toHex(), note = "手动发送"))
        return runCatching { ble.write(bytes) }.getOrDefault(false)
    }

    fun changeWriteCmd(cmd: Int) {
        _state.value = _state.value.copy(writeCmd = cmd)
    }

    /** Falls back to the status block's `device_type` register when the advertisement was empty. */
    private fun resolveModelFromStatus() {
        if (model != DeviceModel.UNKNOWN) return
        val type = statusValues["device_type"] as? Int
        if (type != null) {
            model = when (type) {
                1 -> DeviceModel.X8
                else -> DeviceModel.X8
            }
        }
    }

    private fun handleFrame(frame: ParsedFrame) {
        addLog(
            FrameLogEntry(
                FrameDirection.RX,
                System.currentTimeMillis(),
                frame.cmd,
                frame.addr,
                frame.raw.toHex(),
                "cmd=0x%02X addr=0x%04X len=%d".format(frame.cmd, frame.addr, frame.payload.size),
            ),
        )
        if (frame.cmd == Commands.OTA) return

        val statusSize = statusSpace.defs.sumOf { it.byteSize }
        val configSize = configSpace.defs.sumOf { it.byteSize }

        val changed = runCatching {
            if (frame.addr == Commands.READ_ALL) {
                when {
                    frame.payload.size == statusSize -> statusSpace.applyAll(frame.payload, statusValues)
                    frame.payload.size == configSize -> configSpace.applyAll(frame.payload, configValues)
                    frame.payload.size < configSize -> statusSpace.applyAll(frame.payload, statusValues)
                    else -> configSpace.applyAll(frame.payload, configValues)
                }
            } else {
                val fromStatus = statusSpace.applyOne(frame.addr, frame.payload, statusValues)
                val fromConfig = if (fromStatus == null) {
                    configSpace.applyOne(frame.addr, frame.payload, configValues)
                } else {
                    null
                }
                listOfNotNull(fromStatus, fromConfig)
            }
        }.getOrElse {
            addLog(
                FrameLogEntry(
                    FrameDirection.INFO,
                    System.currentTimeMillis(),
                    -1,
                    -1,
                    "",
                    "解析失败：${it.javaClass.simpleName} ${it.message.orEmpty()}",
                ),
            )
            emptyList()
        }

        if (changed.isNotEmpty()) {
            resolveModelFromStatus()
            _state.value = _state.value.copy(
                model = model,
                status = statusValues.toMap(),
                config = configValues.toMap(),
            )
        }
    }

    private fun addLog(entry: FrameLogEntry) {
        _frames.value = (listOf(entry) + _frames.value).take(MAX_LOG)
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    private companion object {
        const val POLL_INTERVAL_MS = 1_000L
        const val RECONNECT_DELAY_MS = 2_000L
        const val MAX_RECONNECT = 15
        const val MAX_LOG = 300
    }
}
