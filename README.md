# LifeOS

> 一个面向个人使用场景的知识管理系统。  
> 把笔记、AI 整理、任务转化、复习队列和周复盘串成一条完整闭环。

## 项目简介

LifeOS 不是一个通用聊天工具，而是一个围绕个人知识沉淀构建的系统。  
项目的核心目标是把这条链路做通：

`快速记录 -> 组织沉淀 -> 搜索找回 -> AI 整理 -> 复习复用 -> 转化为行动项`

它适合作为：
- 校招 / 应届生项目展示
- 个人作品集项目
- 微服务、异步任务流、知识管理方向的练手项目

## 功能亮点

- 用户注册、登录、JWT 鉴权、用户资料维护
- 知识笔记创建、编辑、搜索、置顶、复习状态管理
- AI 摘要、AI 整理、AI 提取任务、AI 周复盘
- 从笔记中提取任务，并在任务完成后回写行为统计
- Dashboard 展示待复习笔记、AI inbox、知识任务和高频标签
- RocketMQ 支撑行为事件和 AI 作业异步任务流
- 一键启动环境与后端服务
- Swagger / OpenAPI 文档
- 可重复重置的测试数据

## 项目预览

当前项目已经具备完整的演示链路，推荐展示顺序：

1. 登录系统
2. 创建或编辑一篇笔记
3. 触发 AI 摘要或整理
4. 查看 AI 作业历史
5. 从笔记提取任务
6. 在任务页完成任务
7. 回到 Dashboard 查看统计变化
8. 触发周复盘查看本周总结

## 技术栈

**前端**
- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- 独立 `admin-web` 管理后台

**后端**
- Java 17
- Spring Boot 3.2
- Spring Cloud Gateway
- Spring Cloud Alibaba Nacos
- MyBatis-Plus
- ShardingSphere
- Redis
- RocketMQ
- MySQL

**工程化**
- Docker Compose + PowerShell 辅助脚本
- `.env` 统一配置
- Swagger / OpenAPI
- Maven 多模块工程

## 系统架构

```text
lifeos-web
admin-web
  -> /api/*
  -> /admin-api/*
  -> lifeos-gateway
     -> lifeos-user-service
     -> lifeos-note-service
     -> lifeos-task-service
     -> lifeos-ai-service
     -> lifeos-behavior-service
     -> lifeos-admin-service

MySQL
Redis
Nacos
RocketMQ
```

各模块职责：
- `lifeos-gateway`：统一入口、JWT 校验、登录态校验、服务转发
- `lifeos-user-service`：注册、登录、用户资料、密码管理
- `lifeos-note-service`：笔记、搜索、复习、AI 作业调度
- `lifeos-task-service`：任务创建、更新、完成、来源笔记关联
- `lifeos-ai-service`：摘要、整理、提取任务、周复盘处理
- `lifeos-behavior-service`：行为埋点、Dashboard 聚合统计
- `lifeos-common`：统一响应、JWT 工具等公共能力
- `lifeos-api`：服务间共享 DTO、Feign 接口、消息常量

## 目录结构

```text
LifeOS
├─ lifeos-web/                       前端工程
├─ admin-web/                        管理后台前端
├─ lifeos-backend/                   后端多模块工程
│  ├─ lifeos-common/                 公共工具
│  ├─ lifeos-api/                    DTO / Feign / MQ 常量
│  ├─ lifeos-gateway/                网关
│  ├─ lifeos-admin-service/          管理后台后端
│  ├─ lifeos-user-service/           用户服务
│  ├─ lifeos-note-service/           笔记服务
│  ├─ lifeos-task-service/           任务服务
│  ├─ lifeos-ai-service/             AI 服务
│  ├─ lifeos-behavior-service/       行为服务
│  └─ db/                            SQL、迁移、测试种子数据
├─ docker-compose.full.yml           全量容器编排
├─ docker-start-all.ps1              一键启动 Docker Compose
├─ docker-stop-all.ps1               停止 Docker Compose
├─ docker-status-all.ps1             查看 Docker Compose 状态
├─ reset-test-data.ps1               重置测试数据
└─ .env.example                      本地环境变量模板
```

## 快速开始

### 方式一：全 Docker 启动

这是当前最推荐的启动方式。  
它会同时拉起：

- `MySQL`
- `Redis`
- `Nacos`
- `RocketMQ`
- `lifeos-user-service`
- `lifeos-task-service`
- `lifeos-note-service`
- `lifeos-ai-service`
- `lifeos-behavior-service`
- `lifeos-gateway`
- `lifeos-web`
- `lifeos-admin-service`
- `lifeos-admin-web`

先准备配置：

```powershell
Copy-Item .env.example .env
```

至少确认这些变量：
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `LIFEOS_JWT_SECRET`
- `LIFEOS_AI_API_KEY`
- `MYSQL_PORT`，默认是 `13306`，这样可以避开本机已占用的 `3306`

启动整套容器：

```powershell
powershell -ExecutionPolicy Bypass -File .\docker-start-all.ps1
```

这个脚本会先顺序拉取基础镜像，再串行构建业务镜像，最后启动容器，适合网络不稳定或 Docker Hub 偶发超时的场景。

或者直接使用 Compose：

```powershell
docker compose -f .\docker-compose.full.yml up --build -d
```

查看状态：

```powershell
powershell -ExecutionPolicy Bypass -File .\docker-status-all.ps1
```

停止整套容器：

```powershell
powershell -ExecutionPolicy Bypass -File .\docker-stop-all.ps1
```

访问地址：
- 前端：`http://127.0.0.1:5173`
- 管理后台：`http://127.0.0.1:5174`
- 网关：`http://127.0.0.1:8080`
- Swagger：`http://127.0.0.1:8080/swagger-ui.html`
- Nacos：`http://127.0.0.1:8848/nacos`

Docker 模式的说明：
- 容器内服务会自动改用 `mysql / redis / nacos / rocketmq-namesrv` 这些容器名通信，不依赖你本机的 `127.0.0.1`
- MySQL 首次启动时会自动导入结构和测试数据
- 默认会生成 5 个测试用户，统一密码仍然是 `Pass123456`
- 如果拉镜像时报 `auth.docker.io/token` 相关 EOF，通常是 Docker Hub 临时网络问题，重新执行 `docker-start-all.ps1` 即可
- 如果要彻底清空 Docker 数据卷：

```powershell
docker compose -f .\docker-compose.full.yml down -v
```

### 运行要求

- Docker Desktop
- Node.js 20 或更高版本
- JDK 17
- Maven 3.9 或更高版本

更多启动说明见 [BACKEND_STARTUP.md](./BACKEND_STARTUP.md)。

## 接口文档

统一 Swagger UI 入口：

```text
http://127.0.0.1:8080/swagger-ui.html
```

聚合后的 OpenAPI JSON：
- `http://127.0.0.1:8080/service-docs/user`
- `http://127.0.0.1:8080/service-docs/note`
- `http://127.0.0.1:8080/service-docs/task`
- `http://127.0.0.1:8080/service-docs/ai`
- `http://127.0.0.1:8080/service-docs/behavior`
- `http://127.0.0.1:8080/service-docs/admin`

## 管理后台

当前版本新增独立后台站点和独立后台服务：

- 站点：`http://127.0.0.1:5174`
- API：`/admin-api/**`
- 服务：`lifeos-admin-service`

默认后台管理员账号由 `.env` 控制：

- `LIFEOS_ADMIN_DEFAULT_USERNAME`
- `LIFEOS_ADMIN_DEFAULT_PASSWORD`
- `LIFEOS_ADMIN_DEFAULT_DISPLAY_NAME`
- `LIFEOS_ADMIN_DEFAULT_EMAIL`

第一版后台覆盖：

- 用户全局查询、启用/禁用、重置密码
- 笔记全局查询、删除、强制改复习状态
- 任务全局查询、改状态、删除
- AI 作业查询、失败重试、取消
- 行为明细查询
- 应用内运维页面：服务状态、关键配置只读、入口链接、测试数据重置、日志命令

## 测试数据

项目支持一键清空并重建测试数据：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\reset-test-data.ps1
```

测试账号统一密码：

```text
Pass123456
```

默认会生成 5 个带真实使用习惯的测试用户：
- `liwen_pm`：产品经理，偏发布、访谈、首页设计
- `zhouyi_dev`：后端开发，偏异步链路、Redis、接口联调
- `heqing_fit`：健身用户，偏训练、饮食、睡眠恢复
- `susu_creator`：内容创作者，偏选题、脚本、品牌合作
- `chenyu_grad`：研究生，偏论文、实验设计、导师反馈

## 核心业务能力

### 用户系统
- 用户注册
- 用户登录
- JWT 登录态
- 用户资料修改
- 密码修改
- 退出登录

### 知识笔记
- 新建笔记
- 编辑笔记
- 删除笔记
- 关键词搜索
- 标签筛选
- 置顶
- 复习状态管理
- 复习队列视图

### AI 工作流
- 异步摘要生成
- 异步整理建议
- 异步任务提取
- 异步周复盘
- AI 作业历史查看

### 任务系统
- 普通任务
- 来源于笔记的任务
- 任务完成
- 与笔记双向关联展示

### Dashboard
- 待复习笔记数
- 本周新增笔记数
- 本周整理完成数
- 待处理 AI 作业
- 待执行知识任务
- 最近更新笔记
- 高频标签

## 项目亮点

- 不是简单 CRUD，而是有明确业务主线的知识管理系统
- 微服务拆分清晰，职责边界明确
- 使用 Gateway + JWT + Redis 完成统一鉴权
- 使用 RocketMQ 实现行为埋点和 AI 作业异步化
- 使用 ShardingSphere 对笔记表做分表
- 支持一键环境启动、测试种子数据和 Swagger 文档

## 当前状态

当前版本已经适合作为校招 / 应届生项目展示。  
它已经具备完整闭环，但仍然定位为“可运行、可演示、可扩展”的项目，而不是面向生产环境的最终版本。

仍可继续改进的方向包括：
- 更严格的安全策略
- 更完整的监控、重试和死信队列
- 更多自动化测试
- 更细粒度的 UTF-8 清理

## License

仅用于学习、演示和个人项目展示。
