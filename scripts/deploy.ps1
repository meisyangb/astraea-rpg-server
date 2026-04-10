#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Astraea RPG — 一键构建部署脚本
    构建 + Checkstyle检查 + 部署到服务器

.USAGE
    .\deploy.ps1              # 部署所有插件
    .\deploy.ps1 -Only RPGCore,GuangDianBoard   # 只部署指定插件
    .\deploy.ps1 -SkipChecks  # 跳过静态检查（紧急修复用，不推荐）
#>

param(
    [string[]]$Only = @(),
    [switch]$SkipChecks
)

$env:JAVA_HOME = "e:\原创RPG服务端\tools\jdk-21.0.10+7"
$GRADLE = "D:\gradle\gradle-9.4.0\bin\gradle.bat"
$SERVER_PLUGINS = "e:\原创RPG服务端\server\plugins"
$PROJECT_ROOT = "e:\原创RPG服务端"

Set-Location $PROJECT_ROOT

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║         Astraea RPG — 构建 & 部署系统                       ║" -ForegroundColor Magenta
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta

# Step 1: 迁移扫描（快速）
if (-not $SkipChecks) {
    Write-Host ""
    Write-Host "⏳ Step 1/3: 运行迁移扫描..." -ForegroundColor Cyan
    & "$PROJECT_ROOT\scripts\scan-migration.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ 发现架构违规，构建终止。修复后重试。" -ForegroundColor Red
        Write-Host "   跳过检查（仅紧急情况）: .\deploy.ps1 -SkipChecks" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "✅ 扫描通过" -ForegroundColor Green
}

# Step 2: Gradle 构建（含 Checkstyle）
Write-Host ""
Write-Host "⏳ Step 2/3: 构建插件..." -ForegroundColor Cyan

$buildTask = if ($SkipChecks) {
    "build -x checkstyleMain -x test"
} else {
    "build --no-configuration-cache -x test"
}

if ($Only.Count -gt 0) {
    $tasks = $Only | ForEach-Object { ":plugins:${_}:$buildTask" }
    & $GRADLE @tasks
} else {
    & $GRADLE $buildTask
}

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ 构建失败！查看上方错误信息。" -ForegroundColor Red
    exit 1
}
Write-Host "✅ 构建成功" -ForegroundColor Green

# Step 3: 部署
Write-Host ""
Write-Host "⏳ Step 3/3: 部署到服务器..." -ForegroundColor Cyan

$AllPlugins = @(
    "RPGCore", "GuangDianArmorStats", "GuangDianPoints",
    "GuangDianGuild", "GuangDianBoard", "GuangDianName",
    "GuangDianTab", "GuangDianMobHealth", "GuangDianNPC",
    "GuangDianForge", "GuangDianCaveFu", "GuangDianMarket",
    "GuangDianTrade", "GuangDianMarriage", "GuangDianMenu",
    "GuangDianHolo", "GuangDianChat", "GuangDianCleaner",
    "GuangDianDropControl", "GuangDianItemTrigger",
    "GuangDianWorld", "GuangDianLocation", "GuangDianQuest",
    "GuangDianDecompose", "GuangDianGift", "GuangDianMCP"
)

$TargetPlugins = if ($Only.Count -gt 0) { $Only } else { $AllPlugins }
$DeployedCount = 0

foreach ($plugin in $TargetPlugins) {
    $src = "plugins\$plugin\build\libs\$plugin-1.0.0.jar"
    $dst = "$SERVER_PLUGINS\$plugin-1.0.0.jar"

    if (Test-Path $src) {
        Copy-Item $src $dst -Force
        Write-Host "  ✅ $plugin" -ForegroundColor Green
        $DeployedCount++
    } else {
        Write-Host "  ⚠️  $plugin — JAR未找到: $src" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  部署完成! 共部署 $DeployedCount 个插件                              ║" -ForegroundColor Green
Write-Host "║  服务器目录: server/plugins/                                 ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Green
