$ErrorActionPreference = "Stop"
$JAVA_HOME = "D:\Deployment\jdk-21.0.10+7"
$ProjectDir = "e:\RPG\plugins\GuangDianSect"
$OutputDir = "$ProjectDir\build\classes"

# 依赖文件
$Libs = @(
    "e:\RPG\plugins\GuangDianArmorStats\libs\paper-api-1.21.6.jar",
    "e:\RPG\plugins\RPGCore\build\libs\RPGCore-1.0.0.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\PlaceholderAPI.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\adventure-api.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\adventure-key.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\adventure-nbt.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\adventure-platform-api.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\adventure-text-minimessage.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\adventure-text-serializer-legacy.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\bungeecord-chat.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\examination-api.jar",
    "e:\RPG\plugins\GuangDianArmorStats\libs\examination-string.jar"
)

Write-Host "正在编译 GuangDianSect 门派插件..." -ForegroundColor Cyan

# 创建输出目录
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

# 查找所有Java文件
$JavaFiles = Get-ChildItem -Path "$ProjectDir\src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

# 编译
Write-Host "编译Java文件..." -ForegroundColor Yellow
$ClassPath = $Libs -join ";"
$JavacArgs = @("-encoding", "UTF-8", "-source", "21", "-target", "21", "-cp", $ClassPath, "-d", $OutputDir, "-Xlint:-removal") + $JavaFiles

& "$JAVA_HOME\bin\javac.exe" $JavacArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!" -ForegroundColor Red
    exit 1
}

# 复制资源文件
Write-Host "复制资源文件..." -ForegroundColor Yellow
Copy-Item -Path "$ProjectDir\src\main\resources\*" -Destination $OutputDir -Recurse -Force

# 创建jar文件
Write-Host "创建jar文件..." -ForegroundColor Yellow
$JarFile = "$ProjectDir\build\GuangDianSect-1.0.0.jar"
& "$JAVA_HOME\bin\jar.exe" cvf $JarFile -C $OutputDir .

# 部署到服务器 - 使用 UTF8 编码的路径
Write-Host "部署到服务器..." -ForegroundColor Yellow
$DestDir = "e:\RPG"
$ServerPlugins = Get-ChildItem -Path $DestDir -Directory | Where-Object { $_.Name -like "*传说*" }
if ($ServerPlugins.Count -gt 0) {
    $TargetDir = Join-Path $ServerPlugins[0].FullName "plugins"
    Copy-Item -Path $JarFile -Destination "$TargetDir\GuangDianSect-1.0.0.jar" -Force
    Write-Host "编译成功! 已部署到 $TargetDir\GuangDianSect-1.0.0.jar" -ForegroundColor Green
} else {
    Write-Host "警告: 未找到服务器plugins目录，jar文件位于: $JarFile" -ForegroundColor Yellow
}