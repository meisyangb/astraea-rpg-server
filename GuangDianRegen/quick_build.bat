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
set LIBS_DIR=%BUILD_DIR%\libs

set PAPER_API=C:\Users\24141\.gradle\caches\modules-2\files-2.1\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar

set TARGET_DIR=e:\RPG\艾德拉 - 副本\plugins

echo.
echo 清理构建目录...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"
mkdir "%LIBS_DIR%"

echo.
echo 编译Java文件...
set JAVA_FILES=
for /r "%SRC_DIR%" %%f in (*.java) do (
    set JAVA_FILES=!JAVA_FILES! "%%f"
)

javac -encoding UTF-8 -cp "%PAPER_API%" -d "%CLASSES_DIR%" %JAVA_FILES%

if %ERRORLEVEL% neq 0 (
    echo 编译失败!
    exit /b 1
)

echo 编译成功!

echo.
echo 复制资源文件...
xcopy /s /y /q "%RES_DIR%\*" "%CLASSES_DIR%\" >nul

echo.
echo 创建JAR文件...
cd "%CLASSES_DIR%"
jar cf "%LIBS_DIR%\GuangDianRegen-1.0.0.jar" *

if not exist "%LIBS_DIR%\GuangDianRegen-1.0.0.jar" (
    echo JAR创建失败!
    exit /b 1
)

for %%F in ("%LIBS_DIR%\GuangDianRegen-1.0.0.jar") do (
    echo JAR创建成功: %%~nxF (%%~zF bytes)
)

echo.
echo ========================================
echo 部署到服务器
echo ========================================

echo.
echo 目标目录: %TARGET_DIR%

if not exist "%TARGET_DIR%" (
    echo 目标目录不存在!
    exit /b 1
)

echo.
echo 复制JAR文件...
copy /Y "%LIBS_DIR%\GuangDianRegen-1.0.0.jar" "%TARGET_DIR%\" >nul

if %ERRORLEVEL% neq 0 (
    echo 复制失败!
    exit /b 1
)

echo 部署成功!

echo.
echo 验证部署...
if exist "%TARGET_DIR%\GuangDianRegen-1.0.0.jar" (
    for %%F in ("%TARGET_DIR%\GuangDianRegen-1.0.0.jar") do (
        echo 文件大小: %%~zF bytes
        echo 修改时间: %%~tF
    )
) else (
    echo 验证失败!
    exit /b 1
)

echo.
echo ========================================
echo 构建和部署完成!
echo ========================================

endlocal
