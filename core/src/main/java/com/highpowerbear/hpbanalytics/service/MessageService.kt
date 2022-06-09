package com.highpowerbear.hpbanalytics.service

import com.highpowerbear.hpbanalytics.config.HanSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

/**
 * Created by robertk on 12/27/2017.
 */
@Service
class MessageService @Autowired constructor(private val websocketSessionsMap: Map<String, List<WebSocketSession>>) {

    fun sendWsReloadRequestMessage(endpoint: String) {
        sendWsMessage(endpoint, HanSettings.WS_RELOAD_REQUEST_MESSAGE)
    }

    private fun sendWsMessage(endpoint: String, content: String) {
        websocketSessionsMap[endpoint]
            ?.forEach{ session ->
                val message = session.textMessage(content)
                val monoMessage = Mono.just(message)
                session.send(monoMessage).subscribe()
            }
    }
}