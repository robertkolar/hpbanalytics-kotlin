package com.highpowerbear.hpbanalytics.config

import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.StompEndpointRegistry

/**
 * Created by robertk on 4/5/2020.
 */
@Configuration
@EnableWebSocketMessageBroker
open class WebsocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker(WsTopic.TOPIC_PREFIX)
        config.setApplicationDestinationPrefixes(WsTopic.APPLICATION_PREFIX)
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint(WsTopic.STOMP_ENDPOINT).withSockJS()
    }
}