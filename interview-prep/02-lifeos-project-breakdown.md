# LifeOS 项目拆解

这份文档的目标不是帮你“读源码”，而是帮你把项目讲明白。

## 一句话项目定位

`LifeOS` 是一个面向个人知识管理的微服务系统，把笔记、AI 整理、任务转化和行为统计串成闭环。

## 名词提示

- `微服务`：按业务领域拆分成多个独立服务的架构方式。
- `DTO`：数据传输对象，用于接口入参、出参或服务间传递数据。
- `JWT`：无状态身份凭证。
- `Gateway`：统一入口层。
- `Feign`：声明式 HTTP 调用工具。
- `RocketMQ`：消息队列组件。
- `Nacos`：服务注册发现组件。
- `幂等`：重复执行多次，结果仍然一致。

面试时建议这样表述：

> 我做的是一个知识管理方向的微服务项目，核心链路是用户记录笔记后，可以触发 AI 摘要和整理，再把笔记里的任务抽取出来，任务完成后回写行为数据，最后在 Dashboard 做聚合展示。

## 系统架构

前端统一访问网关，网关负责路由和登录态校验，后端按领域拆成多个服务：

- `lifeos-gateway`：统一入口、鉴权、转发
- `lifeos-user-service`：注册、登录、用户资料
- `lifeos-note-service`：笔记、搜索、复习、AI 作业提交
- `lifeos-task-service`：任务创建、更新、完成
- `lifeos-ai-service`：AI 摘要、整理、任务提取、周复盘
- `lifeos-behavior-service`：行为事件记录、Dashboard 聚合
- `lifeos-api`：`DTO`、`Feign`、`MQ` 常量
- `lifeos-common`：统一返回、JWT 工具

## 主链路 1：登录鉴权

这是最值得作为面试主线讲的链路。

### 流程

1. 用户访问 `/api/user/login`
2. 网关白名单放行登录接口
3. 用户服务校验用户名密码
4. 登录成功后生成 `JWT`
5. 同时把 token 写入 Redis，作为当前用户有效登录态
6. 后续请求先经过网关
7. 网关先验 `JWT` 签名和过期时间
8. 再去 Redis 校验该 token 是否仍然有效
9. 校验通过后把 `X-User-Id` 等请求头透传给下游服务

### 对应代码入口

- `JWT` 生成与解析：
  - `lifeos-backend/lifeos-common/src/main/java/com/lifeos/common/utils/JwtUtil.java`
- 登录逻辑：
  - `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/service/impl/UserServiceImpl.java`
- 登录接口：
  - `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/controller/UserController.java`
- 网关鉴权：
  - `lifeos-backend/lifeos-gateway/src/main/java/com/lifeos/gateway/filter/AuthFilter.java`
- 网关路由：
  - `lifeos-backend/lifeos-gateway/src/main/resources/application.yml`

### 你要讲出的设计点

- 只用 JWT 不够，因为无法优雅支持退出登录和强制失效
- 所以项目采用 `JWT + Redis`：
  - `JWT` 负责无状态身份表达
  - Redis 负责登录态有效性控制
- 登录成功后把最新 token 写入 Redis，等于实现“单用户当前有效 token”
- 用户退出登录时删 Redis，对应 token 立刻失效
- 网关统一做鉴权，业务服务不用重复写登录校验逻辑

### 面试可能追问

- 为什么 token 要放 Redis？
- 这种方案还是完全无状态吗？
- 如果 Redis 挂了会怎样？
- 多端登录如何支持？

## 主链路 2：任务列表缓存

这是 Redis 的一个很好讲的落地场景。

### 流程

1. 用户请求任务列表
2. 任务服务先查 Redis
3. 命中则直接返回
4. 未命中则查 MySQL
5. 查到后回填 Redis，缓存 1 小时
6. 新增、更新、删除任务时主动删缓存

### 对应代码入口

- `lifeos-backend/lifeos-task-service/src/main/java/com/lifeos/task/service/impl/TaskServiceImpl.java`

### 你要讲出的设计点

- 读多写少列表适合缓存
- 当前策略是“旁路缓存 + 写后删缓存”
- 优点是实现简单、命中率高、适合中小项目
- 风险是短暂脏读窗口仍可能存在，但对任务列表这种场景可接受

### 可主动补充的优化点

- 给热点列表加随机过期时间，降低雪崩风险
- 如果数据一致性要求更高，可以引入延迟双删或消息通知更新

## 主链路 3：AI 异步作业

这是你项目里最像企业真实场景的亮点之一。

### 流程

1. 用户在笔记服务发起摘要、整理或任务提取
2. 笔记服务先把作业记录落库到 `ai_workflow_job`
3. 再把作业命令发到 `RocketMQ`
4. AI 服务消费消息并执行具体处理
5. AI 服务通过 `Feign` 回调笔记服务，更新作业状态和结果
6. 成功后笔记服务再把摘要等结果落到业务表

### 对应代码入口

- 提交 AI 作业：
  - `lifeos-backend/lifeos-note-service/src/main/java/com/lifeos/note/service/impl/AiJobServiceImpl.java`
- 消费 AI 作业：
  - `lifeos-backend/lifeos-ai-service/src/main/java/com/lifeos/ai/messaging/AiJobConsumer.java`

### 你要讲出的设计点

- 选择异步而不是同步调用，是因为 AI 耗时不可控
- 先落库再发 MQ，保证前端能立即拿到一个作业 ID 和状态
- AI 服务只负责处理，不直接改笔记库，而是回调笔记服务更新结果
- 这种做法更符合服务边界，避免跨服务直接写库

### 面试可讲的风险与处理

- `MQ` 发送失败：作业状态改成失败
- 消费失败：状态改成失败并抛异常，方便重试或排障
- 状态更新失败：`Feign` 返回非 200 时直接报错

## 主链路 4：行为事件异步聚合

这是 `RocketMQ` 的第二个亮点。

### 流程

1. 用户完成任务、整理笔记、从笔记提取任务
2. 对应服务发出行为事件消息
3. 行为服务消费消息并落库到 `user_behavior`
4. Dashboard 接口聚合任务数、笔记数、AI inbox、近 7 日趋势、高频标签

### 对应代码入口

- 事件发送：
  - `lifeos-backend/lifeos-task-service/src/main/java/com/lifeos/task/service/impl/TaskServiceImpl.java`
  - `lifeos-backend/lifeos-note-service/src/main/java/com/lifeos/note/service/impl/AiJobServiceImpl.java`
- 事件消费：
  - `lifeos-backend/lifeos-behavior-service/src/main/java/com/lifeos/behavior/messaging/BehaviorEventConsumer.java`
- 聚合逻辑：
  - `lifeos-backend/lifeos-behavior-service/src/main/java/com/lifeos/behavior/service/impl/BehaviorServiceImpl.java`

### 你要讲出的设计点

- 行为统计天然适合异步，因为不该阻塞主业务流程
- 行为事件用了 `eventId`
- 行为服务对重复事件做了`幂等`兜底：捕获 `DuplicateKeyException` 后忽略
- 好处是主业务更轻，统计逻辑更独立

## `Nacos` 和服务治理

### 当前项目里的落地点

- 每个服务通过 `@EnableDiscoveryClient` 注册到 Nacos
- 网关通过 `lb://service-name` 做服务发现与转发
- 配置文件中通过 `spring.cloud.nacos.discovery.server-addr` 指向注册中心

### 你要讲出的设计点

- 服务拆分后需要统一注册发现，否则路由维护成本很高
- Nacos 让网关和服务调用方不需要写死具体 IP/端口
- 对中小厂项目来说，注册中心最重要的价值是“解耦部署地址”

## 数据库和索引

### 当前项目里的亮点

- 笔记表按分片表设计：`note_0 ~ note_3`
- 查询类索引做过专项补充
- 任务、AI 作业、行为事件也有针对用户和时间维度的索引

### 重点文件

- `lifeos-backend/db/migrations/20260323_query_indexes.sql`

### 你要讲出的设计点

- 索引不是越多越好，要围绕真实查询条件设计
- 这个项目的索引围绕：
  - 用户维度查询
  - 时间倒序列表
  - 复习状态筛选
  - 任务状态筛选
  - AI 作业按类型/时间筛选
- 这类设计比死记概念更像真实工程经验

## 项目里最值得讲的 3 个亮点

优先顺序建议如下：

1. `JWT + Redis + Gateway` 统一鉴权
2. `RocketMQ` 驱动 AI 作业和行为事件异步链路
3. `Redis` 处理登录限流和任务列表缓存

如果面试官继续追问，再补：

- `Nacos` 注册发现
- `OpenFeign` 服务间调用
- 查询索引优化

## 你不该主动讲太深的点

- JVM 底层源码
- Spring 自动装配源码
- RocketMQ 消息存储机制
- 复杂分布式一致性理论

你现在要做的是把项目讲得“像自己做的”，而不是把所有底层原理背到研究生水平。
