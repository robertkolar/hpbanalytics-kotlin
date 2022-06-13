package com.highpowerbear.hpbanalytics.service

import com.highpowerbear.hpbanalytics.common.HanUtil
import com.highpowerbear.hpbanalytics.config.HanSettings
import com.highpowerbear.hpbanalytics.database.Execution
import com.highpowerbear.hpbanalytics.database.Trade
import com.highpowerbear.hpbanalytics.database.TradeRepository
import com.highpowerbear.hpbanalytics.enums.Currency
import com.highpowerbear.hpbanalytics.enums.TradeType
import com.ib.client.Types
import com.ib.client.Types.SecType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by robertk on 10/10/2016.
 */
@Service
class TaxReportService @Autowired constructor(
    private val exchangeRateService: ExchangeRateService,
    private val tradeRepository: TradeRepository,
    private val tradeCalculationService: TradeCalculationService) {

    private val NL = "\n"
    private val DL = ","
    private val acquireType = "A - nakup"
    private val secTypeMap: MutableMap<SecType, String> = EnumMap(SecType::class.java)
    private val tradeTypeMap: MutableMap<TradeType, String> = EnumMap(TradeType::class.java)
    private val dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val nf = NumberFormat.getInstance(Locale.US)

    init {
        setup()
    }

    private fun setup() {
        secTypeMap[SecType.FUT] = "01 - terminska pogodba"
        secTypeMap[SecType.CFD] = "02 - pogodba na razliko"
        secTypeMap[SecType.OPT] = "03 - opcija"
        secTypeMap[SecType.FOP] = "03 - opcija"
        tradeTypeMap[TradeType.LONG] = "običajni"
        tradeTypeMap[TradeType.SHORT] = "na kratko"
        nf.minimumFractionDigits = HanSettings.DECIMAL_SCALE
        nf.maximumFractionDigits = HanSettings.DECIMAL_SCALE
        nf.isGroupingUsed = false
    }

    fun generate(year: Int, endMonth: Int, tradeType: TradeType): String {
        log.info("BEGIN IfiCsvGenerator.generate year=$year, endMonth=$endMonth, tradeType=$tradeType")
        val beginDate = LocalDate.ofYearDay(year, 1).atStartOfDay()
        val endDate = YearMonth.of(year, endMonth).atEndOfMonth().plusDays(1).atStartOfDay()
        val trades = tradeRepository.findByTypeAndCloseDateBetweenOrderByOpenDateAsc(tradeType, beginDate, endDate)
        log.info("beginDate=" + beginDate + ", endDate=" + endDate + ", trades=" + trades.size)
        val sb = StringBuilder()
        if (TradeType.SHORT == tradeType) {
            writeCsvHeaderShort(sb)
        } else if (TradeType.LONG == tradeType) {
            writeCsvHeaderLong(sb)
        }
        var tCount = 0
        var sumPl = BigDecimal.ZERO
        for (trade in trades) {
            if (!HanUtil.isDerivative(trade.secType)) {
                continue
            }
            var tradePl = BigDecimal.ZERO
            tCount++
            writeTrade(sb, trade, tCount)
            var eCount = 0
            var currentPos = 0.0
            for (execution in trade.executions) {
                val action = execution.action
                currentPos += if (action == Types.Action.BUY) execution.quantity!! else -execution.quantity!!
                eCount++
                if (TradeType.SHORT == tradeType && Types.Action.SELL == action) {
                    writeTradeShortExecutionSell(sb, execution, tCount, eCount)
                } else if (TradeType.SHORT == tradeType && Types.Action.BUY == action) {
                    val pl = writeTradeShortExecutionBuy(sb, trade, execution, currentPos, tCount, eCount)
                    if (pl != null) {
                        tradePl = pl
                    }
                } else if (TradeType.LONG == tradeType && Types.Action.BUY == action) {
                    writeTradeLongExecutionBuy(sb, execution, tCount, eCount)
                } else if (TradeType.LONG == tradeType && Types.Action.SELL == action) {
                    val pl = writeTradeLongExecutionSell(sb, trade, execution, currentPos, tCount, eCount)
                    if (pl != null) {
                        tradePl = pl
                    }
                }
            }
            sumPl = sumPl.add(tradePl)
            sb.append(NL)
        }
        sb.append(NL).append("SKUPAJ")
        sb.append(DL.repeat(14))
        sb.append(nf.format(sumPl))
        log.info("END IfiCsvGenerator.generate year=$year, tradeType=$tradeType")
        return sb.toString()
    }

    private fun writeCsvHeaderShort(sb: StringBuilder) {
        sb.append("Zap. št.").append(DL)
                .append("Vrsta IFI").append(DL)
                .append("Vrsta posla").append(DL)
                .append("Trgovalna koda").append(DL)
                .append("Datum odsvojitve").append(DL)
                .append("Količina odsvojenega IFI").append(DL)
                .append("Vrednost ob odsvojitvi (na enoto) USD").append(DL)
                .append("Vrednost ob odsvojitvi (na enoto) EUR").append(DL)
                .append("Datum pridobitve").append(DL)
                .append("Način pridobitve").append(DL)
                .append("Količina").append(DL)
                .append("Vrednost ob pridobitvi na enoto) USD").append(DL)
                .append("Vrednost ob pridobitvi (na enoto) EUR").append(DL)
                .append("Zaloga IFI").append(DL)
                .append("Dobiček Izguba EUR").append(NL)
    }

    private fun writeCsvHeaderLong(sb: StringBuilder) {
        sb.append("Zap. št.").append(DL)
                .append("Vrsta IFI").append(DL)
                .append("Vrsta posla").append(DL)
                .append("Trgovalna koda").append(DL)
                .append("Datum pridobitve").append(DL)
                .append("Način pridobitve").append(DL)
                .append("Količina").append(DL)
                .append("Nabavna vrednost ob pridobitvi (na enoto) USD").append(DL)
                .append("Nabavna vrednost ob pridobitvi (na enoto) EUR").append(DL)
                .append("Datum odsvojitve").append(DL)
                .append("Količina odsvojenega IFI").append(DL)
                .append("Vrednost ob odsvojitvi (na enoto) USD").append(DL)
                .append("Vrednost ob odsvojitvi (na enoto) EUR").append(DL)
                .append("Zaloga IFI").append(DL)
                .append("Dobiček Izguba EUR").append(NL)
    }

    private fun writeTrade(sb: StringBuilder, trade: Trade, tCount: Int) {
        sb.append(tCount).append(DL)
                .append(secTypeMap[trade.secType]).append(DL)
                .append(tradeTypeMap[trade.type]).append(DL)
                .append(trade.symbol).append(DL)
        sb.append(DL.repeat(10))
        sb.append(NL)
    }

    private fun writeTradeShortExecutionSell(sb: StringBuilder, execution: Execution, tCount: Int, eCount: Int) {
        sb.append(tCount).append("_").append(eCount).append(DL).append(DL).append(DL).append(DL)
                .append(execution.fillDate!!.format(dtf)).append(DL)
                .append(execution.quantity).append(DL)
                .append(if (execution.currency == Currency.USD) nf.format(fillValue(execution)) else "").append(DL)
                .append(nf.format(fillValueBase(execution))).append(DL)
        sb.append(DL.repeat(5))
        sb.append(NL)
    }

    private fun writeTradeShortExecutionBuy(sb: StringBuilder, trade: Trade, execution: Execution, currentPos: Double, tCount: Int, eCount: Int): BigDecimal? {
        sb.append(tCount).append("_").append(eCount)
        sb.append(DL.repeat(7))
        sb.append(DL)
                .append(execution.fillDate!!.format(dtf)).append(DL)
                .append(acquireType).append(DL)
                .append(execution.quantity).append(DL)
                .append(if (execution.currency == Currency.USD) nf.format(fillValue(execution)) else "").append(DL)
                .append(nf.format(fillValueBase(execution))).append(DL)
                .append(currentPos.toInt()).append(DL)
        var profitLoss: BigDecimal? = null
        if (currentPos == 0.0) {
            profitLoss = tradeCalculationService.calculatePlPortfolioBaseOpenClose(trade)
            sb.append(nf.format(profitLoss.toDouble()))
        }
        sb.append(NL)
        return profitLoss
    }

    private fun writeTradeLongExecutionBuy(sb: StringBuilder, execution: Execution, tCount: Int, eCount: Int) {
        sb.append(tCount).append("_").append(eCount).append(DL).append(DL).append(DL).append(DL)
                .append(execution.fillDate!!.format(dtf)).append(DL)
                .append(acquireType).append(DL)
                .append(execution.quantity).append(DL)
                .append(if (execution.currency == Currency.USD) nf.format(fillValue(execution)) else "").append(DL)
                .append(nf.format(fillValueBase(execution))).append(DL)
        sb.append(DL.repeat(4))
        sb.append(NL)
    }

    private fun writeTradeLongExecutionSell(sb: StringBuilder, trade: Trade, execution: Execution, currentPos: Double, tCount: Int, eCount: Int): BigDecimal? {
        sb.append(tCount).append("_").append(eCount)
        sb.append(DL.repeat(8))
        sb.append(DL)
                .append(execution.fillDate!!.format(dtf)).append(DL)
                .append(execution.quantity).append(DL)
                .append(if (execution.currency == Currency.USD) nf.format(fillValue(execution)) else "").append(DL)
                .append(nf.format(fillValueBase(execution))).append(DL)
                .append(currentPos.toInt()).append(DL)
        var profitLoss: BigDecimal? = null
        if (currentPos == 0.0) {
            profitLoss = tradeCalculationService.calculatePlPortfolioBaseOpenClose(trade)
            sb.append(nf.format(profitLoss.toDouble()))
        }
        sb.append(NL)
        return profitLoss
    }

    private fun fillValue(execution: Execution): Double {
        val contractFillPrice = execution.fillPrice
        val multiplier = BigDecimal.valueOf(execution.multiplier!!)
        return contractFillPrice!!.multiply(multiplier).toDouble()
    }

    private fun fillValueBase(execution: Execution): Double {
        val date = execution.fillDate!!.toLocalDate()
        val currency = execution.currency
        val exchangeRate = exchangeRateService.getExchangeRate(date, currency!!)
        val contractFillPrice = execution.fillPrice
        val multiplier = BigDecimal.valueOf(execution.multiplier!!)
        return contractFillPrice!!.divide(exchangeRate, HanSettings.DECIMAL_SCALE, RoundingMode.HALF_UP).multiply(multiplier).toDouble()
    }

    companion object {
        private val log = LoggerFactory.getLogger(TaxReportService::class.java)
    }
}