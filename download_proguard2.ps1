# Download ProGuard core
$url = "https://repo1.maven.org/maven2/com/guardsquare/proguard-core/7.5.0/proguard-core-7.5.0.jar"
$out = "c:\Users\Admin\Desktop\BedWars\BedWars1058-master\proguard-core-7.5.0.jar"

Write-Host "Downloading ProGuard Core..."
try {
    Invoke-WebRequest -Uri $url -OutFile $out -TimeoutSec 30
    Write-Host "Downloaded successfully"
} catch {
    Write-Host "Download failed: $_"
    exit 1
}

# Also download guava dependency if needed
$url2 = "https://repo1.maven.org/maven2/com/google/guava/guava/32.0.1-jre/guava-32.0.1-jre.jar"
$out2 = "c:\Users\Admin\Desktop\BedWars\BedWars1058-master\guava-32.0.1-jre.jar"

Write-Host "Downloading Guava..."
try {
    Invoke-WebRequest -Uri $url2 -OutFile $out2 -TimeoutSec 30
    Write-Host "Downloaded successfully"
} catch {
    Write-Host "Guava download failed (may not be needed): $_"
}