$env:JAVA_HOME = "D:\Deployment\jdk-21.0.10+7"
$env:PROJECT_DIR = "e:\RPG\plugins\GuangDianEnhance"
$env:GRADLE_CACHE = "c:\Users\yang\.gradle\caches\modules-2\files-2.1"

# 创建输出目录
if (!(Test-Path "$env:PROJECT_DIR\build\classes")) {
    New-Item -ItemType Directory -Path "$env:PROJECT_DIR\build\classes" -Force | Out-Null
}

# 设置classpath
$classpath = "$env:GRADLE_CACHE\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\adventure-api\4.23.0\f6b5b75465f7112dbaf38dd8cfdc5f6b906c53a0\adventure-api-4.23.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\adventure-key\4.23.0\be07b3e282f9dbc1c0b24a65f0d442ebc504d4d\adventure-key-4.23.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\adventure-text-minimessage\4.23.0\3230c09453a50090ae354bfc0d8dd6b72026d88\adventure-text-minimessage-4.23.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\adventure-text-serializer-legacy\4.23.0\e9cacd4d98c1ecab249805e8058cf2eece047d5b\adventure-text-serializer-legacy-4.23.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\examination-api\1.3.0\8a2d185275307f1e2ef2adf7152b9a0d1d44c30b\examination-api-1.3.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\examination-string\1.3.0\6f34afef5c54ccce4996bc321abf77518b55b4bd\examination-string-1.3.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\option\1.1.0\593fecb9c42688eebc7d8da5d6ea127f4d4c92a2\option-1.1.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\adventure-text-serializer-gson\4.23.0\bf81a8f6f9509e2c826fac29a0e580e8c377ba37\adventure-text-serializer-gson-4.23.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.kyori\adventure-text-serializer-json\4.23.0\955b9e7560304cc32ac489832fe3db9931f20b1\adventure-text-serializer-json-4.23.0.jar"
$classpath += ";$env:GRADLE_CACHE\net.md-5\bungeecord-chat\1.21-R0.2-deprecated+build.21\a87a9222a1dcfa429b4a06264899f65313a4ed5c\bungeecord-chat-1.21-R0.2-deprecated+build.21.jar"
$classpath += ";e:\RPG\plugins\libs\RPGCore-1.0.0.jar"
$classpath += ";e:\RPG\plugins\RPGItems\build\RPGItems-1.0.0.jar"
$classpath += ";e:\RPG\plugins\libs\PlaceholderAPI.jar"

# 编译所有Java文件
$javaFiles = Get-ChildItem -Path "$env:PROJECT_DIR\src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

Write-Host "正在编译 GuangDianEnhance 插件..."
Write-Host "找到 $($javaFiles.Count) 个 Java 文件"

& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 -source 21 -target 21 -cp "$classpath" -d "$env:PROJECT_DIR\build\classes" $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!"
    exit 1
}

Write-Host "编译成功!"

# 复制资源文件
Write-Host "复制资源文件..."
Copy-Item -Path "$env:PROJECT_DIR\src\main\resources\*" -Destination "$env:PROJECT_DIR\build\classes" -Recurse -Force

# 创建jar文件
Write-Host "创建jar文件..."
& "$env:JAVA_HOME\bin\jar.exe" cvf "$env:PROJECT_DIR\build\GuangDianEnhance-1.0.0.jar" -C "$env:PROJECT_DIR\build\classes" .

Write-Host "编译完成!"
Write-Host "输出文件: $env:PROJECT_DIR\build\GuangDianEnhance-1.0.0.jar"