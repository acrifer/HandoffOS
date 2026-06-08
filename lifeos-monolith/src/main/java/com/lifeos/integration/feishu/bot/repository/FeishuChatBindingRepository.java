package com.lifeos.integration.feishu.bot.repository;

import com.lifeos.integration.feishu.bot.entity.FeishuChatBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeishuChatBindingRepository extends JpaRepository<FeishuChatBinding, Long> {
    Optional<FeishuChatBinding> findByChatIdAndEnabledTrue(String chatId);
    Optional<FeishuChatBinding> findByChatId(String chatId);
    Optional<FeishuChatBinding> findByIdAndUserId(Long id, Long userId);
    List<FeishuChatBinding> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<FeishuChatBinding> findByUserIdAndSkillIdOrderByCreateTimeDesc(Long userId, Long skillId);
    List<FeishuChatBinding> findBySkillIdAndEnabledTrue(Long skillId);
    long countByEnabledTrue();
}
