package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.StatusUpdateEvent;
import com.remote.system_pulse.dto.TelemetryDTO;
import com.remote.system_pulse.exception.InvalidAgentTokenException;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.model.enums.ServerStatus;
import com.remote.system_pulse.repository.ServerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerTelemetryService {

    private final ServerRepository serverRepository;
    private final NotificationService notificationService;

    private static final long OFFLINE_THRESHOLD_SECONDS = 60;

    /**
     * Validates the agent token, updates telemetry inside a transaction, and
     * returns a DTO so the caller can broadcast AFTER commit. The Server entity
     * is intentionally NOT exposed because it carries the agent token.
     */
    @Transactional
    public StatusUpdateEvent updateTelemetry(UUID agentToken, TelemetryDTO telemetryDTO, HttpServletRequest request) {
        if (agentToken == null) {
            throw new InvalidAgentTokenException("Missing X-Agent-Token header");
        }

        Server server = serverRepository.findServerByToken(agentToken)
                .orElseThrow(() -> new InvalidAgentTokenException("Unknown agent token"));

        server.setUsageCpu(telemetryDTO.usageCpu());
        server.setUsageRam(telemetryDTO.usageRam());
        server.setUsageDisk(telemetryDTO.usageDisk());
        server.setStatus(ServerStatus.ONLINE);
        server.setLastHeartbeat(LocalDateTime.now());
        server.setIp(request.getRemoteAddr());

        // Hibernate dirty-checking flushes on commit; an explicit save() is
        // redundant and would force an early flush.

        log.debug("Heartbeat received for server: {}", server.getName());

        return toEvent(server);
    }

    /**
     * Marks ONLINE servers whose last heartbeat is older than the threshold as
     * OFFLINE and broadcasts the change. The filter is pushed to the database
     * via {@link ServerRepository#findStaleOnlineServers} so we do not stream
     * every row over the wire every 10s.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkOfflineServers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(OFFLINE_THRESHOLD_SECONDS);

        List<Server> stale = serverRepository.findStaleOnlineServers(ServerStatus.ONLINE, threshold);

        stale.forEach(server -> {
            server.setStatus(ServerStatus.OFFLINE);
            notificationService.notifyStatusChange(toEvent(server));
            log.warn("Server marked as OFFLINE: {}", server.getName());
        });
    }

    private StatusUpdateEvent toEvent(Server server) {
        return new StatusUpdateEvent(
                server.getId(),
                server.getIp(),
                server.getStatus(),
                server.getUsageCpu(),
                server.getUsageRam(),
                server.getUsageDisk(),
                server.getLastHeartbeat()
        );
    }
}
