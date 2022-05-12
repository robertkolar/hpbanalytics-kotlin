package com.highpowerbear.hpbanalytics.common

import java.time.format.DateTimeFormatter
import com.highpowerbear.hpbanalytics.config.HanSettings
import java.time.LocalDate
import com.ib.client.Types.SecType

/**
 * Created by robertk on 5/29/2017.
 */
object HanUtil {
    private val exchangeRateDateFormatter = DateTimeFormatter.ofPattern(HanSettings.EXCHANGE_RATE_DATE_FORMAT)
    fun toDurationString(durationSeconds: Long): String {
        val days = durationSeconds / (24 * 60 * 60)
        val daysRemainder = durationSeconds % (24 * 60 * 60)
        val hours = daysRemainder / (60 * 60)
        val hoursRemainder = daysRemainder % (60 * 60)
        val minutes = hoursRemainder / 60
        val seconds = hoursRemainder % 60
        val h = String.format("%02d", hours)
        val m = String.format("%02d", minutes)
        val s = String.format("%02d", seconds)
        return days.toString() + "d " + h + ":" + m + ":" + s
    }

    fun round(number: Double, decimalPlaces: Int): Double {
        val modifier = Math.pow(10.0, decimalPlaces.toDouble())
        return Math.round(number * modifier) / modifier
    }

    fun round2(number: Double): Double {
        return round(number, 2)
    }

    fun formatExchangeRateDate(localDate: LocalDate): String {
        return localDate.format(exchangeRateDateFormatter)
    }

    fun removeWhiteSpaces(input: String): String {
        return input.replace("\\s".toRegex(), "")
    }

    fun isDerivative(secType: SecType?): Boolean {
        return when (secType) {
            SecType.FUT, SecType.OPT, SecType.FOP, SecType.CFD -> true
            else -> false
        }
    }
}