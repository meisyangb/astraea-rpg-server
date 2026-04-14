param(
    [switch]$DryRun
)

$plugins = @(
    "GuangDianAccessory",
    "GuangDianArmorStats",
    "GuangDianAuth",
    "GuangDianBank",
    "GuangDianBattlePass",
    "GuangDianBoard",
    "GuangDianCaveFu",
    "GuangDianChain",
    "GuangDianChat",
    "GuangDianClass",
    "GuangDianCleaner",
    "GuangDianCollection",
    "GuangDianCombat",
    "GuangDianDecompose",
    "GuangDianDropControl",
    "GuangDianForge",
    "GuangDianGearScore",
    "GuangDianGift",
    "GuangDianGuild",
    "GuangDianHolo",
    "GuangDianItemTrigger",
    "GuangDianLocation",
    "GuangDianMarket",
    "GuangDianMarriage",
    "GuangDianMenu",
    "GuangDianMobHealth",
    "GuangDianMCP",
    "GuangDianMonthlyCard",
    "GuangDianName",
    "GuangDianNPC",
    "GuangDianPoints",
    "GuangDianQuest",
    "GuangDianRaid",
    "GuangDianSignIn",
    "GuangDianSoulBind",
    "GuangDianTab",
    "GuangDianTrade",
    "GuangDianWorld"
)

Write-Host "=========================================="
Write-Host " RPGCore Logger Migration Script"
Write-Host " Migrating getLogger() to getGameLogger()"
Write-Host "=========================================="
Write-Host ""

$totalReplaced = 0

foreach ($plugin in $plugins) {
    $packageName = $plugin.Substring(8).ToLower()
    $mainJava = "e:\原创RPG服务端\plugins\$plugin\src\main\java\cn\guangdian\$packageName\$plugin.java"

    if (Test-Path $mainJava) {
        $content = Get-Content $mainJava -Raw
        $originalContent = $content

        $content = $content -replace 'getLogger\(\)\.severe\(', 'getGameLogger().severe('
        $content = $content -replace 'getLogger\(\)\.warning\(', 'getGameLogger().warning('
        $content = $content -replace 'getLogger\(\)\.info\(', 'getGameLogger().info('
        $content = $content -replace 'getLogger\(\)\.log\(', 'getGameLogger().log('

        $severeCount = ([regex]::Matches($originalContent, 'getLogger\(\)\.severe\(')).Count
        $warningCount = ([regex]::Matches($originalContent, 'getLogger\(\)\.warning\(')).Count
        $infoCount = ([regex]::Matches($originalContent, 'getLogger\(\)\.info\(')).Count
        $logCount = ([regex]::Matches($originalContent, 'getLogger\(\)\.log\(')).Count
        $total = $severeCount + $warningCount + $infoCount + $logCount

        if ($total -gt 0) {
            if (-not $DryRun) {
                $content | Set-Content $mainJava -NoNewline
            }
            Write-Host "[FIXED] $plugin - $total replacements"
            $totalReplaced += $total
        } else {
            Write-Host "[SKIP]  $plugin - no changes needed"
        }
    } else {
        Write-Host "[WARN]  $plugin - file not found"
    }
}

Write-Host ""
Write-Host "=========================================="
Write-Host " Done! Total: $totalReplaced replacements"
Write-Host "=========================================="
