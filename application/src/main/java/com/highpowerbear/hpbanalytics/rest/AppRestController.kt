package com.highpowerbear.hpbanalytics.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.highpowerbear.hpbanalytics.config.ApplicationProperties
import com.highpowerbear.hpbanalytics.database.*
import com.highpowerbear.hpbanalytics.database.DataFilters.executionSpecification
import com.highpowerbear.hpbanalytics.database.DataFilters.tradeSpecification
import com.highpowerbear.hpbanalytics.enums.TradeStatus
import com.highpowerbear.hpbanalytics.enums.TradeType
import com.highpowerbear.hpbanalytics.model.DataFilterItem
import com.highpowerbear.hpbanalytics.model.Statistics
import com.highpowerbear.hpbanalytics.model.TradeStatistics
import com.highpowerbear.hpbanalytics.rest.model.CalculateStatisticsRequest
import com.highpowerbear.hpbanalytics.rest.model.CloseTradeRequest
import com.highpowerbear.hpbanalytics.rest.model.GenericList
import com.highpowerbear.hpbanalytics.service.AnalyticsService
import com.highpowerbear.hpbanalytics.service.StatisticsService
import com.highpowerbear.hpbanalytics.service.TaxReportService
import com.ib.client.Types
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.abs

/**
 * Created by robertk on 12/21/2017.
 */
@RestController
@RequestMapping("/")
class AppRestController @Autowired constructor(
    private val executionRepository: ExecutionRepository,
    private val tradeRepository: TradeRepository,
    private val statisticsService: StatisticsService,
    private val analyticsService: AnalyticsService,
    private val taxReportService: TaxReportService,
    private val applicationProperties: ApplicationProperties
) {
    private val objectMapper = ObjectMapper()

    @RequestMapping("execution")
    fun getFilteredExecutions(
        @RequestParam("page") page: Int,
        @RequestParam("limit") limit: Int,
        @RequestParam(required = false, value = "filter") jsonFilter: String?
    ): GenericList<Execution> {
        val pageable: Pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "fillDate"))
        val executions: MutableList<Execution?>
        val numExecutions: Long
        if (jsonFilter != null) {
            val filter = listOf(*objectMapper.readValue(jsonFilter, Array<DataFilterItem>::class.java))
            executions = executionRepository.findAll(executionSpecification(filter), pageable).content
            numExecutions = executionRepository.count(executionSpecification(filter))
        } else {
            executions = executionRepository.findAll(pageable).content
            numExecutions = executionRepository.count()
        }
        return GenericList(executions, numExecutions.toInt())
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["execution"])
    fun addExecution(@RequestBody execution: Execution) {
        execution.id = null
        analyticsService.addExecution(execution)
        statisticsService.calculateCurrentStatisticsOnExecution(execution)
    }

    @RequestMapping(method = [RequestMethod.DELETE], value = ["execution/{executionId}"])
    fun deleteExecution(@PathVariable("executionId") executionId: Long) {
        val execution = executionRepository.findById(executionId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "execution not found, id=$executionId")
        analyticsService.deleteExecution(executionId)
        statisticsService.calculateCurrentStatisticsOnExecution(execution)
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["trade/regenerate-all"])
    fun regenerateAllTrades() {
        analyticsService.regenerateAllTrades()
    }

    @RequestMapping("trade")
    fun getFilteredTrades(
        @RequestParam("page") page: Int,
        @RequestParam("limit") limit: Int,
        @RequestParam(required = false, value = "filter") jsonFilter: String?
    ): GenericList<Trade> {
        val pageable: Pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "openDate"))
        val trades: MutableList<Trade?>
        val numTrades: Long
        if (jsonFilter != null) {
            val filter = listOf(*objectMapper.readValue(jsonFilter, Array<DataFilterItem>::class.java))
            trades = tradeRepository.findAll(tradeSpecification(filter), pageable).content
            numTrades = tradeRepository.count(tradeSpecification(filter))
        } else {
            trades = tradeRepository.findAll(pageable).content
            numTrades = tradeRepository.count()
        }
        return GenericList(trades, numTrades.toInt())
    }

    @RequestMapping("trade/statistics")
    fun tradeStatistics(): TradeStatistics {
        return analyticsService.tradeStatistics
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["trade/{tradeId}/close"])
    fun manualCloseTrade(
        @PathVariable("tradeId") tradeId: Long,
        @RequestBody r: CloseTradeRequest
    ) {
        val trade = tradeRepository.findById(tradeId).orElse(null)
        if (trade == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "trade not found, id=$tradeId")
        } else if (TradeStatus.OPEN != trade.status) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "trade not open, id=$tradeId")
        }

        val execution: Execution = Execution().apply {
            reference = r.executionReference
            action = if (trade.type == TradeType.LONG) Types.Action.SELL else Types.Action.BUY
            quantity = abs(trade.openPosition!!)
            symbol = trade.symbol
            underlying = trade.underlying
            currency = trade.currency
            secType = trade.secType
            multiplier = trade.multiplier
            fillDate = r.closeDate
            fillPrice = r.closePrice
        }
        analyticsService.addExecution(execution)
        statisticsService.calculateCurrentStatisticsOnExecution(execution)
    }

    @RequestMapping("statistics")
    fun getStatistics(
        @RequestParam("interval") interval: ChronoUnit?,
        @RequestParam(required = false, value = "tradeType") tradeType: String?,
        @RequestParam(required = false, value = "secType") secType: String?,
        @RequestParam(required = false, value = "currency") currency: String?,
        @RequestParam(required = false, value = "underlying") underlying: String?,
        @RequestParam("start") start: Int,
        @RequestParam("limit") limit: Int
    ): GenericList<Statistics?> {
        val items: MutableList<Statistics> =
            statisticsService.getStatistics(interval, tradeType, secType, currency, underlying, null)
        items.reverse()
        val total = items.size
        return GenericList(page(items, start, limit, total), total)
    }

    @RequestMapping("statistics/current")
    fun getCurrentStatistics(
        @RequestParam(required = false, value = "tradeType") tradeType: String?,
        @RequestParam(required = false, value = "secType") secType: String?,
        @RequestParam(required = false, value = "currency") currency: String?,
        @RequestParam(required = false, value = "underlying") underlying: String?,
        @RequestParam("start") start: Int,
        @RequestParam("limit") limit: Int
    ): GenericList<Statistics> {
        val items = statisticsService.getCurrentStatistics(tradeType, secType, currency, underlying)
        val total = items.size
        return GenericList(page(items, start, limit, total), total)
    }

    @RequestMapping("statistics/underlyings")
    fun getUnderlyings(@RequestParam(required = false, value = "openOnly") openOnly: Boolean): List<String> {
        val underlyings: List<String>
        val underlyingsExtended: MutableList<String> = mutableListOf()

        if (openOnly) {
            underlyings = tradeRepository.findOpenUnderlyings()
            underlyingsExtended.addAll(underlyings)
            applicationProperties.underlyingsPermanent
                .filter { up -> !underlyings.contains(up) }
                .forEach { up -> underlyingsExtended.add(up) }
        } else {
            underlyings = tradeRepository.findAllUnderlyings()
        }
        return underlyings
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["statistics"])
    fun calculateStatistics(@RequestBody r: CalculateStatisticsRequest) {
        statisticsService.calculateStatistics(r.interval!!, r.tradeType!!, r.secType!!, r.currency!!, r.underlying!!)
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["statistics/current"])
    fun calculateCurrentStatistics(@RequestBody r: CalculateStatisticsRequest) {
        statisticsService.calculateCurrentStatistics(r.tradeType!!, r.secType!!, r.currency!!, r.underlying!!, true)
    }

    @RequestMapping("statistics/charts")
    fun getCharts(
        @RequestParam("interval") interval: ChronoUnit?,
        @RequestParam(required = false, value = "tradeType") tradeType: String?,
        @RequestParam(required = false, value = "secType") secType: String?,
        @RequestParam(required = false, value = "currency") currency: String?,
        @RequestParam(required = false, value = "underlying") underlying: String?
    ): GenericList<Statistics> {
        val statistics = statisticsService.getStatistics(interval, tradeType, secType, currency, underlying, 120)
        return GenericList(statistics, statistics.size)
    }

    @RequestMapping("statistics/ifi/years")
    fun ifiYears(): IntRange {
        return taxReportService.ifiYears
    }

    @RequestMapping("statistics/ifi/csv")
    fun getIfiCsv(
        @RequestParam("year") year: Int,
        @RequestParam("endMonth") endMonth: Int,
        @RequestParam("tradeType") tradeType: TradeType?
    ): String {
        return taxReportService.generate(year, endMonth, tradeType!!)
    }

    private fun <T> page(items: List<T>, start: Int, limit: Int, total: Int): List<T> {
        val itemsPaged: List<T> = if (items.isNotEmpty()) {
            val fromIndex = start.coerceAtMost(total - 1)
            val toIndex = (fromIndex + limit).coerceAtMost(total)
            items.subList(fromIndex, toIndex)
        } else {
            emptyList()
        }
        return itemsPaged
    }
}