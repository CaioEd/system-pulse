package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.TelemetryDTO;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServerTelemetryServiceTest {

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ServerTelemetryService serverTelemetryService;

    @Test
    @DisplayName("Should update telemetry successfully if server exists")
    void updateTelemetry_ShouldUpdateServer_WhenServerExists() {
        // Arrange
        UUID token = UUID.randomUUID();
        TelemetryDTO telemetryDTO = new TelemetryDTO(token, 42.5, 60.0, 75.3);

        Server existingServer = new Server();
        existingServer.setId(1L);
        existingServer.setName("Alpha Server");
        existingServer.setToken(token);
        existingServer.setStatus(ServerStatus.OFFLINE);

        when(serverRepository.findServerByToken(token)).thenReturn(Optional.of(existingServer));
        // Returns the same server passed to save(), so the flow continues with our captured state
        when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("192.168.0.10");

        // Act
        serverTelemetryService.updateTelemetry(telemetryDTO, request);

        // Assert: capture the server that was saved and inspect its state
        ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
        verify(serverRepository).save(serverCaptor.capture());
        Server saved = serverCaptor.getValue();

        assertEquals(42.5, saved.getUsageCpu());
        assertEquals(60.0, saved.getUsageRam());
        assertEquals(75.3, saved.getUsageDisk());
        assertEquals(ServerStatus.ONLINE, saved.getStatus());
        assertEquals("192.168.0.10", saved.getIp());
        assertNotNull(saved.getLastHeartbeat());

        // Ensure the WebSocket broadcast was triggered on the correct topic
        verify(messagingTemplate).convertAndSend(eq("/topic/status"), any(Server.class));
    }

    @Test
    @DisplayName("Should throw exception and not save or broadcast when token is not found")
    void updateTelemetry_ShouldThrowException_WhenTokenNotFound() {
        // Arrange
        UUID token = UUID.randomUUID();
        TelemetryDTO telemetryDTO = new TelemetryDTO(token, 42.5, 60.0, 75.3);

        when(serverRepository.findServerByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> serverTelemetryService.updateTelemetry(telemetryDTO, request));

        assertEquals("Server not found with provided token", exception.getMessage());

        // No persistence and no WebSocket broadcast should happen on failure
        verify(serverRepository, never()).save(any(Server.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Server.class));
    }

    @Test
    @DisplayName("Should mark server as offline if last heartbeat is too old")
    void checkOfflineServers_ShouldMarkAsOffline_WhenHeartbeatIsOld() {
        // Arrange
        Server staleServer = new Server();
        staleServer.setName("Stale Server");
        staleServer.setStatus(ServerStatus.ONLINE);
        staleServer.setLastHeartbeat(LocalDateTime.now().minusSeconds(120)); // > 60s threshold

        Server recentServer = new Server();
        recentServer.setName("Recent Server");
        recentServer.setStatus(ServerStatus.ONLINE);
        recentServer.setLastHeartbeat(LocalDateTime.now().minusSeconds(30)); // within threshold

        // Single findAll() returns BOTH servers — the scheduler fetches them in one call
        when(serverRepository.findAll()).thenReturn(Arrays.asList(staleServer, recentServer));

        // Act
        serverTelemetryService.checkOfflineServers();

        // Assert: stale server was flipped to OFFLINE, persisted and broadcast
        assertEquals(ServerStatus.OFFLINE, staleServer.getStatus());
        verify(serverRepository).save(staleServer);
        verify(messagingTemplate).convertAndSend("/topic/status", staleServer);

        // Recent server is untouched: still ONLINE, never saved, never broadcast
        assertEquals(ServerStatus.ONLINE, recentServer.getStatus());
        verify(serverRepository, never()).save(recentServer);
        verify(messagingTemplate, never()).convertAndSend("/topic/status", recentServer);
    }

    @Test
    @DisplayName("Should ignore servers with recent heartbeats and not mark them offline")
    void checkOfflineServers_ShouldIgnoreRecentServers() {
        // Arrange
        Server recentServer = new Server();
        recentServer.setName("Recent Server");
        recentServer.setStatus(ServerStatus.ONLINE);
        recentServer.setLastHeartbeat(LocalDateTime.now().minusSeconds(45)); // within threshold

        when(serverRepository.findAll()).thenReturn(Arrays.asList(recentServer));

        // Act
        serverTelemetryService.checkOfflineServers();

        // Assert: status was not changed and no side-effects were triggered
        assertEquals(ServerStatus.ONLINE, recentServer.getStatus());
        verify(serverRepository, never()).save(any(Server.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Server.class));
    }
}
