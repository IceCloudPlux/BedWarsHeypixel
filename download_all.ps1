# Download ProGuard classic version (more self-contained)
$urls = @(
    @{ url = "https://repo1.maven.org/maven2/com/guardsquare/proguard-core/7.5.0/proguard-core-7.5.0.jar"; out = "proguard-core-7.5.0.jar" },
    @{ url = "https://repo1.maven.org/maven2/com/guardsquare/proguard-base/7.5.0/proguard-base-7.5.0.jar"; out = "proguard-base-7.5.0.jar" },
    @{ url = "https://repo1.maven.org/maven2/org/jdom/jdom2/2.0.6/jdom2-2.0.6.jar"; out = "jdom2-2.0.6.jar" },
    @{ url = "https://repo1.maven.org/maven2/com/google/guava/guava/32.0.1-jre/guava-32.0.1-jre.jar"; out = "guava-32.0.1-jre.jar" }
)

$baseDir = "c:\Users\Admin\Desktop\BedWars\BedWars1058-master"

foreach ($item in $urls) {
    $outPath = Join-Path $baseDir $item.out
    if (-not (Test-Path $outPath)) {
        Write-Host "Downloading $($item.out)..."
        try {
            Invoke-WebRequest -Uri $item.url -OutFile $outPath -TimeoutSec 30
            Write-Host "  OK"
        } catch {
            Write-Host "  FAILED: $_"
        }
    } else {
        Write-Host "$($item.out) already exists"
    }
}