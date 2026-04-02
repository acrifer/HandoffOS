# Day 08

## 今日目标

- 补 MyBatis-Plus 常用能力
- 让 CRUD、条件构造器和分页不再陌生

## 知识速记

- MyBatis-Plus 是对 MyBatis 的增强，目标是减少样板 CRUD 代码。
- `ServiceImpl` 提供了常用增删改查能力。
- `LambdaQueryWrapper` 的优势是字段名安全，避免硬编码字符串。
- 你当前项目里大量使用按用户、ID、状态条件查询，这正是 `LambdaQueryWrapper` 适合的场景。
- 面试时重点讲“为什么好维护”，不要只说“更方便”。

## 标准回答

### MyBatis-Plus 在项目里解决了什么问题

它减少了很多基础 CRUD 样板代码，让我把重点放在查询条件和业务逻辑上。像按用户 ID、任务状态、作业类型查询这些场景，用 `LambdaQueryWrapper` 写起来清晰而且字段名更安全。

### MyBatis-Plus 的边界是什么

它适合常规 CRUD 和中等复杂度查询，但极复杂 SQL、批量优化和特殊性能场景，仍然要回到原生 SQL 或 XML 精细控制。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 8
- [ ] 在项目中搜索 `LambdaQueryWrapper`
- [ ] 看 `UserServiceImpl`、`TaskServiceImpl`、`AiJobServiceImpl` 的查询条件写法
- [ ] 总结 MyBatis-Plus 在这个项目里的 3 个高频用法

## 加分项

- [ ] 补分页插件和批量操作风险

## 代码定位

- [ ] `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/service/impl/UserServiceImpl.java`
- [ ] `lifeos-backend/lifeos-task-service/src/main/java/com/lifeos/task/service/impl/TaskServiceImpl.java`

## 算法

- [ ] 二叉树层序遍历
- [ ] 翻转二叉树

## 今日交付物

- [ ] 1 页 MyBatis-Plus 常用写法清单

## 晚间验收

- [ ] 能解释 `LambdaQueryWrapper` 解决了什么问题
- [ ] 能说出常见 CRUD 和条件查询思路

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
