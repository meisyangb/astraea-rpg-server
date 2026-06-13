@echo off
setlocal enabledelayedexpansion

echo ========================================
echo 编译 GuangDianRegen 插件
echo ========================================

set PLUGIN_DIR=e:\RPG\原创RPG服务端-插件最多提交\plugins\GuangDianRegen
set SRC_DIR=%PLUGIN_DIR%\src\main\java
set RES_DIR=%PLUGIN_DIR%\src\main\resources
set BUILD_DIR=%PLUGIN_DIR%\build
set CLASSES_DIR=%BUILD_DIR%\classes

set PAPER_API=C:\Users\24141\.gradle\caches\modules-2\files-2.1\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar

echo 清理构建目录...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"

echo 编译Java文件...
set JAVA_FILES=
for /r "%SRC_DIR%" %%f in (*.java) do (
    set JAVA_FILES=!JAVA_FILES! "%%f"
)

javac -cp "%PAPER_API%" -d "%CLASSES_DIR%" %JAVA_FILES%

if %ERRORLEVEL% neq 0 (
    echo 编译失败!
    exit /b 1
)

echo 复制资源文件...
xcopy /s /y "%RES_DIR%\*" "%CLASSES_DIR%\"

echo 创建JAR文件...
cd "%CLASSES_DIR%"
jar cf "%BUILD_DIR%\GuangDianRegen-1.0.0.jar" *

echo ========================================
echo 构建完成: %BUILD_DIR%\GuangDianRegen-1.0.0.jar
echo ========================================

dir "%BUILD_DIR%\GuangDianRegen-1.0.0.jar"

endlocal
