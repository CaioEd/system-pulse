package com.remote.system_pulse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import java.util.concurrent.Executors;

@Configuration // avisa ao framework que nessa classe existem definições de objetos que o Spring precisa criar e gerenciar assim que o projeto iniciar
@EnableAsync // habilita o processamento assíncrono
public class AsyncConfig {

    @Bean(name = "taskExecutor")    // guarda o objeto retornado dentro dp Application Context
    public AsyncTaskExecutor applicationTaskExecutor() {
        // Cria um executor que lança uma nova Virtual Thread para cada tarefa
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
