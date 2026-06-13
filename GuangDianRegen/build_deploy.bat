@echo off
setlocal

echo ========================================
echo Building GuangDianRegen Plugin
echo ========================================

set JAVA_HOME=e:\RPG\tools\jdk-21.0.10+7
set PLUGIN_DIR=e:\RPG\原创RPG服务端-插件最多提交\plugins\GuangDianRegen
set PAPER_API=C:\Users\24141\.gradle\caches\modules-2\files-2.1\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar
set CLASSES_DIR=%PLUGIN_DIR%\build\classes
set LIBS_DIR=%PLUGIN_DIR%\build\libs
set TARGET_DIR=e:\RPG\艾德拉 - 副本\plugins

echo.
echo Compiling Java files...
echo.

"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -cp "%PAPER_API%" -d "%CLASSES_DIR%" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\model\RegenType.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\model\RegenBlock.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\model\RegenRegion.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\manager\SelectionManager.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\manager\RegionManager.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\manager\RegenManager.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\listener\BlockBreakListener.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\listener\SelectionListener.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\command\RegenCommand.java" ^
  "%PLUGIN_DIR%\src\main\java\cn\guangdian\regen\GuangDianRegen.java"

if %ERRORLEVEL% neq 0 (
    echo.
    echo Compilation failed!
    exit /b 1
)

echo.
echo Compilation successful!
echo.

echo Copying resources...
xcopy /s /y /q "%PLUGIN_DIR%\src\main\resources\*" "%CLASSES_DIR%\" >nul

echo Creating JAR file...
cd "%CLASSES_DIR%"
"%JAVA_HOME%\bin\jar.exe" cf "%LIBS_DIR%\GuangDianRegen-1.0.0.jar" *

if not exist "%LIBS_DIR%\GuangDianRegen-1.0.0.jar" (
    echo JAR creation failed!
    exit /b 1
)

for %%F in ("%LIBS_DIR%\GuangDianRegen-1.0.0.jar") do (
    echo JAR created: %%~nxF (%%~zF bytes)
)

echo.
echo ========================================
echo Deploying to server
echo ========================================

echo.
echo Target: %TARGET_DIR%
echo.

copy /Y "%LIBS_DIR%\GuangDianRegen-1.0.0.jar" "%TARGET_DIR%\" >nul

if %ERRORLEVEL% neq 0 (
    echo Deployment failed!
    exit /b 1
)

echo Deployment successful!
echo.

for %%F in ("%TARGET_DIR%\GuangDianRegen-1.0.0.jar") do (
    echo File: %%~nxF
    echo Size: %%~zF bytes
    echo Time: %%~tF
)

echo.
echo ========================================
echo Build and Deploy Complete!
echo ========================================

endlocal
