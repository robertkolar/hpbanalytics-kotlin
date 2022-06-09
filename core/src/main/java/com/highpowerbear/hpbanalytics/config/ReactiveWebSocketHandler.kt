package com.highpowerbear.hpbanalytics.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType

@Component
class ReactiveWebSocketHandler @Autowired constructor(private val websocketSessionsMap: Map<String, MutableList<WebSocketSession>>) : WebSocketHandler {

    companion object {
        private val log = LoggerFactory.getLogger(ReactiveWebSocketHandler::class.java)
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        val endpoint = session.handshakeInfo.uri.path.replace(".*/".toRegex(), "")
        log.info("opened " + endpoint + "/" + session.id)
        websocketSessionsMap[endpoint]?.add(session)

        return session.receive()
            .doOnNext { message: WebSocketMessage -> log.info("received message for " + endpoint + "/" + session.id + ", message=" + message.payloadAsText) }
            .doFinally { sig: SignalType ->
                websocketSessionsMap[endpoint]?.remove(session)
                log.info("received " + sig + " for " + endpoint + "/" + session.id + ", sessions=" + websocketSessionsMap[endpoint]?.size)
            }
            .then()
    }
}