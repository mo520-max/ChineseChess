# APK打包指南

## 方法一：使用 Android Studio（推荐）

### 步骤1：安装 Android Studio
1. 下载 Android Studio：https://developer.android.com/studio
2. 安装并启动

### 步骤2：导入项目
1. 打开 Android Studio
2. 选择 "Open an existing Android Studio project"
3. 选择 `ChineseChess` 文件夹
4. 等待 Gradle 同步完成

### 步骤3：构建APK
1. 点击菜单栏 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. 等待构建完成
3. APK文件位置：`app/build/outputs/apk/debug/app-debug.apk`

### 步骤4：导出APK
1. 点击右下角的 `Event Log`
2. 点击 `locate` 链接
3. 复制 `app-debug.apk` 到手机

## 方法二：使用命令行

### 前提条件
1. 安装 JDK 17+
2. 安装 Android SDK
3. 配置环境变量：
   - `JAVA_HOME` - JDK路径
   - `ANDROID_HOME` - Android SDK路径

### 构建命令

```bash
# 进入项目目录
cd ChineseChess

# 清理并构建
gradlew clean
gradlew assembleDebug

# APK位置：app/build/outputs/apk/debug/app-debug.apk
```

## 方法三：使用一键打包脚本

### Windows
双击运行 `打包APK.bat`

### 结果
APK将生成在：`app/build/outputs/apk/debug/app-debug.apk`

## 安装到手机

### 方式1：USB连接
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 方式2：传输文件
1. 将APK复制到手机存储
2. 在手机上点击APK文件安装
3. 允许安装未知来源应用

## 常见问题

### Q: Gradle同步失败？
**解决：**
1. 检查网络连接
2. 更换镜像源（settings.gradle.kts中配置阿里云镜像）
3. 删除 `.gradle` 文件夹重试

### Q: 缺少SDK？
**解决：**
1. 打开 SDK Manager
2. 安装 Android SDK Platform 26-34
3. 安装 Android SDK Build-Tools 34.0.0

### Q: 构建失败？
**解决：**
1. 查看错误日志
2. 确保所有依赖已下载
3. 尝试 `File` → `Invalidate Caches / Restart`

## 发布版本（Release）

### 生成签名密钥
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
```

### 配置签名（app/build.gradle.kts）
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks")
            storePassword = "your-password"
            keyAlias = "my-alias"
            keyPassword = "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 构建Release APK
```bash
gradlew assembleRelease
```

Release APK位置：`app/build/outputs/apk/release/app-release.apk`
