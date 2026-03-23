$ErrorActionPreference = 'Stop'

function Invoke-WithRetry {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Action,
        [int]$MaxAttempts = 3,
        [int]$DelaySeconds = 5,
        [string]$Description = "command"
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            & $Action
            return
        } catch {
            if ($attempt -ge $MaxAttempts) {
                throw
            }
            Write-Host "[retry] $Description failed ($attempt/$MaxAttempts): $($_.Exception.Message)"
            Start-Sleep -Seconds $DelaySeconds
        }
    }
}

$baseImages = @(
    'mysql:8.0.40',
    'redis:7.2',
    'nacos/nacos-server:v2.3.2',
    'apache/rocketmq:5.3.2',
    'maven:3.9.11-eclipse-temurin-17',
    'eclipse-temurin:17-jre',
    'node:20-alpine',
    'nginx:1.27-alpine'
)

foreach ($image in $baseImages) {
    Invoke-WithRetry -Description "docker pull $image" -Action {
        docker pull $image
        if ($LASTEXITCODE -ne 0) {
            throw "docker pull $image exited with code $LASTEXITCODE"
        }
    }
}

$env:COMPOSE_PARALLEL_LIMIT = '1'

Invoke-WithRetry -Description "docker compose build" -Action {
    docker compose -f .\docker-compose.full.yml build
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose build exited with code $LASTEXITCODE"
    }
}

docker compose -f .\docker-compose.full.yml up -d
if ($LASTEXITCODE -ne 0) {
    throw "docker compose up exited with code $LASTEXITCODE"
}
