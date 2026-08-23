# Download ProGuard standalone
$url = "https://repo1.maven.org/maven2/com/guardsquare/proguard-base/7.5.0/proguard-base-7.5.0.jar"
$out = "c:\Users\Admin\Desktop\BedWars\BedWars1058-master\proguard-base-7.5.0.jar"

Write-Host "Downloading ProGuard..."
try {
    Invoke-WebRequest -Uri $url -OutFile $out -TimeoutSec 30
    Write-Host "Downloaded successfully"
} catch {
    Write-Host "Download failed: $_"
    exit 1
}