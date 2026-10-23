# 燃冰散热器控制

Android 客户端，通过蓝牙控制燃冰 X8 / X8U 半导体制冷散热器。

- 包名：`com.anemoventi.ranbing`
- 系统：Android 8.0+（API 26），targetSdk 37
- 技术栈：Kotlin、Jetpack Compose、Material 3、MIUIx

## 功能

- 扫描并连接散热器，自动记住上次设备
- 实时读取冷端 / 热端温度、功率、输入电压、电流、档位和运行状态
- 开关机、1–3 档、温控模式与目标温度、风扇灯、定时关机
- X8U：风扇 / 制冷 PWM、灯效、过热保护
- Material 3 / MIUIx 双主题，跟随系统，可固定主题色
- 异常断线自动重连；手动断开不重连
- 调试页查看蓝牙收发帧

## 构建

需要 JDK 21 和 Android SDK 37。

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:testDebugUnitTest
```

产物在 `app/build/outputs/apk/`。Release 包用 debug keystore 签名，未开 R8。

## BLE

| 项目 | 值 |
|---|---|
| Service | `0000FFE0-…` |
| Write | `0000FFE3-…`，write-no-response |
| Notify | `0000FFE4-…` |
| MTU | 247，分片 `MTU-3`；失败回退 20 字节 |
| 超时 | 连接 15 s |

广播 service data：`0x4252` 携带型号与固件版本，`0x4449` 携带真实 MAC。

## 协议

```
AA | CMD(1) | ADDR(2 LE) | LEN(2 LE) | PAYLOAD | CRC16(2 LE) | BB
CRC16: poly 0xA001, init 0xFFFF, 覆盖 CMD..PAYLOAD
```

- `cmd=0x01` + `addr=0xFFFF` + 空 payload 读 53 字节状态块；`cmd=0x02` 同样方式读 288 字节配置块。
- 写寄存器用 `cmd=0x02`。
- 状态块字段（X8，小端）：`heartpack, l_temp, r_temp, input_voltage, output_voltage, key_grade, power, current, FAN_LED, timer_running, timer_countdown, temp_control_enable, temp_target, slider_voltage, protocol, device_power_on, device_type, temp_control_no_load, ntc_b_cold, ntc_b_hot`。

寄存器表在 `protocol/DeviceModels.kt`，帧编解码单测在 `app/src/test/`。

## 已知限制

- X8U 的控制功能未在实机验证。
- OTA、灯效颜色编辑、NTC 校准未实现。
- X8 的 `protocol` 寄存器读回 `0xFF`，界面显示 `—`。

## 许可

GPL-3.0。UI 参考 InstallerX-Revived（GPL-3.0），导航使用 [MIUIx](https://github.com/compose-miuix-ui/miuix)。
