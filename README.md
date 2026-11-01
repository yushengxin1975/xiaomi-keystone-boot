# Xiaomi / Fengmi Projector Auto Keystone on Boot (`xiaomi-keystone-boot`)

针对小米/峰米定制 Android TV 投影仪（如 `FengMiProjA2`）在断电或冷开机后底层 `KeystoneHal` 未自动应用已保存的梯形校正参数（`/tvinfo/warp_table.txt` 写入失败且系统 `BootReceiver` 未触发硬件 `Warp`）的问题，提供开机自动恢复与自动按键微调的 Android TV 原生自启应用。

## 功能特性

- **开机自动触发**：监听 `BOOT_COMPLETED`、`LOCKED_BOOT_COMPLETED`、`QUICKBOOT_POWERON`，支持 Direct Boot 设备加密存储，开机后按设定延迟自动在后台启动服务。
- **三种校正模式（支持遥控器可视化切换）**：
  1. **模式一：自动唤醒并恢复已保存的四角校准位置（默认推荐）**
     - 自动拉起梯形校正组件并进入四角微调界面，执行一次无损微动（`+1 / -1`），强制光机底层芯片（iChip `KeystoneHal`）加载 `/metadata/keystone/warping_4point.conf`（`Grid 0`）并执行硬件 `Warp`，随后自动退出界面。
  2. **模式二：按快速垂直/水平角度调节**
     - 自动进入「快速调节」界面，按设定的垂直角度与水平角度步数自动发送方向键完成校正。
  3. **模式三：按四角微调步数调节**
     - 自动进入「四角微调」界面，依次选中左上角、右上角、右下角、左下角，按设定的每个角的上/下/左/右步数自动完成微调（支持调节前先自动重置）。
- **TV 遥控器友好界面**：适配投影仪遥控器方向键焦点高亮，支持一键测试运行与实时查看最近一次执行状态。

## 目录结构

- `AndroidManifest.xml`：应用清单与开机自启权限声明
- `src/com/xiaomi/keystoneboot/BootReceiver.java`：开机广播与测试广播接收器
- `src/com/xiaomi/keystoneboot/KeystoneBootService.java`：后台自动梯形校正核心服务
- `src/com/xiaomi/keystoneboot/KeystoneConfig.java`：参数配置持久化管理
- `src/com/xiaomi/keystoneboot/MainActivity.java`：投影仪端可视化配置与测试界面
- `res/drawable/`：应用图标与 Android TV Banner 矢量资源
- `build_and_install.ps1`：命令行编译与打包脚本

## 编译与安装

1. 配置环境变量 `JAVA_HOME`（JDK 17+）与 `ANDROID_HOME`（Android SDK），运行编译脚本：
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\build_and_install.ps1
   ```
2. 通过 ADB 安装到投影仪并授予后台自启白名单权限：
   ```bash
   adb install -r XiaomiKeystoneBoot.apk
   adb shell dumpsys deviceidle whitelist +com.xiaomi.keystoneboot
   adb shell cmd appops set com.xiaomi.keystoneboot RUN_IN_BACKGROUND allow
   adb shell cmd appops set com.xiaomi.keystoneboot RUN_ANY_IN_BACKGROUND allow
   ```
