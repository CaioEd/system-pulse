package com.remote.system_pulse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import java.util.concurrent.Executors;

@Configuration // tells the framework that this class contains bean definitions Spring should create and manage at startup
@EnableAsync // enables asynchronous processing
public class AsyncConfig {

    @Bean(name = "taskExecutor")    // stores the returned object in the Application Context
    public AsyncTaskExecutor applicationTaskExecutor() {
        // Creates an executor that spawns a new Virtual Thread for each task
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
