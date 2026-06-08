package com.lifeos.integration.feishu.bot.repository;

import com.lifeos.integration.feishu.bot.entity.FeishuBotEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeishuBotEventRepository extends JpaRepository<FeishuBotEvent, Long> {
    Optional<FeishuBotEvent> findByEventId(String eventId);
    List<FeishuBotEvent> findByOrderByCreateTimeDesc(Pageable pageable);
    List<FeishuBotEvent> findByChatIdInOrderByCreateTimeDesc(Collection<String> chatIds, Pageable pageable);
    List<FeishuBotEvent> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime start, LocalDateTime end);
    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(String status);
    long countByStatusAndCreateTimeBetween(String status, LocalDateTime start, LocalDateTime end);
    long countByChatIdIn(Collection<String> chatIds);

    @Query("select e.commandType, count(e) from FeishuBotEvent e where e.createTime between ?1 and ?2 group by e.commandType")
    List<Object[]> countCommandDistribution(LocalDateTime start, LocalDateTime end);
}
