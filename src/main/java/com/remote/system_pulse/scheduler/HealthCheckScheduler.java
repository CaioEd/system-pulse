package com.remote.system_pulse.scheduler;

import com.remote.system_pulse.repository.ServerRepository;
import com.remote.system_pulse.service.ConnectivityProbeService;
import com.remote.system_pulse.model.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthCheckScheduler {

    private final ServerRepository serverRepository;
    private final ConnectivityProbeService probeService;

    // Executa a cada 10 segundos (ajuste conforme necessidade)
    @Scheduled(fixedRate = 10000)
    public void triggerHealthChecks() {
        List<Server> servers = serverRepository.findAll();
        
        log.info("Iniciando varredura em {} servidores...", servers.size());

        for (Server server : servers) {
            // O loop não bloqueia aqui. Ele dispara a tarefa para a Virtual Thread e continua.
            probeService.checkServerAvailability(   // envia dados como parâmetro para o serviço de Thread
                server.getId(), 
                server.getIp(), 
                server.getPort()
            );
        }
    }
}
