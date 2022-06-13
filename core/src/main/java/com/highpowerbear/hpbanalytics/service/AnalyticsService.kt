package com.highpowerbear.hpbanalytics.service

import com.highpowerbear.hpbanalytics.config.WsEndpoint
import com.highpowerbear.hpbanalytics.database.Execution
import com.highpowerbear.hpbanalytics.database.ExecutionRepository
import com.highpowerbear.hpbanalytics.database.Trade
import com.highpowerbear.hpbanalytics.database.TradeRepository
import com.highpowerbear.hpbanalytics.model.ExecutionContract
import com.highpowerbear.hpbanalytics.model.TradeStatistics
import com.ib.client.Types
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.temporal.ChronoUnit
import java.util.function.Consumer

/**
 *
 * Created by robertk on 4/26/2015.
 */
@Service
open class AnalyticsService @Autowired constructor(
    private val executionRepository: ExecutionRepository,
    private val tradeRepository: TradeRepository,
    private val tradeCalculationService: TradeCalculationService,
    private val messageService: MessageService) {

    open var tradeStatistics: TradeStatistics = TradeStatistics() // must be open because of spring injection
    init {
        updateTradeStatistics()
    }

    @Transactional
    open fun regenerateAllTrades() {
        log.info("BEGIN trade regeneration")
        val tradeCount = tradeRepository.count()
        val numExec = executionRepository.disassociateAllExecutions()
        log.info("disassociated $numExec executions, deleting $tradeCount trades")
        tradeRepository.deleteAll()
        val executions = executionRepository.findAllByOrderByFillDateAsc()
        if (executions.isEmpty()) {
            log.info("END trade regeneration, no executions, skipping")
            return
        }
        val regeneratedTades = generateTrades(executions)
        saveRegeneratedTrades(regeneratedTades)
        log.info("END trade regeneration")
    }

    @Transactional
    open fun deleteExecution(executionId: Long) {
        log.info("received request to delete execution $executionId")
        val execution = executionRepository.findById(executionId).orElse(null)
        if (execution == null) {
            log.warn("execution $executionId does not exists, cannot delete")
            return
        }
        val ec = ExecutionContract.forExecution(execution)
        val tradesAffected = deleteTradesAffected(ec)
        log.info("deleting execution $execution")
        executionRepository.delete(execution)
        val firstTradeAffected = tradesAffected[0]
        val firstExecution = firstTradeAffected.executions[0]
        val executionsToAnalyzeAgain = executionRepository
                .findExecutionsToAnalyzeAgain(ec.symbol(), ec.currency(), ec.secType(), ec.multiplier(), firstExecution.fillDate)
        val regeneratedTrades = generateTradesSingleContract(executionsToAnalyzeAgain)
        saveRegeneratedTrades(regeneratedTrades)
    }

    @Transactional
    open fun addExecution(execution: Execution) {
        var executionMod = execution
        log.info("received request to add new execution for symbol " + executionMod.symbol)
        adjustFillDate(executionMod)
        adjustInTheMoney(executionMod)
        val ec = ExecutionContract.forExecution(executionMod)
        val tradesAffected = deleteTradesAffected(ec)
        log.info("saving new execution $executionMod")
        executionMod = executionRepository.save(executionMod)
        if (tradesAffected.isEmpty()) {
            val regeneratedTrades = generateTradesSingleContract(listOf(executionMod))
            saveRegeneratedTrades(regeneratedTrades)
            return
        }
        val firstTradeAffected = tradesAffected[0]
        val firstExecution = firstTradeAffected.executions[0]
        val cutoffDate = listOf(firstExecution, executionMod)
                .map { it.fillDate }
                .minByOrNull { it!! }
        val executionsToAnalyzeAgain = executionRepository
                .findExecutionsToAnalyzeAgain(ec.symbol(), ec.currency(), ec.secType(), ec.multiplier(), cutoffDate)
        val regeneratedTrades = generateTradesSingleContract(executionsToAnalyzeAgain)
        saveRegeneratedTrades(regeneratedTrades)
    }

    private fun deleteTradesAffected(ec: ExecutionContract): List<Trade> {
        val tradesAffected = tradeRepository
                .findTradesAffectedByExecution(ec.symbol(), ec.currency(), ec.secType(), ec.multiplier(), ec.execution().fillDate)
        if (tradesAffected.isEmpty()) {
            log.info("no trades affected")
            return tradesAffected
        }
        val sb = StringBuilder("trades affected:\n")
        tradesAffected.forEach(Consumer { trade: Trade? -> sb.append(trade).append("\n") })
        log.info(sb.toString())
        for (trade in tradesAffected) {
            val tradeExecutions = trade.executions
            for (te in tradeExecutions) {
                te.trade = null // save handled by transaction
                log.info("disassociated execution " + te.id)
            }
        }
        log.info("deleting " + tradesAffected.size + " trades")
        tradeRepository.deleteAll(tradesAffected)
        return tradesAffected
    }

    private fun adjustFillDate(execution: Execution) {
        var fillDate = execution.fillDate!!
        while (executionRepository.existsByFillDate(fillDate)) {
            fillDate = fillDate.plus(1, ChronoUnit.MICROS)
        }
        if (fillDate.isAfter(execution.fillDate)) {
            log.info("adjusting fill date to $fillDate")
            execution.fillDate = fillDate
        }
    }

    private fun adjustInTheMoney(execution: Execution) {
        if (execution.secType == Types.SecType.OPT || execution.secType == Types.SecType.FOP) {
            if (execution.inTheMoney == null) {
                log.info("setting in the money to zero")
                execution.inTheMoney = BigDecimal.ZERO
            }
        }
    }

    private fun saveRegeneratedTrades(trades: List<Trade>) {
        log.info("saving " + trades.size + " regenerated trades")
        if (!trades.isEmpty()) {
            tradeRepository.saveAll(trades) // executions update handled by transaction
        }
        updateTradeStatistics()
        messageService.sendWsReloadRequestMessage(WsEndpoint.EXECUTION)
        messageService.sendWsReloadRequestMessage(WsEndpoint.TRADE)
    }

    private fun updateTradeStatistics() {
        tradeStatistics.apply {
            numAllTrades = tradeRepository.count()
            numAllUnderlyings = tradeRepository.countAllUnderlyings()
            numOpenTrades = tradeRepository.countOpenTrades()
            numOpenUnderlyings = tradeRepository.countOpenUnderlyings()
        }
    }

    private fun generateTrades(executions: List<Execution>): List<Trade> {
        val trades: MutableList<Trade> = ArrayList()
        val cids = executions.map { e -> ExecutionContract.cid(e) }.toSet()
        val executionsPerCidMap: MutableMap<String, MutableList<Execution>> = HashMap() // contract identifier -> list of executions
        cids.forEach(Consumer { cid: String -> executionsPerCidMap[cid] = ArrayList() })
        for (execution in executions) {
            val cid = ExecutionContract.cid(execution)
            executionsPerCidMap[cid]!!.add(execution)
        }
        for (cid in cids) {
            val executionsPerCid: List<Execution> = executionsPerCidMap[cid]!!
            log.info("generating trades for $cid")
            val generatedTradesPerCid = generateTradesSingleContract(executionsPerCid)
            trades.addAll(generatedTradesPerCid)
        }
        return trades
    }

    private fun generateTradesSingleContract(executions: List<Execution>): List<Trade> {
        val trades: MutableList<Trade> = ArrayList()
        var currentPos = 0.0
        val singleContractSet: MutableSet<Execution> = LinkedHashSet(executions)
        while (singleContractSet.isNotEmpty()) {
            val trade = Trade()
            for (execution in singleContractSet) {
                trade.executions.add(execution)
                execution.trade = trade
                val oldPos = currentPos
                currentPos += if (execution.action == Types.Action.BUY) execution.quantity!! else -execution.quantity!!
                log.info("associated $execution, currentPos=$currentPos")
                check(!detectReversal(oldPos, currentPos)) { "execution resulting in reversal trade not permitted $execution" }
                if (currentPos == 0.0) {
                    break
                }
            }
            tradeCalculationService.calculateFields(trade)
            log.info("generated trade $trade")
            trades.add(trade)
            trade.executions.forEach(Consumer { o: Execution -> singleContractSet.remove(o) })
        }
        return trades
    }

    private fun detectReversal(oldPos: Double, currentPos: Double): Boolean {
        return oldPos > 0 && currentPos < 0 || oldPos < 0 && currentPos > 0
    }

    companion object {
        private val log = LoggerFactory.getLogger(AnalyticsService::class.java)
    }
}