# Day 06

## 今日目标

- 讲清统一返回、异常处理和当前项目的写法
- 知道“现在怎么做”和“之后怎么优化”

## 知识速记

- 当前项目统一返回结构是 `Result<T>`，包含 `code`、`message`、`data`。
- 当前异常处理主要是 Controller 内部 `try/catch` 后返回错误结果。
- 这种方式的优点是直观，容易上手。
- 问题是重复代码多，异常处理不集中，维护成本更高。
- 更常见的企业写法是 `@RestControllerAdvice + 统一异常枚举/业务异常`。

## 标准回答

### 统一返回结构有什么价值

统一返回可以让前后端约定稳定，成功和失败响应格式一致，前端更容易做统一处理。同时日志排查和接口文档也更规范。

### 当前项目的异常处理还有什么优化空间

目前是各个 Controller 自己 `try/catch`，可读性还行，但重复度比较高。后续可以把业务异常统一抛出，在全局异常处理器里集中转换成标准响应，这样更利于维护和扩展。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 6
- [ ] 阅读 `lifeos-backend/lifeos-common/src/main/java/com/lifeos/common/response/Result.java`
- [ ] 观察 `UserController`、`NoteController` 里的 `try/catch + Result<T>`
- [ ] 写出“当前写法的优点和问题”各 3 条

## 加分项

- [ ] 思考如果换成全局异常处理该怎么做

## 代码定位

- [ ] 阅读 `lifeos-backend/lifeos-note-service/src/main/java/com/lifeos/note/controller/NoteController.java`

## 算法

- [ ] 有效的括号
- [ ] 最小栈

## 今日交付物

- [ ] 1 份统一返回结构说明
- [ ] 1 份“当前项目异常处理可优化点”清单

## 晚间验收

- [ ] 能解释 `Result<T>` 的作用
- [ ] 能说出为什么全局异常处理更利于维护

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
