# Day 05

## 今日目标

- 把 Spring Boot 基础请求链路讲清楚
- 看懂 Controller -> Service -> Mapper 的分层

## 知识速记

- `Controller` 负责接收请求、参数绑定和返回响应。
- `Service` 负责业务逻辑编排。
- `Mapper` 负责数据库访问。
- IOC 的价值是把对象创建和依赖管理交给容器，降低耦合。
- AOP 的价值是把日志、事务、鉴权这类横切逻辑抽出去。

## 标准回答

### 一次 HTTP 请求如何到达 Controller

请求先到 Web 容器，再交给 Spring MVC 的 `DispatcherServlet`。它会根据请求路径找到对应的 Controller 方法，完成参数绑定后执行方法逻辑。最后把返回对象序列化成 JSON 响应给前端。

### 为什么后端要分层

分层的核心是职责单一。Controller 不该写太多业务逻辑，Service 不该直接关心 HTTP 细节，Mapper 不该承担业务编排。这样可维护性和测试性都更好。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 5
- [ ] 阅读 `interview-prep/04-high-frequency-qa.md` 中 Spring 18-21 题
- [ ] 阅读 `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/controller/UserController.java`
- [ ] 阅读 `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/service/impl/UserServiceImpl.java`

## 加分项

- [ ] 自己画出一次登录请求从 Controller 到 Redis 的链路

## 代码定位

- [ ] 找出登录接口、获取用户信息接口、退出登录接口

## 算法

- [ ] 盛最多水的容器
- [ ] 三数之和

## 今日交付物

- [ ] 1 张“请求到达 Controller”的流程图
- [ ] 1 份用户服务分层说明

## 晚间验收

- [ ] 能讲清 IOC 的作用
- [ ] 能讲清一次 HTTP 请求如何到达 Controller

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
