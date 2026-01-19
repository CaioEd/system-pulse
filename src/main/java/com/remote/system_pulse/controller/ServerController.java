package com.remote.system_pulse.controller;

import java.util.List;

import com.remote.system_pulse.dto.ServerRequestDTO;
import com.remote.system_pulse.dto.ServerResponseDTO;
import com.remote.system_pulse.service.ServerService;
import jakarta.validation.Valid; // Importação correta para validação
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController     // RestController is a special type of controller that handles HTTP requests 
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor    // Lombok for constructor injection
public class ServerController {

    private final ServerService serverService;
    
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


}
