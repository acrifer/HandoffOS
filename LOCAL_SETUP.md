# LifeOS 本地开发指南（无 Docker）

## 🎯 快速启动（5 分钟）

### 前置要求
- JDK 17
- Maven 3.6+
- PostgreSQL 16（本地安装）
- Redis（本地安装或使用 H2 内存数据库）
- Node.js 20+

---

## 📦 方案 1：完全本地运行（推荐）

### Step 1: 安装 PostgreSQL
```bash
# 下载 PostgreSQL 16
# https://www.postgresql.org/download/windows/

# 或使用 Chocolatey
choco install postgresql16

# 启动 PostgreSQL 服务
net start postgresql-x64-16
```

### Step 2: 创建数据库
```bash
# 使用 psql 连接
psql -U postgres

# 创建数据库
CREATE DATABASE lifeos;

# 安装 pgvector 扩展
CREATE EXTENSION vector;

# 退出
\q
```

### Step 3: 配置应用
创建 `lifeos-monolith/src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lifeos
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  
  data:
    redis:
      host: localhost
      port: 6379
      # 如果没有 Redis，注释掉这部分

server:
  port: 8080

# JWT 配置
jwt:
  secret: your-secret-key-must-be-at-least-32-characters-long
  expiration: 86400000

# AI 配置（Mock 模式）
ai:
  base-url: https://api.deepseek.com
  api-key: mock
  model: deepseek-chat
```

### Step 4: 启动后端
```bash
cd lifeos-monolith

# 使用 Maven 启动
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 或者先编译再运行
mvn clean package -DskipTests
java -jar target/lifeos-monolith-2.0.0-SNAPSHOT.jar --spring.profiles.active=local
```

### Step 5: 启动前端
```bash
cd lifeos-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

### Step 6: 访问应用
- 前端: http://localhost:5173
- 后端: http://localhost:8080
- API 文档: http://localhost:8080/swagger-ui.html

---

## 📦 方案 2：使用 H2 内存数据库（最简单）

如果不想安装 PostgreSQL，可以临时使用 H2：

### 修改 pom.xml
在 `lifeos-monolith/pom.xml` 添加：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 修改 application-local.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:lifeos
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

# 注意：H2 不支持 pgvector，RAG 功能会降级为 Mock 模式
```

---

## 📦 方案 3：只用 Docker 运行数据库

如果 Docker 只是拉取镜像慢，可以只用它运行数据库：

```bash
# 只启动数据库容器
docker compose up -d postgres redis

# 本地运行应用
cd lifeos-monolith
mvn spring-boot:run
```

---

## 🛠️ Maven 配置（使用阿里云镜像）

创建 `C:\Users\crifer\.m2\settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

---

## 🔧 常见问题

### Q1: PostgreSQL 连接失败
```bash
# 检查服务是否启动
net start postgresql-x64-16

# 检查端口
netstat -ano | findstr :5432
```

### Q2: Maven 依赖下载慢
```bash
# 使用阿里云镜像（见上面的 settings.xml）
# 或者使用 IDE 的 Maven 设置
```

### Q3: 没有 Redis 怎么办
```yaml
# 在 application-local.yml 中禁用 Redis
spring:
  data:
    redis:
      enabled: false
```

---

## 🎯 推荐方案

**如果你有 1 小时**：
- 安装 PostgreSQL + Redis（本地）
- 使用方案 1

**如果你只有 10 分钟**：
- 使用方案 2（H2 内存数据库）
- 功能会降级，但能快速跑起来

**如果 Docker 只是慢**：
- 使用方案 3（Docker 只跑数据库）
- 应用本地运行，开发体验最好

---

## 📝 下一步

启动成功后，参考 `QUICKSTART.md` 进行功能测试。
