package com.remote.system_pulse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // Prefixo das mensagens enviadas ao cliente
        config.setApplicationDestinationPrefixes("/app"); // Prefixo que o cliente usa para enviar mensagens ao servidor
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-pulse") // Ponto de conexão do Frontend - canal websocket aberto
                .setAllowedOriginPatterns("*") // Ajuste conforme CORS
                .withSockJS(); // Fallback para navegadores antigos
    }
}
