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
    var bigWinner: BigDecimal = BigDecimal.ZERO
    var bigLoser: BigDecimal = BigDecimal.ZERO
    var winnersProfit: BigDecimal = BigDecimal.ZERO
    var losersLoss: BigDecimal = BigDecimal.ZERO
    var timeValueBought: BigDecimal = BigDecimal.ZERO
    var timeValueSold: BigDecimal = BigDecimal.ZERO
    var timeValueSum: BigDecimal = BigDecimal.ZERO
    var profitLoss: BigDecimal = BigDecimal.ZERO
    var profitLossTaxReport: BigDecimal = BigDecimal.ZERO
    var cumulProfitLoss: BigDecimal = BigDecimal.ZERO

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