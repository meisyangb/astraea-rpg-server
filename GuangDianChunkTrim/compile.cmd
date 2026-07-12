@echo off
setlocal enabledelayedexpansion

echo === GuangDianChunkTrim 编译脚本 ===
echo.

set JAVA_HOME=D:\Deployment\jdk-21.0.10+7
set PLUGIN_NAME=GuangDianChunkTrim
set PLUGIN_DIR=e:\RPG\plugins\%PLUGIN_NAME%
set SERVER_DIR=e:\RPG\译梦传说\plugins

REM 设置 CLASSPATH (Paper API + Adventure)
set GRADLE_CACHE=c:\Users\yang\.gradle\caches\modules-2\files-2.1
set CLASSPATH=%GRADLE_CACHE%\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar
set CLASSPATH=%CLASSPATH%;%GRADLE_CACHE%\net.kyori\adventure-api\4.23.0\f6b5b75465f7112dbaf38dd8cfdc5f6b906c53a0\adventure-api-4.23.0.jar
set CLASSPATH=%CLASSPATH%;%GRADLE_CACHE%\net.kyori\adventure-key\4.23.0\be07b3e282f9dbc1c0b24a65f0d442ebc504d4d\adventure-key-4.23.0.jar
set CLASSPATH=%CLASSPATH%;%GRADLE_CACHE%\net.kyori\adventure-text-minimessage\4.23.0\3230c09453a50090ae354bfc0d8dd6b72026d88\adventure-text-minimessage-4.23.0.jar
set CLASSPATH=%CLASSPATH%;%GRADLE_CACHE%\net.kyori\adventure-text-serializer-legacy\4.23.0\e9cacd4d98c1ecab249805e8058cf2eece047d5b\adventure-text-serializer-legacy-4.23.0.jar
set CLASSPATH=%CLASSPATH%;%GRADLE_CACHE%\net.kyori\examination-api\1.3.0\8a2d185275307f1e2ef2adf7152b9a0d1d44c30b\examination-api-1.3.0.jar
set CLASSPATH=%CLASSPATH%;%GRADLE_CACHE%\net.kyori\examination-string\1.3.0\6f34afef5c54ccce4996bc321abf77518b55b4bd\examination-string-1.3.0.jar
set CLASSPATH=%CLASSPATH%;%GRADLE_CACHE%\net.kyori\option\1.1.0\593fecb9c42688eebc7d8da5d6ea127f4d4c92a2\option-1.1.0.jar

REM 添加 RPGCore (如果存在)
if exist "%SERVER_DIR%\RPGCore-1.0.0.jar" (
    set CLASSPATH=!CLASSPATH!;%SERVER_DIR%\RPGCore-1.0.0.jar
)

REM 创建输出目录
if not exist "%PLUGIN_DIR%\build\classes" mkdir "%PLUGIN_DIR%\build\classes"
if not exist "%PLUGIN_DIR%\build\jar" mkdir "%PLUGIN_DIR%\build\jar"

echo 编译 Java 文件...
echo.

REM 编译所有 Java 文件
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -source 21 -target 21 -cp "%CLASSPATH%" -d "%PLUGIN_DIR%\build\classes" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\chunktrim\GuangDianChunkTrim.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\chunktrim\config\TrimConfig.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\chunktrim\manager\TrimManager.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\chunktrim\task\TrimTask.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\chunktrim\command\ChunkTrimCommand.java"

if errorlevel 1 (
    echo.
    echo 编译失败!
    pause
    exit /b 1
)

echo.
echo 编译成功! 创建 JAR 文件...

REM 复制资源文件
xcopy /Y /S "%PLUGIN_DIR%\src\main\resources\*" "%PLUGIN_DIR%\build\classes\"

REM 创建 JAR 文件
"%JAVA_HOME%\bin\jar.exe" cf "%PLUGIN_DIR%\build\jar\%PLUGIN_NAME%-1.0.0.jar" -C "%PLUGIN_DIR%\build\classes" .

REM 复制到服务器
if exist "%PLUGIN_DIR%\build\jar\%PLUGIN_NAME%-1.0.0.jar" (
    copy /Y "%PLUGIN_DIR%\build\jar\%PLUGIN_NAME%-1.0.0.jar" "%SERVER_DIR%"
    echo.
    echo 已部署到: %SERVER_DIR%\%PLUGIN_NAME%-1.0.0.jar
) else (
    echo JAR 文件创建失败!
    pause
    exit /b 1
)

echo.
echo === 编译完成 ===
pause