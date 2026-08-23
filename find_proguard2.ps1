$baseDir = "c:\Users\Admin\Desktop\BedWars\BedWars1058-master"

# Try downloading proguard-core from GitHub releases
$urls = @(
    "https://github.com/Guardsquare/proguard-core/releases/download/v7.5.0/proguard-core-7.5.0.jar",
    "https://github.com/Guardsquare/proguard-core/releases/download/v7.4.2/proguard-core-7.4.2.jar",
    "https://repo1.maven.org/maven2/com/guardsquare/proguard-core-jdk11/7.4.2/proguard-core-jdk11-7.4.2.jar",
    "https://repo1.maven.org/maven2/com/guardsquare/proguard-core-jdk17/7.4.2/proguard-core-jdk17-7.4.2.jar"
)

foreach ($url in $urls) {
    $fileName = $url.Substring($url.LastIndexOf("/") + 1)
    $outPath = Join-Path $baseDir $fileName
    Write-Host "Trying $url..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $outPath -TimeoutSec 15
        Write-Host "  SUCCESS: $fileName"
        break
    } catch {
        Write-Host "  Failed"
    }
}