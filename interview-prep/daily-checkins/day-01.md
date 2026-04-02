# Day 01

## 今日目标

- 建立 Java 集合基础认知
- 先把 `ArrayList`、`LinkedList`、`HashMap` 讲顺

## 知识速记

- `ArrayList` 底层是动态数组，优点是随机访问快，适合读多写少。
- `LinkedList` 底层是双向链表，优点是插入删除方便，但随机访问慢。
- `HashMap` 底层是 `数组 + 链表/红黑树`，通过 `hash` 定位桶位。
- `HashMap` 线程不安全的核心原因是并发写入时没有加锁，可能出现数据覆盖和结构不一致。
- 面试先给结论：业务里默认优先用 `ArrayList`，并发场景下的 `Map` 优先考虑 `ConcurrentHashMap`。

## 标准回答

### `ArrayList` 为什么查询快、插入慢

`ArrayList` 底层是连续内存的动态数组，按下标定位元素是 `O(1)`，所以查询快。插入时如果发生在中间位置，需要整体搬移后面的元素，所以插入和删除成本更高。

### `HashMap` 为什么线程不安全

`HashMap` 在并发 `put` 时没有同步控制，多线程可能同时修改同一个桶或触发扩容，导致数据覆盖、链表或树结构异常，所以线程不安全。并发场景一般用 `ConcurrentHashMap`。

## 必做

- [ ] 阅读 `interview-prep/01-4-week-roadmap.md` 里的 Day 1
- [ ] 阅读 `interview-prep/04-high-frequency-qa.md` 中 Java 基础前 2 题
- [ ] 自己总结 `ArrayList`、`LinkedList`、`HashMap` 各 3 个关键词
- [ ] 口述 2 轮：`ArrayList 为什么查询快`、`HashMap 为什么线程不安全`

## 加分项

- [ ] 额外补 `ConcurrentHashMap` 和 `HashMap` 的区别

## 代码定位

- [ ] 浏览 `lifeos-backend/lifeos-user-service/src/main/java/com/lifeos/user/service/impl/UserServiceImpl.java`
- [ ] 观察项目里 `LambdaQueryWrapper` 的使用方式，不要求全懂

## 算法

- [ ] 两数之和
- [ ] 有效的字母异位词

## 今日交付物

- [ ] 写 1 页集合笔记
- [ ] 录 2 段 1 分钟以内口述音频

## 晚间验收

- [ ] 不看稿说出 `ArrayList` 和 `LinkedList` 差异
- [ ] 不看稿说出 `HashMap` 线程不安全原因

## 打卡

- 开始时间：
- 结束时间：
- 今天最卡的点：
- 明天先补什么：
