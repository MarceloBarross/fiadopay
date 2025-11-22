package edu.ucsal.fiadopay.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {
    @Bean(name = "poolPayment")
    public Executor poolPayment() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(10);
        ex.setThreadNamePrefix("poolPayment");
        ex.initialize();
        return ex;
    }

    @Bean(name = "poolWebhook")
    public Executor poolWebhook() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(5);
        ex.setThreadNamePrefix("poolWebhook");
        ex.initialize();
        return ex;
    }
}
