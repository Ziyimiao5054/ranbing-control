# 燃冰散热器控制（Ranbing Control）

Android 原生客户端，用于管理 **X8 / X8U 系列半导体制冷散热器**。协议由微信小程序
`_696524928_18.wxapkg` 静态逆向 + 真机实测校准而来。

- 包名：`com.anemoventi.ranbing`
- 最低系统：Android 8.0（API 26），目标 API 37
- 技术栈：Kotlin + Jetpack Compose + Material 3 + [MIUIx](https://github.com/compose-miuix-ui/miuix) + miuix-nav

## 功能

| 页面 | 内容 |
|---|---|
| 控制 | 扫描 / 连接（自动记住上次设备）、开关机、风扇灯、1–3 档与温控、目标温度、调压、定时、实时温度 / 功率 / 电压 / 电流 |
| 调试 | 全量读状态 / 读配置、空载荷探针、手动发帧、收发帧日志、强制 20 字节分片 |
| 设置 | 界面风格（跟随系统 / Material 3 / MIUIx）、深色模式、动态取色、关于 |

- **双主题**：Material 3 与 MIUIx 两套 UI，默认跟随系统（HyperOS / MIUI 使用 MIUIx）。
  MIUIx 按 HyperOS 规范固定为纯黑背景（#000000）与主题色 #287AF9，不跟随壁纸取色；
  底栏选中态是中性灰胶囊，无主题色描边、无矩形水波纹。
- **主页大卡片**：移植自参考 App 的 MIUIx 状态卡（170dp 背景图标、20sp 标题、
  14sp 描述、36dp 间距、第三行详情），连接成功为绿色勾、未连接/失败为红色叉。
- **底栏结构**：外层固定 `NavigationBar` + `HorizontalPager`，每个标签页自己带可折叠大标题
  （`SkinMainScaffold` / `SkinTabPage`）；点击底栏走 `MainPagerState.animateToPage`，
  选中态立即更新、内容用固定曲线滑动，不会整屏推走底栏。
- **预见式返回**：导航使用 miuix-nav 的 `NavDisplay`，Manifest 开启
  `android:enableOnBackInvokedCallback`；首页非首个标签时按返回先回到「控制」。
- **断线重连**：手动断开不重连；异常断线 30 秒内自动重连（最多 15 次，间隔 2 秒）。
- 左温度通道未接 NTC 时固件返回 `-273.15`，界面显示 `--`。

## 构建

```bash
# JDK 25（Android Studio 自带 JBR 即可）
./gradlew :app:assembleDebug          # 调试包
./gradlew :app:assembleRelease        # 发布包（本地 debug keystore 签名，R8 关闭）
./gradlew :app:testDebugUnitTest      # 协议单测
```

产物：`app/build/outputs/apk/debug/app-debug.apk`、`app/build/outputs/apk/release/app-release.apk`。

依赖仓库在 `settings.gradle.kts` 中优先使用阿里云镜像，`build.gradle.kts` 把 Kotlin
Gradle 插件固定为 2.4.21（AGP 9 内置的 2.2 编译器读不了 miuix 0.9.4 的 2.4 元数据）。

## BLE 参数

| 项目 | 值 |
|---|---|
| Service | `0000FFE0-…`（代码按 16 位 UUID 匹配） |
| Write | `0000FFE3-…`，**仅支持 write-no-response**（props `0x04`） |
| Notify | `0000FFE4-…`（props `0x10`），写 CCCD 订阅 notification |
| MTU | 请求 247，分片 `MTU-3`；被拒绝时回退 20 字节，包间隔 50 ms |
| 连接超时 | 15 s |

广播解析（`BleClient.parseAdvertisement`）：

- 项目 UUID **0x4252**（`"RB"`）的 service data：`[category, fwVersionByte, model ASCII…]`，
  版本字节按 `V{n/10}.{n%10}` 显示，型号字符串即 `X8` / `X8U`。
- **0x4449**（`"ID"`）的 service data：前 6 字节为逆序 MAC，用于展示真实 MAC。

## 帧协议（实测结论）

```
AA | CMD(1) | ADDR(2 LE) | LEN(2 LE) | PAYLOAD[LEN] | CRC16(2 LE) | BB
CRC16: poly 0xA001, init 0xFFFF, 覆盖 CMD..PAYLOAD
```

实测与交接文档的**差异**（以真机固件 V1.3 为准）：

1. **全量读取必须发送空 payload**。
   `AA 01 FF FF 00 00 19 E4 BB` 才会得到 53 字节状态帧；带上寄存器序列化内容时设备
   完全不回包。配置块同理：`cmd=0x02 + addr=0xFFFF + 空 payload` 返回 288 字节配置块。
2. **写寄存器使用 CMD `0x02`**，不是文档猜测的 `0x04`。例：关机
   `AA 02 0A 00 01 00 00?? BB`（addr=10 `device_power_on`）。
   实测关机后功率 10.0 W → 0.1 W、电流 1.52 A → 0.04 A。
3. 设备对 `cmd=0x01/0x02` 的空载荷读都有响应；`cmd=0x04` 无响应。
4. 状态帧内容（X8，53 字节，小端）与 `cmdConfigMap.js` 的空间 1 完全一致：
   `heartpack u8, l_temp f32, r_temp f32, input_voltage f32, output_voltage f32,
   key_grade u8, power f32, current f32, FAN_LED u8, timer_running u8,
   timer_countdown u32, temp_control_enable u8, temp_target f32, slider_voltage f32,
   protocol u8, device_power_on u8, device_type u8, temp_control_no_load u8,
   ntc_b_cold f32, ntc_b_hot f32`

寄存器表定义在 `protocol/DeviceModels.kt`（X8 / X8U 两套空间），单测在
`app/src/test/.../FrameCodecTest.kt`。

## 目录结构

```
app/src/main/java/com/anemoventi/ranbing/
├── App.kt / MainActivity.kt
├── ble/BleClient.kt              # 扫描、连接、MTU/分片、写队列、通知
├── device/DeviceRepository.kt    # 会话状态、1s 轮询、寄存器读写、重连
├── protocol/                     # 帧编解码、CRC、寄存器表
├── data/AppSettings.kt           # SharedPreferences 设置与上次设备
└── ui/
    ├── theme/AppTheme.kt         # MD3 / MIUIx 双主题
    ├── widgets/Skin.kt           # 两套皮肤的统一组件（大标题、卡片、胶囊底栏…）
    ├── navigation/               # miuix-nav 容器、Navigator、路由
    └── page/                     # ControlTab / DebugTab / SettingsTab / ThemePage / AboutPage
```

## 已知限制

- X8U 的控制项（风扇 / 制冷 PWM、灯效、过热保护）已按寄存器表实现，但未接 X8U 实物验证。
- OTA 升级、灯效颜色编辑、NTC B 值校准未实现（交接文档列为二期）。
- `protocol` 寄存器在 X8 V1.3 上读回 `0xFF`（未使用），界面显示 `—`。

## 许可

GPL-3.0。UI 骨架与视觉语言参考 InstallerX-Revived（GPL-3.0），导航运行时来自 MIUIx。
