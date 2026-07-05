# RPGItems 构建脚本
$JAVA_HOME = "D:\Deployment\jdk-21.0.10+7"
$PROJECT_DIR = "e:\RPG\plugins\RPGItems"
$GRADLE_CACHE = "c:\Users\yang\.gradle\caches\modules-2\files-2.1"

# 创建输出目录
New-Item -ItemType Directory -Path "$PROJECT_DIR\build\classes" -Force -ErrorAction SilentlyContinue

Write-Host "正在编译..." -ForegroundColor Cyan

# Java文件列表
$javaFiles = Get-ChildItem -Path "$PROJECT_DIR\src\main\java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName

# 使用cmd执行编译(避免PowerShell的分号问题)
$cmd = @"
@echo off
set JAVA_HOME=$JAVA_HOME
set CLASSPATH=$GRADLE_CACHE\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar;$GRADLE_CACHE\net.kyori\adventure-api\4.23.0\f6b5b75465f7112dbaf38dd8cfdc5f6b906c53a0\adventure-api-4.23.0.jar;$GRADLE_CACHE\net.kyori\adventure-key\4.23.0\be07b3e282f9dbc1c0b24a65f0d442ebc504d4d\adventure-key-4.23.0.jar;$GRADLE_CACHE\net.kyori\adventure-text-minimessage\4.23.0\3230c09453a50090ae354bfc0d8dd6b72026d88\adventure-text-minimessage-4.23.0.jar;$GRADLE_CACHE\net.kyori\adventure-text-serializer-legacy\4.23.0\e9cacd4d98c1ecab249805e8058cf2eece047d5b\adventure-text-serializer-legacy-4.23.0.jar;$GRADLE_CACHE\net.kyori\examination-api\1.3.0\8a2d185275307f1e2ef2adf7152b9a0d1d44c30b\examination-api-1.3.0.jar;$GRADLE_CACHE\net.kyori\examination-string\1.3.0\6f34afef5c54ccce4996bc321abf77518b55b4bd\examination-string-1.3.0.jar;$GRADLE_CACHE\net.kyori\option\1.1.0\593fecb9c42688eebc7d8da5d6ea127f4d4c92a2\option-1.1.0.jar;e:\RPG\艾尔丽雅\plugins\RPGCore-1.0.0.jar
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -source 21 -target 21 -cp "%CLASSPATH%" -d "$PROJECT_DIR\build\classes" $javaFiles
"@

# 执行编译
$cmd | Out-File -FilePath "$PROJECT_DIR\compile.cmd" -Encoding ASCII
cmd /c "$PROJECT_DIR\compile.cmd"

if ($LASTEXITCODE -eq 0) {
    Write-Host "编译成功!" -ForegroundColor Green
    
    # 复制资源文件
    Copy-Item -Path "$PROJECT_DIR\src\main\resources\*" -Destination "$PROJECT_DIR\build\classes" -Recurse -Force
    
    # 创建jar
    & "$JAVA_HOME\bin\jar.exe" cvf "$PROJECT_DIR\build\RPGItems-1.0.0.jar" -C "$PROJECT_DIR\build\classes" .
    
    # 部署
    Copy-Item "$PROJECT_DIR\build\RPGItems-1.0.0.jar" -Destination "e:\RPG\艾尔丽雅\plugins\RPGItems-1.0.0.jar" -Force
    
    Write-Host "部署完成!" -ForegroundColor Green
} else {
    Write-Host "编译失败!" -ForegroundColor Red
}