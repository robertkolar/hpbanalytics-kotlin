package com.highpowerbear.shared

import java.lang.IllegalStateException
import java.io.Serializable

/**
 * Created by robertk on 11/1/2020.
 */
class ExchangeRateDTO : Serializable {
    var date // yyyy-MM-dd
            : String = ""
    var eurUsd: Double = 0.0
    var eurGbp: Double = 0.0
    var eurChf: Double = 0.0
    var eurAud: Double = 0.0
    var eurJpy: Double = 0.0
    var eurKrw: Double = 0.0
    var eurHkd: Double = 0.0
    var eurSgd: Double = 0.0

    fun getRate(baseCurrency: String, transactionCurrency: String): Double {
        if ("EUR" == baseCurrency) {
            when (transactionCurrency) {
                "EUR" -> return 1.0
                "USD" -> return eurUsd
                "GBP" -> return eurGbp
                "CHF" -> return eurChf
                "AUD" -> return eurAud
                "JPY" -> return eurJpy
                "KRW" -> return eurKrw
                "HKD" -> return eurHkd
                "SGD" -> return eurSgd
            }
        }
        throw IllegalStateException("exchange rate not available for $baseCurrency/$transactionCurrency")
    }

    override fun toString(): String {
        return "ExchangeRateDTO{" +
                "date='" + date + '\'' +
                ", eurUsd=" + eurUsd +
                ", eurGbp=" + eurGbp +
                ", eurChf=" + eurChf +
                ", eurAud=" + eurAud +
                ", eurJpy=" + eurJpy +
                ", eurKrw=" + eurKrw +
                ", eurHkd=" + eurHkd +
                ", eurSgd=" + eurSgd +
                '}'
    }

    companion object {
        private const val serialVersionUID = 2177784790473590136L
    }
}