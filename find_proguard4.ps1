$baseDir = "c:\Users\Admin\Desktop\BedWars\BedWars1058-master"

# Try downloading proguard 4.11 from different locations
$urls = @(
    # ProGuard 7.5.0 from official site
    "https://github.com/Guardsquare/proguard/releases/download/v7.5.0/proguard-7.5.0.zip",
    # ProGuard 4.11
    "https://sourceforge.net/projects/proguard/files/proguard/4.11/proguard-4.11.jar/download",
    # Classic ProGuard from Maven
    "https://repo1.maven.org/maven2/proguard/proguard-all/4.9/proguard-all-4.9.jar",
    # Another Maven artifact
    "https://repo1.maven.org/maven2/proguard/proguard/5.3.3/proguard-5.3.3.jar"
)

foreach ($url in $urls) {
    $fileName = $url.Substring($url.LastIndexOf("/") + 1)
    $fileName = $fileName -replace '/download', ''
    $outPath = Join-Path $baseDir $fileName
    Write-Host "Trying $url..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $outPath -TimeoutSec 20 -MaximumRedirection 3
        if ((Get-Item $outPath).Length -gt 50000) {
            Write-Host "  SUCCESS: $fileName ($((Get-Item $outPath).Length) bytes)"
            break
        } else {
            Write-Host "  File too small, removing"
            Remove-Item $outPath -ErrorAction SilentlyContinue
        }
    } catch {
        Write-Host "  Failed: $($_.Exception.Message)"
    }
}