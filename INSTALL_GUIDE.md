# PostgreSQL 和 Redis 安装指南（Windows）

## 📦 安装 PostgreSQL 16

### 方法 1：官方安装包（推荐）

1. **下载安装包**
   - 访问：https://www.postgresql.org/download/windows/
   - 或直接下载：https://www.enterprisedb.com/downloads/postgres-postgresql-downloads
   - 选择 PostgreSQL 16.x for Windows x86-64

2. **运行安装程序**
   - 双击 `postgresql-16.x-windows-x64.exe`
   - 安装路径：默认 `C:\Program Files\PostgreSQL\16`
   - 端口：默认 5432
   - **设置密码**：输入 `postgres`（记住这个密码）
   - Locale：默认
   - 完成安装

3. **验证安装**
   ```bash
   # 打开新的命令行窗口
   psql --version
   # 应该显示：psql (PostgreSQL) 16.x
   ```

4. **创建数据库**
   ```bash
   # 连接到 PostgreSQL
   psql -U postgres
   # 输入密码：postgres

   # 创建数据库
   CREATE DATABASE lifeos;

   # 安装 pgvector 扩展
   \c lifeos
   CREATE EXTENSION vector;

   # 退出
   \q
   ```

---

## 📦 安装 Redis

### 方法 1：使用 Memurai（Redis for Windows）

1. **下载 Memurai**
   - 访问：https://www.memurai.com/get-memurai
   - 下载 Memurai Developer Edition（免费）

2. **安装**
   - 运行安装程序
   - 默认端口：6379
   - 安装为 Windows 服务

3. **验证安装**
   ```bash
   redis-cli --version
   # 或
   memurai-cli --version
   ```

### 方法 2：使用 WSL2 + Redis（推荐）

如果你有 WSL2：

```bash
# 在 WSL2 中安装 Redis
sudo apt update
sudo apt install redis-server

# 启动 Redis
sudo service redis-server start

# 验证
redis-cli ping
# 应该返回：PONG
```

### 方法 3：Docker 只跑 Redis（最简单）

```bash
# 只启动 Redis 容器
docker run -d --name lifeos-redis -p 6379:6379 redis:7-alpine

# 验证
docker ps | grep redis
```

---

## 🚀 启动服务

### 启动 PostgreSQL
```bash
# PostgreSQL 通常会自动启动
# 如果没有，手动启动：
net start postgresql-x64-16
```

### 启动 Redis

**如果用 Memurai**：
```bash
net start memurai
```

**如果用 WSL2**：
```bash
sudo service redis-server start
```

**如果用 Docker**：
```bash
docker start lifeos-redis
```

---

## ✅ 验证安装

运行以下命令确认一切正常：

```bash
# 测试 PostgreSQL
psql -U postgres -d lifeos -c "SELECT version();"

# 测试 Redis
redis-cli ping
# 或
memurai-cli ping
```

---

## 🎯 下一步

安装完成后，运行：

```bash
cd lifeos-monolith
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 🔧 快速方案（如果安装太麻烦）

### 选项 A：只用 Docker 跑数据库
```bash
# 启动 PostgreSQL 和 Redis
docker compose up -d postgres redis

# 等待 20 秒让服务启动
timeout 20

# 创建 pgvector 扩展
docker compose exec postgres psql -U postgres -d lifeos -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 本地运行应用
cd lifeos-monolith
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 选项 B：使用 H2 内存数据库（无需安装任何数据库）
见 `LOCAL_SETUP.md` 方案 2

---

## 📞 遇到问题？

1. **PostgreSQL 端口被占用**
   - 修改 `application-local.yml` 中的端口
   - 或停止占用 5432 端口的程序

2. **Redis 连接失败**
   - 检查服务是否启动：`net start memurai`
   - 或在 `application-local.yml` 中禁用 Redis

3. **pgvector 扩展安装失败**
   - 确保使用 PostgreSQL 16
   - 或使用 Docker 版本（自带 pgvector）
