package com.highpowerbear.hpbanalytics.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.highpowerbear.hpbanalytics.model.Statistics;
import com.highpowerbear.shared.ExchangeRateDTO;
import com.highpowerbear.shared.ExecutionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by robertk on 10/4/2020.
 */
@Configuration
public class HazelcastConfig {
    private static final Logger log = LoggerFactory.getLogger(HazelcastConfig.class);

    @Bean("hanHazelcastInstance")
    public HazelcastInstance hanHazelcastInstance() {

        Config config = new Config(HanSettings.HAZELCAST_INSTANCE_NAME)
                .addQueueConfig(executionQueueConfig())
                .addMapConfig(exchangeRateMapConfig())
                .addMapConfig(statisticsMapConfig())
                .addMapConfig(currentStatisticsMapConfig());

        log.info("hazelcast queue configs " + config.getQueueConfigs());
        log.info("hazelcast map configs " + config.getMapConfigs());

        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    @DependsOn("hanHazelcastInstance")
    public Map<String, ExchangeRateDTO> exchangeRateMap() {
        return hanHazelcastInstance().getMap(HanSettings.HAZELCAST_EXCHANGE_RATE_MAP_NAME);
    }

    @Bean
    @DependsOn("hanHazelcastInstance")
    public BlockingQueue<ExecutionDTO> executionQueue() {
        return hanHazelcastInstance().getQueue(HanSettings.HAZELCAST_EXECUTION_QUEUE_NAME);
    }

    @Bean
    @DependsOn("hanHazelcastInstance")
    public Map<String, List<Statistics>> statisticsMap() {
        return hanHazelcastInstance().getMap(HanSettings.HAZELCAST_STATISTICS_MAP_NAME);
    }

    @Bean
    @DependsOn("hanHazelcastInstance")
    public Map<String, List<Statistics>> currentStatisticsMap() {
        return hanHazelcastInstance().getMap(HanSettings.HAZELCAST_CURRENT_STATISTICS_MAP_NAME);
    }

    private QueueConfig executionQueueConfig() {
        log.info("configuring hazelcast execution queue");

        return new QueueConfig(HanSettings.HAZELCAST_EXECUTION_QUEUE_NAME)
                .setMaxSize(HanSettings.HAZELCAST_EXECUTION_QUEUE_MAX_SZE)
                .setStatisticsEnabled(true);
    }

    private MapConfig exchangeRateMapConfig() {
        log.info("configuring hazelcast exchange rate map");

        return new MapConfig(HanSettings.HAZELCAST_EXCHANGE_RATE_MAP_NAME)
                .setEvictionConfig(new EvictionConfig()
                        .setEvictionPolicy(EvictionPolicy.NONE)
                        .setMaxSizePolicy(MaxSizePolicy.PER_PARTITION))
                .setMaxIdleSeconds(HanSettings.HAZELCAST_EXCHANGE_RATE_MAP_TIME_MAX_IDLE_SECONDS);
    }

    private MapConfig statisticsMapConfig() {
        log.info("configuring hazelcast statistics map");

        return new MapConfig(HanSettings.HAZELCAST_STATISTICS_MAP_NAME)
                .setEvictionConfig(new EvictionConfig()
                        .setEvictionPolicy(EvictionPolicy.NONE)
                        .setMaxSizePolicy(MaxSizePolicy.PER_PARTITION));
    }

    private MapConfig currentStatisticsMapConfig() {
        log.info("configuring hazelcast current statistics map");

        return new MapConfig(HanSettings.HAZELCAST_CURRENT_STATISTICS_MAP_NAME)
                .setEvictionConfig(new EvictionConfig()
                        .setEvictionPolicy(EvictionPolicy.NONE)
                        .setMaxSizePolicy(MaxSizePolicy.PER_PARTITION));
    }
}
