# LifeOS 2.0 - 快速启动指南

## 🚀 5 分钟启动

### 前置要求
- Docker Desktop（已安装并运行）
- Git

### 启动步骤

```bash
# 1. 进入项目目录
cd C:\Users\crifer\Documents\project\LifeOS

# 2. 创建环境变量文件
echo "AI_API_KEY=mock" > .env
echo "JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long" >> .env

# 3. 启动所有服务（一键启动）
docker compose up --build -d

# 4. 查看服务状态
docker compose ps
```

### 访问应用

- **前端界面**: http://localhost:5173
- **后端 API**: http://localhost:8080/api
- **API 文档**: http://localhost:8080/swagger-ui.html

### 测试账号

```
用户名: admin
密码: Pass123456
```

---

## 📦 服务说明

启动后会运行 4 个容器：

1. **PostgreSQL** (5432) - 数据库 + pgvector 向量检索
2. **Redis** (6379) - 缓存
3. **lifeos-app** (8080) - Spring Boot 后端
4. **lifeos-web** (5173) - Vue 3 前端

---

## 🎯 核心功能快速体验

### 1. RAG 智能问答
1. 登录后进入"笔记"页面
2. 创建几条笔记（例如：学习笔记、项目经验）
3. 进入"AI 对话"页面
4. 提问："我学过哪些技术？"

### 2. 知识图谱
1. 创建笔记后，系统自动提取实体和关系
2. 进入"知识图谱"页面
3. 查看可视化的知识网络

### 3. 智能洞察
1. 创建多条笔记（不同标签、不同时间）
2. 进入"智能洞察"页面
3. 查看学习模式分析和个性化推荐

---

## 🛠️ 常用命令

```bash
# 查看日志
docker compose logs -f lifeos-app

# 重启服务
docker compose restart

# 停止所有服务
docker compose down

# 完全清理（包括数据）
docker compose down -v

# 重新构建并启动
docker compose up --build -d
```

---

## 🔧 Mock 模式说明

默认使用 Mock 模式（无需 API Key）：
- RAG 问答：返回模拟回答
- 知识图谱：返回模拟实体/关系
- 智能洞察：返回模拟分析结果

**启用真实 AI 功能**：
1. 获取 DeepSeek API Key（https://platform.deepseek.com）
2. 修改 `.env` 文件：`AI_API_KEY=sk-your-real-key`
3. 重启服务：`docker compose restart lifeos-app`

---

## ❓ 常见问题

### 端口被占用
```bash
# 修改 docker-compose.yml 中的端口映射
# 例如：将 5173:5173 改为 3000:5173
```

### 服务启动失败
```bash
# 查看详细日志
docker compose logs lifeos-app

# 检查数据库连接
docker compose exec lifeos-app curl http://localhost:8080/actuator/health
```

### 数据库初始化失败
```bash
# 清理并重新启动
docker compose down -v
docker compose up --build -d
```

---

## 📖 更多文档

- **完整使用指南**: [USER_GUIDE.md](USER_GUIDE.md)
- **面试演示指南**: [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md)
- **技术总结**: [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)
- **项目报告**: [FINAL_REPORT.md](FINAL_REPORT.md)

---

## 🎓 面试演示

如果需要快速演示给面试官：

1. **30 秒电梯演讲**（见 INTERVIEW_GUIDE.md）
2. **核心功能演示**（5 分钟）
   - RAG 智能问答
   - 知识图谱可视化
   - 智能洞察分析
3. **技术亮点讲解**（5 分钟）
   - pgvector 向量检索
   - LLM 驱动的知识抽取
   - 多维度学习分析

---

**祝你面试顺利！** 🎉
