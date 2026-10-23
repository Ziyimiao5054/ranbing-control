package com.anemoventi.ranbing.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FrameCodecTest {

    private fun hex(bytes: ByteArray): String = bytes.joinToString(" ") { "%02X".format(it) }

    @Test
    fun `crc16 known vectors`() {
        // AA 01 FF FF 00 00 19 E4 BB -> CRC over 01 FF FF 00 00
        assertEquals(0xE419, FrameCodec.crc16(byteArrayOf(0x01, 0xFF.toByte(), 0xFF.toByte(), 0x00, 0x00)))
    }

    @Test
    fun `pack read all frame`() {
        assertEquals("AA 01 FF FF 00 00 19 E4 BB", hex(FrameCodec.pack(1, 0xFFFF, byteArrayOf())))
    }

    @Test
    fun `pack power on frame`() {
        // CMD=2, ADDR=10 (device_power_on), payload=[1]
        assertEquals("AA 02 0A 00 01 00 01 09 F8 BB", hex(FrameCodec.pack(2, 10, byteArrayOf(1))))
    }

    @Test
    fun `pack key grade frame`() {
        assertEquals("AA 02 01 00 01 00 03 2D F8 BB", hex(FrameCodec.pack(2, 1, byteArrayOf(3))))
    }

    @Test
    fun `parse round trip`() {
        val frame = FrameCodec.pack(2, 10, byteArrayOf(1))
        val parsed = FrameCodec.parse(frame)
        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(2, parsed.cmd)
        assertEquals(10, parsed.addr)
        assertArrayEquals(byteArrayOf(1), parsed.payload)
    }

    @Test
    fun `parse rejects bad crc`() {
        val frame = FrameCodec.pack(1, 0xFFFF, byteArrayOf())
        frame[7] = (frame[7].toInt() xor 0x01).toByte()
        assertNull(FrameCodec.parse(frame))
    }

    @Test
    fun `assembler handles fragmentation and glueing`() {
        // Empty payload frames are 9 bytes; the reference parser requires at least 10, so use a
        // one byte payload for the fragmentation test.
        val a = FrameCodec.pack(1, 0xFFFF, byteArrayOf(0))
        val b = FrameCodec.pack(2, 10, byteArrayOf(1))
        val assembler = FrameCodec.Assembler()

        // Split the first frame into 3 byte chunks (as a 20 byte MTU would do for larger frames).
        val frames = mutableListOf<ParsedFrame>()
        a.forEach { frames += assembler.append(byteArrayOf(it)) }
        // Glue second frame together with a leading garbage byte.
        frames += assembler.append(byteArrayOf(0x00, 0x11) + b)

        assertEquals(2, frames.size)
        assertEquals(1, frames[0].cmd)
        assertEquals(0xFFFF, frames[0].addr)
        assertEquals(2, frames[1].cmd)
        assertEquals(10, frames[1].addr)
    }

    @Test
    fun `register space serializes little endian`() {
        val space = DeviceModel.X8.statusSpace
        val values = space.initialValues().toMutableMap()
        values["slider_voltage"] = 5f
        val bytes = space.serialize(space.def("slider_voltage")!!, 5f)
        assertEquals("00 00 A0 40", hex(bytes))
    }

    @Test
    fun `read all payload size matches register table`() {
        val space = DeviceModel.X8.statusSpace
        val payload = space.serializeAll(space.initialValues())
        // 12 uint8/uint32 fields -> 4*4 + ... computed from the table
        assertEquals(space.defs.sumOf { it.byteSize }, payload.size)
        val values = space.initialValues().toMutableMap()
        val changed = space.applyAll(payload, values)
        assertEquals(emptyList<String>(), changed)
    }

    @Test
    fun `apply all parses status payload`() {
        val space = DeviceModel.X8.statusSpace
        val values = space.initialValues().toMutableMap()
        val payload = space.serializeAll(
            values + mapOf(
                "l_temp" to 12.5f,
                "r_temp" to 13.5f,
                "input_voltage" to 12.1f,
                "power" to 30f,
                "key_grade" to 3,
                "timer_countdown" to 60L,
            ),
        )
        val changed = space.applyAll(payload, values)
        assertEquals(12.5f, values["l_temp"])
        assertEquals(13.5f, values["r_temp"])
        assertEquals(3, values["key_grade"])
        assertEquals(60L, values["timer_countdown"])
        assertEquals(true, changed.contains("l_temp"))
    }
}
