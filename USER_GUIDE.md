# LifeOS 2.0 - 完整使用指南

## 🚀 快速开始

### 环境要求
- Docker Desktop
- Git

### 一键启动
```bash
# 1. 克隆项目
git clone <repository-url>
cd LifeOS

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，设置以下变量：
# - AI_API_KEY=your_deepseek_api_key  # 可选，不设置则使用 Mock 模式
# - JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long

# 3. 启动所有服务
docker compose up --build -d

# 4. 等待服务启动（约 15 秒）
docker compose ps

# 5. 访问应用
# 前端: http://localhost:5173
# API: http://localhost:8080/api
# Swagger: http://localhost:8080/swagger-ui.html
```

### 测试账号
```
用户名: admin
密码: Pass123456
```

---

## 📚 核心功能使用

### 1. RAG 智能问答

#### 创建笔记
```bash
1. 访问 http://localhost:5173
2. 登录后进入"笔记"页面
3. 点击"新建笔记"
4. 输入标题和内容
5. 保存（后台自动向量化）
```

#### 智能问答
```bash
1. 进入"AI 聊天"页面 (/ai/chat)
2. 输入问题，例如：
   - "我写过哪些关于 Redis 的笔记？"
   - "如何解决缓存穿透问题？"
   - "总结一下我学习 Spring Boot 的笔记"
3. 查看答案和来源笔记
4. 点击来源笔记可跳转查看详情
```

**特点**：
- ✅ 语义搜索（不是关键词匹配）
- ✅ 来源引用追踪
- ✅ 相关度评分
- ✅ 支持 Mock 模式（无需 API Key）

---

### 2. 知识图谱

#### 构建知识图谱
```bash
1. 进入"Skill"页面
2. 创建新的 Skill
3. 添加飞书文档或群聊内容
4. 点击"蒸馏"按钮
5. 等待知识图谱构建完成
```

#### 查看知识图谱
```bash
1. 进入"知识图谱"页面 (/skill/graph)
2. 查看实体节点（人员、项目、流程、概念）
3. 查看关系边（负责、依赖、前置、相关）
4. 点击节点查看详情
5. 查看冲突检测结果
```

**特点**：
- ✅ 自动提取实体和关系
- ✅ 冲突检测
- ✅ 置信度评分
- ✅ SVG 可视化

---

### 3. 智能洞察

#### 查看学习洞察
```bash
1. 进入"学习洞察"页面 (/dashboard/insights)
2. 选择"本周"或"本月"
3. 查看整体评分
4. 查看主题分布
5. 查看学习模式分析
6. 查看个性化建议
```

**分析维度**：
- **时间分布**：早晨/下午/晚上/深夜
- **学习深度**：深度笔记 vs 碎片笔记
- **学习一致性**：学习频率和规律性

**推荐策略**：
- 主题拓展建议
- 学习时间优化
- 学习频率提醒
- 深度学习鼓励

---

## 🎯 API 文档

### Swagger UI
访问：http://localhost:8080/swagger-ui.html

### 核心接口

#### 用户认证
```bash
# 注册
POST /api/auth/register
{
  "username": "testuser",
  "password": "Pass123456",
  "email": "test@example.com"
}

# 登录
POST /api/auth/login
{
  "username": "testuser",
  "password": "Pass123456"
}

# 返回
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "testuser"
  }
}
```

#### 笔记管理
```bash
# 创建笔记（自动向量化）
POST /api/notes
Authorization: Bearer <token>
{
  "title": "Redis 缓存穿透解决方案",
  "content": "使用布隆过滤器可以有效防止缓存穿透...",
  "tags": "Redis,缓存"
}

# 列表
GET /api/notes?page=0&size=20
Authorization: Bearer <token>

# 搜索
GET /api/notes/search?keyword=Redis&page=0&size=20
Authorization: Bearer <token>
```

#### RAG 智能问答
```bash
# 智能问答
POST /api/ai/rag/query
Authorization: Bearer <token>
{
  "query": "如何解决 Redis 缓存穿透？",
  "topK": 5
}

# 返回
{
  "code": 200,
  "data": {
    "answer": "根据您的笔记，解决 Redis 缓存穿透的方法是使用布隆过滤器...",
    "sources": [
      {
        "noteId": 123,
        "title": "Redis 缓存穿透解决方案",
        "excerpt": "使用布隆过滤器...",
        "relevanceScore": 0.92
      }
    ],
    "retrievedCount": 5,
    "responseTimeMs": 1234,
    "model": "deepseek-chat"
  }
}

# 统计信息
GET /api/ai/rag/stats
Authorization: Bearer <token>
```

#### 知识图谱
```bash
# 构建知识图谱
POST /api/ai/knowledge/build/{skillId}
Authorization: Bearer <token>
[
  "张三负责支付模块的开发",
  "支付模块依赖于用户认证模块"
]

# 获取知识图谱
GET /api/ai/knowledge/graph/{skillId}
Authorization: Bearer <token>

# 冲突检测
POST /api/ai/knowledge/conflicts/detect
Authorization: Bearer <token>
[
  {
    "sourceType": "DOCUMENT",
    "content": "流程有3步..."
  },
  {
    "sourceType": "CHAT",
    "content": "流程有4步..."
  }
]
```

#### 智能洞察
```bash
# 周报告
GET /api/ai/insight/weekly
Authorization: Bearer <token>

# 月报告
GET /api/ai/insight/monthly
Authorization: Bearer <token>

# 返回
{
  "code": 200,
  "data": {
    "period": "本周",
    "statistics": {
      "totalNotes": 15,
      "totalWords": 8500,
      "avgWordsPerNote": 566
    },
    "topicClusters": [...],
    "patterns": [...],
    "recommendations": [...],
    "overallScore": 0.82,
    "summary": "本周您共记录了 15 篇笔记..."
  }
}
```

---

## 🛠️ 开发指南

### 项目结构
```
LifeOS/
├── lifeos-monolith/              # 后端单体应用
│   ├── src/main/java/com/lifeos/
│   │   ├── LifeOsApplication.java
│   │   ├── config/               # 配置类
│   │   ├── common/               # 公共类
│   │   ├── user/                 # 用户模块
│   │   ├── note/                 # 笔记模块
│   │   └── ai/                   # AI 模块
│   │       ├── embedding/        # 向量化
│   │       ├── rag/              # RAG 查询
│   │       ├── knowledge/        # 知识图谱
│   │       └── insight/          # 智能洞察
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
│           └── V1__init_schema.sql
├── lifeos-web/                   # 前端应用
│   └── src/
│       ├── views/
│       │   ├── ai/
│       │   │   └── chat.vue      # RAG 对话
│       │   ├── skill/
│       │   │   └── graph.vue     # 知识图谱
│       │   └── dashboard/
│       │       └── insights.vue  # 学习洞察
│       └── router/index.js
├── docker-compose.yml            # 容器编排
└── README_V2.md                  # 项目文档
```

### 本地开发

#### 后端开发
```bash
cd lifeos-monolith

# 编译
mvn clean package -DskipTests

# 运行
java -jar target/lifeos-monolith-2.0.0-SNAPSHOT.jar

# 或使用 IDE 直接运行 LifeOsApplication
```

#### 前端开发
```bash
cd lifeos-web

# 安装依赖
npm install

# 开发模式
npm run dev

# 构建
npm run build
```

### 环境变量说明
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=5432
DB_NAME=lifeos
DB_USER=postgres
DB_PASSWORD=postgres

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT 配置
JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long

# AI 配置
AI_BASE_URL=https://api.deepseek.com
AI_API_KEY=your_api_key  # 可选，不设置则使用 Mock 模式
AI_MODEL=deepseek-chat
AI_EMBEDDING_MODEL=text-embedding-3-small

# 飞书配置（可选）
FEISHU_APP_ID=
FEISHU_APP_SECRET=
```

---

## 🐛 故障排查

### 常见问题

#### 1. 容器启动失败
```bash
# 检查容器状态
docker compose ps

# 查看日志
docker compose logs lifeos-app

# 重启服务
docker compose restart lifeos-app
```

#### 2. 数据库连接失败
```bash
# 检查 PostgreSQL 是否启动
docker compose ps postgres

# 查看数据库日志
docker compose logs postgres

# 进入数据库容器
docker compose exec postgres psql -U postgres -d lifeos
```

#### 3. RAG 查询无结果
```bash
# 检查向量化覆盖率
GET /api/ai/rag/stats

# 如果覆盖率为 0，说明笔记还未向量化
# 解决方案：等待几秒钟，向量化是异步的
```

#### 4. AI 功能返回 Mock 数据
```bash
# 原因：未配置 AI_API_KEY
# 解决方案：
# 1. 编辑 .env 文件
# 2. 设置 AI_API_KEY=your_deepseek_api_key
# 3. 重启服务：docker compose restart lifeos-app
```

---

## 📊 性能优化

### 数据库优化
```sql
-- 创建向量索引（加速检索）
CREATE INDEX idx_note_embedding_vector 
ON note_embedding 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);

-- 分析表统计信息
ANALYZE note_embedding;
```

### 应用优化
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 连接池大小
      minimum-idle: 5
      
  data:
    redis:
      lettuce:
        pool:
          max-active: 8  # Redis 连接池
```

---

## 🔒 安全建议

### 生产环境配置
```bash
# 1. 修改默认密码
# 2. 使用强 JWT Secret（至少 32 字符）
# 3. 启用 HTTPS
# 4. 配置防火墙规则
# 5. 定期备份数据库
```

### 数据备份
```bash
# 备份 PostgreSQL
docker compose exec postgres pg_dump -U postgres lifeos > backup.sql

# 恢复
docker compose exec -T postgres psql -U postgres lifeos < backup.sql
```

---

## 📞 支持与反馈

### 问题反馈
- GitHub Issues: <repository-url>/issues
- Email: your-email@example.com

### 文档更新
- 项目文档：README_V2.md
- API 文档：http://localhost:8080/swagger-ui.html
- 使用指南：本文档

---

## 📄 License

仅用于学习、演示和个人项目展示。
