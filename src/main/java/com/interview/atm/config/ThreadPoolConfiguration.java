package com.interview.atm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolConfiguration {
    @Value("${atm.task.executor.pool.size:10}")
    private int poolSize;

    @Value("${atm.task.executor.max.pool.size:10}")
    private int maxPoolSize;

    @Value("${atm.task.executor.queue.capacity:1000000}")
    private int queueCapacity;

    @Bean
    public ThreadPoolTaskExecutor transactionTaskExecutor() {
        ThreadPoolTaskExecutor processorTaskExecutor = new ThreadPoolTaskExecutor();
        processorTaskExecutor.setCorePoolSize(poolSize);
        processorTaskExecutor.setMaxPoolSize(maxPoolSize);
        processorTaskExecutor.setQueueCapacity(queueCapacity);
        processorTaskExecutor.setThreadNamePrefix("transaction-");
        return processorTaskExecutor;
    }

}
