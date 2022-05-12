package com.highpowerbear.hpbanalytics.config

/**
 * Created by robertk on 4/5/2020.
 */
object WsTopic {
    // topics
    const val EXECUTION = "execution"
    const val TRADE = "trade"
    const val STATISTICS = "statistics"
    const val CURRENT_STATISTICS = "current-statistics"

    // settings
    const val TOPIC_PREFIX = "/topic"
    const val APPLICATION_PREFIX = "/app"
    const val STOMP_ENDPOINT = "/websocket"
}