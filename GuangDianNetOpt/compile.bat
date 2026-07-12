@echo off
chcp 65001 >nul
set JAVA_HOME=D:\Deployment\jdk-21.0.10+7
set PROJECT_DIR=e:\RPG\plugins\GuangDianNetOpt
set GRADLE_CACHE=c:\Users\yang\.gradle\caches\modules-2\files-2.1

echo Compiling GuangDianNetOpt plugin...

:: Create output directory
if not exist "%PROJECT_DIR%\build\classes" mkdir "%PROJECT_DIR%\build\classes"

:: Set classpath
set CLASSPATH=%GRADLE_CACHE%\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar

:: Compile all Java files
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -source 21 -target 21 -cp "%CLASSPATH%" -d "%PROJECT_DIR%\build\classes" ^
"%PROJECT_DIR%\src\main\java\cn\guangdian\netopt\GuangDianNetOpt.java" ^
"%PROJECT_DIR%\src\main\java\cn\guangdian\netopt\config\NetOptConfig.java" ^
"%PROJECT_DIR%\src\main\java\cn\guangdian\netopt\command\NetOptCommand.java" ^
"%PROJECT_DIR%\src\main\java\cn\guangdian\netopt\listener\PacketListener.java" ^
"%PROJECT_DIR%\src\main\java\cn\guangdian\netopt\manager\BatchPacketManager.java" ^
"%PROJECT_DIR%\src\main\java\cn\guangdian\netopt\manager\BandwidthMonitor.java" ^
"%PROJECT_DIR%\src\main\java\cn\guangdian\netopt\manager\EntityOptManager.java" ^
"%PROJECT_DIR%\src\main\java\cn\guangdian\netopt\manager\ChunkOptManager.java"

if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!

:: Copy resource files
echo Copying resource files...
xcopy /s /y "%PROJECT_DIR%\src\main\resources" "%PROJECT_DIR%\build\classes"

:: Create jar file
echo Creating jar file...
"%JAVA_HOME%\bin\jar.exe" cvf "%PROJECT_DIR%\build\GuangDianNetOpt.jar" -C "%PROJECT_DIR%\build\classes" .

:: Deploy to server
echo Deploying to server...
copy /y "%PROJECT_DIR%\build\GuangDianNetOpt.jar" "e:\RPG\译梦传说\plugins\GuangDianNetOpt.jar"

echo Deployment complete!
pause