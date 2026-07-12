# GuangDianPoints 编译脚本
$JAVA_HOME = "D:\Deployment\jdk-21.0.10+7"
$PROJECT_DIR = "e:\RPG\plugins\GuangDianPoints"
$GRADLE_CACHE = "c:\Users\yang\.gradle\caches\modules-2\files-2.1"
$PAPI = "$PROJECT_DIR\libs\PlaceholderAPI.jar"
$VAULT = "$PROJECT_DIR\libs\Vault.jar"
$OUTPUT_DIR = "$PROJECT_DIR\build\classes"
$JAR_OUTPUT = "$PROJECT_DIR\build\libs\GuangDianPoints-1.2.0.jar"

Write-Host "正在编译 GuangDianPoints 插件..." -ForegroundColor Green

# 创建输出目录
New-Item -ItemType Directory -Path $OUTPUT_DIR -Force | Out-Null
New-Item -ItemType Directory -Path "$PROJECT_DIR\build\libs" -Force | Out-Null

# 设置classpath
$PAPER_API = "$GRADLE_CACHE\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar"
$ADVENTURE_API = "$GRADLE_CACHE\net.kyori\adventure-api\4.23.0\f6b5b75465f7112dbaf38dd8cfdc5f6b906c53a0\adventure-api-4.23.0.jar"
$ADVENTURE_KEY = "$GRADLE_CACHE\net.kyori\adventure-key\4.23.0\be07b3e282f9dbc1c0b24a65f0d442ebc504d4d\adventure-key-4.23.0.jar"
$MINIMESSAGE = "$GRADLE_CACHE\net.kyori\adventure-text-minimessage\4.23.0\3230c09453a50090ae354bfc0d8dd6b72026d88\adventure-text-minimessage-4.23.0.jar"
$LEGACY = "$GRADLE_CACHE\net.kyori\adventure-text-serializer-legacy\4.23.0\e9cacd4d98c1ecab249805e8058cf2eece047d5b\adventure-text-serializer-legacy-4.23.0.jar"
$EXAMINATION_API = "$GRADLE_CACHE\net.kyori\examination-api\1.3.0\8a2d185275307f1e2ef2adf7152b9a0d1d44c30b\examination-api-1.3.0.jar"
$EXAMINATION_STRING = "$GRADLE_CACHE\net.kyori\examination-string\1.3.0\6f34afef5c54ccce4996bc321abf77518b55b4bd\examination-string-1.3.0.jar"
$OPTION = "$GRADLE_CACHE\net.kyori\option\1.1.0\593fecb9c42688eebc7d8da5d6ea127f4d4c92a2\option-1.1.0.jar"

$CLASSPATH = "$PAPER_API;$ADVENTURE_API;$ADVENTURE_KEY;$MINIMESSAGE;$LEGACY;$EXAMINATION_API;$EXAMINATION_STRING;$OPTION;$PAPI;$VAULT"

# 编译
$env:JAVA_HOME = $JAVA_HOME
$env:PATH = "$JAVA_HOME\bin;$env:PATH"

Write-Host "编译 Java 文件..." -ForegroundColor Yellow

# 使用绝对路径的Java文件
$JavaFiles = @(
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\GuangDianPoints.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\monitor\LocalPlayerLockManager.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\monitor\OperationTimer.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\monitor\PerformanceMetrics.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\monitor\PerformanceMonitor.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\monitor\PerformanceReport.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\placeholder\PointsPlaceholder.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\storage\DatabaseStorage.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\transaction\TransactionLogger.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\transaction\UnfinishedTransaction.java",
    "$PROJECT_DIR\src\main\java\cn\guangdian\points\util\MessageUtils.java"
)

& javac -encoding UTF-8 -source 21 -target 21 -cp $CLASSPATH -d $OUTPUT_DIR $JavaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!" -ForegroundColor Red
    exit 1
}

Write-Host "编译成功!" -ForegroundColor Green

# 复制资源文件
Write-Host "复制资源文件..." -ForegroundColor Yellow
Copy-Item -Path "$PROJECT_DIR\src\main\resources\*" -Destination $OUTPUT_DIR -Recurse -Force

# 创建jar文件
Write-Host "创建 jar 文件..." -ForegroundColor Yellow
& jar cvf $JAR_OUTPUT -C $OUTPUT_DIR .

if ($LASTEXITCODE -ne 0) {
    Write-Host "创建 jar 失败!" -ForegroundColor Red
    exit 1
}

# 部署到译梦传说服务器
Write-Host "部署到译梦传说服务器..." -ForegroundColor Yellow
Copy-Item -Path $JAR_OUTPUT -Destination "e:\RPG\译梦传说\plugins\GuangDianPoints.jar" -Force

Write-Host "部署完成!" -ForegroundColor Green
Write-Host "插件已部署到: e:\RPG\译梦传说\plugins\GuangDianPoints.jar" -ForegroundColor Cyan