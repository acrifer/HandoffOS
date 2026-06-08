# LifeOS 3.0 面试演示指南

## 项目定位

**团队交接 Skill 蒸馏平台**

这个项目不是普通 CRUD，也不是单独包一层聊天接口。它模拟真实企业里的交接场景：资料在飞书文档里，关键决策在群聊里，新人需要快速知道“我该看什么、谁负责什么、上线风险在哪里”。LifeOS 负责把这些资料同步到 Dify Knowledge，并通过 Workflow 蒸馏成可问答的 Skill。

## 30 秒电梯演讲

> 我把 LifeOS 重构成团队交接 Skill 蒸馏平台。Spring Boot 作为控制面，管理用户、Skill、飞书来源、Dify dataset 映射和 AI 作业审计；飞书提供真实文档和群聊来源；Dify 托管 Knowledge Base 和 Agentic Workflow。这样既跟上 Agent/Skill/RAG 的技术趋势，又能体现 Java 后端在系统集成、状态管理、fail-fast 错误处理和可观测性上的工程深度。

## 5-10 分钟演示流程

当前推荐演示模式是 **真实 Dify Cloud + 真实飞书 OpenAPI**：所有核心数据都来自真实接口。飞书或 Dify 凭证不可用时，系统会显示明确错误并记录失败作业，不再用脱敏 fixture 伪造成功链路。

### 1. 架构开场

展示 README 架构图，说明三层：

- Vue Skill Workspace：面试官看到的操作台。
- Spring Boot Control Plane：业务状态、来源同步、Dify 映射、作业审计。
- Dify + 飞书：低代码 Agent Runtime 和真实企业数据源。

话术：

> 我没有选择把所有 Agent 编排都手写在 Java 里，而是把 Java 后端定位为控制面，把 Dify 定位为运行时。这样工程边界更接近真实企业系统：业务系统要管权限、审计、数据同步和失败恢复，低代码平台负责模型编排和知识库能力。

### 2. 创建 Skill

操作：

1. 登录系统。
2. 进入 Skill 页面。
3. 新建「支付后端交接助手」。
4. 填写角色边界：支付回调、订单状态机、上线回滚、告警处理。

讲解点：

- `handoff_skill` 保存业务对象。
- 每个 Skill 后续会映射到一个 Dify Dataset。
- 这是从“笔记”升级到“可复用 Skill”的关键抽象。

### 3. 同步飞书来源

操作：

1. 输入飞书文档链接或 ID。
2. 输入群聊 `chat_id` 和时间范围。
3. 点击同步来源。
4. 展示来源数量、文档数、群聊数、Dify dataset id、入库状态。

话术：

> 这里走真实飞书 API：后端获取 tenant access token，读取 docx raw content，并按 chat_id 和时间范围拉群聊消息。同步后会做内容 hash 去重，再写入 Dify Knowledge。如果凭证、权限或网络失败，接口直接返回错误并记录失败 job，不会写入伪来源。

技术点：

- token 缓存。
- 文档和群聊统一成 `handoff_skill_source`。
- `content_hash` 去重，`dify_document_id` 记录入库映射。
- 如果 Dify dataset 被手动删除，下一次同步会检测失效并重建新 dataset；如果是网络或鉴权失败，则直接报错。
- `ai_workflow_job` 记录同步作业。

### 4. 蒸馏 Skill

操作：

1. 点击开始蒸馏。
2. 等待作业完成。
3. 切换查看角色边界、工作原则、决策规则、检查清单、沟通风格、风险提示、交接问题。

话术：

> 蒸馏不是简单总结，而是把交接材料结构化成一个可复用 Skill。Dify Workflow 负责 LLM 编排，LifeOS 保存结构化结果和 workflow_run_id。这样面试时我可以讲 prompt/agent，也可以讲 Java 系统如何把 AI 输出纳入业务状态。

### 5. Skill 问答

操作：

1. 提问：「新人第一天应该先看什么？」
2. 展示回答和引用来源。
3. 打开 AI 历史或作业状态，展示 request/result/error/workflow_run_id。

话术：

> 问答走 Dify Chatflow，检索 Dify Knowledge 后生成答案。LifeOS 会把问题、答案、引用和 Dify workflow run id 保存下来，便于审计和复盘。这也是我区别于 demo 项目的点：不是只展示一个回答，而是能追踪 AI 到底做过什么。

### 6. 作业审计

操作：

1. 滚动到「作业审计」区域。
2. 展示最近 10 条同步、蒸馏、问答记录。
3. 指出 `datasetId`、`difyWorkflowRunId`、失败错误信息。

话术：

> AI 应用落地后，最怕的是“模型说了什么没人知道、失败了也不好排查”。我把每次同步、蒸馏和问答都抽象成 AI job，前端可以直接看到状态、Dify run id 和错误原因。这让项目从玩具 demo 更接近企业系统。

## 常见问题

### Q1: 为什么选 Dify，而不是全部自己写？

答：

> 我早期已经实现过 pgvector + Embedding + LLM 的自研 RAG，所以知道底层原理。精品版选择 Dify，是因为校招项目需要同时体现技术趋势和工程落地。Dify 负责 Knowledge 和 Workflow，Java 负责控制面、数据同步、权限、审计和异常处理。这种边界更接近企业真实使用 AI 平台的方式。

### Q2: 你的 Java 后端技术深度在哪里？

答：

> 深度不在“调一次模型接口”，而在控制面设计：Skill 生命周期、飞书来源同步、Dify dataset/document 映射、内容去重、作业审计、fail-fast 错误返回、统一 API 契约、JWT 认证和 Docker 化部署。这些都是 AI 应用真正落地时绕不开的后端问题。

### Q3: 如果飞书或 Dify 现场不可用怎么办？

答：

> 现在项目取消了 mock/demo fallback。飞书或 Dify 不可用时会返回明确错误，例如凭证缺失、权限不足、Workflow 未发布。这样演示的数据可信，也能体现系统对外部依赖失败的处理能力。

### Q4: 和普通 RAG 项目相比创新在哪里？

答：

> 普通 RAG 多是“上传文档然后聊天”。我这里加入了 Skill 蒸馏：把文档和群聊转成角色边界、工作原则、决策规则、检查清单和风险提示。它更像一个可以交给新人的工作助手，而不是一个搜索框。

### Q5: 这个项目怎么体现架构思考？

答：

> 我保留了旧微服务作为反例：原先 7 个服务共享数据库，属于过度设计。新版改为模块化单体，并把 AI 运行时外包给 Dify。整体思路是降低不必要的分布式复杂度，把复杂度放到真正有价值的地方：数据接入、控制面、可观测和 AI 工作流。

## 演示前检查

- [ ] Dify Workflow 和 Chatflow 已发布
- [ ] `.\scripts\dify-cloud-smoke.ps1`
- [ ] `docker compose up --build -d` 或本地 Spring Boot 已启动
- [ ] 前端可访问：http://localhost:5173
- [ ] Swagger 可访问：http://localhost:8080/api/swagger-ui.html
- [ ] 能登录并创建 Skill
- [ ] `.\scripts\lifeos-e2e-smoke.ps1`
- [ ] 如需真实飞书，运行 `.\scripts\feishu-smoke.ps1 -DocumentRef "..." -ChatId "..."`
- [ ] 确认 `DIFY_DEMO_MODE=false`、`FEISHU_DEMO_FALLBACK_ENABLED=false`

## 推荐展示代码

- `SkillService`：主业务编排，串联飞书、Dify、作业审计。
- `DifyClient`：Knowledge/Workflow/Chatflow 运行时适配。
- `FeishuClient`：真实飞书 OpenAPI 适配和 token 缓存。
- `AiWorkflowJobService`：AI 作业审计和前端轮询。

## 收尾话术

> 这个项目的价值不是炫技，而是把最近的 Agent/Skill/RAG 技术放进一个能解释、能运行、能报错、能审计的 Java 后端项目里。面试时我既能讲 AI 应用趋势，也能讲后端系统设计和工程落地。
