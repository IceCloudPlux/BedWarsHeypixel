Download ProGuard 4.9 (classic, self-contained version)
$urls = @(
    "https://sourceforge.net/projects/proguard/files/proguard/4.9/proguard-4.9/proguard-4.9.jar/download",
    "https://repo1.maven.org/maven2/net/sf/proguard/proguard/4.9/proguard-4.9.jar"
)

$baseDir = "c:\Users\Admin\Desktop\BedWars\BedWars1058-master"

foreach ($url in $urls) {
    $outPath = Join-Path $baseDir "proguard-4.9.jar"
    Write-Host "Trying $url..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $outPath -TimeoutSec 20 -MaximumRedirection 5
        if (Test-Path $outPath -and (Get-Item $outPath).Length -gt 100000) {
            Write-Host "  SUCCESS"
            break
        } else {
            Write-Host "  File too small or missing"
            Remove-Item $outPath -ErrorAction SilentlyContinue
        }
    } catch {
        Write-Host "  Failed: $_"
    }
}