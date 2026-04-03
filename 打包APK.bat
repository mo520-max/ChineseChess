@echo off
chcp 65001 >nul
title 中国象棋 - APK打包工具
color 0A

echo ==========================================
echo    中国象棋 APK 打包工具
echo ==========================================
echo.

REM 检查 Java 环境
if not defined JAVA_HOME (
    echo [错误] 未设置 JAVA_HOME 环境变量
    echo.
    echo 请先安装 JDK 17 或更高版本：
    echo https://adoptium.net/
    echo.
    echo 安装后设置环境变量 JAVA_HOME
    pause
    exit /b 1
)

echo [1/6] 检查 Java 版本...
"%JAVA_HOME%\bin\java" -version 2>&1 | findstr "version"
if errorlevel 1 (
    echo [错误] Java 环境配置不正确
    pause
    exit /b 1
)
echo [OK] Java 环境正常
echo.

REM 检查 Android SDK
if not defined ANDROID_HOME (
    echo [警告] 未设置 ANDROID_HOME 环境变量
    echo.
    echo 请安装 Android Studio 并配置 SDK：
    echo https://developer.android.com/studio
    echo.
    echo 或者手动设置 ANDROID_HOME 环境变量
    pause
    exit /b 1
)

echo [2/6] 检查 Android SDK...
if not exist "%ANDROID_HOME%\platforms" (
    echo [错误] Android SDK 路径不正确
    pause
    exit /b 1
)
echo [OK] Android SDK 正常
echo.

echo [3/6] 清理旧构建...
call gradlew.bat clean
if errorlevel 1 (
    echo [错误] 清理失败，尝试继续...
)
echo [OK] 清理完成
echo.

echo [4/6] 下载依赖（首次可能需要较长时间）...
call gradlew.bat dependencies --configuration implementation
if errorlevel 1 (
    echo [警告] 依赖检查失败，尝试继续构建...
)
echo [OK] 依赖准备完成
echo.

echo [5/6] 构建 Debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo.
    echo [错误] 构建失败！
    echo.
    echo 常见解决方案：
    echo 1. 检查网络连接（需要下载依赖）
    echo 2. 检查 Gradle 配置
    echo 3. 查看错误日志
    echo.
    pause
    exit /b 1
)
echo [OK] 构建成功
echo.

echo [6/6] 检查APK文件...
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if not exist "%APK_PATH%" (
    echo [错误] 未找到APK文件
    pause
    exit /b 1
)

echo.
echo ==========================================
echo    打包完成！
echo ==========================================
echo.
echo APK 文件位置:
echo   %CD%\%APK_PATH%
echo.
for %%I in (%APK_PATH%) do (
    echo 文件大小: %%~zI 字节
echo   约 %%~zI / 1024 / 1024 MB
)
echo.
echo 安装到手机:
echo   1. 使用USB连接手机
echo   2. 开启USB调试模式
echo   3. 运行: adb install -r %APK_PATH%
echo.
echo 或者直接复制APK到手机安装
echo.

REM 复制APK到桌面（可选）
set DESKTOP=%USERPROFILE%\Desktop
copy "%APK_PATH%" "%DESKTOP%\中国象棋.apk" >nul 2>&1
if not errorlevel 1 (
    echo [提示] APK已复制到桌面: 中国象棋.apk
    echo.
)

echo 按任意键退出...
pause >nul
