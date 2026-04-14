$plugin = "GuangDianMarket"
$packageName = $plugin.Substring(8).ToLower()
$mainJava = "e:\原创RPG服务端\plugins\$plugin\src\main\java\cn\guangdian\$packageName\$plugin.java"

Write-Host "Plugin: $plugin"
Write-Host "PackageName: $packageName"
Write-Host "Path: $mainJava"
Write-Host "Exists: $(Test-Path $mainJava)"

# Try alternative path format
$altPath = "e:\原创RPG服务端\plugins\GuangDianMarket\src\main\java\cn\guangdian\market\GuangDianMarket.java"
Write-Host "Alt Path: $altPath"
Write-Host "Alt Exists: $(Test-Path $altPath)"
