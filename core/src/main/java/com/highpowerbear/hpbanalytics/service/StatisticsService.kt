package com.highpowerbear.hpbanalytics.service

import com.highpowerbear.hpbanalytics.common.HanUtil
import com.highpowerbear.hpbanalytics.config.HanSettings
import com.highpowerbear.hpbanalytics.config.WsEndpoint
import com.highpowerbear.hpbanalytics.database.DataFilters
import com.highpowerbear.hpbanalytics.database.Execution
import com.highpowerbear.hpbanalytics.database.Trade
import com.highpowerbear.hpbanalytics.database.TradeRepository
import com.highpowerbear.hpbanalytics.enums.Currency
import com.highpowerbear.hpbanalytics.enums.TradeType
import com.highpowerbear.hpbanalytics.model.Statistics
import com.highpowerbear.hpbanalytics.service.helper.StatisticsHelper
import com.ib.client.Types
import com.ib.client.Types.SecType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ScheduledExecutorService

/**
 * Created by robertk on 4/26/2015.
 */
@Service
class StatisticsService @Autowired constructor(private val tradeRepository: TradeRepository,
                                               private val messageService: MessageService,
                                               private val tradeCalculationService: TradeCalculationService,
                                               private val helper: StatisticsHelper,
                                               private val executorService: ScheduledExecutorService,
                                               private val statisticsMap: MutableMap<String, List<Statistics>>,
                                               private val currentStatisticsMap: MutableMap<String, List<Statistics>>) {

    fun getStatistics(interval: ChronoUnit?, tradeType: String?, secType: String?, currency: String?, underlying: String?, maxPoints: Int?): MutableList<Statistics> {
        val maxPointsMod: Int
        val statisticsList = statisticsMap[helper.statisticsKey(interval, tradeType, secType, currency, underlying)]
                ?: return mutableListOf()
        val size = statisticsList.size
        maxPointsMod = if (maxPoints == null || size < maxPoints) {
            size
        } else {
            maxPoints
        }
        val firstIndex = size - maxPointsMod
        return ArrayList(statisticsList.subList(firstIndex, size)) // copy because reverse will be performed on it
    }

    fun getCurrentStatistics(tradeType: String?, secType: String?, currency: String?, underlying: String?): List<Statistics> {
        val currentStatisticsList = currentStatisticsMap[helper.statisticsKey(null, tradeType, secType, currency, underlying)]
        return Objects.requireNonNullElse(currentStatisticsList, emptyList())
    }

    fun calculateStatistics(interval: ChronoUnit, tradeType: String, secType: String, currency: String, underlying: String) {
        executorService.execute {
            // execute in a new thread
            log.info("BEGIN statistics calculation for interval=$interval, tradeType=$tradeType, secType=$secType, currency=$currency, undl=$underlying")
            val tradeSpecification = DataFilters.tradeSpecification(
                    helper.normalizeEnumParam<TradeType>(tradeType),
                    helper.normalizeEnumParam<SecType>(secType),
                    helper.normalizeEnumParam<Currency>(currency),
                    if (HanSettings.ALL == underlying) null else underlying,
                    null)
            val trades = tradeRepository.findAll(tradeSpecification, Sort.by(Sort.Direction.ASC, "openDate")).filterNotNull().toMutableList()
            log.info("found " + trades.size + " trades matching the filter criteria, calculating statistics...")
            val statisticsList = calculate(trades, interval)
            statisticsMap[helper.statisticsKey(interval, tradeType, secType, currency, underlying)] = statisticsList
            log.info("END statistics calculation for interval=" + interval + ", included " + trades.size + " trades")
            messageService.sendWsReloadRequestMessage(WsEndpoint.STATISTICS)
        }
    }

    fun calculateCurrentStatistics(tradeType: String, secType: String, currency: String, underlying: String?, reload: Boolean) {
        log.info("BEGIN current statistics calculation for tradeType=$tradeType, secType=$secType, currency=$currency, undl=$underlying")
        val cutoffDate = helper.toBeginOfPeriod(LocalDateTime.now(), ChronoUnit.YEARS)
        val tradeSpecification = DataFilters.tradeSpecification(
                helper.normalizeEnumParam<TradeType>(tradeType),
                helper.normalizeEnumParam<SecType>(secType),
                helper.normalizeEnumParam<Currency>(currency),
                if (HanSettings.ALL == underlying) null else underlying,
                cutoffDate
        )
        val trades = tradeRepository.findAll(tradeSpecification, Sort.by(Sort.Direction.ASC, "openDate")).filterNotNull().toMutableList()
        val daily = calculateCurrent(trades, ChronoUnit.DAYS)
        val monthly = calculateCurrent(trades, ChronoUnit.MONTHS)
        val yearly = calculateCurrent(trades, ChronoUnit.YEARS)
        currentStatisticsMap[helper.statisticsKey(null, tradeType, secType, currency, underlying)] = java.util.List.of(daily, monthly, yearly)
        log.info("END current statistics calculation, included " + trades.size + " trades")
        if (reload) {
            messageService.sendWsReloadRequestMessage(WsEndpoint.CURRENT_STATISTICS)
        }
    }

    fun calculateCurrentStatisticsOnExecution(execution: Execution) {
        executorService.execute {
            val all = HanSettings.ALL
            val secType = execution.secType!!.name
            val undl = execution.underlying
            listOf(all, secType).forEach { st: String ->
                listOf(all, undl).forEach { u: String? -> calculateCurrentStatistics(all, st, all, u, false) }
            }
            messageService.sendWsReloadRequestMessage(WsEndpoint.CURRENT_STATISTICS)
        }
    }

    private fun calculate(trades: MutableList<Trade>, interval: ChronoUnit): List<Statistics> {
        val statisticsList: MutableList<Statistics> = ArrayList()
        if (trades.isEmpty()) {
            return statisticsList
        }
        val firstPeriodDate = helper.toBeginOfPeriod(helper.firstDate(trades), interval)
        val lastPeriodDate = helper.toBeginOfPeriod(helper.lastDate(trades), interval)
        var periodDate = firstPeriodDate
        var cumulProfitLoss = BigDecimal.ZERO
        var statsCount = 1
        while (!periodDate.isAfter(lastPeriodDate)) {
            val statistics = calculatePeriod(trades, interval, periodDate)
            cumulProfitLoss = cumulProfitLoss.add(statistics.profitLoss)
            statistics.id = statsCount++
            statistics.cumulProfitLoss = cumulProfitLoss
            statisticsList.add(statistics)
            periodDate = periodDate.plus(1, interval)
        }
        return statisticsList
    }

    private fun calculateCurrent(trades: MutableList<Trade>, interval: ChronoUnit): Statistics {
        val statistics = calculatePeriod(trades, interval, helper.toBeginOfPeriod(LocalDateTime.now(), interval))
        val statisticsId = if (interval == ChronoUnit.DAYS) 1 else if (interval == ChronoUnit.MONTHS) 2 else 3
        return statistics.apply {
            id = statisticsId
            cumulProfitLoss = statistics.profitLoss
        }
    }

    private fun calculatePeriod(trades: MutableList<Trade>, interval: ChronoUnit, periodDate: LocalDateTime): Statistics {
        if (trades.isEmpty()) {
            return Statistics()
        }
        val tradesOpenedForPeriod = helper.getTradesOpenedForPeriod(trades, periodDate, interval)
        val tradesClosedForPeriod = helper.getTradesClosedForPeriod(trades, periodDate, interval)
        val executionsForPeriod = helper.getExecutionsForPeriod(trades, periodDate, interval)
        var numWinners = 0
        var numLosers = 0
        var bigWinner = BigDecimal.ZERO
        var bigLoser = BigDecimal.ZERO
        var winnersProfit = BigDecimal.ZERO
        var losersLoss = BigDecimal.ZERO
        var profitLoss = BigDecimal.ZERO
        var profitLossTaxReport = BigDecimal.ZERO
        for (trade in tradesClosedForPeriod) {
            val pl = tradeCalculationService.calculatePlPortfolioBaseCloseOnly(trade)
            profitLoss = profitLoss.add(pl)
            if (HanUtil.isDerivative(trade.secType)) {
                val plTr = tradeCalculationService.calculatePlPortfolioBaseOpenClose(trade)
                profitLossTaxReport = profitLossTaxReport.add(plTr)
            }
            if (pl.compareTo(BigDecimal.ZERO) > 0) {
                numWinners++
                winnersProfit = winnersProfit.add(pl)
                if (pl.compareTo(bigWinner) > 0) {
                    bigWinner = pl
                }
            } else {
                numLosers++
                losersLoss = losersLoss.add(pl.negate())
                if (pl.compareTo(bigLoser) < 0) {
                    bigLoser = pl
                }
            }
        }
        val pctWinners = if (!tradesClosedForPeriod.isEmpty()) numWinners.toDouble() / tradesClosedForPeriod.size.toDouble() * 100.0 else 0.0
        val timeValueBought = helper.timeValueSum(executionsForPeriod, Types.Action.BUY)
        val timeValueSold = helper.timeValueSum(executionsForPeriod, Types.Action.SELL)
        val timeValueSum = timeValueBought.add(timeValueSold)
        return Statistics().apply {
            this.periodDate = periodDate
            this.numExecs = executionsForPeriod.size
            this.numOpened = tradesOpenedForPeriod.size
            this.numClosed = tradesClosedForPeriod.size
            this.numWinners = numWinners
            this.numLosers = numLosers
            this.pctWinners = HanUtil.round2(pctWinners)
            this.bigWinner = bigWinner
            this.bigLoser = bigLoser.negate()
            this.winnersProfit = winnersProfit
            this.losersLoss = losersLoss
            this.timeValueBought = timeValueBought
            this.timeValueSold = timeValueSold
            this.timeValueSum = timeValueSum
            this.profitLoss = profitLoss
            this.profitLossTaxReport = profitLossTaxReport
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(StatisticsService::class.java)
    }
}