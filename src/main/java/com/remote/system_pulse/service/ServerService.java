package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.ServerRequestDTO;
import com.remote.system_pulse.dto.ServerResponseDTO;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
// On Java 16+, .toList() works directly.
// Otherwise use: .collect(Collectors.toList());

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;

    @Transactional
    public ServerResponseDTO createServer(ServerRequestDTO serverRequestDTO) {
        Server server = new Server();
        // Mapping DTO (Record) to Entity
        server.setName(serverRequestDTO.name());
        server.setDescription(serverRequestDTO.description());

        Server savedServer = serverRepository.save(server);
        return mapToResponseDTO(savedServer);
    }

    @Transactional(readOnly = true)
    public List<ServerResponseDTO> getAllServers() {
        return serverRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO) // Converts each Entity to DTO
                .toList(); // Requires Java 16+. Otherwise use .collect(Collectors.toList())
    }

    @Transactional(readOnly = true)
    public ServerResponseDTO getServerById(Long id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found with id: " + id));
        return mapToResponseDTO(server);
    }

    @Transactional
    public ServerResponseDTO updateServer(Long id, ServerRequestDTO serverRequestDTO) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found with id: " + id));
        server.setName(serverRequestDTO.name());
        server.setDescription(serverRequestDTO.description());
        Server updatedServer = serverRepository.save(server);
        return mapToResponseDTO(updatedServer);
    }

    @Transactional
    public void deleteServer(Long id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found with id: " + id));
        serverRepository.delete(server);
    }

    // Conversion helper (Entity -> DTO)
    // Centralizes the mapping logic to avoid duplication
    private ServerResponseDTO mapToResponseDTO(Server server) {
        return new ServerResponseDTO(
            server.getId(),
            server.getToken(),
            server.getName(),
            server.getDescription(),
            server.getIp(),
            server.getStatus(),
            server.getLastHeartbeat(),
            server.getUsageCpu(),
            server.getUsageRam(),
            server.getUsageDisk()
        );
    }
}