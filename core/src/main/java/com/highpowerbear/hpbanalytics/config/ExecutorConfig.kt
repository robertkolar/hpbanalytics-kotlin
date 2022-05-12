package com.highpowerbear.hpbanalytics.config

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Created by robertk on 4/5/2020.
 */
@Configuration
open class ExecutorConfig {
    @Bean
    open fun scheduledExecutorService(): ScheduledExecutorService {
        return Executors.newScheduledThreadPool(HanSettings.SCHEDULED_THREAD_POOL_SIZE)
    }
}