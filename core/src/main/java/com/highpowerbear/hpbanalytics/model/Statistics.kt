package com.highpowerbear.hpbanalytics.model

import java.io.Serializable
import java.time.LocalDateTime
import java.math.BigDecimal

/**
 *
 * Created by robertk on 4/26/2015.
 */
class Statistics : Serializable {
    var id = 0
    var periodDate: LocalDateTime? = null
    var numExecs = 0
    var numOpened = 0
    var numClosed = 0
    var numWinners = 0
    var numLosers = 0
    var pctWinners = 0.0
    var bigWinner: BigDecimal? = null
    var bigLoser: BigDecimal? = null
    var winnersProfit: BigDecimal? = null
    var losersLoss: BigDecimal? = null
    var timeValueBought: BigDecimal? = null
    var timeValueSold: BigDecimal? = null
    var timeValueSum: BigDecimal? = null
    var profitLoss: BigDecimal? = null
    var profitLossTaxReport: BigDecimal? = null
    var cumulProfitLoss: BigDecimal? = null

    fun setId(id: Int): Statistics {
        this.id = id
        return this
    }

    fun setPeriodDate(periodDate: LocalDateTime?): Statistics {
        this.periodDate = periodDate
        return this
    }

    fun setNumExecs(numExecs: Int): Statistics {
        this.numExecs = numExecs
        return this
    }

    fun setNumOpened(numOpened: Int): Statistics {
        this.numOpened = numOpened
        return this
    }

    fun setNumClosed(numClosed: Int): Statistics {
        this.numClosed = numClosed
        return this
    }

    fun setNumWinners(numWinners: Int): Statistics {
        this.numWinners = numWinners
        return this
    }

    fun setNumLosers(numLosers: Int): Statistics {
        this.numLosers = numLosers
        return this
    }

    fun setPctWinners(pctWinners: Double): Statistics {
        this.pctWinners = pctWinners
        return this
    }

    fun setBigWinner(bigWinner: BigDecimal?): Statistics {
        this.bigWinner = bigWinner
        return this
    }

    fun setBigLoser(bigLoser: BigDecimal?): Statistics {
        this.bigLoser = bigLoser
        return this
    }

    fun setWinnersProfit(winnersProfit: BigDecimal?): Statistics {
        this.winnersProfit = winnersProfit
        return this
    }

    fun setLosersLoss(losersLoss: BigDecimal?): Statistics {
        this.losersLoss = losersLoss
        return this
    }

    fun setTimeValueBought(timeValueBought: BigDecimal?): Statistics {
        this.timeValueBought = timeValueBought
        return this
    }

    fun setTimeValueSold(timeValueSold: BigDecimal?): Statistics {
        this.timeValueSold = timeValueSold
        return this
    }

    fun setTimeValueSum(timeValueSum: BigDecimal?): Statistics {
        this.timeValueSum = timeValueSum
        return this
    }

    fun setProfitLoss(profitLoss: BigDecimal?): Statistics {
        this.profitLoss = profitLoss
        return this
    }

    fun setProfitLossTaxReport(profitLossTaxReport: BigDecimal?): Statistics {
        this.profitLossTaxReport = profitLossTaxReport
        return this
    }

    fun setCumulProfitLoss(cumulProfitLoss: BigDecimal?): Statistics {
        this.cumulProfitLoss = cumulProfitLoss
        return this
    }

    override fun toString(): String {
        return "Statistics{" +
                "id=" + id +
                ", periodDate=" + periodDate +
                ", numExecs=" + numExecs +
                ", numOpened=" + numOpened +
                ", numClosed=" + numClosed +
                ", numWinners=" + numWinners +
                ", numLosers=" + numLosers +
                ", pctWinners=" + pctWinners +
                ", bigWinner=" + bigWinner +
                ", bigLoser=" + bigLoser +
                ", winnersProfit=" + winnersProfit +
                ", losersLoss=" + losersLoss +
                ", timeValueBought=" + timeValueBought +
                ", timeValueSold=" + timeValueSold +
                ", timeValueSum=" + timeValueSum +
                ", profitLoss=" + profitLoss +
                ", profitLossTaxReport=" + profitLossTaxReport +
                ", cumulProfitLoss=" + cumulProfitLoss +
                '}'
    }

    companion object {
        private const val serialVersionUID = 8464224239795026258L
    }
}