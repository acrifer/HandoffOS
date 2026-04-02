# Day 16

## 今日目标

- 讲清 Nacos 的注册发现作用
- 知道项目里它具体落在哪里

## 知识速记

- 微服务拆分后，服务地址会变化，所以需要注册中心。
- Nacos 的基础价值是让服务调用方不必写死 IP 和端口。
- 当前项目里各服务通过 `@EnableDiscoveryClient` 注册到 Nacos。
- 网关通过 `lb://service-name` 配合 Nacos 做负载均衡和转发。
- 对你现在的面试目标，重点讲注册发现价值，不必深挖源码。

## 标准回答

### Nacos 在项目里起什么作用

Nacos 在这个项目里主要承担服务注册发现。各个服务启动后把自己注册上去，网关和调用方就可以按服务名调用，而不需要写死具体地址。这样服务扩容、重启或地址变化时，系统维护成本会低很多。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 16
- [ ] 阅读 `lifeos-backend/lifeos-user-service/src/main/resources/application.yml`
- [ ] 阅读 `lifeos-backend/lifeos-note-service/src/main/resources/application.properties`
- [ ] 阅读 `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/UserApplication.java`
- [ ] 阅读 `lifeos-backend/lifeos-note-service/src/main/java/com/lifeos/note/NoteApplication.java`

## 加分项

- [ ] 总结配置中心和注册中心的区别

## 代码定位

- [ ] 找出 `@EnableDiscoveryClient`
- [ ] 找出 `spring.cloud.nacos.discovery.server-addr`

## 算法

- [ ] 反转链表二刷
- [ ] 二叉树层序遍历二刷

## 今日交付物

- [ ] 1 段 1 分钟 Nacos 标准回答

## 晚间验收

- [ ] 能讲清为什么服务注册发现重要
- [ ] 能讲清 `lb://service-name` 为什么能工作

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
