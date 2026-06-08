# HandoffOS Dify DSL

This directory contains two Dify apps used by HandoffOS:

- `handoffos-skill-distiller.yml`: Workflow app for Skill distillation.
- `handoffos-skill-qa-chatflow.yml`: Advanced Chat app for grounded Q&A.

## Import Order

1. Dify Console -> Studio -> Import DSL.
2. Import `handoffos-skill-distiller.yml`.
3. Open the LLM node and choose an available model in your Dify workspace.
4. Publish the Workflow app.
5. Copy its API key into `.env`:

```env
DIFY_DISTILL_WORKFLOW_KEY=app-...
```

6. Import `handoffos-skill-qa-chatflow.yml`.
7. Open the LLM node and choose an available model in your Dify workspace.
8. Publish the Chatflow app.
9. Copy its API key into `.env`:

```env
DIFY_ASK_APP_KEY=app-...
```

## Required Inputs

The distill Workflow must keep these input variable names:

```text
skill_name
role_description
dataset_id
sources_json
sources
```

The QA Chatflow must keep these input variable names:

```text
skill_name
dataset_id
retrieved_context
citations
```

HandoffOS calls Dify by these names. Renaming them will break the API chain.

## Model Note

The DSL files use `openai / gpt-4o-mini` as a portable default. If your Dify workspace uses DeepSeek or another OpenAI-compatible provider, change the model in each LLM node after import, then publish.

## Smoke Test

After publishing both apps and updating `.env`, run:

```powershell
docker compose up --build -d lifeos-app
.\scripts\dify-cloud-smoke.ps1
```

Then run the full Feishu + Dify chain:

```powershell
.\scripts\lifeos-e2e-smoke.ps1 -DocumentRefs "https://xcncmhy2n1xv.feishu.cn/wiki/OFZ8wTwvRiF8tFkmsZ1cAFdLnud?from=from_copylink" -ChatId "oc_03f40812cec2ed8e6af53b20f19bfe22"
```
