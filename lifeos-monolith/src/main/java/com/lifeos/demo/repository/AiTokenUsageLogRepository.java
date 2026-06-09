package com.lifeos.demo.repository;

import com.lifeos.demo.entity.AiTokenUsageLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiTokenUsageLogRepository extends JpaRepository<AiTokenUsageLog, Long> {

    List<AiTokenUsageLog> findByDeviceIdOrderByCreateTimeDesc(String deviceId, Pageable pageable);
}
