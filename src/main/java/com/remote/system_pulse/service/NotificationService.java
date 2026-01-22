package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.StatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor    // Não precisa de constructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyStatusChange(StatusUpdateEvent event) {
        log.info("Mudança de status: Servidor {} -> {}", event.serverId(), event.status());
        // Envia para todos os inscritos em /topic/status
        messagingTemplate.convertAndSend("/topic/status", event);
    }
}