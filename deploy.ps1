# 部署插件到服务器文件夹
$sourceDir = "e:\原创RPG服务端\plugins"
$targetDir = "e:\原创RPG服务端\server\plugins"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "部署插件到服务器文件夹" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 创建目标目录
if (!(Test-Path $targetDir)) {
    Write-Host "创建目标目录: $targetDir" -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

Write-Host ""
Write-Host "正在复制插件..." -ForegroundColor Green
Write-Host ""

$copiedCount = 0

# 获取所有 GuangDian* 和 RPGCore 插件
$pluginDirs = Get-ChildItem -Path $sourceDir -Directory | Where-Object { $_.Name -like "GuangDian*" -or $_.Name -eq "RPGCore" }

foreach ($dir in $pluginDirs) {
    $jarFiles = Get-ChildItem -Path "$($dir.FullName)\build\libs" -Filter "*.jar" -ErrorAction SilentlyContinue
    foreach ($jar in $jarFiles) {
        Write-Host "复制: $($jar.Name)" -ForegroundColor White
        Copy-Item -Path $jar.FullName -Destination $targetDir -Force
        $copiedCount++
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "部署完成!" -ForegroundColor Green
Write-Host "共复制 $copiedCount 个插件" -ForegroundColor Green
Write-Host "目标目录: $targetDir" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
