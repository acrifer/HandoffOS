param(
    [string]$EnvFile = ".env",
    [string]$DocumentRef = "",
    [string]$ChatId = ""
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $EnvFile)) {
    throw "Env file not found: $EnvFile"
}

$envMap = @{}
Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        $envMap[$matches[1]] = $matches[2]
    }
}

foreach ($key in @("FEISHU_APP_ID", "FEISHU_APP_SECRET")) {
    if ([string]::IsNullOrWhiteSpace($envMap[$key])) {
        throw "$key is empty in $EnvFile"
    }
}

$baseUrl = if ($envMap["FEISHU_BASE_URL"]) { $envMap["FEISHU_BASE_URL"].TrimEnd("/") } else { "https://open.feishu.cn/open-apis" }

function Write-Step($message) {
    Write-Host ""
    Write-Host "==> $message"
}

function Normalize-DocumentRef($value) {
    $normalized = ""
    if ($null -ne $value) {
        $normalized = $value.Trim()
    }
    if ($normalized.Contains("?")) {
        $normalized = $normalized.Substring(0, $normalized.IndexOf("?"))
    }
    if ($normalized.Contains("#")) {
        $normalized = $normalized.Substring(0, $normalized.IndexOf("#"))
    }
    foreach ($separator in @("/docx/", "/wiki/", "/docs/", "/doc/")) {
        $index = $normalized.IndexOf($separator)
        if ($index -ge 0) {
            $normalized = $normalized.Substring($index + $separator.Length)
            break
        }
    }
    if ($normalized.Contains("/")) {
        $normalized = $normalized.Substring($normalized.LastIndexOf("/") + 1)
    }
    return $normalized
}

Write-Step "Getting tenant access token"
$tokenBody = @{
    app_id = $envMap["FEISHU_APP_ID"]
    app_secret = $envMap["FEISHU_APP_SECRET"]
} | ConvertTo-Json -Compress
$tokenResponse = Invoke-RestMethod -Method Post -Uri "$baseUrl/auth/v3/tenant_access_token/internal" -ContentType "application/json" -Body $tokenBody -TimeoutSec 30
if ($tokenResponse.code -ne 0) {
    throw "Feishu token failed: $($tokenResponse.msg)"
}
$tenantToken = $tokenResponse.tenant_access_token
Write-Host "Tenant token OK. Expires in: $($tokenResponse.expire)s"

$headers = @{ Authorization = "Bearer $tenantToken" }

if (-not [string]::IsNullOrWhiteSpace($DocumentRef)) {
    Write-Step "Fetching document raw content"
    $documentId = Normalize-DocumentRef $DocumentRef
    $document = Invoke-RestMethod -Method Get -Uri "$baseUrl/docx/v1/documents/$documentId/raw_content" -Headers $headers -TimeoutSec 30
    if ($document.code -ne 0) {
        throw "Feishu document failed: $($document.msg)"
    }
    $content = if ($document.data.content) { $document.data.content } elseif ($document.data.raw_content) { $document.data.raw_content } else { $document.data | ConvertTo-Json -Compress }
    Write-Host "Document OK: $documentId"
    Write-Host "Content preview: $($content.Substring(0, [Math]::Min(120, $content.Length)))"
}

if (-not [string]::IsNullOrWhiteSpace($ChatId)) {
    Write-Step "Fetching chat messages"
    $chatUrl = "$baseUrl/im/v1/messages?container_id_type=chat&container_id=$ChatId&sort_type=ByCreateTimeDesc&page_size=10"
    $messages = Invoke-RestMethod -Method Get -Uri $chatUrl -Headers $headers -TimeoutSec 30
    if ($messages.code -ne 0) {
        throw "Feishu chat failed: $($messages.msg)"
    }
    $count = if ($messages.data.items) { $messages.data.items.Count } else { 0 }
    Write-Host "Chat OK: $ChatId, messages: $count"
}

Write-Host ""
Write-Host "Feishu smoke test passed."
