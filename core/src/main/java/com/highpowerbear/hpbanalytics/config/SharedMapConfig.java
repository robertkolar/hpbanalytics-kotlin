package com.highpowerbear.hpbanalytics.config;

import com.hazelcast.core.HazelcastInstance;
import com.highpowerbear.hpbanalytics.model.Statistics;
import com.highpowerbear.shared.ExchangeRateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Created by robertk on 2/3/2022.
 */
@Configuration
public class SharedMapConfig {

    private final HazelcastInstance hazelcastInstance;

    @Autowired
    public SharedMapConfig(@Qualifier("hanHazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Bean
    public Map<String, ExchangeRateDTO> exchangeRateMap() {
        return hazelcastInstance.getMap(HanSettings.HAZELCAST_EXCHANGE_RATE_MAP);
    }

    @Bean
    public Map<String, List<Statistics>> statisticsMap() {
        return hazelcastInstance.getMap(HanSettings.HAZELCAST_STATISTICS_MAP);
    }

    @Bean
    public Map<String, List<Statistics>> currentStatisticsMap() {
        return hazelcastInstance.getMap(HanSettings.HAZELCAST_CURRENT_STATISTICS_MAP);
    }
}
