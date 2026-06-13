# GuangDianRegen 构建和部署脚本

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "构建 GuangDianRegen 插件" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 路径配置
$PluginDir = "e:\RPG\原创RPG服务端-插件最多提交\plugins\GuangDianRegen"
$SrcDir = Join-Path $PluginDir "src\main\java"
$ResDir = Join-Path $PluginDir "src\main\resources"
$BuildDir = Join-Path $PluginDir "build"
$LibsDir = Join-Path $BuildDir "libs"
$ClassesDir = Join-Path $BuildDir "classes"

# Paper API 路径
$PaperApi = "C:\Users\24141\.gradle\caches\modules-2\files-2.1\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar"

# 目标服务器路径
$TargetDir = "e:\RPG\艾德拉 - 副本\plugins"

# 清理构建目录
Write-Host "`n清理构建目录..." -ForegroundColor Yellow
if (Test-Path $BuildDir) {
    Remove-Item -Path $BuildDir -Recurse -Force
}
New-Item -ItemType Directory -Path $ClassesDir -Force | Out-Null
New-Item -ItemType Directory -Path $LibsDir -Force | Out-Null

# 查找所有Java文件
Write-Host "查找Java文件..." -ForegroundColor Yellow
$JavaFiles = Get-ChildItem -Path $SrcDir -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

if ($JavaFiles.Count -eq 0) {
    Write-Host "未找到Java文件!" -ForegroundColor Red
    exit 1
}

Write-Host "找到 $($JavaFiles.Count) 个Java文件" -ForegroundColor Green

# 编译Java文件
Write-Host "`n编译Java文件..." -ForegroundColor Yellow
$JavaFilesStr = $JavaFiles -join " "
$Command = "javac -cp `"$PaperApi`" -d `"$ClassesDir`" $JavaFilesStr"

Invoke-Expression $Command

if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!" -ForegroundColor Red
    exit 1
}

Write-Host "编译成功!" -ForegroundColor Green

# 复制资源文件
Write-Host "`n复制资源文件..." -ForegroundColor Yellow
Copy-Item -Path "$ResDir\*" -Destination $ClassesDir -Recurse -Force

# 创建JAR文件
Write-Host "`n创建JAR文件..." -ForegroundColor Yellow
$JarFile = Join-Path $LibsDir "GuangDianRegen-1.0.0.jar"

Push-Location $ClassesDir
jar cf $JarFile *
Pop-Location

if (-not (Test-Path $JarFile)) {
    Write-Host "JAR创建失败!" -ForegroundColor Red
    exit 1
}

$JarInfo = Get-Item $JarFile
Write-Host "JAR创建成功: $($JarInfo.Name) ($($JarInfo.Length) bytes)" -ForegroundColor Green

# 部署到服务器
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "部署到服务器" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`n目标目录: $TargetDir" -ForegroundColor Yellow

if (-not (Test-Path $TargetDir)) {
    Write-Host "目标目录不存在!" -ForegroundColor Red
    exit 1
}

# 复制JAR文件
Copy-Item -Path $JarFile -Destination $TargetDir -Force

Write-Host "`n部署完成!" -ForegroundColor Green
Write-Host "已复制到: $TargetDir\GuangDianRegen-1.0.0.jar" -ForegroundColor Green

# 验证部署
$DeployedJar = Join-Path $TargetDir "GuangDianRegen-1.0.0.jar"
if (Test-Path $DeployedJar) {
    $DeployedInfo = Get-Item $DeployedJar
    Write-Host "`n验证成功!" -ForegroundColor Green
    Write-Host "文件大小: $($DeployedInfo.Length) bytes" -ForegroundColor Green
    Write-Host "修改时间: $($DeployedInfo.LastWriteTime)" -ForegroundColor Green
} else {
    Write-Host "`n验证失败!" -ForegroundColor Red
    exit 1
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "构建和部署完成!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
