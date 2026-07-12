@echo off
chcp 65001 >nul
set JAVA_HOME=D:\Deployment\jdk-21.0.10+7
set PROJECT_DIR=e:\RPG\plugins\GuangDianArmorStats
set GRADLE_CACHE=c:\Users\yang\.gradle\caches\modules-2\files-2.1

echo 正在编译 GuangDianArmorStats 插件...

:: 创建输出目录
if not exist "%PROJECT_DIR%\build\classes" mkdir "%PROJECT_DIR%\build\classes"

:: 设置classpath (使用所有必要的依赖)
set CLASSPATH=%GRADLE_CACHE%\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar;%GRADLE_CACHE%\net.kyori\adventure-api\4.23.0\f6b5b75465f7112dbaf38dd8cfdc5f6b906c53a0\adventure-api-4.23.0.jar;%GRADLE_CACHE%\net.kyori\adventure-key\4.23.0\be07b3e282f9dbc1c0b24a65f0d442ebc504d4d\adventure-key-4.23.0.jar;%GRADLE_CACHE%\net.kyori\adventure-text-minimessage\4.23.0\3230c09453a50090ae354bfc0d8dd6b72026d88\adventure-text-minimessage-4.23.0.jar;%GRADLE_CACHE%\net.kyori\adventure-text-serializer-legacy\4.23.0\e9cacd4d98c1ecab249805e8058cf2eece047d5b\adventure-text-serializer-legacy-4.23.0.jar;%GRADLE_CACHE%\net.kyori\examination-api\1.3.0\8a2d185275307f1e2ef2adf7152b9a0d1d44c30b\examination-api-1.3.0.jar;%GRADLE_CACHE%\net.kyori\examination-string\1.3.0\6f34afef5c54ccce4996bc321abf77518b55b4bd\examination-string-1.3.0.jar;%GRADLE_CACHE%\net.kyori\option\1.1.0\593fecb9c42688eebc7d8da5d6ea127f4d4c92a2\option-1.1.0.jar;e:\RPG\艾尔丽雅\plugins\RPGCore-1.0.0.jar;e:\RPG\艾尔丽雅\plugins\RPGItems-1.0.0.jar;e:\RPG\艾尔丽雅\plugins\[占位符]PlaceholderAPI.jar;e:\RPG\艾尔丽雅\plugins\[生物]MythicMobs.jar

:: 使用 PowerShell 收集所有 Java 文件
powershell -Command "Get-ChildItem -Path '%PROJECT_DIR%\src\main\java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName } | Out-File -Encoding utf8 '%PROJECT_DIR%\java_files.txt'"

:: 编译所有Java文件
echo 正在编译...
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -source 21 -target 21 -cp "%CLASSPATH%" -d "%PROJECT_DIR%\build\classes" @%PROJECT_DIR%\java_files.txt

if %ERRORLEVEL% neq 0 (
    echo 编译失败!
    pause
    exit /b 1
)

echo 编译成功!

:: 复制资源文件
echo 复制资源文件...
xcopy /s /y "%PROJECT_DIR%\src\main\resources" "%PROJECT_DIR%\build\classes"

:: 创建jar文件
echo 创建jar文件...
"%JAVA_HOME%\bin\jar.exe" cvf "%PROJECT_DIR%\build\GuangDianArmorStats-1.0.0.jar" -C "%PROJECT_DIR%\build\classes" .

:: 部署到译梦传说服务器
echo 部署到译梦传说服务器...
copy /y "%PROJECT_DIR%\build\GuangDianArmorStats-1.0.0.jar" "e:\RPG\译梦传说\plugins\GuangDianArmorStats-1.0.0.jar"

echo 部署完成!
pause