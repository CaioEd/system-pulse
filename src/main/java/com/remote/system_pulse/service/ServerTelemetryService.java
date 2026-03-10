package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.ServerAlertDTO;
import com.remote.system_pulse.dto.TelemetryDTO;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.model.enums.ServerStatus;
import com.remote.system_pulse.repository.ServerRepository;
import com.remote.system_pulse.service.AlertProducer;
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
    private final AlertProducer alertProducer;
    private final ServerRepository serverRepository;
    private final SimpMessagingTemplate messagingTemplate; // Para WebSocket

    // constante para definir se Servidor está offline
    private static final long OFFLINE_THRESHOLD = 60;
    
    @Transactional
    public void updateTelemetry(TelemetryDTO telemetryDTO, HttpServletRequest request) {
        var server = serverRepository.findServerByToken(telemetryDTO.token())
                .orElseThrow(() -> new RuntimeException("Server not found with provided token"));

        // 1. Atualiza métricas
        server.setUsageCpu(telemetryDTO.usageCpu());
        server.setUsageRam(telemetryDTO.usageRam());
        server.setUsageDisk(telemetryDTO.usageDisk());
        
        // 2. Atualiza Status e Timestamp
        server.setStatus(ServerStatus.ONLINE);
        server.setLastHeartbeat(LocalDateTime.now());

        // 3. Captura IP real da requisição (útil se o IP do cliente mudar)
        String clientIp = request.getRemoteAddr();
        // Tratamento simples para proxies (Nginx/Cloudflare) se necessário no futuro:
        // String clientIp = request.getHeader("X-Forwarded-For"); 
        server.setIp(clientIp);

        // 4. Salva no Banco
        Server savedServer = serverRepository.save(server);

        // 5. Notifica o Frontend em Tempo Real
        messagingTemplate.convertAndSend("/topic/status", savedServer);
        
        log.debug("Heartbeat received for server: {}", server.getName());
    }

    /**
     * Scheduler: Verifica servidores "mortos"
     * Roda a cada 10 segundos
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkOfflineServers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(OFFLINE_THRESHOLD);

        // Busca todos os servidores que estão ONLINE mas não mandam sinal há > 60s
        // O ideal seria criar um método customizado no Repository para filtrar isso no banco,
        // mas para listas pequenas, filtrar via stream funciona.
        List<Server> serversToGoOffline = serverRepository.findAll().stream()
                .filter(s -> s.getStatus() == ServerStatus.ONLINE)
                .filter(s -> s.getLastHeartbeat() == null || s.getLastHeartbeat().isBefore(threshold))
                .toList();

        serversToGoOffline.forEach(server -> {
            server.setStatus(ServerStatus.OFFLINE);
            serverRepository.save(server);
            // Avisa o front que caiu
            messagingTemplate.convertAndSend("/topic/status", server);
            log.warn("Server marked as OFFLINE: {}", server.getName());

            // Monta o DTO e envia para a fila
            ServerAlertDTO alert = new ServerAlertDTO(
                    server.getId(),
                    server.getName(),
                    server.getIp(),
                    ServerStatus.OFFLINE,
                    server.getUsageCpu(),
                    "Server offline - no heartbeat received",
                    LocalDateTime.now()
            );
            alertProducer.sendAlert(alert);
        });
    }
}
