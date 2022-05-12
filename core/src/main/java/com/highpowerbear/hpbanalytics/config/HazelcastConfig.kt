package com.highpowerbear.hpbanalytics.config

import com.hazelcast.config.*
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.highpowerbear.shared.ExecutionDTO
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import java.util.concurrent.BlockingQueue

/**
 * Created by robertk on 10/4/2020.
 */
@Configuration
open class HazelcastConfig {
    @Bean("hanHazelcastInstance")
    open fun hazelcastInstance(): HazelcastInstance {
        val config = Config(HanSettings.HAZELCAST_INSTANCE_NAME)
            .addQueueConfig(executionQueueConfig())
            .addMapConfig(mapConfig(HanSettings.HAZELCAST_STATISTICS_MAP))
            .addMapConfig(mapConfig(HanSettings.HAZELCAST_CURRENT_STATISTICS_MAP))
            .addMapConfig(
                mapConfig(HanSettings.HAZELCAST_EXCHANGE_RATE_MAP)
                    .setMaxIdleSeconds(HanSettings.HAZELCAST_EXCHANGE_RATE_MAP_TIME_MAX_IDLE_SECONDS)
            )
        return Hazelcast.newHazelcastInstance(config)
    }

    @Bean
    @DependsOn("hanHazelcastInstance")
    open fun executionQueue(): BlockingQueue<ExecutionDTO> {
        return hazelcastInstance().getQueue(HanSettings.HAZELCAST_EXECUTION_QUEUE)
    }

    private fun executionQueueConfig(): QueueConfig {
        log.info("configuring hazelcast execution queue")
        return QueueConfig(HanSettings.HAZELCAST_EXECUTION_QUEUE)
            .setMaxSize(HanSettings.HAZELCAST_EXECUTION_QUEUE_MAX_SZE)
            .setStatisticsEnabled(true)
    }

    private fun mapConfig(name: String): MapConfig {
        log.info("configuring hazelcast $name")
        return MapConfig(name)
            .setEvictionConfig(
                EvictionConfig()
                    .setEvictionPolicy(EvictionPolicy.NONE)
                    .setMaxSizePolicy(MaxSizePolicy.PER_PARTITION)
            )
    }

    companion object {
        private val log = LoggerFactory.getLogger(HazelcastConfig::class.java)
    }
}