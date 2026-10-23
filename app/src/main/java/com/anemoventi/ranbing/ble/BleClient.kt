package com.anemoventi.ranbing.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** One radiator (or another BLE peripheral) seen while scanning. */
data class DiscoveredDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val model: String = "",
    val firmwareVersion: String = "",
    val category: Int = 0,
    val advertisedMac: String = "",
    val serviceUuids: List<String> = emptyList(),
    val lastSeenAt: Long = System.currentTimeMillis(),
)

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val address: String, val name: String) : ConnectionState
    data class Disconnected(val reason: String?) : ConnectionState
    data class Failed(val message: String) : ConnectionState
}

/**
 * Thin native BLE central: scans for the radiator advertisement, connects, discovers the FFE0
 * service with FFE3 (write) / FFE4 (notify), negotiates MTU and serializes writes into 50 ms
 * spaced chunks (falling back to 20 byte chunks when the firmware rejects the negotiated size).
 */
@SuppressLint("MissingPermission")
class BleClient(context: Context) {

    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = manager?.adapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _notifications = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val notifications: SharedFlow<ByteArray> = _notifications.asSharedFlow()

    private val _log = MutableSharedFlow<String>(extraBufferCapacity = 128)
    val log: SharedFlow<String> = _log.asSharedFlow()

    private val found = ConcurrentHashMap<String, DiscoveredDevice>()
    private var scanActive = false
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null
    private var chunkSize = DEFAULT_CHUNK
    private var connectTimeoutJob: Job? = null

    private val writeQueue = Channel<WriteRequest>(Channel.UNLIMITED)

    @Volatile
    private var pendingWrite: CompletableDeferred<Boolean>? = null

    @Volatile
    private var pendingDescriptor: CompletableDeferred<Boolean>? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::onResult)
        }

        override fun onScanFailed(errorCode: Int) {
            scanActive = false
            _scanning.value = false
            emitLog("扫描失败 errorCode=$errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            // Ignore callbacks from a GATT instance we already replaced/closed.
            if (g !== gatt) {
                runCatching { g.close() }
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectTimeoutJob?.cancel()
                    emitLog("已连接，发现服务…")
                    g.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasConnected = _state.value is ConnectionState.Connected
                    closeGatt()
                    _state.value = ConnectionState.Disconnected(
                        when {
                            !wasConnected -> null
                            status == BluetoothGatt.GATT_SUCCESS -> "连接已断开"
                            else -> "连接断开 status=$status"
                        },
                    )
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (g !== gatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail(g, "服务发现失败 status=$status")
                return
            }
            val service = g.services.firstOrNull { it.uuid.toString().shortUuid16() == 0xFFE0 }
            if (service == null) {
                fail(g, "未找到 FFE0 服务")
                return
            }
            writeChar = service.characteristics.firstOrNull { it.uuid.toString().shortUuid16() == 0xFFE3 }
            notifyChar = service.characteristics.firstOrNull { it.uuid.toString().shortUuid16() == 0xFFE4 }
            if (writeChar == null || notifyChar == null) {
                fail(g, "缺少 FFE3/FFE4 特征值")
                return
            }
            emitLog(
                "FFE3 props=0x%02X FFE4 props=0x%02X".format(
                    writeChar?.properties ?: 0,
                    notifyChar?.properties ?: 0,
                ),
            )
            emitLog(
                if ((writeChar?.properties ?: 0) and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                    "写入类型 with-response"
                } else {
                    "写入类型 no-response"
                },
            )
            g.setCharacteristicNotification(notifyChar, true)
            val descriptor = notifyChar?.getDescriptor(CCCD)
            if (descriptor != null) {
                val supportsNotify =
                    (notifyChar?.properties ?: 0) and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                @Suppress("DEPRECATION")
                descriptor.value = if (supportsNotify) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                emitLog(if (supportsNotify) "订阅 notification" else "订阅 indication")
                @Suppress("DEPRECATION")
                g.writeDescriptor(descriptor)
            } else {
                onReady(g)
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (g !== gatt) return
            emitLog("通知订阅${if (status == BluetoothGatt.GATT_SUCCESS) "成功" else "失败($status)"}")
            pendingDescriptor?.complete(status == BluetoothGatt.GATT_SUCCESS)
            onReady(g)
        }

        override fun onMtuChanged(g: BluetoothGatt, newMtu: Int, status: Int) {
            if (g !== gatt) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                chunkSize = (newMtu - 3).coerceIn(MIN_CHUNK, MAX_CHUNK)
            }
            emitLog("MTU=$newMtu 分片=$chunkSize")
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (g !== gatt) return
            android.util.Log.d("Ranbing", "RX ${value.size}B ${value.toHex()}")
            emitLog("RX ${value.size}B ${value.toHex()}")
            _notifications.tryEmit(value)
        }

        @Deprecated("Deprecated in API 33, kept for older devices")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (g !== gatt) return
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            android.util.Log.d("Ranbing", "RX ${value.size}B ${value.toHex()}")
            emitLog("RX ${value.size}B ${value.toHex()}")
            _notifications.tryEmit(value)
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (g !== gatt) return
            android.util.Log.d("Ranbing", "WRITE callback status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) emitLog("写入失败 status=$status")
            pendingWrite?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    init {
        scope.launch {
            for (request in writeQueue) {
                val ok = sendFrame(request.bytes)
                if (!request.deferred.isCompleted) request.deferred.complete(ok)
            }
        }
    }

    val isBluetoothEnabled: Boolean get() = adapter?.isEnabled == true

    fun hasPermissions(): Boolean {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return required.all { appContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    fun startScan() {
        val adapter = adapter ?: return
        if (!adapter.isEnabled || scanActive) return
        val scanner = adapter.bluetoothLeScanner ?: return
        found.clear()
        _devices.value = emptyList()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        // No hardware filter: the model is carried in service data and some firmwares omit the
        // FFE0 UUID from the advertisement packet.
        scanner.startScan(emptyList<ScanFilter>(), settings, scanCallback)
        scanActive = true
        _scanning.value = true
        emitLog("开始扫描")
    }

    fun stopScan() {
        if (!scanActive) return
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanActive = false
        _scanning.value = false
        emitLog("停止扫描")
    }

    fun connect(address: String, name: String) {
        val adapter = adapter ?: run {
            _state.value = ConnectionState.Failed("蓝牙不可用")
            return
        }
        val current = _state.value
        if (current is ConnectionState.Connected && current.address == address) {
            // Already linked; do not tear down a healthy connection.
            return
        }
        _state.value = ConnectionState.Connecting
        stopScan()
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
        if (device == null) {
            _state.value = ConnectionState.Failed("无效地址 $address")
            return
        }
        closeGatt()
        chunkSize = DEFAULT_CHUNK
        pendingDescriptor = CompletableDeferred()
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        if (gatt == null) {
            _state.value = ConnectionState.Failed("connectGatt 返回 null")
            return
        }
        connectTimeoutJob = scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (_state.value is ConnectionState.Connecting) {
                emitLog("连接超时")
                closeGatt()
                _state.value = ConnectionState.Failed("连接超时（15 秒）")
            }
        }
        emitLog("连接 $name ($address)…")
    }

    fun disconnect() {
        stopScan()
        connectTimeoutJob?.cancel()
        runCatching { gatt?.disconnect() }
        closeGatt()
        if (_state.value !is ConnectionState.Idle) {
            _state.value = ConnectionState.Disconnected("已手动断开")
        }
    }

    fun shutdown() {
        stopScan()
        closeGatt()
        scope.cancel()
    }

    /** Debug helper: force 20 byte chunks regardless of the negotiated MTU. */
    fun setFixedChunk20(enabled: Boolean) {
        chunkSize = if (enabled) MIN_CHUNK else MAX_CHUNK
        emitLog("分片大小 = $chunkSize")
    }

    val currentChunkSize: Int get() = chunkSize

    /** Queues a frame; resolves true once every chunk was accepted by the peripheral. */
    suspend fun write(bytes: ByteArray, timeoutMs: Long = 3_000): Boolean {
        val request = WriteRequest(bytes, CompletableDeferred())
        writeQueue.send(request)
        return withTimeout(timeoutMs) { request.deferred.await() }
    }

    private suspend fun sendFrame(payload: ByteArray): Boolean {
        return try {
            transmit(payload, chunkSize)
        } catch (_: ChunkRejected) {
            if (chunkSize > MIN_CHUNK) {
                emitLog("MTU 分片被拒绝，回退 20 字节分片重发")
                chunkSize = MIN_CHUNK
                try {
                    transmit(payload, chunkSize)
                } catch (_: ChunkRejected) {
                    false
                }
            } else {
                false
            }
        }
    }

    private suspend fun transmit(payload: ByteArray, size: Int): Boolean {
        val g = gatt ?: return false
        val characteristic = writeChar ?: return false
        val withResponse =
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
        var offset = 0
        while (offset < payload.size) {
            val end = minOf(offset + size, payload.size)
            val chunk = payload.copyOfRange(offset, end)
            val deferred = CompletableDeferred<Boolean>()
            pendingWrite = deferred
            val accepted = writeChunk(g, characteristic, chunk, withResponse)
            if (!accepted) throw ChunkRejected()
            if (withResponse) {
                val ok = withTimeout(2_000) { deferred.await() }
                if (!ok) throw ChunkRejected()
            } else {
                // No-response writes are fire-and-forget: give the controller time to push the
                // chunk out before queueing the next one.
                delay(if (end < payload.size) WRITE_INTERVAL_MS else NO_RESPONSE_SETTLE_MS)
            }
            offset = end
        }
        return true
    }

    private fun writeChunk(
        g: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        chunk: ByteArray,
        withResponse: Boolean,
    ): Boolean {
        val writeType = if (withResponse) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = g.writeCharacteristic(characteristic, chunk, writeType)
            android.util.Log.d("Ranbing", "write type=$writeType chunk=${chunk.size} result=$result")
            result == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = chunk
            @Suppress("DEPRECATION")
            val result = g.writeCharacteristic(characteristic)
            android.util.Log.d("Ranbing", "write type=$writeType chunk=${chunk.size} result=$result")
            result
        }
    }

    private fun onReady(g: BluetoothGatt) {
        val device = g.device
        _state.value = ConnectionState.Connected(device.address, device.name ?: "散热器")
        emitLog("服务就绪")
        g.requestMtu(REQUESTED_MTU)
    }

    private fun fail(g: BluetoothGatt, message: String) {
        emitLog(message)
        closeGatt()
        _state.value = ConnectionState.Failed(message)
    }

    private fun closeGatt() {
        connectTimeoutJob?.cancel()
        runCatching { gatt?.close() }
        gatt = null
        writeChar = null
        notifyChar = null
        pendingDescriptor?.cancel()
        pendingDescriptor = null
    }

    private fun onResult(result: ScanResult) {
        val record = result.scanRecord
        val address = result.device.address
        val name = record?.deviceName ?: result.device.name.orEmpty()
        val info = parseAdvertisement(record)
        val services = record?.serviceUuids?.map { it.uuid.toString() }.orEmpty()

        val looksLikeRadiator = info.model != null || info.firmware.isNotEmpty() ||
            services.any { it.shortUuid16() == 0xFFE0 } ||
            name.contains("X8", ignoreCase = true) ||
            name.contains("燃冰")

        if (!looksLikeRadiator) return

        found[address] = DiscoveredDevice(
            address = address,
            name = name.ifBlank { "未知设备" },
            rssi = result.rssi,
            model = info.model.orEmpty(),
            firmwareVersion = info.firmware,
            category = info.category,
            advertisedMac = info.mac,
            serviceUuids = services,
        )
        _devices.value = found.values.sortedByDescending { it.rssi }
    }

    private fun parseAdvertisement(record: ScanRecord?): AdvertInfo {
        var model: String? = null
        var firmware = ""
        var category = 0
        var mac = ""
        record?.serviceData?.forEach { (uuid, data) ->
            when (uuid.uuid.toString().shortUuid16()) {
                // "RB" little endian: category + firmware version + model string.
                0x4252, 0x5242 -> if (data.size >= 2) {
                    category = data[0].toInt() and 0xFF
                    firmware = formatVersion(data[1].toInt() and 0xFF)
                    if (data.size > 2) {
                        model = String(data, 2, data.size - 2, Charsets.US_ASCII)
                            .trim('\u0000', ' ')
                            .ifBlank { null }
                    }
                }
                // "ID": six byte little endian MAC.
                0x4449, 0x4944 -> if (data.size >= 6) {
                    mac = data.copyOfRange(0, 6).reversed().joinToString(":") { "%02X".format(it) }
                }
            }
        }
        return AdvertInfo(model, firmware, category, mac)
    }

    private fun formatVersion(raw: Int): String = "V${raw / 10}.${raw % 10}"

    private fun emitLog(message: String) {
        _log.tryEmit(message)
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    private data class WriteRequest(
        val bytes: ByteArray,
        val deferred: CompletableDeferred<Boolean>,
    )

    private data class AdvertInfo(
        val model: String?,
        val firmware: String,
        val category: Int,
        val mac: String,
    )

    private class ChunkRejected : Exception()

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val WRITE_UUID: UUID = UUID.fromString("0000FFE3-0000-1000-8000-00805F9B34FB")
        val NOTIFY_UUID: UUID = UUID.fromString("0000FFE4-0000-1000-8000-00805F9B34FB")
        private val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        private const val REQUESTED_MTU = 247
        private const val MIN_CHUNK = 20
        private const val MAX_CHUNK = 244
        private const val DEFAULT_CHUNK = 20
        private const val WRITE_INTERVAL_MS = 50L
        private const val NO_RESPONSE_SETTLE_MS = 25L
        private const val CONNECT_TIMEOUT_MS = 15_000L
    }
}

/** Extracts the 16 bit part of a standard Bluetooth base UUID (`0000xxxx-0000-1000-...`). */
private fun String.shortUuid16(): Int {
    val text = uppercase()
    return if (text.startsWith("0000") && text.endsWith("-0000-1000-8000-00805F9B34FB")) {
        text.substring(4, 8).toIntOrNull(16) ?: -1
    } else {
        -1
    }
}
