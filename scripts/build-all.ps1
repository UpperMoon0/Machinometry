$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$targets = @(
    @{ Path = 'platforms/forge-1.20.1'; Java = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot' },
    @{ Path = 'platforms/fabric-1.20.1'; Java = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot' },
    @{ Path = 'platforms/neoforge-1.21.1'; Java = 'C:\Program Files\Java\jdk-21' },
    @{ Path = 'platforms/fabric-1.21.1'; Java = 'C:\Program Files\Java\jdk-21' },
    @{ Path = 'platforms/neoforge-26.1.2'; Java = 'C:\Windows\system32\config\systemprofile\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2' },
    @{ Path = 'platforms/fabric-26.1.2'; Java = 'C:\Windows\system32\config\systemprofile\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2' }
)
foreach ($target in $targets) {
    $project = Join-Path $root $target.Path
    Write-Host "==> $($target.Path)"
    $oldJavaHome = $env:JAVA_HOME
    $oldPath = $env:Path
    try {
        $env:JAVA_HOME = $target.Java
        $env:Path = "$($target.Java)\bin;$oldPath"
        & (Join-Path $project 'gradlew.bat') --no-daemon clean test build
        if ($LASTEXITCODE -ne 0) { throw "Build failed: $($target.Path)" }
    } finally {
        $env:JAVA_HOME = $oldJavaHome
        $env:Path = $oldPath
    }
}
