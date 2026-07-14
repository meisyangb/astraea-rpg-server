@echo off
chcp 65001
cd /d "e:\RPG\plugins\GuangDianSect"

echo 正在编译 GuangDianSect 门派插件...

:: 设置JAVA_HOME
set JAVA_HOME=D:\Deployment\jdk-21.0.10+7

:: 创建输出目录
if not exist "build\classes" mkdir build\classes

:: 编译所有Java文件
echo 编译Java文件...
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -source 21 -target 21 -cp "c:\Users\yang\.gradle\caches\modules-2\files-2.1\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar;e:\RPG\plugins\RPGCore\build\libs\RPGCore-1.0.0.jar;e:\RPG\plugins\GuangDianArmorStats\libs\PlaceholderAPI.jar" -d build\classes src\main\java\cn\guangdian\sect\*.java

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
"%JAVA_HOME%\bin\jar.exe" cvf build\GuangDianSect-1.0.0.jar -C build\classes .

:: 部署到服务器
echo 部署到服务器...
copy /Y build\GuangDianSect-1.0.0.jar "e:\RPG\译梦传说2\plugins\GuangDianSect-1.0.0.jar"

echo 编译成功! 已部署到 e:\RPG\译梦传说2\plugins\GuangDianSect-1.0.0.jar
pause