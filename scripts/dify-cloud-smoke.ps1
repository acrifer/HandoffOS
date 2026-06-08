param(
    [string]$EnvFile = ".env",
    [string]$DatasetName = "",
    [switch]$KeepDataset
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

foreach ($key in @("DIFY_BASE_URL", "DIFY_API_KEY", "DIFY_DISTILL_WORKFLOW_KEY", "DIFY_ASK_APP_KEY")) {
    if ([string]::IsNullOrWhiteSpace($envMap[$key])) {
        throw "$key is empty in $EnvFile"
    }
}

$baseUrl = $envMap["DIFY_BASE_URL"].TrimEnd("/")
if ([string]::IsNullOrWhiteSpace($DatasetName)) {
    $DatasetName = "HandoffOS Smoke Knowledge " + (Get-Date -Format "yyyyMMddHHmmss")
}
$datasetHeaders = @{
    Authorization = "Bearer " + $envMap["DIFY_API_KEY"]
    "Content-Type" = "application/json"
}
$workflowHeaders = @{
    Authorization = "Bearer " + $envMap["DIFY_DISTILL_WORKFLOW_KEY"]
    "Content-Type" = "application/json"
}
$chatHeaders = @{
    Authorization = "Bearer " + $envMap["DIFY_ASK_APP_KEY"]
    "Content-Type" = "application/json"
}

function Write-Step($message) {
    Write-Host ""
    Write-Host "==> $message"
}

Write-Step "Checking Knowledge API"
$datasets = Invoke-RestMethod -Method Get -Uri "$baseUrl/datasets?page=1&limit=5" -Headers $datasetHeaders
Write-Host "Knowledge API OK. Current dataset total: $($datasets.total)"

$dataset = $null
try {
    Write-Step "Creating smoke dataset"
    $datasetBody = @{
        name = $DatasetName
        description = "Smoke dataset created by LifeOS Dify Cloud validation script"
        indexing_technique = "high_quality"
        permission = "only_me"
    } | ConvertTo-Json -Compress
    $dataset = Invoke-RestMethod -Method Post -Uri "$baseUrl/datasets" -Headers $datasetHeaders -Body $datasetBody
    Write-Host "Dataset created: $($dataset.id)"

    Write-Step "Uploading smoke document"
    $docBody = @{
        name = "release-checklist"
        text = "Before release, confirm database migrations, feature flags, rollback scripts, alert owners, and core API smoke tests."
        indexing_technique = "high_quality"
        process_rule = @{ mode = "automatic" }
    } | ConvertTo-Json -Depth 6 -Compress
    $document = Invoke-RestMethod -Method Post -Uri "$baseUrl/datasets/$($dataset.id)/document/create-by-text" -Headers $datasetHeaders -Body $docBody
    Write-Host "Document created: $($document.document.id), status: $($document.document.indexing_status)"

    Write-Step "Running distill workflow"
    $sourcesJson = (@(
        @{
            title = "release-checklist"
            contentPreview = "Before release, confirm database migrations, feature flags, rollback scripts, alert owners, and core API smoke tests."
        }
    ) | ConvertTo-Json -Depth 6 -Compress)
    $workflowBody = @{
        inputs = @{
            skill_name = "Payment Backend Handoff"
            role_description = "Owns payment callback, release checklist, and incident rollback"
            dataset_id = $dataset.id
            sources_json = $sourcesJson
            sources = $sourcesJson
        }
        response_mode = "blocking"
        user = "lifeos-smoke"
    } | ConvertTo-Json -Depth 8 -Compress
    $workflow = Invoke-RestMethod -Method Post -Uri "$baseUrl/workflows/run" -Headers $workflowHeaders -Body $workflowBody
    Write-Host "Workflow OK. Run id: $($workflow.data.workflow_run_id)"

    Write-Step "Running ask app"
    $retrieveBody = @{
        query = "What should be checked before release?"
        external_retrieval_model = @{
            top_k = 5
            score_threshold_enabled = $false
        }
    } | ConvertTo-Json -Depth 8 -Compress
    $retrieval = Invoke-RestMethod -Method Post -Uri "$baseUrl/datasets/$($dataset.id)/retrieve" -Headers $datasetHeaders -Body $retrieveBody
    $retrievedContext = ""
    $citations = @()
    $index = 1
    foreach ($record in $retrieval.records) {
        if ($record.segment.content) {
            $source = if ($record.segment.document.name) { $record.segment.document.name } elseif ($record.segment.document_name) { $record.segment.document_name } else { "segment-$index" }
            $citations += $source
            $retrievedContext += "[$index] $source`n$($record.segment.content)`n`n"
            $index += 1
        }
    }
    $chatBody = @{
        inputs = @{
            skill_name = "Payment Backend Handoff"
            dataset_id = $dataset.id
            retrieved_context = $retrievedContext.Trim()
            citations = ($citations -join "`n")
        }
        query = "What should be checked before release?"
        response_mode = "blocking"
        user = "lifeos-smoke"
    } | ConvertTo-Json -Depth 8 -Compress
    $chat = Invoke-RestMethod -Method Post -Uri "$baseUrl/chat-messages" -Headers $chatHeaders -Body $chatBody
    Write-Host "Chat OK. Message id: $($chat.message_id)"
    Write-Host "Answer preview: $($chat.answer.Substring(0, [Math]::Min(120, $chat.answer.Length)))"
    Write-Host ""
    Write-Host "Dify Cloud smoke test passed."
} catch {
    Write-Warning "Dify smoke test failed. If the error is Workflow not published, publish both Dify apps first. Workflow inputs: skill_name, role_description, dataset_id, sources. Chat inputs: skill_name, dataset_id."
    throw
} finally {
    if ($dataset -and -not $KeepDataset) {
        Write-Step "Cleaning up smoke dataset"
        try {
            Invoke-RestMethod -Method Delete -Uri "$baseUrl/datasets/$($dataset.id)" -Headers $datasetHeaders | Out-Null
            Write-Host "Dataset deleted: $($dataset.id)"
        } catch {
            Write-Warning "Could not delete smoke dataset $($dataset.id). You can delete it from Dify manually."
        }
    }
}
