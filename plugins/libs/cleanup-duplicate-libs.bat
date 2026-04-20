@echo off
chcp 65001 >nul
echo ========================================
echo 清理重复的依赖库文件
echo ========================================
echo.

set PLUGINS_DIR=%~dp0..

echo 正在删除重复的 Vault.jar...
if exist "%PLUGINS_DIR%\GuangDianItemTrigger\libs\Vault.jar" (
    del "%PLUGINS_DIR%\GuangDianItemTrigger\libs\Vault.jar"
    echo   [已删除] GuangDianItemTrigger\libs\Vault.jar
)
if exist "%PLUGINS_DIR%\GuangDianMarket\libs\Vault.jar" (
    del "%PLUGINS_DIR%\GuangDianMarket\libs\Vault.jar"
    echo   [已删除] GuangDianMarket\libs\Vault.jar
)
if exist "%PLUGINS_DIR%\GuangDianMenu\libs\Vault.jar" (
    del "%PLUGINS_DIR%\GuangDianMenu\libs\Vault.jar"
    echo   [已删除] GuangDianMenu\libs\Vault.jar
)
if exist "%PLUGINS_DIR%\GuangDianPoints\libs\Vault.jar" (
    del "%PLUGINS_DIR%\GuangDianPoints\libs\Vault.jar"
    echo   [已删除] GuangDianPoints\libs\Vault.jar
)
if exist "%PLUGINS_DIR%\GuangDianTab\libs\Vault.jar" (
    del "%PLUGINS_DIR%\GuangDianTab\libs\Vault.jar"
    echo   [已删除] GuangDianTab\libs\Vault.jar
)

echo.
echo 正在删除重复的 PlaceholderAPI.jar...
if exist "%PLUGINS_DIR%\GuangDianArmorStats\libs\PlaceholderAPI.jar" (
    del "%PLUGINS_DIR%\GuangDianArmorStats\libs\PlaceholderAPI.jar"
    echo   [已删除] GuangDianArmorStats\libs\PlaceholderAPI.jar
)

echo.
echo ========================================
echo 清理完成！
echo ========================================
echo.
echo 注意: 请更新各插件的 build.gradle 文件，使用共享依赖路径
echo       compileOnly files('../../libs/Vault.jar')
echo       compileOnly files('../../libs/PlaceholderAPI.jar')
echo.
pause
