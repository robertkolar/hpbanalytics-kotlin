package com.highpowerbear.hpbanalytics.rest.model

import java.time.LocalDateTime
import java.math.BigDecimal

/**
 * Created by robertk on 4/9/2018.
 */
class CloseTradeRequest {
    val executionReference: String? = null
    val closeDate: LocalDateTime = LocalDateTime.MIN
    val closePrice: BigDecimal = BigDecimal.ZERO
}