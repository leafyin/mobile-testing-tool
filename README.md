# Logan — Android 移动测试工具

Logan 是一款基于 Java Swing 的桌面工具，用于通过 ADB 管理 Android 测试设备。支持设备列表、屏幕镜像、按键控制、文字输入、SDCard 文件浏览与批量导出。

## 功能

| 功能 | 说明 |
|------|------|
| 设备管理 | 列出已连接设备，单选切换控制目标 |
| 路径配置 | ADB / Scrcpy 路径自动保存，下次启动无需重复选择 |
| 屏幕预览 | 通过 [scrcpy](https://github.com/Genymobile/scrcpy) 实时镜像设备屏幕 |
| 按键控制 | Home、返回、电源/关屏、音量±、确认 |
| 文字输入 | 面板内输入框直接发送文字，支持中文（剪贴板粘贴），带历史记录下拉 |
| 文件浏览 | 浏览设备 `/sdcard` 目录，双击进入子目录 |
| 批量导出 | 多选文件，`adb pull` 导出到本地目录 |
| 媒体过滤 | 仅显示图片、视频文件及目录 |

## 环境要求

- **Java** 11 或更高版本（推荐 JDK 17+）
- **ADB**：Android SDK Platform-Tools（[下载地址](https://developer.android.com/tools/releases/platform-tools)）
- **Scrcpy**（可选，用于屏幕预览）：`brew install scrcpy`
- Android 设备已开启 **USB 调试**，并通过 USB 或无线 ADB 连接

## 快速开始

### 1. 编译

```bash
cd mobile-testing-tool
mkdir -p out/production/mobile-testing-tool
javac -d out/production/mobile-testing-tool -sourcepath src \
  src/GUI.java \
  src/com/leaf/utils/*.java \
  src/com/leaf/service/*.java \
  src/com/leaf/model/*.java \
  src/com/leaf/gui/*.java
```

### 2. 运行

```bash
java -cp out/production/mobile-testing-tool GUI
```

也可在 IntelliJ IDEA 中直接运行 `GUI.main()`。

### 3. 首次配置

1. 点击 **选择 ADB**，选择 platform-tools 目录（内含 `adb` 可执行文件）
2. （可选）点击 **选择 Scrcpy**，选择 scrcpy 可执行文件；不配置时会尝试自动检测 `/opt/homebrew/bin/scrcpy` 等常见路径
3. 点击 **刷新 device**，在左侧选中一台设备

配置保存在 `~/.logan/config.properties`。

## 使用说明

### 设备选择与预览

- 左侧列表单击选中设备
- 右侧点击 **开始预览** 启动 scrcpy 镜像窗口；再次点击 **停止预览** 关闭

### 文字输入

- 在 **输入文字** 区域的文本框中输入内容
- 点击 **发送** 或按 **Enter** 发送到设备
- 点击文本框右侧下拉箭头可选取历史记录（最多 20 条，自动保存）
- 发送中文前，请先在设备上 **聚焦目标输入框**

### SDCard 文件

- 默认从 `/sdcard` 开始浏览
- **双击目录** 进入；**上级目录** 返回
- **Ctrl/Cmd + 点击** 或 **Shift + 点击** 多选文件
- **批量导出** 将选中文件保存到本地文件夹
- **仅媒体** 过滤图片、视频文件

### 按键控制

| 按钮 | 作用 |
|------|------|
| Home | 返回桌面 |
| 返回 | 返回键 |
| 电源/关屏 | 电源键 |
| 音量+ / 音量- | 调节音量 |
| 确认 | Enter 键 |

## 项目结构

```
src/
├── GUI.java                          # 程序入口，主窗口
└── com/leaf/
    ├── gui/
    │   └── DeviceControlPanel.java   # 设备控制面板
    ├── service/
    │   ├── AdbService.java           # ADB 命令封装
    │   └── ScrcpyService.java        # scrcpy 进程管理
    ├── model/
    │   └── RemoteEntry.java          # 远程文件/目录模型
    └── utils/
        ├── CmdUtil.java              # 命令行执行
        └── ConfigUtil.java           # 配置持久化
```

## 配置文件

路径：`~/.logan/config.properties`

```properties
adb.path=/Users/you/platform-tools
scrcpy.path=/opt/homebrew/bin/scrcpy
input.history.0=最近输入的文字
input.history.1=上一条历史记录
```

## 常见问题

**Q: 刷新后看不到设备？**  
确认 USB 调试已开启，终端执行 `{adb路径}/adb devices` 能看到 `device` 状态。

**Q: 预览启动失败？**  
- 确认已安装 scrcpy 4.x
- scrcpy 4.0 通过环境变量 `ADB` 指定 adb 路径，程序已自动处理
- 检查顶部 Scrcpy 路径是否正确

**Q: 中文输入无效？**  
程序通过剪贴板 + Ctrl+V 粘贴中文，请确保设备输入框已获得焦点。

**Q: 无法访问 SDCard？**  
部分 Android 版本限制 shell 访问，可尝试浏览 `/sdcard/DCIM` 等子目录。