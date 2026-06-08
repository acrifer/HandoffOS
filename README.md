# LifeOS 3.0 - 团队交接 Skill 蒸馏平台

LifeOS 是一个面向校招展示的 **Java 后端 + AI 应用** 项目。新版把原来的个人知识管理/RAG 项目升级为团队交接 Skill 平台：Spring Boot 负责业务控制面，飞书负责真实企业资料来源，Dify 负责 Knowledge Base、Workflow 和 Chatflow，最终生成可问答、可审计、可演示的交接助手。

## 项目亮点

- **真实业务场景**：从飞书文档和群聊拉取项目交接资料，解决新人接手项目时“资料散、口径乱、没人讲”的问题。
- **Agent/Skill 技术潮流**：把非结构化资料蒸馏成角色边界、工作原则、决策规则、检查清单、风险提示和问答 Skill。
- **Java 控制面深度**：Spring Boot 管用户、Skill、来源、Dify dataset 映射、作业审计、fail-fast 错误返回和 API 契约。
- **低代码平台落地**：Dify 托管 Knowledge Base 与 Workflow，LifeOS 不重复造平台轮子，而是做企业系统集成和可观测控制面。
- **真实链路演示**：飞书和 Dify 必须使用真实凭证；凭证缺失或外部失败会返回明确错误，不伪造成功数据。
- **架构取舍可讲**：保留 pgvector 自研 RAG 作为早期方案，对比说明为什么精品版选择 Dify 托管知识库。

## 架构

```text
Vue 3 Skill Workspace
        |
Spring Boot 3.3 Control Plane
        |-- User/Auth
        |-- Handoff Skill API
        |-- AI Workflow Job Audit
        |-- Feishu Adapter
        |-- Dify Adapter
        |
PostgreSQL + pgvector + Redis
        |
External Runtime
        |-- Feishu OpenAPI: docs + chat messages
        |-- Dify Knowledge: dataset + documents
        |-- Dify Workflow/Chatflow: distill + ask
```

## 核心流程

```text
1. 创建 Skill
2. 输入飞书文档链接/ID、群 chat_id、时间范围
3. 后端拉取飞书文档 raw content 和群聊消息
4. LifeOS 去重、记录 source、同步到 Dify dataset
5. 触发 Dify distill workflow，生成结构化交接 Skill
6. 用户向 Skill 提问，Dify chatflow 基于 Knowledge 回答
7. LifeOS 保存 answer、citations、workflow_run_id 和作业状态
```

## 技术栈

- 后端：Spring Boot 3.3、Spring Data JPA、PostgreSQL 16、pgvector、Redis、JWT、OpenAPI
- 前端：Vue 3、Vite、Axios
- AI/Agent：Dify Knowledge Base、Dify Workflow/Chatflow、DeepSeek/OpenAI 兼容接口
- 集成：Feishu OpenAPI、Lark/Feishu OpenAPI SDK 依赖、REST Adapter
- 部署：Docker Compose

## 快速启动

```bash
cp .env.example .env
docker compose up --build -d
```

访问地址：

- 前端：http://localhost:5173
- API：http://localhost:8080/api
- Swagger：http://localhost:8080/api/swagger-ui.html

默认情况下 `DIFY_DEMO_MODE=false`、`FEISHU_DEMO_FALLBACK_ENABLED=false`。需要先配置真实 Dify 和飞书凭证；凭证缺失或 API 失败会直接返回错误。

## 接入真实 Dify

详细配置见 [DIFY_CLOUD_SETUP.md](./DIFY_CLOUD_SETUP.md)。Dify Cloud 场景下 API Base URL 使用 `https://api.dify.ai/v1`。

1. 部署或使用已有 Dify，确认 API Base URL，例如 `https://api.dify.ai/v1` 或 `http://localhost:5001/v1`。
2. 创建 Knowledge API Key，填入 `DIFY_API_KEY`。
3. 发布一个用于 Skill 蒸馏的 Workflow，填入 `DIFY_DISTILL_WORKFLOW_KEY`。
4. 发布一个用于 Skill 问答的 Chatflow/Chat App，填入 `DIFY_ASK_APP_KEY`。
5. 确认 `DIFY_DEMO_MODE=false`。

发布两个 Dify 应用后，可以运行 Cloud 冒烟脚本：

```powershell
.\scripts\dify-cloud-smoke.ps1
```

本地后端启动后，可以运行完整 LifeOS API 冒烟脚本：

```powershell
.\scripts\lifeos-e2e-smoke.ps1
```

验证真实飞书凭证和权限：

```powershell
.\scripts\feishu-smoke.ps1 -DocumentRef "飞书文档链接或 ID" -ChatId "群 chat_id"
```

建议 Workflow 输入：

- `skill_name`
- `role_description`
- `dataset_id`
- `sources`

建议输出字段：

- `handoffSkill.roleBoundaries`
- `handoffSkill.workPrinciples`
- `handoffSkill.decisionRules`
- `handoffSkill.workflowChecklists`
- `handoffSkill.communicationStyle`
- `handoffSkill.riskWarnings`
- `handoffSkill.handoffQuestions`

## 接入真实飞书

在飞书开放平台创建企业自建应用，并配置：

```env
FEISHU_APP_ID=cli_xxx
FEISHU_APP_SECRET=xxx
FEISHU_DEMO_FALLBACK_ENABLED=false
```

后端会优先调用真实飞书 OpenAPI：

- 获取 tenant access token
- 读取 docx raw content
- 按 chat_id 和时间范围读取群聊消息

如果权限、网络或凭证失败，系统会返回错误并记录失败作业，不会写入脱敏演示来源。

## 主要 API

- `POST /api/skill` 创建交接 Skill
- `GET /api/skill` Skill 列表
- `GET /api/skill/{skillId}` Skill 详情
- `POST /api/skill/{skillId}/sources/sync` 同步飞书资料并写入 Dify Knowledge
- `POST /api/skill/{skillId}/distill` 触发 Dify Workflow 蒸馏
- `POST /api/skill/{skillId}/ask` 触发 Dify Chatflow 问答
- `GET /api/skill/{skillId}/jobs` 查询 Skill 作业
- `GET /api/note/jobs/{jobId}` 前端轮询通用作业详情

## 面试讲法

30 秒版本：

> 我把 LifeOS 从普通知识管理项目升级成团队交接 Skill 蒸馏平台。Spring Boot 做控制面，负责用户、Skill、飞书来源、Dify dataset 映射和作业审计；Dify 托管 Knowledge Base 和 Agentic Workflow；飞书提供真实企业资料来源。这个项目展示了我对 RAG、Agent Workflow、系统集成、fail-fast 错误处理和架构取舍的理解。

技术深度可以展开三点：

- **控制面和运行时分离**：Java 系统管业务状态与审计，Dify 负责模型编排和知识检索。
- **真实数据接入**：飞书 token 缓存、分页拉取、去重、入库状态、失败作业审计。
- **可观测 AI 工作流**：每次同步、蒸馏、问答都有 job、request、result、workflow_run_id 和 error message。

## 验证命令

```bash
cd lifeos-monolith
mvn -q test

cd ../lifeos-web
npm run build
```

真实 Dify Cloud 链路：

```powershell
.\scripts\dify-cloud-smoke.ps1
```

如果 Dify 返回 `Workflow not published`，说明应用 API Key 已能鉴权，但 Workflow/Chatflow 还没有在 Dify 控制台发布。

## 项目边界

- 旧 `lifeos-backend` 微服务目录保留为架构对比材料，不作为新版运行入口。
- 现有 pgvector RAG 仍保留，作为“自研 RAG 到 Dify 托管 Knowledge”的演进故事。
- 本轮主链路优先团队交接 Skill，不重写个人笔记、任务和行为看板全部历史功能。
