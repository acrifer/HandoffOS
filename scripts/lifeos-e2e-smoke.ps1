param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Username = ("handoff_smoke_" + (Get-Date -Format "yyyyMMddHHmmss")),
    [string]$Password = "Pass123456",
    [string]$Email = "handoff-smoke@lifeos.local",
    [Parameter(Mandatory = $true)]
    [string[]]$DocumentRefs,
    [string]$ChatId = ""
)

$ErrorActionPreference = "Stop"

function Write-Step($message) {
    Write-Host ""
    Write-Host "==> $message"
}

function Invoke-LifeOS($Method, $Path, $Body = $null, $Token = $null, $TimeoutSec = 120) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $options = @{
        Method = $Method
        Uri = ($BaseUrl.TrimEnd("/") + $Path)
        Headers = $headers
        TimeoutSec = $TimeoutSec
    }
    if ($null -ne $Body) {
        $options["Body"] = ($Body | ConvertTo-Json -Depth 12 -Compress)
    }

    $response = Invoke-RestMethod @options
    if ($response.code -ne 200) {
        throw "LifeOS API failed at ${Path}: $($response.message)"
    }
    return $response.data
}

Write-Step "Checking LifeOS API"
try {
    Invoke-RestMethod -Method Get -Uri ($BaseUrl.TrimEnd("/") + "/v3/api-docs") -TimeoutSec 15 | Out-Null
    Write-Host "API is reachable: $BaseUrl"
} catch {
    throw "LifeOS API is not reachable at $BaseUrl. Start the backend before running this script."
}

Write-Step "Registering smoke user"
$login = Invoke-LifeOS "POST" "/auth/register" @{
    username = $Username
    password = $Password
    email = $Email
}
$token = $login.token
Write-Host "User created: $($login.username), id: $($login.userId)"

Write-Step "Creating handoff skill"
$skill = Invoke-LifeOS "POST" "/skill" @{
    name = "支付后端交接助手"
    roleDescription = "负责支付回调、订单状态机、上线检查、回滚和告警处理"
} $token
Write-Host "Skill created: $($skill.id), status: $($skill.status)"

Write-Step "Syncing real Feishu sources into Dify"
$skill = Invoke-LifeOS "POST" "/skill/$($skill.id)/sources/sync" @{
    documentRefs = $DocumentRefs
    chatId = $ChatId
    limit = 20
} $token 180
Write-Host "Synced sources: $($skill.sourceCount), dataset: $($skill.difyDatasetId)"

Write-Step "Distilling skill"
$distillJob = Invoke-LifeOS "POST" "/skill/$($skill.id)/distill" $null $token 180
Write-Host "Distill job: $($distillJob.id), status: $($distillJob.status), run: $($distillJob.difyWorkflowRunId)"
if ($distillJob.status -eq "FAILED") {
    Write-Warning $distillJob.errorMessage
}

Write-Step "Asking skill"
$askJob = Invoke-LifeOS "POST" "/skill/$($skill.id)/ask" @{
    question = "上线前需要检查哪些事项？"
} $token 180
Write-Host "Ask job: $($askJob.id), status: $($askJob.status), run: $($askJob.difyWorkflowRunId)"
if ($askJob.status -eq "FAILED") {
    Write-Warning $askJob.errorMessage
}

Write-Step "Reading job audit"
$jobs = Invoke-LifeOS "GET" "/skill/$($skill.id)/jobs?limit=10" $null $token
$jobs | Select-Object id, jobType, status, difyWorkflowRunId, errorMessage | Format-Table -AutoSize

Write-Host ""
Write-Host "LifeOS E2E smoke finished."
Write-Host "Skill id: $($skill.id)"
Write-Host "Dataset id: $($skill.difyDatasetId)"
