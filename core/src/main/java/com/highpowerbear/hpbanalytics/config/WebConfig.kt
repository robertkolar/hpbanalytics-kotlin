package com.highpowerbear.hpbanalytics.config

import com.highpowerbear.hpbanalytics.config.WsEndpoint.CURRENT_STATISTICS
import com.highpowerbear.hpbanalytics.config.WsEndpoint.ENDPOINT_PREFIX
import com.highpowerbear.hpbanalytics.config.WsEndpoint.EXECUTION
import com.highpowerbear.hpbanalytics.config.WsEndpoint.STATISTICS
import com.highpowerbear.hpbanalytics.config.WsEndpoint.TRADE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import java.util.function.Consumer

@Configuration
open class WebConfig @Autowired constructor(private val webSocketHandler: WebSocketHandler) {

    @Bean
    open fun handlerMapping(): HandlerMapping? {
        val map: MutableMap<String, WebSocketHandler> = mutableMapOf()
        setOf(EXECUTION, TRADE, STATISTICS, CURRENT_STATISTICS)
            .forEach(Consumer { endpoint -> map["$ENDPOINT_PREFIX/$endpoint"] = webSocketHandler })

        return SimpleUrlHandlerMapping(map, 1)
    }

    @Bean
    open fun handlerAdapter(): WebSocketHandlerAdapter? {
        return WebSocketHandlerAdapter()
    }
}