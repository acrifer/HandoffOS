# Day 11

## 今日目标

- 把 JWT 讲到面试能用
- 重点讲“为什么不能只靠 JWT 自身校验”

## 知识速记

- JWT 是一种无状态身份凭证，常见组成是头、载荷、签名。
- 优点是轻量、易传递、跨服务验证方便。
- 缺点是如果只靠 JWT 自身，很难优雅支持退出登录、强制失效和单端登录。
- 本项目用 `JwtUtil` 生成和解析 token，再把 token 放 Redis 里控制有效性。
- 面试时要突出“JWT 负责身份表达，Redis 负责状态控制”。

## 标准回答

### 为什么不能只靠 JWT 自身校验

因为纯 JWT 只要签名合法、没过期就会被认为有效，但服务端很难主动让它失效。比如用户退出登录、账号被封、想做单端登录，这些都不方便。所以这个项目在 JWT 外再加了一层 Redis 校验。

### 项目里的 JWT 方案怎么讲

用户登录成功后，用户服务生成 JWT 并写入 Redis。之后所有请求先到网关，网关先校验 JWT 的签名和过期时间，再去 Redis 比对当前用户的有效 token，二者都通过才放行。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 11
- [ ] 阅读 `lifeos-backend/lifeos-common/src/main/java/com/lifeos/common/utils/JwtUtil.java`
- [ ] 从 `03-project-scripts.md` 提炼 1 版 JWT 标准口述
- [ ] 口述 3 轮：`为什么本项目用 JWT + Redis`

## 加分项

- [ ] 思考多端登录和 token 刷新怎么做

## 代码定位

- [ ] 看 `JwtUtil.generateToken`
- [ ] 看 `AuthFilter` 里的解析和 Redis 校验

## 算法

- [ ] 二分查找
- [ ] 搜索旋转排序数组

## 今日交付物

- [ ] 1 份 JWT 结构和项目实现笔记

## 晚间验收

- [ ] 能说清 JWT 解决什么问题
- [ ] 能说清 Redis 又额外解决什么问题

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
