package com.highpowerbear.hpbanalytics.config

import com.hazelcast.core.HazelcastInstance
import com.highpowerbear.hpbanalytics.config.WsEndpoint.CURRENT_STATISTICS
import com.highpowerbear.hpbanalytics.config.WsEndpoint.EXECUTION
import com.highpowerbear.hpbanalytics.config.WsEndpoint.STATISTICS
import com.highpowerbear.hpbanalytics.config.WsEndpoint.TRADE
import com.highpowerbear.hpbanalytics.model.Statistics
import com.highpowerbear.shared.ExchangeRateDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.socket.WebSocketSession

/**
 * Created by robertk on 2/3/2022.
 */
@Configuration
open class SharedMapConfig @Autowired constructor(@Qualifier("hanHazelcastInstance") private val hazelcastInstance: HazelcastInstance) {

    @Bean
    open fun websocketSessionsMap(): Map<String, List<WebSocketSession>> {
        val map: MutableMap<String, List<WebSocketSession>> = mutableMapOf()
        setOf(EXECUTION, TRADE, STATISTICS, CURRENT_STATISTICS)
            .forEach { endpoint: String -> map[endpoint] = mutableListOf() }
        return map
    }

    @Bean
    open fun exchangeRateMap(): Map<String, ExchangeRateDTO> {
        return hazelcastInstance.getMap(HanSettings.HAZELCAST_EXCHANGE_RATE_MAP)
    }

    @Bean
    open fun statisticsMap(): Map<String, List<Statistics>> {
        return hazelcastInstance.getMap(HanSettings.HAZELCAST_STATISTICS_MAP)
    }

    @Bean
    open fun currentStatisticsMap(): Map<String, List<Statistics>> {
        return hazelcastInstance.getMap(HanSettings.HAZELCAST_CURRENT_STATISTICS_MAP)
    }
}