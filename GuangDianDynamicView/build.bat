@echo off
chcp 65001 >nul
echo ========================================
echo GuangDianDynamicView 插件构建脚本
echo ========================================

cd /d e:\RPG\原创RPG服务端-插件最多提交
set JAVA_HOME=e:\RPG\原创RPG服务端-插件最多提交\tools\jdk-21.0.10+7

echo.
echo [1/3] 清理旧构建...
if exist "plugins\GuangDianDynamicView\build" rmdir /s /q "plugins\GuangDianDynamicView\build"

echo.
echo [2/3] 开始构建...
D:\gradle\gradle-9.4.0\bin\gradle.bat :plugins:GuangDianDynamicView:build --no-configuration-cache -x test

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] 构建失败！
    pause
    exit /b 1
)

echo.
echo [3/3] 检查构建结果...
if exist "plugins\GuangDianDynamicView\build\libs\GuangDianDynamicView.jar" (
    echo.
    echo ========================================
    echo [成功] 插件构建完成！
    echo 输出文件: plugins\GuangDianDynamicView\build\libs\GuangDianDynamicView.jar
    echo ========================================
    
    REM 复制到服务器
    if exist "e:\RPG\译梦传说\plugins\" (
        copy /y "plugins\GuangDianDynamicView\build\libs\GuangDianDynamicView.jar" "e:\RPG\译梦传说\plugins\"
        echo.
        echo [部署] 已复制到服务器插件目录
    )
) else (
    echo.
    echo [错误] 未找到构建输出文件
    pause
    exit /b 1
)

pause
