package com.lifeos.task.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.mq.BehaviorMqConstants;
import com.lifeos.task.domain.dto.TaskCreateDTO;
import com.lifeos.task.domain.dto.TaskUpdateDTO;
import com.lifeos.task.domain.entity.Task;
import com.lifeos.task.mapper.TaskMapper;
import com.lifeos.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
@Slf4j
/**
 * 任务主流程服务。
 *
 * 任务在项目里承担两个角色：
 * 1. 直接作为待办事项管理
 * 2. 作为“从笔记中提炼出的可执行动作”落地
 *
 * 这里的关键实现有两块：
 * - 列表查询做 Redis 快照缓存，减少频繁读取数据库
 * - 任务完成后发行为事件，让 Dashboard 能统计完成情况
 */
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // ==================== Redis 作用：任务列表缓存 ====================
    // Redis 在任务服务里只做“用户任务列表快照缓存”。
    // 不做单条 task 的细粒度缓存，而是采用：
    // - 读：整列表命中 Redis
    // - 写：统一删整包缓存
    // 这样实现简单，也更符合当前业务规模。
    // ===============================================================

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TASK_LIST_KEY_PREFIX = "user:task:list:";

    @Override
    public Long createTask(Long userId, TaskCreateDTO createDTO) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTitle(createDTO.getTitle());
        task.setDescription(createDTO.getDescription());
        task.setDeadline(createDTO.getDeadline());
        task.setTags(createDTO.getTags());
        task.setSourceNoteId(createDTO.getSourceNoteId());
        task.setStatus(0); // 0-Pending

        this.save(task);

        // ===== Redis 失效：user:task:list:* =====
        // 任务列表缓存是按用户整体缓存，任何写操作都直接整包失效。
        // Redis 在这里的作用是减少“同一用户频繁打开任务页”时的数据库查询次数。
        clearUserTaskCache(userId);
        if (createDTO.getSourceNoteId() != null) {
            recordBehaviorEvent(userId, "EXTRACT_TASK_FROM_NOTE", createDTO.getSourceNoteId());
        }
        return task.getId();
    }

    @Override
    public void updateTask(Long userId, TaskUpdateDTO updateDTO) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getId, updateDTO.getId()).eq(Task::getUserId, userId);

        Task task = this.getOne(wrapper);
        if (task == null) {
            throw new RuntimeException("Task not found or access denied");
        }

        if (updateDTO.getTitle() != null)
            task.setTitle(updateDTO.getTitle());
        if (updateDTO.getDescription() != null)
            task.setDescription(updateDTO.getDescription());
        if (updateDTO.getDeadline() != null)
            task.setDeadline(updateDTO.getDeadline());
        if (updateDTO.getTags() != null)
            task.setTags(updateDTO.getTags());
        if (updateDTO.getSourceNoteId() != null)
            task.setSourceNoteId(updateDTO.getSourceNoteId());
        if (updateDTO.getStatus() != null)
            task.setStatus(updateDTO.getStatus());

        this.updateById(task);

        // 任务内容更新后，用户任务列表缓存需要立即失效。
        clearUserTaskCache(userId);
    }

    @Override
    public void deleteTask(Long userId, Long taskId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getId, taskId).eq(Task::getUserId, userId);

        if (this.remove(wrapper)) {
            // ===== Redis 失效：user:task:list:* =====
            // 删除任务后，Redis 里的整包列表快照已经过时，必须一起删掉。
            clearUserTaskCache(userId);
        }
    }

    @Override
    public List<Task> listUserTasks(Long userId) {
        String cacheKey = TASK_LIST_KEY_PREFIX + userId;
        String cachedList = stringRedisTemplate.opsForValue().get(cacheKey);

        // ===== Redis 读取：user:task:list:* =====
        // 这里故意使用粗粒度缓存：
        // 不做单条 task 的精细缓存，避免多处更新时维护成本过高。
        // Redis 的目标是“让列表页更快”，不是把任务服务完全做成缓存驱动。
        if (cachedList != null) {
            return JSON.parseArray(cachedList, Task.class);
        }

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId).orderByDesc(Task::getCreateTime);

        List<Task> tasks = this.list(wrapper);

        // ===== Redis 写入：user:task:list:* =====
        // 任务列表读取相对频繁，缓存 1 小时；写操作会主动删缓存。
        // 因为有显式失效逻辑，所以这里的 1 小时更像兜底过期时间，而不是唯一一致性手段。
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(tasks), 1, TimeUnit.HOURS);

        return tasks;
    }

    @Override
    public void completeTask(Long userId, Long taskId) {
        // 完成任务复用 updateTask，保证缓存失效逻辑只维护一套。
        Task task = getOwnedTask(userId, taskId);
        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setId(taskId);
        updateDTO.setStatus(2); // 2-Completed
        this.updateTask(userId, updateDTO);
        recordBehaviorEvent(userId, "FINISH_TASK", taskId);
        if (task.getSourceNoteId() != null) {
            recordBehaviorEvent(userId, "TASK_FROM_NOTE_COMPLETED", task.getSourceNoteId());
        }
    }

    private void clearUserTaskCache(Long userId) {
        // ===== Redis 统一删缓存入口 =====
        // 统一从这里删除 Redis 缓存，避免各个写操作散落不同 key 规则。
        stringRedisTemplate.delete(TASK_LIST_KEY_PREFIX + userId);
    }

    private Task getOwnedTask(Long userId, Long taskId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getId, taskId).eq(Task::getUserId, userId);
        Task task = this.getOne(wrapper);
        if (task == null) {
            throw new RuntimeException("Task not found or access denied");
        }
        return task;
    }

    private void recordBehaviorEvent(Long userId, String actionType, Long targetId) {
        try {
            BehaviorEventCommand command = new BehaviorEventCommand();
            command.setEventId(UUID.randomUUID().toString());
            command.setUserId(userId);
            command.setActionType(actionType);
            command.setTargetId(targetId);
            rocketMQTemplate.syncSend(BehaviorMqConstants.TOPIC, JSON.toJSONString(command),
                    BehaviorMqConstants.PRODUCER_TIMEOUT_MS);
        } catch (Exception ex) {
            log.warn("Failed to record behavior event {} for user {}", actionType, userId, ex);
        }
    }
}
