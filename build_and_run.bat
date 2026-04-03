@echo off
chcp 65001 >nul
echo ==========================================
echo    中国象棋应用 - 构建和运行脚本
echo ==========================================
echo.

REM 检查 Java 环境
if not defined JAVA_HOME (
    echo [错误] 未设置 JAVA_HOME 环境变量
    echo 请先安装 JDK 17 或更高版本，并配置 JAVA_HOME
    pause
    exit /b 1
)

echo [1/5] 检查 Java 版本...
"%JAVA_HOME%\bin\java" -version
echo.

REM 检查 Android SDK
if not defined ANDROID_HOME (
    echo [警告] 未设置 ANDROID_HOME 环境变量
    echo 某些功能可能无法正常工作
    echo.
)

echo [2/5] 清理旧构建...
call gradlew.bat clean
if errorlevel 1 (
    echo [错误] 清理失败
    pause
    exit /b 1
)
echo.

echo [3/5] 构建项目...
call gradlew.bat build
if errorlevel 1 (
    echo [错误] 构建失败
    pause
    exit /b 1
)
echo.

echo [4/5] 运行单元测试...
call gradlew.bat test
if errorlevel 1 (
    echo [警告] 部分测试失败
) else (
    echo [成功] 所有测试通过
)
echo.

echo [5/5] 生成 APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo [错误] APK 生成失败
    pause
    exit /b 1
)
echo.

echo ==========================================
echo    构建完成！
echo ==========================================
echo.
echo APK 位置: app\build\outputs\apk\debug\app-debug.apk
echo.
echo 安装到设备:
echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.
pause
