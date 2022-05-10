package com.highpowerbear.hpbanalytics.service

import com.highpowerbear.hpbanalytics.config.HanSettings
import com.highpowerbear.hpbanalytics.database.Execution
import com.highpowerbear.hpbanalytics.database.Trade
import com.highpowerbear.hpbanalytics.enums.TradeStatus
import com.highpowerbear.hpbanalytics.enums.TradeType
import com.ib.client.Types
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Created by robertk on 12/25/2017.
 */
@Service
class TradeCalculationService @Autowired constructor(private val exchangeRateService: ExchangeRateService) {
    fun calculateFields(trade: Trade) {
        val firstExecution = trade.executions[0]
        val lastExecution = trade.executions[trade.executions.size - 1]
        trade.setType(if (firstExecution.action == Types.Action.BUY) TradeType.LONG else TradeType.SHORT)
                .setSymbol(firstExecution.symbol)
                .setUnderlying(firstExecution.underlying)
                .setCurrency(firstExecution.currency)
                .setSecType(firstExecution.secType).multiplier = firstExecution.multiplier
        val tradeType = trade.type
        var cumulativeOpenPrice = BigDecimal.ZERO
        var cumulativeClosePrice = BigDecimal.ZERO
        var openPosition = 0.0
        var cumulativeQuantity = 0.0
        for (execution in trade.executions) {
            val action = execution.action
            val quantity = execution.quantity
            val fillPrice = execution.fillPrice
            openPosition += if (action == Types.Action.BUY) quantity else -quantity
            if (tradeType == TradeType.LONG && action == Types.Action.BUY || tradeType == TradeType.SHORT && action == Types.Action.SELL) {
                cumulativeQuantity += quantity
                cumulativeOpenPrice = cumulativeOpenPrice.add(BigDecimal.valueOf(quantity).multiply(fillPrice))
            } else if (tradeType == TradeType.LONG && action == Types.Action.SELL || tradeType == TradeType.SHORT && action == Types.Action.BUY) {
                cumulativeClosePrice = cumulativeClosePrice.add(BigDecimal.valueOf(quantity).multiply(fillPrice))
            }
        }
        val avgOpenPrice = cumulativeOpenPrice.divide(BigDecimal.valueOf(cumulativeQuantity), RoundingMode.HALF_UP)
        trade.setOpenPosition(openPosition)
                .setStatus(if (openPosition != 0.0) TradeStatus.OPEN else TradeStatus.CLOSED)
                .setCumulativeQuantity(cumulativeQuantity)
                .setOpenDate(firstExecution.fillDate).avgOpenPrice = avgOpenPrice
        if (trade.status == TradeStatus.CLOSED) {
            val avgClosePrice = cumulativeClosePrice.divide(BigDecimal.valueOf(cumulativeQuantity), RoundingMode.HALF_UP)
            trade.setAvgClosePrice(avgClosePrice).closeDate = lastExecution.fillDate
            val cumulativePriceDiff = if (TradeType.LONG == tradeType) cumulativeClosePrice.subtract(cumulativeOpenPrice) else cumulativeOpenPrice.subtract(cumulativeClosePrice)
            val profitLoss = cumulativePriceDiff.multiply(BigDecimal.valueOf(trade.multiplier))
            trade.profitLoss = profitLoss
        }
        val timeValueSum = trade.executions.stream()
                .map { obj: Execution -> obj.timeValue }
                .filter { obj: BigDecimal? -> Objects.nonNull(obj) }
                .reduce(BigDecimal.ZERO) { obj: BigDecimal, augend: BigDecimal? -> obj.add(augend) }
        trade.timeValueSum = timeValueSum
    }

    fun calculatePlPortfolioBaseOpenClose(trade: Trade): BigDecimal {
        require(TradeStatus.CLOSED == trade.status) { "cannot calculate pl, trade not closed $trade" }
        val tradeType = trade.type
        var cumulativeOpenPrice = BigDecimal.ZERO
        var cumulativeClosePrice = BigDecimal.ZERO
        for (execution in trade.executions) {
            val action = execution.action
            val quantity = execution.quantity
            val date = execution.fillDate.toLocalDate()
            val currency = execution.currency
            val exchangeRate = exchangeRateService.getExchangeRate(date, currency)
            val fillPriceBase = execution.fillPrice.divide(exchangeRate, HanSettings.DECIMAL_SCALE, RoundingMode.HALF_UP)
            if (tradeType == TradeType.LONG && action == Types.Action.BUY || tradeType == TradeType.SHORT && action == Types.Action.SELL) {
                cumulativeOpenPrice = cumulativeOpenPrice.add(BigDecimal.valueOf(quantity).multiply(fillPriceBase))
            } else if (tradeType == TradeType.LONG && action == Types.Action.SELL || tradeType == TradeType.SHORT && action == Types.Action.BUY) {
                cumulativeClosePrice = cumulativeClosePrice.add(BigDecimal.valueOf(quantity).multiply(fillPriceBase))
            }
        }
        val cumulativeDiff: BigDecimal = if (TradeType.LONG == tradeType) {
            cumulativeClosePrice.subtract(cumulativeOpenPrice)
        } else {
            cumulativeOpenPrice.subtract(cumulativeClosePrice)
        }
        return cumulativeDiff.multiply(BigDecimal.valueOf(trade.multiplier))
    }

    fun calculatePlPortfolioBaseCloseOnly(trade: Trade): BigDecimal {
        require(TradeStatus.CLOSED == trade.status) { "cannot calculate pl, trade not closed $trade" }
        val date = trade.closeDate.toLocalDate()
        val currency = trade.currency
        val exchangeRate = exchangeRateService.getExchangeRate(date, currency)
        return trade.profitLoss.divide(exchangeRate, HanSettings.DECIMAL_SCALE, RoundingMode.HALF_UP)
    }
}