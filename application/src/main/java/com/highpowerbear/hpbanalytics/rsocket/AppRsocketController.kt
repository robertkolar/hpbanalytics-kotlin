package com.highpowerbear.hpbanalytics.rsocket

import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class AppRsocketController {

    companion object {
        private val log = LoggerFactory.getLogger(AppRsocketController::class.java)
    }

    @MessageMapping("test")
    fun requestResponse(clientMessage: String): String {
        log.info("message from rsocket client: $clientMessage")

        val message = "server time " + LocalDateTime.now()
        log.info("sending rsocket message: $message")

        return message
    }
}