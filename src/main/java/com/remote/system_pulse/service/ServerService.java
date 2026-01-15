package com.remote.system_pulse.service;

import org.springframework.stereotype.Service;

import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.repository.ServerRepository;

import jakarta.transaction.Transactional;

@Service
public class ServerService {

    private final ServerRepository serverRepository;

    public ServerService(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    @Transactional
    public Server createServer(Server server) {
        return serverRepository.save(server);
    }
    
}
