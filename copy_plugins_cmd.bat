@echo off
echo ==========================================
echo 复制插件到服务器文件夹
echo ==========================================
echo.

set "SOURCE_DIR=e:\原创RPG服务端\plugins"
set "TARGET_DIR=e:\原创RPG服务端\server\plugins"

if not exist "%TARGET_DIR%" (
    echo 创建目标目录...
    mkdir "%TARGET_DIR%"
)

echo 正在复制插件...
echo.

set COPIED=0

for /d %%D in ("%SOURCE_DIR%\GuangDian*", "%SOURCE_DIR%\RPGCore") do (
    if exist "%%D\build\libs\*.jar" (
        for %%F in ("%%D\build\libs\*.jar") do (
            echo 复制: %%~nF.jar
            copy /Y "%%F" "%TARGET_DIR%\" >nul
            set /a COPIED+=1
        )
    )
)

echo.
echo ==========================================
echo 部署完成!
echo 共复制 %COPIED% 个插件
echo 目标目录: %TARGET_DIR%
echo ==========================================
