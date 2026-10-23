package com.anemoventi.ranbing.protocol

/**
 * Register tables, transcribed from the mini program's `utils/cmdConfigMap.js`.
 *
 * Space 1 (command `0x01`) is the status block that the device reports; space 2 (command
 * `0x02`) is the writable configuration block. Addresses are register indices, not byte offsets.
 */
enum class DeviceModel(
    val modelName: String,
    val displayName: String,
    /** Command used to read the status block. */
    val statusReadCmd: Int = Commands.LONG_READ,
    /** Command used to read the configuration block. */
    val configReadCmd: Int = Commands.STATUS,
    /**
     * Command used to write configuration registers. The mini program writes with `0x02`; the
     * handover document guessed `0x04`. Kept as a field so the debug screen can probe both.
     */
    val writeCmd: Int = Commands.STATUS,
) {
    X8("X8", "X8"),
    X8U("X8U", "X8U"),
    UNKNOWN("", "未知型号");

    val spaces: Map<Int, RegisterSpace>
        get() = when (this) {
            X8 -> X8_SPACES
            X8U -> X8U_SPACES
            UNKNOWN -> X8_SPACES
        }

    val statusSpace: RegisterSpace get() = spaces.getValue(SPACE_STATUS)
    val configSpace: RegisterSpace get() = spaces.getValue(SPACE_CONFIG)

    companion object {
        const val SPACE_STATUS = 1
        const val SPACE_CONFIG = 2

        fun fromName(raw: String?): DeviceModel {
            val name = raw.orEmpty().trim('\u0000', ' ', '\t').uppercase()
            return entries.firstOrNull { it.modelName.isNotEmpty() && (name == it.modelName || name.startsWith(it.modelName)) }
                ?: UNKNOWN
        }

        private fun f32(name: String, addr: Int, default: Float = 0f) =
            RegisterDef(name, addr, RegType.F32, defaultFloat = default)

        private fun u8(name: String, addr: Int, default: Int = 0) =
            RegisterDef(name, addr, RegType.U8, defaultInt = default)

        private fun u32(name: String, addr: Int, default: Int = 0) =
            RegisterDef(name, addr, RegType.U32, defaultInt = default)

        private fun bytes(name: String, addr: Int, len: Int) =
            RegisterDef(name, addr, RegType.U8, arrayLen = len)

        private val X8_STATUS = RegisterSpace(
            listOf(
                u8("heartpack", 0),
                f32("l_temp", 1, 25f),
                f32("r_temp", 2, 25f),
                f32("input_voltage", 3, 5f),
                f32("output_voltage", 4, 3.3f),
                u8("key_grade", 5),
                f32("power", 6),
                f32("current", 7),
                u8("FAN_LED", 8),
                u8("timer_running", 9),
                u32("timer_countdown", 10),
                u8("temp_control_enable", 11),
                f32("temp_target", 12),
                f32("slider_voltage", 13, 5f),
                u8("protocol", 14, 255),
                u8("device_power_on", 15, 1),
                u8("device_type", 16, 1),
                u8("temp_control_no_load", 17),
                f32("ntc_b_value_cold", 18),
                f32("ntc_b_value_hot", 19),
            ),
        )

        private val X8_CONFIG = RegisterSpace(
            listOf(
                u8("heartpack", 0),
                u8("key_grade", 1),
                u8("FAN_LED", 2),
                u8("timer_running", 3),
                u32("timer_countdown", 4),
                u8("temp_control_enable", 5),
                f32("temp_target", 6),
                f32("slider_voltage", 7, 5f),
                u8("advertising_enable", 8, 1),
                u8("check_color", 9),
                u8("device_power_on", 10, 1),
                u8("device_type", 11, 1),
                u8("input_voltage_grade", 12, 1),
                f32("ntc_b_value_cold", 13),
                f32("ntc_b_value_hot", 14),
                u8("reboot_flag", 15),
                bytes("upgrade_info_block", 16, 256),
                u8("info_flag", 17),
            ),
        )

        private val X8U_STATUS = RegisterSpace(
            listOf(
                u8("heartpack", 0),
                f32("l_temp", 1, 25f),
                f32("r_temp", 2, 25f),
                f32("input_voltage", 3, 5f),
                u32("fan_speed", 4),
            ),
        )

        private val X8U_CONFIG = RegisterSpace(
            listOf(
                u8("heartpack", 0),
                u8("device_power_on", 1, 1),
                u8("device_type", 2, 1),
                u8("input_voltage_grade", 3, 1),
                u8("output_voltage_grade", 4, 1),
                u8("pn_pwm", 5),
                u8("fan_pwm", 6),
                u8("temp_control_enable", 7),
                f32("temp_target", 8),
                u8("check_color", 9),
                u8("light_show_flag", 10, 1),
                u8("led_boot_slot", 11),
                u8("load_slot", 12),
                u8("save_slot", 13),
                u8("advertising_enable", 14, 1),
                bytes("device_name", 15, 29),
                u8("info_flag", 16),
                u8("reboot_flag", 17),
                f32("overheat_temp", 18, 65f),
            ),
        )

        private val X8_SPACES = mapOf(SPACE_STATUS to X8_STATUS, SPACE_CONFIG to X8_CONFIG)
        private val X8U_SPACES = mapOf(SPACE_STATUS to X8U_STATUS, SPACE_CONFIG to X8U_CONFIG)
    }
}
