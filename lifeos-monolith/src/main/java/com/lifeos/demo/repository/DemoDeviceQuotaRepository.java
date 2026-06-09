package com.lifeos.demo.repository;

import com.lifeos.demo.entity.DemoDeviceQuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DemoDeviceQuotaRepository extends JpaRepository<DemoDeviceQuota, Long> {

    Optional<DemoDeviceQuota> findByDeviceId(String deviceId);

    List<DemoDeviceQuota> findAllByOrderByUpdateTimeDesc();
}
