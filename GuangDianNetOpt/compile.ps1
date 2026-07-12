$JAVA_HOME = "D:\Deployment\jdk-21.0.10+7"
$PROJECT_DIR = "e:\RPG\plugins\GuangDianNetOpt"
$GRADLE_CACHE = "c:\Users\yang\.gradle\caches\modules-2\files-2.1"

Write-Host "Compiling GuangDianNetOpt plugin..."

# Create output directory
$buildDir = "$PROJECT_DIR\build\classes"
if (-not (Test-Path $buildDir)) {
    New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
}

# Set classpath with all dependencies
$CLASSPATH = @(
    "$GRADLE_CACHE\io.papermc.paper\paper-api\1.21.6-R0.1-SNAPSHOT\72c1661b05fb67a0e1e31c95b67aef24986ad1c8\paper-api-1.21.6-R0.1-SNAPSHOT.jar",
    "$GRADLE_CACHE\net.kyori\adventure-api\4.23.0\f6b5b75465f7112dbaf38dd8cfdc5f6b906c53a0\adventure-api-4.23.0.jar",
    "$GRADLE_CACHE\net.kyori\adventure-key\4.23.0\be07b3e282f9dbc1c0b24a65f0d442ebc504d4d\adventure-key-4.23.0.jar",
    "$GRADLE_CACHE\net.kyori\examination-api\1.3.0\8a2d185275307f1e2ef2adf7152b9a0d1d44c30b\examination-api-1.3.0.jar",
    "$GRADLE_CACHE\net.kyori\examination-string\1.3.0\6f34afef5c54ccce4996bc321abf77518b55b4bd\examination-string-1.3.0.jar",
    "$GRADLE_CACHE\net.kyori\option\1.1.0\593fecb9c42688eebc7d8da5d6ea127f4d4c92a2\option-1.1.0.jar",
    "$GRADLE_CACHE\net.md-5\bungeecord-chat\1.21-R0.2-deprecated+build.21\a87a9222a1dcfa429b4a06264899f65313a4ed5c\bungeecord-chat-1.21-R0.2-deprecated+build.21.jar"
) -join ";"

# Collect all Java files
$javaFiles = Get-ChildItem -Path "$PROJECT_DIR\src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

# Compile
$javacArgs = @(
    "-encoding", "UTF-8",
    "-source", "21",
    "-target", "21",
    "-cp", $CLASSPATH,
    "-d", $buildDir
) + $javaFiles

& "$JAVA_HOME\bin\javac.exe" $javacArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!"
    exit 1
}

Write-Host "Compilation successful!"

# Copy resource files
Write-Host "Copying resource files..."
Copy-Item -Path "$PROJECT_DIR\src\main\resources\*" -Destination $buildDir -Recurse -Force

# Create jar file
Write-Host "Creating jar file..."
$jarFile = "$PROJECT_DIR\build\GuangDianNetOpt.jar"
& "$JAVA_HOME\bin\jar.exe" cvf $jarFile -C $buildDir "." 

# Deploy to server
Write-Host "Deploying to server..."
Copy-Item -Path $jarFile -Destination "e:\RPG\译梦传说\plugins\GuangDianNetOpt.jar" -Force

Write-Host "Deployment complete!"