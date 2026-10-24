# 鐕冨啺鏁ｇ儹鍣ㄦ帶鍒?
Android 瀹㈡埛绔紝閫氳繃钃濈墮鎺у埗鐕冨啺 X8 / X8U 鍗婂浣撳埗鍐锋暎鐑櫒銆?
- 鍖呭悕锛歚com.anemoventi.ranbing`
- 绯荤粺锛欰ndroid 8.0+锛圓PI 26锛夛紝targetSdk 37
- 鎶€鏈爤锛欿otlin銆丣etpack Compose銆丮aterial 3銆丮IUIx

## 鍔熻兘

- 鎵弿骞惰繛鎺ユ暎鐑櫒锛岃嚜鍔ㄨ浣忎笂娆¤澶?- 瀹炴椂璇诲彇鍐风 / 鐑娓╁害銆佸姛鐜囥€佽緭鍏ョ數鍘嬨€佺數娴併€佹。浣嶅拰杩愯鐘舵€?- 寮€鍏虫満銆?鈥? 妗ｃ€佹俯鎺фā寮忎笌鐩爣娓╁害銆佽秴棰戯紙15 V锛夈€侀鎵囩伅
- 鍒跺喎鍔熺巼闂幆锛?鈥?0 W 婊戞潯 + 鏁板瓧杈撳叆锛岃蒋浠惰皟鍘嬮€艰繎鐩爣鍔熺巼
- X8U锛氶鎵?/ 鍒跺喎 PWM銆佺伅鏁堛€佽繃鐑繚鎶?- Material 3 / MIUIx 鍙屼富棰橈紝璺熼殢绯荤粺锛屽彲鍥哄畾涓婚鑹?- 寮傚父鏂嚎鑷姩閲嶈繛锛涙墜鍔ㄦ柇寮€涓嶉噸杩?- 璋冭瘯椤垫煡鐪嬭摑鐗欐敹鍙戝抚

## 鏋勫缓

闇€瑕?JDK 21 鍜?Android SDK 37銆?
```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:testDebugUnitTest
```

浜х墿鍦?`app/build/outputs/apk/`銆俁elease 鍖呯敤 debug keystore 绛惧悕锛屾湭寮€ R8銆?
## BLE

| 椤圭洰 | 鍊?|
|---|---|
| Service | `0000FFE0-鈥 |
| Write | `0000FFE3-鈥锛寃rite-no-response |
| Notify | `0000FFE4-鈥 |
| MTU | 247锛屽垎鐗?`MTU-3`锛涘け璐ュ洖閫€ 20 瀛楄妭 |
| 瓒呮椂 | 杩炴帴 15 s |

骞挎挱 service data锛歚0x4252` 鎼哄甫鍨嬪彿涓庡浐浠剁増鏈紝`0x4449` 鎼哄甫鐪熷疄 MAC銆?
## 鍗忚

```
AA | CMD(1) | ADDR(2 LE) | LEN(2 LE) | PAYLOAD | CRC16(2 LE) | BB
CRC16: poly 0xA001, init 0xFFFF, 瑕嗙洊 CMD..PAYLOAD
```

- `cmd=0x01` + `addr=0xFFFF` + 绌?payload 璇?53 瀛楄妭鐘舵€佸潡锛沗cmd=0x02` 鍚屾牱鏂瑰紡璇?288 瀛楄妭閰嶇疆鍧椼€?- 鍐欏瘎瀛樺櫒鐢?`cmd=0x02`銆?- 鐘舵€佸潡瀛楁锛圶8锛屽皬绔級锛歚heartpack, l_temp, r_temp, input_voltage, output_voltage, key_grade, power, current, FAN_LED, timer_running, timer_countdown, temp_control_enable, temp_target, slider_voltage, protocol, device_power_on, device_type, temp_control_no_load, ntc_b_cold, ntc_b_hot`銆?
瀵勫瓨鍣ㄨ〃鍦?`protocol/DeviceModels.kt`锛屽抚缂栬В鐮佸崟娴嬪湪 `app/src/test/`銆?
## 宸茬煡闄愬埗

- X8U 鐨勬帶鍒跺姛鑳芥湭鍦ㄥ疄鏈洪獙璇併€?- OTA銆佺伅鏁堥鑹茬紪杈戙€丯TC 鏍″噯鏈疄鐜般€?- X8 鐨?`protocol` 瀵勫瓨鍣ㄨ鍥?`0xFF`锛岀晫闈㈡樉绀?`鈥擿銆?
## 璁稿彲

GPL-3.0銆俇I 鍙傝€?InstallerX-Revived锛圙PL-3.0锛夛紝瀵艰埅浣跨敤 [MIUIx](https://github.com/compose-miuix-ui/miuix)銆?