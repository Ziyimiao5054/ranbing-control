package com.anemoventi.ranbing.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Register value types used by the firmware configuration tables. */
enum class RegType(val size: Int) {
    U8(1),
    U16(2),
    U32(4),
    F32(4),
}

/**
 * One entry of a register space.
 *
 * @param arrayLen non-zero for fixed size byte arrays (e.g. the 256 byte OTA info block).
 *   For arrays the value is stored as a [ByteArray].
 */
data class RegisterDef(
    val name: String,
    val addr: Int,
    val type: RegType,
    val arrayLen: Int = 0,
    val defaultInt: Int = 0,
    val defaultFloat: Float = 0f,
) {
    fun defaultValue(): Any = when (type) {
        RegType.U32 -> defaultInt.toLong()
        RegType.F32 -> defaultFloat
        RegType.U8, RegType.U16 -> defaultInt
    }

    val byteSize: Int get() = if (arrayLen > 0) arrayLen * type.size else type.size
}

/** A logical block of registers addressable with one command. */
class RegisterSpace(val defs: List<RegisterDef>) {

    private val byName: Map<String, RegisterDef> = defs.associateBy { it.name }
    private val byAddr: Map<Int, RegisterDef> = defs.associateBy { it.addr }

    fun def(name: String): RegisterDef? = byName[name]

    fun defByAddr(addr: Int): RegisterDef? = byAddr[addr]

    fun initialValues(): Map<String, Any> = defs.associate { def ->
        def.name to if (def.arrayLen > 0) ByteArray(def.byteSize) else def.defaultValue()
    }

    /** Serializes every register in declaration order, used for `READ_ALL` requests. */
    fun serializeAll(values: Map<String, Any>): ByteArray {
        val out = ByteArray(defs.sumOf { it.byteSize })
        var offset = 0
        for (def in defs) {
            val bytes = serialize(def, values[def.name])
            bytes.copyInto(out, offset)
            offset += bytes.size
        }
        return out
    }

    /**
     * Parses a `READ_ALL` payload into [values]; returns the names that actually changed.
     */
    fun applyAll(payload: ByteArray, values: MutableMap<String, Any>): List<String> {
        val changed = mutableListOf<String>()
        var offset = 0
        for (def in defs) {
            if (offset + def.byteSize > payload.size) break
            val parsed = parse(def, payload, offset)
            offset += def.byteSize
            if (values[def.name] != parsed) {
                values[def.name] = parsed
                changed += def.name
            }
        }
        return changed
    }

    /** Parses a single register payload; returns the register name when it changed. */
    fun applyOne(addr: Int, payload: ByteArray, values: MutableMap<String, Any>): String? {
        val def = defByAddr(addr) ?: return null
        // Short ACK/echo frames carry no register data; keep the previous value.
        if (payload.size < def.byteSize) return null
        val parsed = parse(def, payload, 0)
        if (values[def.name] == parsed) return null
        values[def.name] = parsed
        return def.name
    }

    fun serialize(def: RegisterDef, value: Any?): ByteArray {
        if (def.arrayLen > 0) {
            return (value as? ByteArray)?.copyOf(def.byteSize) ?: ByteArray(def.byteSize)
        }
        return when (def.type) {
            RegType.U8 -> byteArrayOf(((value as? Number)?.toInt() ?: 0).toByte())
            RegType.U16 -> ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort(((value as? Number)?.toInt() ?: 0).toShort()).array()

            RegType.U32 -> ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt((value as? Number)?.toInt() ?: 0).array()

            RegType.F32 -> ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putFloat((value as? Number)?.toFloat() ?: 0f).array()
        }
    }

    fun parse(def: RegisterDef, payload: ByteArray, offset: Int): Any {
        if (def.arrayLen > 0) {
            if (offset < 0 || offset + def.byteSize > payload.size) return ByteArray(def.byteSize)
            return payload.copyOfRange(offset, offset + def.byteSize)
        }
        if (offset < 0 || offset + def.type.size > payload.size) return def.defaultValue()
        val buffer = ByteBuffer.wrap(payload, offset, def.type.size).order(ByteOrder.LITTLE_ENDIAN)
        return when (def.type) {
            RegType.U8 -> payload[offset].toInt() and 0xFF
            RegType.U16 -> buffer.short.toInt() and 0xFFFF
            RegType.U32 -> buffer.int.toLong() and 0xFFFFFFFFL
            RegType.F32 -> buffer.float
        }
    }
}
