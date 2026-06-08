package com.lifeos.integration.feishu.bot.service;

import com.lifeos.integration.feishu.FeishuClient;
import com.lifeos.integration.feishu.FeishuProperties;
import com.lifeos.integration.feishu.bot.dto.FeishuBotEventResponse;
import com.lifeos.integration.feishu.bot.dto.FeishuBotStatusResponse;
import com.lifeos.integration.feishu.bot.dto.FeishuChatBindingRequest;
import com.lifeos.integration.feishu.bot.dto.FeishuChatBindingResponse;
import com.lifeos.integration.feishu.bot.entity.FeishuBotEvent;
import com.lifeos.integration.feishu.bot.entity.FeishuChatBinding;
import com.lifeos.integration.feishu.bot.repository.FeishuBotEventRepository;
import com.lifeos.integration.feishu.bot.repository.FeishuChatBindingRepository;
import com.lifeos.skill.entity.HandoffSkill;
import com.lifeos.skill.repository.HandoffSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeishuBotBindingService {

    private final FeishuChatBindingRepository bindingRepository;
    private final FeishuBotEventRepository eventRepository;
    private final HandoffSkillRepository skillRepository;
    private final FeishuProperties feishuProperties;
    private final FeishuClient feishuClient;

    @Transactional
    public FeishuChatBindingResponse bind(Long userId, FeishuChatBindingRequest request) {
        String chatId = request.getChatId() == null ? "" : request.getChatId().trim();
        if (chatId.isBlank()) {
            throw new RuntimeException("chat_id is required");
        }
        if (request.getSkillId() == null) {
            throw new RuntimeException("skillId is required");
        }
        HandoffSkill skill = skillRepository.findByIdAndUserId(request.getSkillId(), userId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        FeishuChatBinding binding = bindingRepository.findByChatId(chatId).orElseGet(FeishuChatBinding::new);
        if (binding.getId() != null && !userId.equals(binding.getUserId())) {
            throw new RuntimeException("This chat_id is already bound by another user");
        }
        binding.setUserId(userId);
        binding.setChatId(chatId);
        binding.setChatName(firstNonBlank(request.getChatName(), skill.getName() + " 飞书群"));
        binding.setSkillId(skill.getId());
        binding.setEnabled(request.getEnabled() == null || request.getEnabled());
        return toBindingResponse(bindingRepository.save(binding));
    }

    public List<FeishuChatBindingResponse> listBindings(Long userId, Long skillId) {
        List<FeishuChatBinding> bindings = skillId == null
                ? bindingRepository.findByUserIdOrderByCreateTimeDesc(userId)
                : bindingRepository.findByUserIdAndSkillIdOrderByCreateTimeDesc(userId, skillId);
        return bindings.stream().map(this::toBindingResponse).toList();
    }

    @Transactional
    public void disable(Long userId, Long bindingId) {
        FeishuChatBinding binding = bindingRepository.findByIdAndUserId(bindingId, userId)
                .orElseThrow(() -> new RuntimeException("Feishu chat binding not found"));
        binding.setEnabled(false);
        bindingRepository.save(binding);
    }

    public List<FeishuBotEventResponse> listEvents(Long userId, Long skillId, int limit) {
        List<FeishuChatBinding> bindings = skillId == null
                ? bindingRepository.findByUserIdOrderByCreateTimeDesc(userId)
                : bindingRepository.findByUserIdAndSkillIdOrderByCreateTimeDesc(userId, skillId);
        List<String> chatIds = bindings.stream().map(FeishuChatBinding::getChatId).toList();
        if (chatIds.isEmpty()) {
            return List.of();
        }
        return eventRepository.findByChatIdInOrderByCreateTimeDesc(chatIds, PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    public FeishuBotStatusResponse status() {
        FeishuBotStatusResponse response = new FeishuBotStatusResponse();
        response.setCredentialsConfigured(feishuClient.isConfigured());
        response.setBotEnabled(Boolean.TRUE.equals(feishuProperties.getBotEnabled()));
        response.setLongConnectionEnabled(Boolean.TRUE.equals(feishuProperties.getBotLongConnectionEnabled()));
        response.setBindingCount(bindingRepository.countByEnabledTrue());
        response.setEventCount(eventRepository.count());
        response.setFailedEventCount(eventRepository.countByStatus("FAILED"));
        return response;
    }

    private FeishuChatBindingResponse toBindingResponse(FeishuChatBinding binding) {
        FeishuChatBindingResponse response = new FeishuChatBindingResponse();
        response.setId(binding.getId());
        response.setUserId(binding.getUserId());
        response.setChatId(binding.getChatId());
        response.setChatName(binding.getChatName());
        response.setSkillId(binding.getSkillId());
        response.setEnabled(binding.getEnabled());
        response.setCreateTime(binding.getCreateTime());
        response.setUpdateTime(binding.getUpdateTime());
        skillRepository.findById(binding.getSkillId()).map(HandoffSkill::getName).ifPresent(response::setSkillName);
        return response;
    }

    private FeishuBotEventResponse toEventResponse(FeishuBotEvent event) {
        FeishuBotEventResponse response = new FeishuBotEventResponse();
        response.setId(event.getId());
        response.setEventId(event.getEventId());
        response.setMessageId(event.getMessageId());
        response.setChatId(event.getChatId());
        response.setSenderOpenId(event.getSenderOpenId());
        response.setCommandType(event.getCommandType());
        response.setRequestText(event.getRequestText());
        response.setStatus(event.getStatus());
        response.setErrorMessage(event.getErrorMessage());
        response.setJobId(event.getJobId());
        response.setQaLogId(event.getQaLogId());
        response.setCreateTime(event.getCreateTime());
        response.setUpdateTime(event.getUpdateTime());
        return response;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
