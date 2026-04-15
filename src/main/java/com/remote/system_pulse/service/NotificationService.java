package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.StatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyStatusChange(StatusUpdateEvent event) {
        log.info("Status change: Server {} -> {}", event.serverId(), event.status());
        // Sends to all subscribers of /topic/status
        messagingTemplate.convertAndSend("/topic/status", event);
    }
}