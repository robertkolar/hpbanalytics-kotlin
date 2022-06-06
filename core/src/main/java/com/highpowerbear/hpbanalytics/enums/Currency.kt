package com.highpowerbear.hpbanalytics.enums

/**
 * Created by robertk on 11/18/2017.
 */
enum class Currency {
    EUR, USD, AUD, GBP, CHF, JPY, KRW, HKD, SGD;

    companion object {
        fun findByValue(value: String): Currency? {
            var result: Currency? = null
            for (currency in values()) {
                if (currency.name.equals(value, ignoreCase = true)) {
                    result = currency
                    break
                }
            }
            return result
        }
    }
}