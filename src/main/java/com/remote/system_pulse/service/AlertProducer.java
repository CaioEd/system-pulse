package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.ServerAlertDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AlertProducer {
    private static final Logger log = LoggerFactory.getLogger(AlertProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name:alerts_exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.key:alerts_routing_key}")
    private String routingKey;

    public AlertProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendAlert(ServerAlertDTO alertDTO) {
        log.info("Enviando alerta do servidor {} para a fila...", alertDTO.name());
        rabbitTemplate.convertAndSend(exchange, routingKey, alertDTO);
        log.info("Alerta do servidor {} enviado com sucesso.", alertDTO.name());
    }
}
