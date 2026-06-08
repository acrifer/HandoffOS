param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Username = ("handoff_complete_" + (Get-Date -Format "yyyyMMddHHmmss")),
    [string]$Password = "Pass123456",
    [string[]]$DocumentRefs = @("https://xcncmhy2n1xv.feishu.cn/wiki/OFZ8wTwvRiF8tFkmsZ1cAFdLnud?from=from_copylink"),
    [string]$ChatId = "oc_03f40812cec2ed8e6af53b20f19bfe22"
)

$ErrorActionPreference = "Stop"

function Write-Step($message) {
    Write-Host ""
    Write-Host "==> $message"
}

function Invoke-HandoffOS($Method, $Path, $Body = $null, $Token = $null, $TimeoutSec = 240) {
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
        $options["Body"] = ($Body | ConvertTo-Json -Depth 20 -Compress)
    }

    $response = Invoke-RestMethod @options
    if ($response.code -ne 200) {
        throw "HandoffOS API failed at ${Path}: $($response.message)"
    }
    return $response.data
}

Write-Step "Checking API"
Invoke-RestMethod -Method Get -Uri ($BaseUrl.TrimEnd("/") + "/v3/api-docs") -TimeoutSec 20 | Out-Null
Write-Host "API OK: $BaseUrl"

Write-Step "Registering demo user"
$email = $Username + "@handoffos.demo"
$login = Invoke-HandoffOS "POST" "/auth/register" @{
    username = $Username
    password = $Password
    email = $email
}
$token = $login.token
Write-Host "User: $($login.username), id: $($login.userId)"

Write-Step "Creating draft handoff notes"
$note1 = $null
$note2 = $null
try {
    $note1 = Invoke-HandoffOS "POST" "/notes" @{
        title = "支付后端交接资料草稿 - 上线检查"
        content = "发布前需要确认数据库迁移、配置开关、回滚脚本、告警联系人、核心接口冒烟结果。支付链路变更需要后端负责人和测试负责人双人确认。"
        tags = "交接资料,支付,上线,风险"
        pinned = $true
    } $token
    $note2 = Invoke-HandoffOS "POST" "/notes" @{
        title = "知识缺口补充清单 - 权限与负责人"
        content = "需要补充：生产日志查看权限、回调重放脚本负责人、退款异常处理负责人、Dify 数据集维护人。"
        tags = "知识缺口,权限,负责人"
        pinned = $false
    } $token
    Write-Host "Notes: $($note1.id), $($note2.id)"
} catch {
    Write-Warning "Note endpoints are unavailable in this runtime, skipping draft notes. $($_.Exception.Message)"
}

Write-Step "Creating handoff action items"
$taskIds = @()
try {
    $task1 = Invoke-HandoffOS "POST" "/task" @{
        title = "补充支付回调重放脚本文档"
        description = "把回调重放脚本入口、执行权限、负责人和禁止重复补单风险写入知识库。"
        deadline = (Get-Date).AddDays(2).ToUniversalTime().ToString("o")
        tags = "知识缺口,支付,回调"
        sourceNoteId = if ($note2) { $note2.id } else { $null }
    } $token
    $task2 = Invoke-HandoffOS "POST" "/task" @{
        title = "确认发布回滚负责人"
        description = "发布前确认后端负责人、测试负责人、回滚审批人在线，并补充到 Skill 资料。"
        deadline = (Get-Date).AddDays(1).ToUniversalTime().ToString("o")
        tags = "上线,负责人"
        sourceNoteId = if ($note1) { $note1.id } else { $null }
    } $token
    $task3 = Invoke-HandoffOS "POST" "/task" @{
        title = "完成新人第一天阅读清单"
        description = "阅读飞书交接文档、支付补充资料和最近问答记录，并标记未覆盖问题。"
        deadline = (Get-Date).AddDays(3).ToUniversalTime().ToString("o")
        tags = "新人,阅读清单"
        sourceNoteId = $null
    } $token
    Invoke-HandoffOS "POST" "/task/$($task3.id)/complete" $null $token | Out-Null
    $taskIds = @($task1.id, $task2.id, $task3.id)
    Write-Host "Tasks: $($taskIds -join ',')"
} catch {
    Write-Warning "Task endpoints are unavailable in this runtime, skipping handoff action items. $($_.Exception.Message)"
}

Write-Step "Creating Skill"
$skill = Invoke-HandoffOS "POST" "/skill" @{
    name = "完整演示-支付后端交接助手-" + (Get-Date -Format "HHmmss")
    roleDescription = "面向新人接手支付后端模块，覆盖飞书资料同步、知识库入库、Skill 蒸馏、RAG 问答、引用来源、反馈和作业审计。"
} $token
$skillId = $skill.id
Write-Host "Skill id: $skillId"

Write-Step "Syncing real Feishu sources into Dify Knowledge"
$sync = Invoke-HandoffOS "POST" "/skill/$skillId/sources/sync" @{
    documentRefs = $DocumentRefs
    chatId = $ChatId
    limit = 20
} $token 360
Write-Host "Sources: total=$($sync.sourceCount), docs=$($sync.documentSourceCount), chats=$($sync.chatSourceCount), dataset=$($sync.difyDatasetId)"

Write-Step "Adding manual knowledge documents"
$manualContent = @"
# 支付后端交接补充资料

## 上线检查
1. 确认数据库迁移脚本已在预发执行。
2. 确认 feature flag 默认关闭，并记录开启负责人。
3. 确认核心接口冒烟：创建订单、支付回调、退款、超时关闭。
4. 确认告警联系人和回滚负责人在线。

## 回滚规则
如果支付成功率低于 98% 且持续 10 分钟，应先关闭新开关，再回滚应用版本。回滚后需要检查幂等表和第三方回调日志。

## 风险点
不要直接重复补单；先查幂等表、第三方请求日志和订单状态机。
"@
$doc = Invoke-HandoffOS "POST" "/skills/$skillId/documents" @{
    title = "支付后端交接补充资料"
    sourceType = "MANUAL_MARKDOWN"
    sourceUrl = "handoffos://demo/payment-handoff"
    content = $manualContent
} $token
$parsed = Invoke-HandoffOS "POST" "/skills/$skillId/documents/$($doc.id)/parse" @{
    parseMode = "markdown"
} $token
$vector = Invoke-HandoffOS "POST" "/skills/$skillId/documents/$($doc.id)/vectorize" @{
    embeddingModel = "dify-managed"
} $token 360
Write-Host "Manual document: id=$($doc.id), chunks=$($parsed.chunkCount), difyDocument=$($vector.difyDocumentId)"

Write-Step "Generating skill summary and distillation"
$summary = Invoke-HandoffOS "POST" "/skills/$skillId/summary" $null $token 240
$distillJob = Invoke-HandoffOS "POST" "/skill/$skillId/distill" $null $token 300
Write-Host "Summary document: $($summary.id), status=$($summary.status)"
Write-Host "Distill job: $($distillJob.id), status=$($distillJob.status), run=$($distillJob.difyWorkflowRunId)"

Write-Step "Creating Q&A records"
$questions = @(
    "上线前需要检查哪些事项？",
    "支付回调异常时应该先查什么？",
    "新人第一天接手这个模块应该先看什么？",
    "当前知识库里有没有今年绩效规则？"
)

$qaLogIds = @()
foreach ($question in $questions) {
    $answer = Invoke-HandoffOS "POST" "/skills/$skillId/ask" @{
        question = $question
    } $token 300
    $qaLogIds += $answer.qaLogId
    Write-Host "QA: log=$($answer.qaLogId), noAnswer=$($answer.noAnswer), question=$question"
}

Write-Step "Creating Skill chat job record"
$skillAskJob = Invoke-HandoffOS "POST" "/skill/$skillId/ask" @{
    question = "请用交接清单形式总结支付后端发布前的关键检查项。"
} $token 300
Write-Host "Skill chat job: $($skillAskJob.id), status=$($skillAskJob.status), run=$($skillAskJob.difyWorkflowRunId)"

Write-Step "Adding answer feedback"
if ($qaLogIds.Count -gt 0) {
    $feedback1 = Invoke-HandoffOS "POST" "/ai/qa/$($qaLogIds[0])/feedback" @{
        rating = 5
        feedbackType = "HELPFUL"
        comment = "回答包含上线检查步骤，可用于新人交接演示。"
    } $token
    Write-Host "Positive feedback: $($feedback1.feedbackId)"
}
if ($qaLogIds.Count -gt 3) {
    $feedback2 = Invoke-HandoffOS "POST" "/ai/qa/$($qaLogIds[3])/feedback" @{
        rating = 2
        feedbackType = "KNOWLEDGE_GAP"
        comment = "绩效规则不属于当前知识库范围，应提示补充资料。"
    } $token
    Write-Host "Gap feedback: $($feedback2.feedbackId)"
}

Write-Step "Reading search, history, jobs, stats"
$search = Invoke-HandoffOS "GET" "/skills/$skillId/search?query=%E5%9B%9E%E6%BB%9A&limit=5" $null $token
$history = Invoke-HandoffOS "GET" "/skills/$skillId/qa-history?page=0&size=20" $null $token
$jobs = Invoke-HandoffOS "GET" "/skill/$skillId/jobs?limit=20" $null $token
$notes = Invoke-HandoffOS "GET" "/note/list" $null $token
$tasks = Invoke-HandoffOS "GET" "/task/list" $null $token
$stats = Invoke-HandoffOS "GET" "/admin/ai/stats?skillId=$skillId" $null $token
$analysis = Invoke-HandoffOS "POST" "/admin/ai/log-analysis" @{
    skillId = $skillId
} $token

Write-Host "Search hits: $($search.Count)"
Write-Host "QA history: $($history.Count)"
Write-Host "Jobs: $($jobs.Count)"
Write-Host "Notes: $($notes.Count)"
Write-Host "Tasks: $($tasks.Count)"
Write-Host "Stats usage=$($stats.usage), failed=$($stats.failedCount), noAnswer=$($stats.noAnswerCount), negative=$($stats.negativeFeedbackCount)"
Write-Host "Analysis summary: $($analysis.summary)"

Write-Step "Complete demo data ready"
Write-Host "Frontend: http://localhost:5173"
Write-Host "Username: $Username"
Write-Host "Password: $Password"
Write-Host "SkillId: $skillId"
Write-Host "DifyDatasetId: $($sync.difyDatasetId)"
if ($note1 -or $note2) {
    Write-Host "NoteIds: $(@($note1.id, $note2.id) | Where-Object { $_ } | Join-String -Separator ',')"
}
if ($taskIds.Count -gt 0) {
    Write-Host "TaskIds: $($taskIds -join ',')"
}
Write-Host "QaLogIds: $($qaLogIds -join ',')"
