# Dify Cloud Setup for HandoffOS

This project uses Dify Cloud as the managed Knowledge + Workflow runtime.

## 1. Environment

Set these values in `.env`:

```env
DIFY_BASE_URL=https://api.dify.ai/v1
DIFY_API_KEY=dataset-...
DIFY_DISTILL_WORKFLOW_KEY=app-...
DIFY_ASK_APP_KEY=app-...
DIFY_DEMO_MODE=false
```

`DIFY_API_KEY` must be a Knowledge Service API key. The two `app-...` keys must come from the published Workflow and Chat/Chatflow apps.

## 2. Knowledge

LifeOS can create a Dify dataset automatically during `POST /api/skill/{skillId}/sources/sync`.

If a Dify dataset is deleted manually, the backend checks it on the next sync, creates a new dataset, clears stale document mappings, and re-indexes the current sources.

## 3. Distill Workflow

Create a Workflow app named:

```text
HandoffOS Skill Distiller
```

Add these input variables:

```text
skill_name
role_description
dataset_id
sources
sources_json
```

The Workflow should output one or both of these fields:

```text
answer
handoffSkill
```

Recommended prompt:

```text
你是团队交接 Skill 蒸馏助手。请只基于输入资料生成结构化交接说明，不要编造资料里没有的信息。

【Skill 名称】{{skill_name}}
【角色说明】{{role_description}}
【Dify Dataset ID】{{dataset_id}}
【资料来源 JSON】
{{sources_json}}

请返回 JSON，字段如下：
{
  "roleBoundaries": ["..."],
  "workPrinciples": ["..."],
  "decisionRules": ["..."],
  "workflowChecklists": ["..."],
  "communicationStyle": ["..."],
  "riskWarnings": ["..."],
  "handoffQuestions": ["..."]
}

要求：
1. 每条规则都必须能从资料来源中找到依据。
2. 如果资料不足，相关字段返回空数组。
3. 如果资料冲突，写入 riskWarnings，并提示需要负责人确认。
4. 只输出 JSON，不要输出 Markdown。
```

Publish the Workflow before using its API key. If it is not published, Dify returns:

```json
{"code":"invalid_param","message":"Workflow not published","status":400}
```

## 4. Ask Chatflow

Create a Chatflow or Chatbot app named:

```text
HandoffOS Skill QA
```

Add these input variables:

```text
skill_name
dataset_id
retrieved_context
citations
```

HandoffOS retrieves chunks from Dify Knowledge before calling this app, then passes the retrieved text through `retrieved_context`. Keep the Chatflow simple: it only needs to generate a grounded answer from the provided context. Do not add a fixed Knowledge node unless you intentionally want to bind the app to one dataset.

Recommended system instruction:

```text
You are a team handoff Q&A assistant. Answer only from the provided retrieved_context.

Skill: {{skill_name}}
Dataset ID: {{dataset_id}}
Citations: {{citations}}
Retrieved context:
{{retrieved_context}}

Rules:
1. Start with a short direct answer.
2. If the answer involves a process, list the steps.
3. Mark risks with "Risk:".
4. Cite source numbers such as [1] when the context includes them.
5. If retrieved_context is empty or insufficient, answer: "当前知识库没有足够信息", then list what information should be added.
6. Do not invent owners, commands, dates, URLs, or release decisions.
```

Publish the app before using its API key.

## 5. Smoke Test

Run this after the two apps are published:

```powershell
.\scripts\dify-cloud-smoke.ps1
```

The script checks:

1. Knowledge API key
2. Dataset creation
3. Document upload
4. Distill Workflow API
5. Ask Chat/Chatflow API
