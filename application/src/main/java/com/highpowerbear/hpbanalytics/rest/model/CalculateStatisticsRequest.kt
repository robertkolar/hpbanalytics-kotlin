package com.highpowerbear.hpbanalytics.rest.model

import java.time.temporal.ChronoUnit

/**
 * Created by robertk on 8/25/2020.
 */
class CalculateStatisticsRequest {
    val interval: ChronoUnit? = null
    val tradeType: String? = null
    val secType: String? = null
    val currency: String? = null
    val underlying: String? = null
}