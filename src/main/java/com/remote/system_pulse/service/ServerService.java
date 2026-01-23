package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.ServerRequestDTO;
import com.remote.system_pulse.dto.ServerResponseDTO;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
// Se estiver usando Java 16+, .toList() funciona direto. 
// Caso contrário, use: .collect(Collectors.toList());

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;

    @Transactional
    public ServerResponseDTO createServer(ServerRequestDTO serverRequestDTO) {
        Server server = new Server();
        // Mapeando DTO (Record) para Entidade
        server.setName(serverRequestDTO.name());
        server.setDescription(serverRequestDTO.description());
        server.setIp(serverRequestDTO.ip());
        server.setPort(serverRequestDTO.port());
        server.setStatus(serverRequestDTO.status());
        server.setLastChecked(serverRequestDTO.lastChecked());

        Server savedServer = serverRepository.save(server);
        return mapToResponseDTO(savedServer);
    }

    @Transactional(readOnly = true)
    public List<ServerResponseDTO> getAllServers() {
        return serverRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO) // Converte cada Entidade para DTO
                .toList(); // Requer Java 16+. Se for antigo, use .collect(Collectors.toList())
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
        server.setIp(serverRequestDTO.ip());
        server.setPort(serverRequestDTO.port());
        server.setStatus(serverRequestDTO.status());
        server.setLastChecked(serverRequestDTO.lastChecked());
        Server updatedServer = serverRepository.save(server);
        return mapToResponseDTO(updatedServer);
    }

    @Transactional
    public void deleteServer(Long id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found with id: " + id));
        serverRepository.delete(server);
    }

    // Método auxiliar de conversão (Entidade -> DTO)
    // Centraliza a lógica para não repetir código
    private ServerResponseDTO mapToResponseDTO(Server server) {
        return new ServerResponseDTO(
            server.getId(),
            server.getName(),
            server.getDescription(),
            server.getIp(),
            server.getPort(),
            server.getStatus(),
            server.getLastChecked()
        );
    }
}