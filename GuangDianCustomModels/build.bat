@echo off
chcp 65001 >nul
echo ========== GuangDianCustomModels 构建脚本 ==========
echo.

set JAVA_HOME=e:\RPG\原创RPG服务端-插件最多提交\tools\jdk-21.0.10+7
set GRADLE_HOME=D:\gradle\gradle-9.4.0
set PROJECT_DIR=e:\RPG\原创RPG服务端-插件最多提交

echo [步骤1] 设置环境变量
echo JAVA_HOME=%JAVA_HOME%
echo GRADLE_HOME=%GRADLE_HOME%
echo.

echo [步骤2] 检查JDK
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JDK不存在: %JAVA_HOME%
    pause
    exit /b 1
)
echo JDK检查通过
echo.

echo [步骤3] 检查Gradle
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo ERROR: Gradle不存在: %GRADLE_HOME%
    pause
    exit /b 1
)
echo Gradle检查通过
echo.

echo [步骤4] 开始构建
cd /d "%PROJECT_DIR%"
call "%GRADLE_HOME%\bin\gradle.bat" build --no-configuration-cache -x test

echo.
echo [步骤5] 检查构建结果
if exist "plugins\GuangDianCustomModels\build\libs\GuangDianCustomModels-1.0.0.jar" (
    echo SUCCESS: 构建成功!
    echo JAR文件位置: %PROJECT_DIR%\plugins\GuangDianCustomModels\build\libs\GuangDianCustomModels-1.0.0.jar
    echo.
    dir "plugins\GuangDianCustomModels\build\libs\*.jar"
) else (
    echo ERROR: 构建失败，未找到JAR文件
    pause
    exit /b 1
)

echo.
echo ========== 构建完成 ==========
pause