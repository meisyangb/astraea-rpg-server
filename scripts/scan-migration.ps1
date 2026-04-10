#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Astraea RPG — 插件迁移扫描器
    扫描指定插件（或全部）中的硬编码反模式，生成迁移报告

.USAGE
    # 扫描所有插件
    .\scan-migration.ps1

    # 扫描单个插件
    .\scan-migration.ps1 -Plugin GuangDianArmorStats

    # 扫描并输出报告文件
    .\scan-migration.ps1 -OutputReport
#>

param(
    [string]$Plugin = "*",
    [switch]$OutputReport
)

# ── 配置 ────────────────────────────────────────────────────────────────
$RootDir = "e:\原创RPG服务端\plugins"
$ReportFile = "migration-report-$(Get-Date -Format 'yyyyMMdd-HHmm').txt"

# ── 禁止模式定义 ─────────────────────────────────────────────────────────
$ForbiddenPatterns = @(
    @{
        Pattern = "new BukkitRunnable\(\)"
        Severity = "ERROR"
        Message = "直接使用 BukkitRunnable"
        Fix = "使用 scheduler.runSyncRepeating(() -> {...}, delay, period)"
        Ticket = "TASK-001"
    },
    @{
        Pattern = "Bukkit\.getScheduler\(\)"
        Severity = "ERROR"
        Message = "直接调用 Bukkit 调度器"
        Fix = "使用 RPGCore SyncScheduler"
        Ticket = "TASK-001"
    },
    @{
        Pattern = "LuckPermsProvider\.get\(\)"
        Severity = "ERROR"
        Message = "直接获取 LuckPerms 实例"
        Fix = "使用 externalServices.getPlayerPrefix(player)"
        Ticket = "TASK-002"
    },
    @{
        Pattern = "PlaceholderAPI\.setPlaceholders\("
        Severity = "ERROR"
        Message = "直接调用 PlaceholderAPI.setPlaceholders"
        Fix = "使用 externalServices.parsePlaceholders(player, text)"
        Ticket = "TASK-002"
    },
    @{
        Pattern = '\.unregister\(\s*\)'
        Severity = "WARNING"
        Message = "可能调用了不存在的 PlaceholderExpansion.unregister()"
        Fix = "使用 PlaceholderAPI.unregisterExpansion(this)"
        Ticket = "TASK-003"
    },
    @{
        Pattern = 'new NamespacedKey\("mythicmobs", "item"\)'
        Severity = "ERROR"
        Message = "使用已废弃的 MythicMobs PDC Key 'item'"
        Fix = 'new NamespacedKey("mythicmobs", "type")'
        Ticket = "TASK-004"
    },
    @{
        Pattern = 'getPlugin\("RPGCore"\)'
        Severity = "ERROR"
        Message = "通过名称获取 RPGCore 实例"
        Fix = "使用 RPGCore.getInstance()"
        Ticket = "TASK-005"
    },
    @{
        Pattern = "extends JavaPlugin"
        Severity = "WARNING"
        Message = "插件主类直接继承 JavaPlugin"
        Fix = "继承 AbstractRPGPlugin"
        Ticket = "TASK-006"
    },
    @{
        Pattern = 'ChatColor\.'
        Severity = "WARNING"
        Message = "使用已废弃的 ChatColor"
        Fix = "使用 Adventure API: Component.text(...).color(NamedTextColor.RED)"
        Ticket = "TASK-007"
    }
)

# ── 扫描逻辑 ─────────────────────────────────────────────────────────────
$AllResults = @{}
$TotalErrors = 0
$TotalWarnings = 0

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Astraea RPG — 迁移扫描器                                   ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 获取目标插件目录
$PluginDirs = Get-ChildItem -Path $RootDir -Directory -Filter $Plugin |
    Where-Object { $_.Name -ne "RPGCore" }

foreach ($PluginDir in $PluginDirs) {
    $pluginName = $PluginDir.Name
    $javaFiles = Get-ChildItem -Path $PluginDir.FullName -Recurse -Filter "*.java"

    if ($javaFiles.Count -eq 0) { continue }

    $pluginResults = @()

    foreach ($file in $javaFiles) {
        $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
        if (-not $content) { continue }

        $lineNumber = 0
        foreach ($line in ($content -split "`n")) {
            $lineNumber++
            # 跳过注释行
            $trimmed = $line.Trim()
            if ($trimmed.StartsWith("//") -or $trimmed.StartsWith("*")) { continue }

            foreach ($pattern in $ForbiddenPatterns) {
                if ($line -match $pattern.Pattern) {
                    $relPath = $file.FullName.Replace($PluginDir.FullName, "").TrimStart('\')
                    $pluginResults += @{
                        File = $relPath
                        Line = $lineNumber
                        Severity = $pattern.Severity
                        Message = $pattern.Message
                        Fix = $pattern.Fix
                        Ticket = $pattern.Ticket
                        Code = $trimmed.Substring(0, [Math]::Min($trimmed.Length, 80))
                    }
                    if ($pattern.Severity -eq "ERROR") { $TotalErrors++ }
                    else { $TotalWarnings++ }
                }
            }
        }
    }

    $AllResults[$pluginName] = $pluginResults
}

# ── 输出报告 ─────────────────────────────────────────────────────────────
$reportLines = @()
$reportLines += "Astraea RPG 迁移扫描报告 — $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
$reportLines += "=" * 65

foreach ($pluginName in ($AllResults.Keys | Sort-Object)) {
    $results = $AllResults[$pluginName]
    $errorCount = ($results | Where-Object { $_.Severity -eq "ERROR" }).Count
    $warnCount  = ($results | Where-Object { $_.Severity -eq "WARNING" }).Count

    if ($results.Count -eq 0) {
        $status = "✅"
        $statusColor = "Green"
        $line = "[$status] $pluginName — 无问题，可标记为已迁移"
    } else {
        $status = if ($errorCount -gt 0) { "❌" } else { "⚠️" }
        $statusColor = if ($errorCount -gt 0) { "Red" } else { "Yellow" }
        $line = "[$status] $pluginName — ${errorCount}个错误, ${warnCount}个警告"
    }

    Write-Host $line -ForegroundColor $statusColor
    $reportLines += $line

    foreach ($r in $results) {
        $icon = if ($r.Severity -eq "ERROR") { "  ✗" } else { "  ⚠" }
        $detail = "    $($r.File):$($r.Line)"
        $problem = "    问题: $($r.Message)"
        $fix = "    修复: $($r.Fix)"
        $code = "    代码: $($r.Code)"

        Write-Host "$icon $problem" -ForegroundColor (if ($r.Severity -eq "ERROR") { "Red" } else { "Yellow" })
        Write-Host $detail -ForegroundColor DarkGray
        Write-Host $code -ForegroundColor DarkGray
        Write-Host $fix -ForegroundColor Cyan
        Write-Host ""

        $reportLines += "$icon $problem"
        $reportLines += $detail
        $reportLines += $code
        $reportLines += $fix
        $reportLines += ""
    }
    $reportLines += "-" * 65
}

# ── 汇总 ─────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  扫描完成                                                    ║" -ForegroundColor Cyan
Write-Host "║  总计: $TotalErrors 个错误  |  $TotalWarnings 个警告                         ║" -ForegroundColor Cyan

$cleanPlugins = ($AllResults.Values | Where-Object { $_.Count -eq 0 }).Count
Write-Host "║  干净插件: $cleanPlugins / $($AllResults.Count)                                   ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

# 建议优先级
Write-Host ""
Write-Host "📋 建议迁移优先级:" -ForegroundColor White
$prioritized = $AllResults.GetEnumerator() |
    Where-Object { $_.Value.Count -gt 0 } |
    Sort-Object { ($_.Value | Where-Object { $_.Severity -eq "ERROR" }).Count } -Descending |
    Select-Object -First 5

foreach ($item in $prioritized) {
    $ec = ($item.Value | Where-Object { $_.Severity -eq "ERROR" }).Count
    Write-Host "  P$($prioritized.IndexOf($item)+1). $($item.Key) — $ec 个错误需修复" -ForegroundColor Yellow
}

if ($OutputReport) {
    $reportLines | Out-File -FilePath $ReportFile -Encoding UTF8
    Write-Host ""
    Write-Host "📄 报告已保存: $ReportFile" -ForegroundColor Green
}

# 返回退出码（供CI使用）
exit ($TotalErrors -gt 0 ? 1 : 0)
