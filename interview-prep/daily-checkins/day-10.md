# Day 10

## 今日目标

- 把 Redis 在 `LifeOS` 里的落地讲明白
- 形成第一个真实工程亮点

## 知识速记

- `UserServiceImpl` 里 Redis 用于登录限流和登录态存储。
- `AuthFilter` 里 Redis 用于二次校验 token 是否仍然有效。
- `TaskServiceImpl` 里 Redis 用于任务列表缓存。
- 当前缓存方案是“旁路缓存 + 写后删缓存”，适合中小项目。
- 当前登录方案不是纯 JWT，而是 `JWT + Redis`。

## 标准回答

### 项目里 Redis 用在哪

项目里 Redis 有三个主要落地点。第一是登录限流，按用户名限制单位时间内的登录尝试次数；第二是登录态存储，登录成功后把 token 写入 Redis，网关每次请求都会去 Redis 校验；第三是任务列表缓存，查询先走缓存，写操作后主动删缓存。

### 为什么任务列表适合缓存

任务列表属于相对高频读取、写入不算特别频繁的场景，走缓存能降低数据库压力。当前采用写后删缓存策略，实现简单，性价比高。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 10
- [ ] 阅读 `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/service/impl/UserServiceImpl.java`
- [ ] 阅读 `lifeos-backend/lifeos-gateway/src/main/java/com/lifeos/gateway/filter/AuthFilter.java`
- [ ] 阅读 `lifeos-backend/lifeos-task-service/src/main/java/com/lifeos/task/service/impl/TaskServiceImpl.java`

## 加分项

- [ ] 总结“登录限流、登录态、列表缓存”三者的共同点和差异

## 代码定位

- [ ] 找出登录限流 key
- [ ] 找出 token 存储 key
- [ ] 找出任务列表缓存 key

## 算法

- [ ] 路径总和
- [ ] 二叉搜索树中第 K 小的元素

## 今日交付物

- [ ] 输出“项目里 Redis 用在哪”标准答案

## 晚间验收

- [ ] 能 2 分钟讲完 Redis 三个落地点
- [ ] 能解释任务列表缓存更新策略

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
