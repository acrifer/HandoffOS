package com.lifeos.demo.repository;

import com.lifeos.demo.entity.DemoDeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DemoDeviceSessionRepository extends JpaRepository<DemoDeviceSession, Long> {

    Optional<DemoDeviceSession> findByDeviceId(String deviceId);

    Optional<DemoDeviceSession> findByUserId(Long userId);
}
