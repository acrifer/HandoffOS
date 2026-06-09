package com.lifeos.demo;

import com.lifeos.config.JwtTokenUtil;
import com.lifeos.demo.dto.DeviceLoginRequest;
import com.lifeos.demo.exception.ApiException;
import com.lifeos.demo.repository.AiTokenUsageLogRepository;
import com.lifeos.demo.repository.DemoDeviceQuotaRepository;
import com.lifeos.demo.repository.DemoDeviceSessionRepository;
import com.lifeos.demo.service.DemoDeviceService;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoDeviceServiceTest {

    @Mock
    private DemoDeviceSessionRepository sessionRepository;
    @Mock
    private DemoDeviceQuotaRepository quotaRepository;
    @Mock
    private AiTokenUsageLogRepository usageLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private DemoDeviceService demoDeviceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(demoDeviceService, "defaultTokenLimit", 100L);
        ReflectionTestUtils.setField(demoDeviceService, "adminKey", "secret");
    }

    @Test
    void requireAvailableThrowsWhenQuotaExceeded() {
        var quota = new com.lifeos.demo.entity.DemoDeviceQuota();
        quota.setDeviceId("device-1");
        quota.setQuotaLimit(100L);
        quota.setQuotaUsed(95L);
        quota.setWhitelistEnabled(false);
        quota.setEnabled(true);
        var session = new com.lifeos.demo.entity.DemoDeviceSession();
        session.setDeviceId("device-1");
        session.setUserId(1L);

        when(sessionRepository.findByUserId(1L)).thenReturn(Optional.of(session));
        when(quotaRepository.findByDeviceId("device-1")).thenReturn(Optional.of(quota));

        ApiException error = assertThrows(ApiException.class,
                () -> demoDeviceService.requireAvailable(1L, "SKILL_ASK", 10L));
        assertEquals(429, error.getCode());
    }

    @Test
    void deviceLoginReusesExistingSession() {
        var request = new DeviceLoginRequest();
        request.setDeviceId("device-1");
        request.setDeviceName("Chrome");
        request.setUserAgent("UA");

        var session = new com.lifeos.demo.entity.DemoDeviceSession();
        session.setDeviceId("device-1");
        session.setUserId(7L);
        session.setDeviceName("Old");
        var quota = new com.lifeos.demo.entity.DemoDeviceQuota();
        quota.setDeviceId("device-1");
        quota.setQuotaLimit(100L);
        quota.setQuotaUsed(10L);
        quota.setWhitelistEnabled(false);
        quota.setEnabled(true);
        var user = new com.lifeos.user.entity.User();
        user.setId(7L);
        user.setUsername("device_user");

        when(sessionRepository.findByDeviceId("device-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(quotaRepository.findByDeviceId("device-1")).thenReturn(Optional.of(quota));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(jwtTokenUtil.generateDeviceToken(7L, "device_user", "device-1")).thenReturn("token");

        var response = demoDeviceService.deviceLogin(request);
        assertEquals("token", response.getToken());
        assertEquals(7L, response.getUserId());
        verify(userRepository, never()).save(any());
    }
}
