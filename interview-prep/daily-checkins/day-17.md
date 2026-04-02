# Day 17

## 今日目标

- 把 OpenFeign 和服务间调用讲顺
- 说明为什么不直接跨服务写库

## 知识速记

- OpenFeign 是声明式 HTTP 调用，目标是降低服务间调用样板代码。
- 当前项目里服务间共享 DTO 和接口定义放在 `lifeos-api`。
- 笔记服务会调用行为服务获取统计数据，AI 服务会回调笔记服务更新作业状态。
- 服务边界清晰比“直接改对方数据库”更重要。
- 面试时要强调“跨服务直接写库会破坏边界和维护性”。

## 标准回答

### 为什么服务间调用不用直接连对方数据库

因为那会破坏服务边界。每个服务应该对自己的数据负责，外部只能通过接口访问。这样职责清晰、演进更安全，也更符合微服务治理方式。

### Feign 在项目里解决了什么问题

它让服务间调用更像本地接口调用，减少了手写 HTTP 请求的模板代码，同时配合共享 DTO 和接口定义，让跨服务协作更统一。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 17
- [ ] 阅读 `lifeos-backend/lifeos-note-service/src/main/java/com/lifeos/note/NoteApplication.java`
- [ ] 阅读 `lifeos-backend/lifeos-api/pom.xml`
- [ ] 结合 `02-lifeos-project-breakdown.md` 梳理 AI 服务回调笔记服务的逻辑

## 加分项

- [ ] 思考 Feign 调用失败时的兜底方式

## 代码定位

- [ ] 找出 `@EnableFeignClients`

## 算法

- [ ] 两数之和二刷
- [ ] 有效的括号二刷

## 今日交付物

- [ ] 1 份 OpenFeign 口述提纲

## 晚间验收

- [ ] 能解释 Feign 在项目里的作用
- [ ] 能解释为什么 AI 服务不直接改笔记库

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
