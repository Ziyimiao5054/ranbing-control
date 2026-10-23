package com.anemoventi.ranbing.protocol

import java.io.ByteArrayOutputStream

/**
 * Modbus style register protocol used by the radiator firmware.
 *
 * Wire format (little endian):
 * `AA | CMD(1) | ADDR(2) | LEN(2) | PAYLOAD[LEN] | CRC16(2) | BB`
 *
 * The CRC covers everything between the header and the CRC itself (CMD .. payload).
 */
object FrameCodec {

    const val HEADER = 0xAA
    const val TAIL = 0xBB
    const val MIN_FRAME_SIZE = 10
    const val MAX_PAYLOAD = 1024

    fun crc16(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        var crc = 0xFFFF
        for (i in offset until offset + length) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor 0xA001 else crc ushr 1
            }
        }
        return crc and 0xFFFF
    }

    fun pack(cmd: Int, addr: Int, payload: ByteArray): ByteArray {
        val out = ByteArray(6 + payload.size + 3)
        out[0] = HEADER.toByte()
        out[1] = (cmd and 0xFF).toByte()
        out[2] = (addr and 0xFF).toByte()
        out[3] = ((addr ushr 8) and 0xFF).toByte()
        out[4] = (payload.size and 0xFF).toByte()
        out[5] = ((payload.size ushr 8) and 0xFF).toByte()
        payload.copyInto(out, 6)
        val crc = crc16(out, 1, 6 + payload.size - 1)
        out[6 + payload.size] = (crc and 0xFF).toByte()
        out[7 + payload.size] = ((crc ushr 8) and 0xFF).toByte()
        out[out.lastIndex] = TAIL.toByte()
        return out
    }

    /** Returns the parsed frame, or null when the frame is structurally invalid. */
    fun parse(frame: ByteArray): ParsedFrame? {
        if (frame.size < MIN_FRAME_SIZE) return null
        if (frame[0].toInt() and 0xFF != HEADER) return null
        if (frame[frame.size - 1].toInt() and 0xFF != TAIL) return null

        val cmd = frame[1].toInt() and 0xFF
        val addr = (frame[2].toInt() and 0xFF) or ((frame[3].toInt() and 0xFF) shl 8)
        val len = (frame[4].toInt() and 0xFF) or ((frame[5].toInt() and 0xFF) shl 8)
        if (6 + len + 3 != frame.size) return null
        val crc = (frame[6 + len].toInt() and 0xFF) or ((frame[7 + len].toInt() and 0xFF) shl 8)
        if (crc16(frame, 1, 5 + len) != crc) return null
        return ParsedFrame(cmd, addr, frame.copyOfRange(6, 6 + len), frame.copyOf())
    }

    /**
     * Reassembles BLE notification chunks into frames. BLE splits a frame over several packets
     * (MTU 20 fallback), and the firmware may glue several frames into one notification.
     */
    class Assembler {
        private val buffer = ByteArrayOutputStream()

        fun reset() = buffer.reset()

        fun append(chunk: ByteArray): List<ParsedFrame> {
            buffer.write(chunk)
            val frames = mutableListOf<ParsedFrame>()
            while (true) {
                var bytes = buffer.toByteArray()
                val headerIndex = bytes.indexOfFirst { it.toInt() and 0xFF == HEADER }
                if (headerIndex < 0) {
                    buffer.reset()
                    break
                }
                if (headerIndex > 0) {
                    bytes = bytes.copyOfRange(headerIndex, bytes.size)
                    replaceBuffer(bytes)
                }
                if (bytes.size < 6) break

                val len = (bytes[4].toInt() and 0xFF) or ((bytes[5].toInt() and 0xFF) shl 8)
                if (len > MAX_PAYLOAD) {
                    dropFirstByte()
                    continue
                }
                val total = 6 + len + 3
                if (bytes.size < total) break

                if (bytes[total - 1].toInt() and 0xFF != TAIL) {
                    dropFirstByte()
                    continue
                }
                val frame = bytes.copyOfRange(0, total)
                val parsed = parse(frame)
                if (parsed == null) {
                    dropFirstByte()
                    continue
                }
                frames += parsed
                replaceBuffer(bytes.copyOfRange(total, bytes.size))
            }
            return frames
        }

        private fun replaceBuffer(bytes: ByteArray) {
            buffer.reset()
            buffer.write(bytes)
        }

        private fun dropFirstByte() {
            val bytes = buffer.toByteArray()
            replaceBuffer(if (bytes.size > 1) bytes.copyOfRange(1, bytes.size) else ByteArray(0))
        }
    }
}

data class ParsedFrame(
    val cmd: Int,
    val addr: Int,
    val payload: ByteArray,
    val raw: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedFrame) return false
        return cmd == other.cmd && addr == other.addr && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int = (cmd * 31 + addr) * 31 + payload.contentHashCode()
}

object Commands {
    const val LONG_READ = 0x01
    const val STATUS = 0x02
    const val OTA = 0x03
    const val CONTROL = 0x04
    const val LIGHT = 0x05

    const val READ_ALL = 0xFFFF
}
