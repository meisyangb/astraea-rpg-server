# GuangDianChunkTrim 编译脚本
# 使用 javac 直接编译

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=== GuangDianChunkTrim 编译脚本 ===" -ForegroundColor Cyan
Write-Host ""

# 配置
$JavaHome = "D:\Deployment\jdk-21.0.10+7"
$PluginName = "GuangDianChunkTrim"
$PluginDir = "e:\RPG\plugins\$PluginName"
$ServerDir = "e:\RPG\译梦传说\plugins"
$GradleCache = "c:\Users\yang\.gradle\caches\modules-2\files-2.1"

# 设置 CLASSPATH
$Classpath = @(
    "$GradleCache\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar",
    "$GradleCache\net.kyori\adventure-api\4.23.0\f6b5b75465f7112dbaf38dd8cfdc5f6b906c53a0\adventure-api-4.23.0.jar",
    "$GradleCache\net.kyori\adventure-key\4.23.0\be07b3e282f9dbc1c0b24a65f0d442ebc504d4d\adventure-key-4.23.0.jar",
    "$GradleCache\net.kyori\adventure-text-minimessage\4.23.0\3230c09453a50090ae354bfc0d8dd6b72026d88\adventure-text-minimessage-4.23.0.jar",
    "$GradleCache\net.kyori\adventure-text-serializer-legacy\4.23.0\e9cacd4d98c1ecab249805e8058cf2eece047d5b\adventure-text-serializer-legacy-4.23.0.jar",
    "$GradleCache\net.kyori\examination-api\1.3.0\8a2d185275307f1e2ef2adf7152b9a0d1d44c30b\examination-api-1.3.0.jar",
    "$GradleCache\net.kyori\examination-string\1.3.0\6f34afef5c54ccce4996bc321abf77518b55b4bd\examination-string-1.3.0.jar",
    "$GradleCache\net.kyori\option\1.1.0\593fecb9c42688eebc7d8da5d6ea127f4d4c92a2\option-1.1.0.jar"
) -join ";"

# RPGCore (如果存在)
if (Test-Path "$ServerDir\RPGCore-1.0.0.jar") {
    $Classpath += ";$ServerDir\RPGCore-1.0.0.jar"
}

# 创建输出目录
$ClassesDir = "$PluginDir\build\classes"
$JarDir = "$PluginDir\build\jar"

if (-not (Test-Path $ClassesDir)) { New-Item -ItemType Directory -Path $ClassesDir -Force | Out-Null }
if (-not (Test-Path $JarDir)) { New-Item -ItemType Directory -Path $JarDir -Force | Out-Null }

Write-Host "编译 Java 文件..." -ForegroundColor Yellow
Write-Host ""

# Java 文件列表
$JavaFiles = @(
    "$PluginDir\src\main\java\cn\guangdian\chunktrim\GuangDianChunkTrim.java",
    "$PluginDir\src\main\java\cn\guangdian\chunktrim\config\TrimConfig.java",
    "$PluginDir\src\main\java\cn\guangdian\chunktrim\manager\TrimManager.java",
    "$PluginDir\src\main\java\cn\guangdian\chunktrim\task\TrimTask.java",
    "$PluginDir\src\main\java\cn\guangdian\chunktrim\command\ChunkTrimCommand.java"
)

# 编译
$JavacArgs = @(
    "-encoding", "UTF-8",
    "-source", "21",
    "-target", "21",
    "-cp", $Classpath,
    "-d", $ClassesDir
) + $JavaFiles

& "$JavaHome\bin\javac.exe" @JavacArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "编译成功! 创建 JAR 文件..." -ForegroundColor Yellow

# 复制资源文件
Copy-Item "$PluginDir\src\main\resources\*" $ClassesDir -Recurse -Force

# 创建 JAR
$JarFile = "$JarDir\$PluginName-1.0.0.jar"
& "$JavaHome\bin\jar.exe" cf $JarFile -C $ClassesDir "."

# 部署到服务器
if (Test-Path $JarFile) {
    Copy-Item $JarFile $ServerDir -Force
    Write-Host ""
    Write-Host "已部署到: $ServerDir\$PluginName-1.0.0.jar" -ForegroundColor Green
} else {
    Write-Host "JAR 文件创建失败!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== 编译完成 ===" -ForegroundColor Green