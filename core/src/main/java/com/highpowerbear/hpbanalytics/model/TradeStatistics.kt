package com.highpowerbear.hpbanalytics.model

/**
 * Created by robertk on 10/25/2020.
 */
class TradeStatistics {
    var numAllTrades: Long = 0
    var numAllUnderlyings: Long = 0
    var numOpenTrades: Long = 0
    var numOpenUnderlyings: Long = 0

    fun setNumAllTrades(numAllTrades: Long): TradeStatistics {
        this.numAllTrades = numAllTrades
        return this
    }

    fun setNumAllUnderlyings(numAllUnderlyings: Long): TradeStatistics {
        this.numAllUnderlyings = numAllUnderlyings
        return this
    }

    fun setNumOpenTrades(numOpenTrades: Long): TradeStatistics {
        this.numOpenTrades = numOpenTrades
        return this
    }

    fun setNumOpenUnderlyings(numOpenUnderlyings: Long): TradeStatistics {
        this.numOpenUnderlyings = numOpenUnderlyings
        return this
    }
}