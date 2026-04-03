# 中国象棋 - 安卓双人对战应用

一款支持蓝牙直连和局域网Wi-Fi对战的中国象棋安卓应用。

## 功能特性

### 核心功能
- 完整的中国象棋规则实现
- 棋子拖拽/点击移动
- 移动前高亮合法落点
- 悔棋功能（单机模式）
- 将军、将死判定

### 对战模式
- **单机练习**：本地双人对弈
- **蓝牙对战**：通过蓝牙直连对战
- **局域网对战**：通过Wi-Fi局域网对战

### 时间控制
- 无限制模式
- 总时间模式（每方限时）
- 步时模式（每步限时）
- 实时倒计时显示
- 超时自动判负

### 其他功能
- 认输功能
- 和棋申请
- 暂停/继续
- 音效和震动反馈
- 棋盘主题切换

## 技术栈

- **开发语言**：Kotlin
- **UI框架**：XML布局 + 自定义View
- **网络通信**：
  - 蓝牙：Android Bluetooth API
  - 局域网：Java Socket/TCP + UDP广播
- **架构**：MVVM模式
- **异步处理**：Kotlin Coroutines

## 项目结构

```
app/src/main/java/com/chinesechess/
├── model/              # 数据模型
│   ├── PieceType.kt    # 棋子类型、颜色、游戏状态枚举
│   ├── Piece.kt        # 棋子数据类
│   └── Move.kt         # 移动记录数据类
├── game/               # 游戏逻辑
│   ├── ChessEngine.kt  # 象棋规则引擎
│   └── GameManager.kt  # 游戏管理器
├── network/            # 网络通信
│   └── NetworkManager.kt # 网络通信管理器
├── bluetooth/          # 蓝牙功能
├── wifi/               # 局域网功能
├── ui/                 # UI界面
│   ├── MainActivity.kt       # 主界面
│   ├── GameActivity.kt       # 游戏界面
│   ├── BluetoothActivity.kt  # 蓝牙对战界面
│   ├── WifiActivity.kt       # 局域网对战界面
│   ├── SettingsActivity.kt   # 设置界面
│   └── ChessBoardView.kt     # 棋盘自定义View
└── utils/              # 工具类

app/src/main/res/
├── layout/             # 布局文件
├── values/             # 资源值（字符串、颜色、主题）
└── drawable/           # 图形资源
```

## 安装要求

- Android 8.0 (API 26) 及以上版本
- 蓝牙功能（蓝牙对战模式）
- Wi-Fi功能（局域网对战模式）

## 权限说明

应用需要以下权限：

### 蓝牙权限
- `BLUETOOTH` - 蓝牙连接
- `BLUETOOTH_ADMIN` - 蓝牙管理
- `BLUETOOTH_SCAN` - 蓝牙扫描（Android 12+）
- `BLUETOOTH_CONNECT` - 蓝牙连接（Android 12+）
- `BLUETOOTH_ADVERTISE` - 蓝牙广播（Android 12+）
- `ACCESS_FINE_LOCATION` - 位置信息（蓝牙扫描需要）

### 网络权限
- `INTERNET` - 网络连接
- `ACCESS_NETWORK_STATE` - 网络状态
- `ACCESS_WIFI_STATE` - Wi-Fi状态
- `CHANGE_WIFI_STATE` - 修改Wi-Fi状态

### 其他权限
- `VIBRATE` - 震动反馈

## 使用说明

### 单机练习
1. 在主界面点击"单机练习"
2. 红方先行，双方轮流点击棋子进行移动
3. 选中棋子后会高亮显示合法移动位置
4. 点击高亮位置即可移动棋子

### 蓝牙对战
1. 在主界面点击"蓝牙对战"
2. 一方选择"作为主机"，等待连接
3. 另一方选择"作为从机"，搜索并连接主机
4. 连接成功后自动进入游戏
5. 主机为红方，从机为黑方

### 局域网对战
1. 在主界面点击"局域网对战"
2. 一方选择"创建房间"，显示本机IP地址
3. 另一方选择"加入房间"，搜索或直接输入IP
4. 连接成功后自动进入游戏
5. 创建者为红方，加入者为黑方

### 时间设置
1. 在主界面点击"设置"
2. 选择时间模式：无限制/总时间/步时
3. 设置具体时间值
4. 保存设置后生效

## 开发计划

- [x] 基础框架搭建
- [x] 象棋规则引擎
- [x] 棋盘UI界面
- [x] 单机对战模式
- [x] 蓝牙对战功能
- [x] 局域网对战功能
- [x] 时间控制功能
- [ ] AI对手（单机练习）
- [ ] 观战模式
- [ ] 对战记录保存与复盘
- [ ] 音效和震动反馈

## 通信协议

### 消息格式
所有消息以字符串形式传输，格式为：`命令类型:数据`

### 命令列表
- `MOVE:pieceType,pieceColor,fromX,fromY,toX,toY` - 移动棋子
- `UNDO_REQUEST` - 悔棋请求
- `UNDO_RESPONSE:true/false` - 悔棋响应
- `DRAW_REQUEST` - 和棋请求
- `DRAW_RESPONSE:true/false` - 和棋响应
- `SURRENDER` - 认输
- `GAME_START` - 游戏开始
- `HEARTBEAT` - 心跳包

## 版本历史

### v1.0.0
- 初始版本发布
- 支持单机、蓝牙、局域网三种对战模式
- 实现完整的中国象棋规则
- 支持时间控制功能

## 作者

中国象棋开发团队

## 许可证

MIT License
