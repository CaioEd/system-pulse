package com.remote.system_pulse.controller;

import java.util.List;
import java.util.UUID;

import com.remote.system_pulse.dto.ServerRequestDTO;
import com.remote.system_pulse.dto.ServerResponseDTO;
import com.remote.system_pulse.dto.StatusUpdateEvent;
import com.remote.system_pulse.service.NotificationService;
import com.remote.system_pulse.service.ServerService;
import com.remote.system_pulse.dto.TelemetryDTO;
import com.remote.system_pulse.service.ServerTelemetryService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController     // RestController is a special type of controller that handles HTTP requests 
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor    // Lombok for constructor injection
public class ServerController {

    private final ServerService serverService;
    private final ServerTelemetryService serverTelemetryService;
    private final NotificationService notificationService;
    
    @PostMapping
    public ResponseEntity<ServerResponseDTO> createServer(@Valid @RequestBody ServerRequestDTO request) {
        ServerResponseDTO response = serverService.createServer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ServerResponseDTO>> getAllServers() {
        List<ServerResponseDTO> response = serverService.getAllServers();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerResponseDTO> getServerById(@PathVariable Long id) {
        ServerResponseDTO response = serverService.getServerById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServerResponseDTO> updateServer(@PathVariable Long id, @Valid @RequestBody ServerRequestDTO request) {
        ServerResponseDTO response = serverService.updateServer(id, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> receiveHeartbeat(
            @RequestHeader("X-Agent-Token") UUID agentToken,
            @Valid @RequestBody TelemetryDTO telemetryDTO,
            HttpServletRequest request
    ) {
        // Persist inside the transactional service, then broadcast AFTER commit
        // so a slow STOMP broker can't keep the DB transaction open.
        StatusUpdateEvent event = serverTelemetryService.updateTelemetry(agentToken, telemetryDTO, request);
        notificationService.notifyStatusChange(event);
        return ResponseEntity.ok().build();
    }

}
