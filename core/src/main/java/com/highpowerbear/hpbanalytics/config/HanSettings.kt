package com.highpowerbear.hpbanalytics.config

import com.highpowerbear.hpbanalytics.enums.Currency

/**
 * Created by robertk on 5/29/2017.
 */
object HanSettings {
    const val SCHEDULED_THREAD_POOL_SIZE = 10
    const val IFI_START_YEAR = 2016
    const val DECIMAL_SCALE = 5
    const val NUMBER_OF_FUTURE_EXCHANGE_RATES = 3
    val PORTFOLIO_BASE_CURRENCY = Currency.EUR
    const val EXCHANGE_RATE_DATE_FORMAT = "yyyy-MM-dd"
    const val HAZELCAST_INSTANCE_NAME = "han-instance"
    const val HAZELCAST_EXECUTION_QUEUE = "executionQueue"
    const val HAZELCAST_EXECUTION_QUEUE_MAX_SZE = 1000
    const val HAZELCAST_CONSUMER_START_DELAY_SECONDS = 10
    const val HAZELCAST_EXCHANGE_RATE_MAP = "exchangeRateMap"
    const val HAZELCAST_STATISTICS_MAP = "statisticsMap"
    const val HAZELCAST_CURRENT_STATISTICS_MAP = "currentStatisticsMap"
    const val HAZELCAST_EXCHANGE_RATE_MAP_TIME_MAX_IDLE_SECONDS = 604800 // 1 week
    const val DB_DATABASE = "hpbanalytics"
    const val DB_SCHEMA = "hpbanalytics"
    const val WS_RELOAD_REQUEST_MESSAGE = "reloadRequest"
    const val ALL = "ALL"
}