$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$services = @(
    "apigateway",
    "bookingservice",
    "orderservice",
    "version-1\microservices"
)

foreach ($service in $services) {
    Write-Host "===== $service ====="
    Push-Location (Join-Path $root $service)
    try {
        cmd /c mvnw.cmd test
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }
    finally {
        Pop-Location
    }
}
