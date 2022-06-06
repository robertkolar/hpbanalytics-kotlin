package com.highpowerbear.hpbanalytics.service.helper

import com.highpowerbear.hpbanalytics.config.HanSettings
import com.highpowerbear.hpbanalytics.database.Execution
import com.highpowerbear.hpbanalytics.database.Trade
import com.highpowerbear.hpbanalytics.enums.Currency
import com.highpowerbear.hpbanalytics.service.ExchangeRateService
import com.ib.client.Types
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Created by robertk on 1/18/2022.
 */
@Component
class StatisticsHelper @Autowired constructor(private val exchangeRateService: ExchangeRateService) {
    fun statisticsKey(
        interval: ChronoUnit?,
        tradeType: String?,
        secType: String?,
        currency: String?,
        underlying: String?
    ): String {
        val intervalKey = interval?.name
        val tradeTypeKey = if (tradeType == null || HanSettings.ALL == tradeType) HanSettings.ALL else tradeType
        val secTypeKey = if (secType == null || HanSettings.ALL == secType) HanSettings.ALL else secType
        val currencyKey = if (currency == null || HanSettings.ALL == currency) HanSettings.ALL else currency
        val underlyingKey = underlying ?: HanSettings.ALL
        return (if (intervalKey != null) intervalKey + "_" else "") + tradeTypeKey + "_" + secTypeKey + "_" + currencyKey + "_" + underlyingKey
    }

    inline fun <reified T : Enum<T>> normalizeEnumParam(param: String?): T? {
        return if (param == null || HanSettings.ALL == param) null else enumValueOf<T>(param)
    }

    fun timeValueSum(executions: List<Execution>, action: Types.Action): BigDecimal {
        return executions
            .filter { e -> e.action == action }
            .map { e -> valueBase(e.timeValue, e.fillDate!!.toLocalDate(), e.currency) }
            .fold(BigDecimal.ZERO) { acc, next -> acc.add(next) }
    }

    private fun valueBase(value: BigDecimal, date: LocalDate?, currency: Currency?): BigDecimal {
        val exchangeRate = exchangeRateService.getExchangeRate(date!!, currency!!)
        return value.divide(exchangeRate, HanSettings.DECIMAL_SCALE, RoundingMode.HALF_UP)
    }

    fun firstDate(trades: List<Trade>): LocalDateTime {
        return trades
            .flatMap { t: Trade -> t.executions }
            .map { e: Execution -> e.fillDate }
            .minByOrNull { it!! } ?: LocalDateTime.MIN
    }

    fun lastDate(trades: List<Trade>): LocalDateTime {
        return trades
            .flatMap { t: Trade -> t.executions }
            .map { e: Execution -> e.fillDate }
            .maxByOrNull { it!! } ?: LocalDateTime.MIN

    }

    fun getTradesOpenedForPeriod(trades: List<Trade>, periodDate: LocalDateTime?, interval: ChronoUnit): List<Trade> {
        return trades
            .filter { t: Trade -> toBeginOfPeriod(t.openDate!!, interval).isEqual(periodDate) }
    }

    fun getTradesClosedForPeriod(trades: List<Trade>, periodDate: LocalDateTime?, interval: ChronoUnit): List<Trade> {
        return trades
            .filter { t: Trade -> t.closeDate != null}
            .filter { t: Trade -> toBeginOfPeriod(t.closeDate!!, interval).isEqual(periodDate) }
    }

    fun getExecutionsForPeriod(trades: List<Trade>, periodDate: LocalDateTime?, interval: ChronoUnit): List<Execution> {
        return trades
            .flatMap { it.executions }
            .filter { e: Execution -> toBeginOfPeriod(e.fillDate!!, interval).isEqual(periodDate) }
            .distinct()
    }

    fun toBeginOfPeriod(localDateTime: LocalDateTime, interval: ChronoUnit): LocalDateTime {
        var localDate = localDateTime.toLocalDate()
        if (ChronoUnit.YEARS == interval) {
            localDate = localDate.withDayOfYear(1)
        } else if (ChronoUnit.MONTHS == interval) {
            localDate = localDate.withDayOfMonth(1)
        } else check(ChronoUnit.DAYS == interval) { "unsupported statistics interval $interval" }
        return localDate.atStartOfDay()
    }
}