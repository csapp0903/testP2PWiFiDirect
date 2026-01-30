# testP2PWiFiDirect - Android Wi-Fi Direct (P2P) 测试应用

## 项目简介

这是一个基础的 Android 应用程序，利用 **Wi-Fi Direct (P2P)** 技术来发现并连接附近的设备（包括 Windows 设备）。

Wi-Fi Direct 允许两台设备在不需要无线路由器的情况下直接建立 Wi-Fi 连接，适用于近距离设备间的数据传输场景。

## 功能特性

- **设备发现**: 扫描并列出附近所有支持 Wi-Fi Direct 的设备
- **设备连接**: 点击列表中的设备发起 P2P 连接请求
- **连接管理**: 显示连接状态、Group Owner 信息，支持主动断开连接
- **实时日志**: 在界面底部实时展示所有操作日志，便于调试
- **权限管理**: 自动适配 Android 6.0~13+ 的权限模型

## 项目结构

```
app/src/main/
├── AndroidManifest.xml                          # 应用清单，声明所需权限
├── java/com/example/wifidirectp2p/
│   ├── MainActivity.java                        # 主界面，处理设备发现和连接逻辑
│   ├── WiFiDirectBroadcastReceiver.java         # 广播接收器，监听 Wi-Fi Direct 事件
│   └── DeviceListAdapter.java                   # RecyclerView 适配器，展示设备列表
└── res/
    ├── layout/
    │   ├── activity_main.xml                    # 主界面布局
    │   └── item_device.xml                      # 设备列表项布局
    └── values/
        ├── strings.xml                          # 字符串资源
        └── colors.xml                           # 颜色资源
```

## 核心工作流程

```
1. 应用启动 → 初始化 WifiP2pManager → 注册 BroadcastReceiver
2. 用户点击"发现设备" → 调用 discoverPeers()
3. 系统发送 WIFI_P2P_PEERS_CHANGED_ACTION 广播 → 请求最新设备列表
4. 设备列表展示在 RecyclerView 中
5. 用户点击某个设备 → 弹出确认对话框 → 调用 connect() 发起连接
6. 系统发送 WIFI_P2P_CONNECTION_CHANGED_ACTION 广播 → 获取连接信息
7. 显示 Group Owner IP 地址和本机角色（GO/Client）
```

## 所需权限

| 权限 | 说明 | Android 版本 |
|------|------|-------------|
| `ACCESS_WIFI_STATE` | 获取 Wi-Fi 状态 | 所有版本 |
| `CHANGE_WIFI_STATE` | 修改 Wi-Fi 状态 | 所有版本 |
| `ACCESS_FINE_LOCATION` | 精确位置（扫描设备需要） | 6.0+ |
| `NEARBY_WIFI_DEVICES` | 附近 Wi-Fi 设备权限 | 13+ |
| `INTERNET` | 网络访问 | 所有版本 |
| `ACCESS_NETWORK_STATE` | 网络状态 | 所有版本 |
| `CHANGE_NETWORK_STATE` | 修改网络状态 | 所有版本 |

## 构建与运行

### 环境要求

- Android Studio Arctic Fox 或更高版本
- Android SDK 34 (compileSdk)
- 最低支持 Android 5.0 (API 21)
- 一台支持 Wi-Fi Direct 的 Android 真机（模拟器不支持 Wi-Fi Direct）

### 构建步骤

1. 克隆本仓库：
   ```bash
   git clone <repository-url>
   ```

2. 用 Android Studio 打开项目

3. 同步 Gradle 依赖

4. 连接 Android 真机，点击运行

### 使用步骤

1. 确保设备 Wi-Fi 已打开
2. 授予应用所需的位置/附近设备权限
3. 点击「发现设备」按钮开始扫描
4. 在列表中找到目标设备（如 Windows 电脑），点击发起连接
5. 在目标设备上接受连接请求
6. 连接成功后，界面会显示 Group Owner IP 和本机角色

## 连接 Windows 设备

Windows 10/11 原生支持 Wi-Fi Direct。要让 Windows 设备被 Android 发现：

1. **确保 Windows Wi-Fi 已打开**
2. **Windows 设置 → 网络和 Internet → Wi-Fi → 管理已知网络** 附近确认 Wi-Fi Direct 功能可用
3. Windows 设备可能以 `DIRECT-xx-设备名` 的形式出现在 Android 的发现列表中
4. 部分 Windows 设备需要通过「投影到此电脑」或「附近共享」功能来开启 Wi-Fi Direct 可发现性

> **注意**: 不同 Windows 设备和驱动对 Wi-Fi Direct 的支持程度不同。某些旧版驱动可能不支持 P2P 发现。

## 技术细节

- 使用 `WifiP2pManager` API 管理 P2P 连接的完整生命周期
- 通过 `BroadcastReceiver` 监听四类核心 Wi-Fi Direct 广播事件
- 使用 `WifiP2pConfig` 配置连接参数
- 连接建立后通过 `WifiP2pInfo` 获取 Group Owner 信息
- 适配 Android 13 的 `NEARBY_WIFI_DEVICES` 权限模型

## 许可证

本项目仅用于学习和测试目的。
