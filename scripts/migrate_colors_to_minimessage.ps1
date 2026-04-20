# ======================================== 
# 批量迁移颜色代码到 MiniMessage 格式
# ========================================

# 颜色代码映射表
$colorMap = @{
    '&0' = '<black>'
    '&1' = '<dark_blue>'
    '&2' = '<dark_green>'
    '&3' = '<dark_aqua>'
    '&4' = '<dark_red>'
    '&5' = '<dark_purple>'
    '&6' = '<gold>'
    '&7' = '<gray>'
    '&8' = '<dark_gray>'
    '&9' = '<blue>'
    '&a' = '<green>'
    '&b' = '<aqua>'
    '&c' = '<red>'
    '&d' = '<light_purple>'
    '&e' = '<yellow>'
    '&f' = '<white>'
    '&k' = '<obfuscated>'
    '&l' = '<bold>'
    '&m' = '<strikethrough>'
    '&n' = '<underlined>'
    '&o' = '<italic>'
    '&r' = '<reset>'
    '§0' = '<black>'
    '§1' = '<dark_blue>'
    '§2' = '<dark_green>'
    '§3' = '<dark_aqua>'
    '§4' = '<dark_red>'
    '§5' = '<dark_purple>'
    '§6' = '<gold>'
    '§7' = '<gray>'
    '§8' = '<dark_gray>'
    '§9' = '<blue>'
    '§a' = '<green>'
    '§b' = '<aqua>'
    '§c' = '<red>'
    '§d' = '<light_purple>'
    '§e' = '<yellow>'
    '§f' = '<white>'
    '§k' = '<obfuscated>'
    '§l' = '<bold>'
    '§m' = '<strikethrough>'
    '§n' = '<underlined>'
    '§o' = '<italic>'
    '§r' = '<reset>'
}

function Convert-ToMiniMessage {
    param([string]$content)
    
    $result = $content
    foreach ($code in $colorMap.Keys) {
        $result = $result.Replace($code, $colorMap[$code])
    }
    return $result
}

function Migrate-File {
    param([string]$filePath)
    
    if (-not (Test-Path $filePath)) {
        return
    }
    
    $content = Get-Content -Path $filePath -Raw -Encoding UTF8
    $newContent = Convert-ToMiniMessage -content $content
    
    if ($content -ne $newContent) {
        # 添加注释说明
        if (-not $newContent.StartsWith("# ")) {
            $newContent = "# 使用 MiniMessage 格式 (<color> 替代 &color/§color)`n" + $newContent
        }
        Set-Content -Path $filePath -Value $newContent -Encoding UTF8 -NoNewline
        Write-Host "已迁移: $filePath" -ForegroundColor Green
        return $true
    }
    return $false
}

# 需要迁移的插件列表
$pluginsToMigrate = @(
    "GuangDianQuest",
    "GuangDianForge", 
    "GuangDianRaid",
    "GuangDianSoulBind",
    "GuangDianMonthlyCard",
    "GuangDianGearScore",
    "GuangDianName",
    "GuangDianWorld",
    "GuangDianNPC",
    "GuangDianMarket",
    "GuangDianHolo",
    "GuangDianPoints",
    "GuangDianDropControl",
    "GuangDianTab",
    "GuangDianMarriage",
    "GuangDianGuild",
    "GuangDianMobHealth",
    "GuangDianLocation",
    "GuangDianDecompose",
    "GuangDianBattlePass"
)

$basePath = "e:\原创RPG服务端\plugins"
$migratedCount = 0

foreach ($plugin in $pluginsToMigrate) {
    $pluginPath = Join-Path $basePath $plugin
    if (Test-Path $pluginPath) {
        # 查找所有 yml 文件
        $ymlFiles = Get-ChildItem -Path $pluginPath -Recurse -Filter "*.yml" -ErrorAction SilentlyContinue
        foreach ($file in $ymlFiles) {
            # 排除 target 和 bin 目录
            if ($file.FullName -notmatch "(target|bin)") {
                if (Migrate-File -filePath $file.FullName) {
                    $migratedCount++
                }
            }
        }
    }
}

Write-Host "`n迁移完成! 共迁移 $migratedCount 个文件" -ForegroundColor Cyan
