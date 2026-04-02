# Day 20

## 今日目标

- 吃透行为事件异步链路和幂等处理
- 学会把“统计不阻塞主链路”讲出来

## 知识速记

- 行为统计属于辅助链路，不应该阻塞主业务。
- 任务完成、笔记整理等事件先发 MQ，再由行为服务异步消费。
- 行为服务落库后再做 Dashboard 聚合，主链路更轻。
- 事件里带 `eventId`，消费侧对重复事件做幂等兜底。
- 当前幂等实现方式是捕获 `DuplicateKeyException` 后忽略重复事件。

## 标准回答

### 为什么统计适合异步

统计结果通常不是主流程的直接返回值，用户更关心任务是否完成、笔记是否保存成功，而不是统计是否当场更新。所以把统计异步化，可以让主流程更快，也能让统计逻辑更独立。

### 你们怎么处理重复消费

行为事件带唯一 `eventId`，消费侧落库时如果发现重复主键或唯一键，会捕获异常后忽略。这样即使消息重复投递，也不会重复统计。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 20
- [ ] 阅读 `lifeos-backend/lifeos-task-service/src/main/java/com/lifeos/task/service/impl/TaskServiceImpl.java`
- [ ] 阅读 `lifeos-backend/lifeos-behavior-service/src/main/java/com/lifeos/behavior/messaging/BehaviorEventConsumer.java`
- [ ] 阅读 `lifeos-backend/lifeos-behavior-service/src/main/java/com/lifeos/behavior/service/impl/BehaviorServiceImpl.java`

## 加分项

- [ ] 总结 `DuplicateKeyException` 的幂等意义

## 代码定位

- [ ] 找出行为事件发送逻辑
- [ ] 找出行为服务聚合 Dashboard 的逻辑

## 算法

- [ ] 打家劫舍二刷
- [ ] 最长连续序列二刷

## 今日交付物

- [ ] 1 段行为事件链路口述稿

## 晚间验收

- [ ] 能解释为什么统计适合异步
- [ ] 能解释如何做重复消费兜底

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
