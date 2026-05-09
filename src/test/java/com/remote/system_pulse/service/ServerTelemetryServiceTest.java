package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.StatusUpdateEvent;
import com.remote.system_pulse.dto.TelemetryDTO;
import com.remote.system_pulse.exception.InvalidAgentTokenException;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.model.enums.ServerStatus;
import com.remote.system_pulse.repository.ServerRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServerTelemetryServiceTest {

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ServerTelemetryService serverTelemetryService;

    @Test
    @DisplayName("Should update telemetry and return event without leaking the token")
    void updateTelemetry_ShouldUpdateServer_WhenServerExists() {
        UUID token = UUID.randomUUID();
        TelemetryDTO telemetryDTO = new TelemetryDTO(42.5, 60.0, 75.3);

        Server existingServer = new Server();
        existingServer.setId(1L);
        existingServer.setName("Alpha Server");
        existingServer.setToken(token);
        existingServer.setStatus(ServerStatus.OFFLINE);

        when(serverRepository.findServerByToken(token)).thenReturn(Optional.of(existingServer));
        when(request.getRemoteAddr()).thenReturn("192.168.0.10");

        StatusUpdateEvent event = serverTelemetryService.updateTelemetry(token, telemetryDTO, request);

        // Entity was mutated in place; Hibernate dirty-checking flushes on commit,
        // so we should NOT see an explicit save() call.
        verify(serverRepository, never()).save(any(Server.class));

        assertEquals(42.5, existingServer.getUsageCpu());
        assertEquals(60.0, existingServer.getUsageRam());
        assertEquals(75.3, existingServer.getUsageDisk());
        assertEquals(ServerStatus.ONLINE, existingServer.getStatus());
        assertEquals("192.168.0.10", existingServer.getIp());
        assertNotNull(existingServer.getLastHeartbeat());

        // Returned event carries DTO fields only — no token field exists on it.
        assertNotNull(event);
        assertEquals(1L, event.serverId());
        assertEquals(ServerStatus.ONLINE, event.status());
        assertEquals(42.5, event.usageCpu());
        assertEquals("192.168.0.10", event.ip());

        // Service does not broadcast itself; the controller does that after commit.
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should throw InvalidAgentTokenException when token is unknown")
    void updateTelemetry_ShouldThrow_WhenTokenNotFound() {
        UUID token = UUID.randomUUID();
        TelemetryDTO telemetryDTO = new TelemetryDTO(42.5, 60.0, 75.3);

        when(serverRepository.findServerByToken(token)).thenReturn(Optional.empty());

        InvalidAgentTokenException ex = assertThrows(InvalidAgentTokenException.class,
                () -> serverTelemetryService.updateTelemetry(token, telemetryDTO, request));

        assertEquals("Unknown agent token", ex.getMessage());

        verify(serverRepository, never()).save(any(Server.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should throw InvalidAgentTokenException when token header is missing")
    void updateTelemetry_ShouldThrow_WhenTokenIsNull() {
        TelemetryDTO telemetryDTO = new TelemetryDTO(42.5, 60.0, 75.3);

        InvalidAgentTokenException ex = assertThrows(InvalidAgentTokenException.class,
                () -> serverTelemetryService.updateTelemetry(null, telemetryDTO, request));

        assertEquals("Missing X-Agent-Token header", ex.getMessage());
        verifyNoInteractions(serverRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should mark stale servers OFFLINE and broadcast a token-free event")
    void checkOfflineServers_ShouldMarkAsOffline_WhenHeartbeatIsOld() {
        Server staleServer = new Server();
        staleServer.setId(7L);
        staleServer.setName("Stale Server");
        staleServer.setToken(UUID.randomUUID()); // a token MUST never escape via the broadcast
        staleServer.setStatus(ServerStatus.ONLINE);
        staleServer.setLastHeartbeat(LocalDateTime.now().minusSeconds(120));

        when(serverRepository.findStaleOnlineServers(eq(ServerStatus.ONLINE), any(LocalDateTime.class)))
                .thenReturn(List.of(staleServer));

        serverTelemetryService.checkOfflineServers();

        assertEquals(ServerStatus.OFFLINE, staleServer.getStatus());

        ArgumentCaptor<StatusUpdateEvent> eventCaptor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(notificationService).notifyStatusChange(eventCaptor.capture());
        StatusUpdateEvent event = eventCaptor.getValue();
        assertEquals(7L, event.serverId());
        assertEquals(ServerStatus.OFFLINE, event.status());

        // Dirty-checking handles persistence; no explicit save expected.
        verify(serverRepository, never()).save(any(Server.class));
    }

    @Test
    @DisplayName("Should do nothing when no servers are stale")
    void checkOfflineServers_ShouldDoNothing_WhenNoStaleServers() {
        when(serverRepository.findStaleOnlineServers(eq(ServerStatus.ONLINE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        serverTelemetryService.checkOfflineServers();

        verifyNoInteractions(notificationService);
        verify(serverRepository, never()).save(any(Server.class));
    }
}
