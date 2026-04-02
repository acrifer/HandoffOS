# 4 周执行路线

## 总原则

- 你不是在准备“大而全”的 Java 后端体系，而是在准备“1 个月内可投递”的校招后端面试包。
- 学习顺序必须服从面试命中率：`Spring Boot + MySQL + Redis + 项目表达 + 高频算法` 优先级最高。
- 微服务部分只学到“能解释设计与取舍”，不做源码党。

## 名词预热

- `IOC`：控制反转，指对象创建和依赖管理交给 Spring 容器。
- `AOP`：面向切面编程，指把日志、事务、鉴权这类横切逻辑从业务代码抽离。
- `MVCC`：多版本并发控制，用于提升数据库并发读性能。
- `JWT`：JSON Web Token，一种无状态身份凭证。
- `Gateway`：网关，统一入口层，负责路由和鉴权等横切逻辑。
- `Nacos`：注册中心和配置中心组件，用于服务发现。
- `Feign`：声明式 HTTP 调用工具，用于服务间调用。
- `MQ`：消息队列，用于异步、解耦、削峰。
- `CRUD`：增删改查。

## 第 1 周：后端基本盘补齐

本周目标：

- 能讲清一次请求从前端进入后端的流程
- 能口述 MySQL 索引、事务、线程池、HashMap
- 能自己写简单的登录、查询、更新接口

### Day 1

- Java 集合总览：`List / Set / Map` 差异
- 重点：`ArrayList`、`LinkedList`、`HashMap`
- 口述题：
  - `ArrayList` 为什么查询快、插入慢
  - `HashMap` 为什么线程不安全

### Day 2

- 并发基础：线程、线程池、`synchronized`、`volatile`
- 重点：为什么线程池不能乱用默认参数
- 口述题：
  - 线程池核心参数含义
  - `synchronized` 和 `volatile` 区别

### Day 3

- MySQL 索引、`B+ 树`（MySQL 常见索引结构）、`回表`（先查索引再回主键索引取整行）、最左前缀
- 结合项目看索引设计
- 重点文件：
  - `lifeos-backend/db/migrations/20260323_query_indexes.sql`

### Day 4

- 事务、隔离级别、`MVCC`（多版本并发控制）、锁
- 口述题：
  - 为什么会有脏读、不可重复读、幻读
  - 为什么 RR 比 RC 更强但成本更高

### Day 5

- Spring Boot 基础：`IOC`、`AOP`、自动装配
- `Controller -> Service -> Mapper` 链路
- 重点文件：
  - `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/controller/UserController.java`
  - `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/service/impl/UserServiceImpl.java`

### Day 6

- 统一返回、参数校验、异常处理
- 这个项目当前写法是 Controller 内部 `try/catch + Result<T>`，先讲清这种实现，再补“如果继续优化会怎么做”
- 重点文件：
  - `lifeos-backend/lifeos-common/src/main/java/com/lifeos/common/response/Result.java`

### Day 7

- 小复盘
- 验收：
  - 手写一个简单登录接口
  - 手写一个根据用户 ID 查询数据接口
  - 口述 `HashMap`、线程池、索引、事务

## 第 2 周：企业常用能力

本周目标：

- 能讲清 Redis、JWT、网关、MyBatis-Plus
- 能基于项目讲出 3 个真实工程问题

### Day 8

- MyBatis-Plus 常用 `CRUD`、条件构造器、分页
- 看项目里 `LambdaQueryWrapper` 的真实使用方式

### Day 9

- Redis 基础类型和常见使用场景
- 登录限流、登录态缓存、列表缓存

### Day 10

- 看项目里的 Redis 落地：
  - 登录限流：每用户名每分钟最多 5 次
  - 单点登录态：token 写入 Redis，网关二次校验
  - 任务列表缓存：查库后回填 Redis，写操作时清缓存
- 重点文件：
  - `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/service/impl/UserServiceImpl.java`
  - `lifeos-backend/lifeos-gateway/src/main/java/com/lifeos/gateway/filter/AuthFilter.java`
  - `lifeos-backend/lifeos-task-service/src/main/java/com/lifeos/task/service/impl/TaskServiceImpl.java`

### Day 11

- `JWT`：结构、签名、过期、为什么不能只靠 JWT 自身校验
- 重点：
  - 本项目不是“纯 JWT 无状态”，而是“JWT + Redis 登录态校验”
  - 这能支持退出登录、单端登录、强制失效

### Day 12

- Spring Cloud `Gateway` 和统一鉴权
- 路由转发、白名单、请求头透传
- 重点文件：
  - `lifeos-backend/lifeos-gateway/src/main/resources/application.yml`
  - `lifeos-backend/lifeos-gateway/src/main/java/com/lifeos/gateway/filter/AuthFilter.java`

### Day 13

- Docker / Compose / 环境变量注入
- 项目级别的容器启动方式和排障口径
- 重点文件：
  - `docker-compose.full.yml`
  - `BACKEND_STARTUP.md`

### Day 14

- 小复盘
- 验收：
  - 讲清 Redis 三大问题和解决思路
  - 讲清 JWT + Redis 的设计理由
  - 讲清 Gateway 统一鉴权的价值

## 第 3 周：微服务亮点和项目故事

本周目标：

- 能完整讲一遍 LifeOS 的系统架构
- 能围绕微服务、MQ、Nacos 回答追问

### Day 15

- 系统架构图口述
- 各服务职责：
  - 用户
  - 笔记
  - 任务
  - AI
  - 行为分析
  - 网关

### Day 16

- `Nacos`：服务注册发现、配置中心
- 重点文件：
  - `lifeos-backend/lifeos-user-service/src/main/resources/application.yml`
  - `lifeos-backend/lifeos-note-service/src/main/resources/application.properties`
  - 各服务 `@EnableDiscoveryClient`

### Day 17

- `OpenFeign`：服务间调用
- 本项目里重点是笔记服务调用行为服务、AI 服务回调笔记服务
- 重点文件：
  - `lifeos-backend/lifeos-note-service/src/main/java/com/lifeos/note/NoteApplication.java`
  - `lifeos-backend/lifeos-api/pom.xml`

### Day 18

- `RocketMQ`：异步削峰、服务解耦、最终一致性
- 主链路一：笔记提交 AI 作业 -> 写库 -> 发 MQ -> AI 服务消费 -> 回调状态
- 主链路二：任务完成/笔记整理 -> 发行为事件 -> 行为服务消费 -> 聚合 Dashboard

### Day 19

- 看 AI 异步链路真实代码
- 重点文件：
  - `lifeos-backend/lifeos-note-service/src/main/java/com/lifeos/note/service/impl/AiJobServiceImpl.java`
  - `lifeos-backend/lifeos-ai-service/src/main/java/com/lifeos/ai/messaging/AiJobConsumer.java`

### Day 20

- 看行为异步链路和幂等处理
- 重点文件：
  - `lifeos-backend/lifeos-task-service/src/main/java/com/lifeos/task/service/impl/TaskServiceImpl.java`
  - `lifeos-backend/lifeos-behavior-service/src/main/java/com/lifeos/behavior/messaging/BehaviorEventConsumer.java`
  - `lifeos-backend/lifeos-behavior-service/src/main/java/com/lifeos/behavior/service/impl/BehaviorServiceImpl.java`

### Day 21

- 小复盘
- 验收：
  - 无稿讲完 3-5 分钟项目介绍
  - 能回答：
    - 为什么拆微服务
    - 为什么引入 MQ
    - Redis 和数据库一致性怎么做
    - Gateway 和业务服务分别负责什么

## 第 4 周：面试冲刺

本周目标：

- 算法刷完一轮高频题
- 项目讲稿成型
- 简历项目描述和口头表达一致

### Day 22-24

- 每天 5-6 道高频算法
- 每天 1 小时八股口述
- 每天 1 次 90 秒自我介绍演练

### Day 25-26

- 模拟 30 分钟完整面试
- 补不会讲、讲不顺、讲太虚的地方

### Day 27

- 精修简历项目经历
- 每个亮点按 `问题 -> 方案 -> 结果` 收口

### Day 28

- 完整复盘
- 最终验收：
  - 项目介绍 3 分钟版
  - 技术深挖 10 分钟版
  - 算法高频题二刷
  - 简历、项目、口述三者统一

## 时间不够时的删减顺序

如果你每天只能投入 3 小时，按这个顺序删减：

1. RocketMQ / Nacos 深挖
2. JVM 深挖
3. Spring 源码和自动装配细节
4. 保留 `Spring Boot + MySQL + Redis + 项目表达 + 高频算法`
