param(
    [string]$EnvFile = ".env",
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot $EnvFile
$backendDir = Join-Path $repoRoot "lifeos-monolith"

if (-not (Test-Path $envPath)) {
    throw "Env file not found: $envPath"
}

Get-Content $envPath | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}

Set-Location $backendDir

if ($NoBuild) {
    mvn -q spring-boot:run
} else {
    mvn -q test
    mvn -q spring-boot:run
}
