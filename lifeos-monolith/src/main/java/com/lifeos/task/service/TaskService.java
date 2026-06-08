package com.lifeos.task.service;

import com.lifeos.task.dto.TaskRequest;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.entity.Task;
import com.lifeos.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponse createTask(Long userId, TaskRequest request) {
        Task task = new Task();
        task.setUserId(userId);
        apply(task, request);
        task.setStatus((short) 0);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(Long userId, TaskRequest request) {
        if (request.getId() == null) {
            throw new RuntimeException("Task id is required");
        }
        Task task = taskRepository.findByIdAndUserId(request.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        apply(task, request);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse completeTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus((short) 2);
        task.setCompleteTime(LocalDateTime.now());
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse completeTask(Long userId, Long skillId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (skillId != null && task.getSkillId() != null && !skillId.equals(task.getSkillId())) {
            throw new RuntimeException("Task does not belong to the bound Skill");
        }
        task.setStatus((short) 2);
        task.setCompleteTime(LocalDateTime.now());
        return toResponse(taskRepository.save(task));
    }

    public List<TaskResponse> listTasks(Long userId) {
        return taskRepository.findByUserIdOrderByStatusAscCreateTimeDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public List<TaskResponse> listTasks(Long userId, Long skillId) {
        if (skillId == null) {
            return listTasks(userId);
        }
        return taskRepository.findByUserIdAndSkillIdOrderByStatusAscCreateTimeDesc(userId, skillId)
                .stream().map(this::toResponse).toList();
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setTags(request.getTags());
        task.setSourceNoteId(request.getSourceNoteId());
        task.setSkillId(request.getSkillId());
        task.setSourceQaLogId(request.getSourceQaLogId());
        task.setDeadline(parseDeadline(request.getDeadline()));
    }

    private LocalDateTime parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(deadline);
        } catch (DateTimeParseException ignore) {
            return OffsetDateTime.parse(deadline).toLocalDateTime();
        }
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        BeanUtils.copyProperties(task, response);
        return response;
    }
}
