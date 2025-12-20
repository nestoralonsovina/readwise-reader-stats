package com.reader.analytics.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean("syncExecutor")
    fun syncExecutor(): Executor =
        Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("sync-", 0).factory()
        )
}
