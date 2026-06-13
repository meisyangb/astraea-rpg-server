@echo off
chcp 65001 >nul
echo ========================================
echo 光点聊天插件编译脚本
echo ========================================

set JAVA_HOME=E:\原创RPG服务端\tools\jdk-21.0.10+7
set SERVER_DIR=E:\原创RPG服务端\server
set PLUGIN_DIR=E:\原创RPG服务端\plugins\GuangDianChat
set OUTPUT_DIR=%PLUGIN_DIR%\target

echo 创建输出目录...
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
if not exist "%OUTPUT_DIR%\classes" mkdir "%OUTPUT_DIR%\classes"

echo 复制资源文件...
xcopy /Y /Q "%PLUGIN_DIR%\src\main\resources\*" "%OUTPUT_DIR%\classes\"

echo 编译 Java 源文件...
"%JAVA_HOME%\bin\javac" -encoding UTF-8 -d "%OUTPUT_DIR%\classes" -cp "%SERVER_DIR%\paper.jar;%SERVER_DIR%\plugins\PlaceholderAPI\*" "%PLUGIN_DIR%\src\main\java\cn\guangdian\chat\GuangDianChat.java"

if %ERRORLEVEL% neq 0 (
    echo 编译失败!
    pause
    exit /b 1
)

echo 创建 JAR 文件...
cd "%OUTPUT_DIR%\classes"
"%JAVA_HOME%\bin\jar" cf "%SERVER_DIR%\plugins\[聊天]GuangDianChat.jar" *

if %ERRORLEVEL% neq 0 (
    echo JAR 打包失败!
    pause
    exit /b 1
)

echo ========================================
echo 编译成功! 插件已保存到:
echo %SERVER_DIR%\plugins\[聊天]GuangDianChat.jar
echo ========================================
pause
