@echo off
chcp 65001
cd /d e:\RPG\plugins\RPGItems

echo 正在编译 RPGItems 插件...

:: 设置JAVA_HOME
set JAVA_HOME=D:\Deployment\jdk-21.0.10+7

:: 创建输出目录
if not exist "build\classes" mkdir build\classes

:: 编译所有Java文件 (使用本地Paper jar)
echo 编译Java文件...
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -source 21 -target 21 -cp "c:\Users\yang\.gradle\caches\modules-2\files-2.1\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar;e:\RPG\艾尔丽雅\plugins\RPGCore-1.0.0.jar" -d build\classes src\main\java\cn\guangdian\rpgitems\*.java src\main\java\cn\guangdian\rpgitems\api\*.java src\main\java\cn\guangdian\rpgitems\attribute\*.java src\main\java\cn\guangdian\rpgitems\command\*.java src\main\java\cn\guangdian\rpgitems\config\*.java src\main\java\cn\guangdian\rpgitems\item\*.java src\main\java\cn\guangdian\rpgitems\listener\*.java src\main\java\cn\guangdian\rpgitems\lore\*.java src\main\java\cn\guangdian\rpgitems\migration\*.java src\main\java\cn\guangdian\rpgitems\registry\*.java src\main\java\cn\guangdian\rpgitems\service\*.java src\main\java\cn\guangdian\rpgitems\template\*.java

if %ERRORLEVEL% neq 0 (
    echo 编译失败!
    pause
    exit /b 1
)

:: 复制资源文件
echo 复制资源文件...
xcopy /s /y src\main\resources build\classes

:: 创建jar文件
echo 创建jar文件...
"%JAVA_HOME%\bin\jar.exe" cvf build\RPGItems-1.0.0.jar -C build\classes . -C src\main\resources plugin.yml

echo 编译成功! jar文件位于: build\RPGItems-1.0.0.jar
pause