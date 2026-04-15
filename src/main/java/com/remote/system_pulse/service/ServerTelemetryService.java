package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.TelemetryDTO;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.model.enums.ServerStatus;
import com.remote.system_pulse.repository.ServerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerTelemetryService {
    private final ServerRepository serverRepository;
    private final SimpMessagingTemplate messagingTemplate; // For WebSocket

    // constant that defines the offline threshold for a server
    private static final long OFFLINE_THRESHOLD = 60;
    
    @Transactional
    public void updateTelemetry(TelemetryDTO telemetryDTO, HttpServletRequest request) {
        var server = serverRepository.findServerByToken(telemetryDTO.token())
                .orElseThrow(() -> new RuntimeException("Server not found with provided token"));

        // 1. Update metrics
        server.setUsageCpu(telemetryDTO.usageCpu());
        server.setUsageRam(telemetryDTO.usageRam());
        server.setUsageDisk(telemetryDTO.usageDisk());
        
        // 2. Update status and timestamp
        server.setStatus(ServerStatus.ONLINE);
        server.setLastHeartbeat(LocalDateTime.now());

        // 3. Capture the real request IP (useful if the client IP changes)
        String clientIp = request.getRemoteAddr();
        // Simple handling for proxies (Nginx/Cloudflare) if needed in the future:
        // String clientIp = request.getHeader("X-Forwarded-For"); 
        server.setIp(clientIp);

        // 4. Persist to database
        Server savedServer = serverRepository.save(server);

        // 5. Notify the frontend in real time
        messagingTemplate.convertAndSend("/topic/status", savedServer);
        
        log.debug("Heartbeat received for server: {}", server.getName());
    }

    /**
     * Scheduler: checks for "dead" servers
     * Runs every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkOfflineServers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(OFFLINE_THRESHOLD);

        // Find all servers that are ONLINE but have not sent a signal in > 60s
        // Ideally a custom repository method would filter this at the database level,
        // but for small lists, stream filtering works fine.
        List<Server> serversToGoOffline = serverRepository.findAll().stream()
                .filter(s -> s.getStatus() == ServerStatus.ONLINE)
                .filter(s -> s.getLastHeartbeat() == null || s.getLastHeartbeat().isBefore(threshold))
                .toList();

        serversToGoOffline.forEach(server -> {
            server.setStatus(ServerStatus.OFFLINE);
            
            serverRepository.save(server);
            
            // Notify the frontend that the server went offline
            messagingTemplate.convertAndSend("/topic/status", server);
            
            log.warn("Server marked as OFFLINE: {}", server.getName());
        });
    }
}
