package com.highpowerbear.hpbanalytics.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import com.highpowerbear.hpbanalytics.config.WsTopic
import com.highpowerbear.hpbanalytics.config.HanSettings
import org.springframework.stereotype.Service

/**
 * Created by robertk on 12/27/2017.
 */
@Service
class MessageService @Autowired constructor(private val simpMessagingTemplate: SimpMessagingTemplate) {
    fun sendWsReloadRequestMessage(topic: String) {
        simpMessagingTemplate.convertAndSend(WsTopic.TOPIC_PREFIX + "/" + topic, HanSettings.WS_RELOAD_REQUEST_MESSAGE)
    }
}