# Download gradle-wrapper.jar
$url = "https://github.com/gradle/gradle/raw/v9.4.0/gradle/wrapper/gradle-wrapper.jar"
$output = "e:\RPG\原创RPG服务端-插件最多提交\plugins\GuangDianArmorStats\gradle\wrapper\gradle-wrapper.jar"

# Ensure directory exists
$dir = Split-Path -Parent $output
if (-not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

Write-Host "Downloading gradle-wrapper.jar..."
try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
    Write-Host "Download complete!"
    if (Test-Path $output) {
        Write-Host "File size: $((Get-Item $output).Length) bytes"
    }
} catch {
    Write-Host "Error: $_"
    exit 1
}