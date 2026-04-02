# Day 19

## 今日目标

- 吃透 AI 异步作业链路
- 形成一个 3 分钟可讲的技术亮点

## 知识速记

- 笔记服务提交 AI 作业时不是直接同步等结果，而是先落作业表。
- 作业记录能让前端有可见状态，也方便失败排查和重试。
- 落库后发送 RocketMQ 消息，AI 服务再消费处理。
- AI 服务处理完后通过 Feign 回调笔记服务更新作业状态。
- 这种写法体现的是“服务边界清晰 + 异步链路可观测”。

## 标准回答

### AI 异步链路怎么讲

用户在笔记服务发起摘要或整理后，笔记服务先把作业记录写到 `ai_workflow_job`，再把作业命令发到 RocketMQ。AI 服务消费消息后执行具体处理，再通过 Feign 回调笔记服务更新状态和结果。这样前端不需要长时间等待，系统也更方便处理失败场景。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 19
- [ ] 阅读 `lifeos-backend/lifeos-note-service/src/main/java/com/lifeos/note/service/impl/AiJobServiceImpl.java`
- [ ] 阅读 `lifeos-backend/lifeos-ai-service/src/main/java/com/lifeos/ai/messaging/AiJobConsumer.java`
- [ ] 按顺序写出：提交、落库、发 MQ、消费、回调、更新结果

## 加分项

- [ ] 额外总结失败场景和处理方式

## 代码定位

- [ ] 找出 `createAndDispatchJob`
- [ ] 找出 `updateStatus`

## 算法

- [ ] 二分查找二刷
- [ ] 组合总和二刷

## 今日交付物

- [ ] 1 份 AI 异步链路讲稿

## 晚间验收

- [ ] 能无稿讲完 AI 作业链路
- [ ] 能说出 3 个失败点及处理方式

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
