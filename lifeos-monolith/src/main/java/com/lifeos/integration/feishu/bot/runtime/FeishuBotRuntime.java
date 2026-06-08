package com.lifeos.integration.feishu.bot.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.event.model.Header;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.EventSender;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import com.lark.oapi.service.im.v1.model.UserId;
import com.lark.oapi.ws.Client;
import com.lifeos.integration.feishu.FeishuProperties;
import com.lifeos.integration.feishu.bot.dto.FeishuBotInboundMessage;
import com.lifeos.integration.feishu.bot.service.FeishuBotApplicationService;
import com.lifeos.integration.feishu.bot.service.FeishuBotCommandService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuBotRuntime {

    private final FeishuProperties properties;
    private final FeishuBotApplicationService applicationService;
    private final FeishuBotCommandService commandService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile boolean started = false;
    private Client client;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!Boolean.TRUE.equals(properties.getBotEnabled())
                || !Boolean.TRUE.equals(properties.getBotLongConnectionEnabled())) {
            log.info("Feishu bot long connection is disabled");
            return;
        }
        if (!properties.hasCredentials()) {
            log.warn("Feishu bot long connection is enabled but app credentials are not configured");
            return;
        }
        if (started) {
            return;
        }

        EventDispatcher dispatcher = EventDispatcher
                .newBuilder(properties.getBotVerificationToken(), properties.getBotEncryptKey())
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        handleMessageEvent(event);
                    }
                })
                .build();

        client = new Client.Builder(properties.getAppId(), properties.getAppSecret())
                .eventHandler(dispatcher)
                .autoReconnect(Boolean.TRUE)
                .build();
        started = true;
        executor.submit(() -> {
            try {
                log.info("Starting Feishu bot long connection");
                client.start();
            } catch (Exception e) {
                started = false;
                log.error("Feishu bot long connection failed: {}", e.getMessage(), e);
            }
        });
    }

    private void handleMessageEvent(P2MessageReceiveV1 event) {
        try {
            P2MessageReceiveV1Data data = event.getEvent();
            if (data == null || data.getMessage() == null) {
                return;
            }
            EventMessage message = data.getMessage();
            FeishuBotInboundMessage inbound = new FeishuBotInboundMessage();
            inbound.setEventId(eventId(event, message));
            inbound.setMessageId(message.getMessageId());
            inbound.setChatId(message.getChatId());
            inbound.setMessageType(message.getMessageType());
            inbound.setSenderOpenId(senderOpenId(data.getSender()));
            inbound.setRequestText(commandService.extractText(message.getContent(), message.getMentions()));
            inbound.setRawPayload(toJson(event));
            applicationService.receive(inbound);
        } catch (Exception e) {
            log.error("Failed to handle Feishu message event: {}", e.getMessage(), e);
        }
    }

    private String eventId(P2MessageReceiveV1 event, EventMessage message) {
        Header header = event.getHeader();
        if (header != null && header.getEventId() != null && !header.getEventId().isBlank()) {
            return header.getEventId();
        }
        if (message.getMessageId() != null && !message.getMessageId().isBlank()) {
            return message.getMessageId();
        }
        return UUID.randomUUID().toString();
    }

    private String senderOpenId(EventSender sender) {
        if (sender == null) {
            return "";
        }
        UserId senderId = sender.getSenderId();
        if (senderId == null) {
            return "";
        }
        if (senderId.getOpenId() != null && !senderId.getOpenId().isBlank()) {
            return senderId.getOpenId();
        }
        return senderId.getUserId() == null ? "" : senderId.getUserId();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
        started = false;
    }
}
