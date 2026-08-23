# Try different ProGuard versions
$baseDir = "c:\Users\Admin\Desktop\BedWars\BedWars1058-master"

# Try proguard-core at different versions
$versions = @("7.5.0", "7.4.2", "7.4.1", "7.4.0", "7.3.0", "7.2.0", "7.1.0", "7.0.0")
$artifacts = @("proguard-core", "proguard-core-jdk11", "proguard-core-jdk17")

foreach ($ver in $versions) {
    foreach ($art in $artifacts) {
        $url = "https://repo1.maven.org/maven2/com/guardsquare/$art/$ver/$art-$ver.jar"
        $outPath = Join-Path $baseDir "$art-$ver.jar"
        Write-Host "Trying $art-$ver..."
        try {
            Invoke-WebRequest -Uri $url -OutFile $outPath -TimeoutSec 10
            Write-Host "  SUCCESS: $url"
            break
        } catch {
            Write-Host "  Failed"
        }
    }
    if (Test-Path (Join-Path $baseDir "proguard-core-$ver.jar") -or Test-Path (Join-Path $baseDir "proguard-core-jdk11-$ver.jar") -or Test-Path (Join-Path $baseDir "proguard-core-jdk17-$ver.jar")) {
        Write-Host "Found proguard-core at version $ver"
        break
    }
}