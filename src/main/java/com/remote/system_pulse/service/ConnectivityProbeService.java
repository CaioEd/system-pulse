package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.StatusUpdateEvent;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.model.enums.ServerStatus;
import com.remote.system_pulse.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectivityProbeService {

    private final ServerRepository serverRepository;
    private final NotificationService notificationService;

    @Async("taskExecutor") // Usa nosso executor de Virtual Threads
    @Transactional // Garante consistência ao atualizar o banco
    public void checkServerAvailability(Long serverId, String ip, int port) {
        // Recupera o servidor (ou ignora se foi deletado durante o processo)
        var serverOpt = serverRepository.findById(serverId);
        if (serverOpt.isEmpty()) return;
        
        Server server = serverOpt.get();
        ServerStatus newStatus = ping(ip, port) ? ServerStatus.ONLINE : ServerStatus.OFFLINE;

        // Só atualiza e notifica se houve mudança de status
        if (server.getStatus() != newStatus) {
            server.setStatus(newStatus);
            server.setLastChecked(LocalDateTime.now());
            serverRepository.save(server);

            // Dispara notificação WebSocket
            notificationService.notifyStatusChange(new StatusUpdateEvent(
                server.getId(),
                server.getIp(),
                newStatus,
                LocalDateTime.now()
            ));
        }
    }

    private boolean ping(String ip, int port) {
        try (Socket socket = new Socket()) {
            // Timeout de 2 segundos. Se não responder, considera offline.
            socket.connect(new InetSocketAddress(ip, port), 2000);
            return true;
        } catch (IOException e) {
            return false; // Conexão falhou ou timeout
        }
    }
}