package com.remote.system_pulse.service;

import org.springframework.stereotype.Service;
import java.util.List;

import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.repository.ServerRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ServerService {

    private final ServerRepository serverRepository;

    // Constructor Injection - best practice
    public ServerService(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    @Transactional  // Transactional is used in case of failure, the transaction will be rolled back
    public Server createServer(Server server) {
        return serverRepository.save(server);
    }

    @Transactional(readOnly = true)     // readOnly can avoid overhead in read operations
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Server getServerById(Long id) {
        return serverRepository.findById(id).
            orElseThrow(() -> new RuntimeException("Server not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Server getServerByIpAddress(String ip) {
        return serverRepository.findByIpAddress(ip).orElseThrow(() -> new RuntimeException("Server not found with ip: " + ip));
    }
    
}
