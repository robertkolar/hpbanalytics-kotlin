package com.highpowerbear.hpbanalytics.database

import org.springframework.data.jpa.repository.JpaRepository
import com.highpowerbear.hpbanalytics.enums.Currency
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import com.highpowerbear.hpbanalytics.enums.TradeType
import java.time.LocalDateTime
import com.ib.client.Types.SecType
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * Created by robertk on 4/13/2020.
 */
interface TradeRepository : JpaRepository<Trade?, Long?>, JpaSpecificationExecutor<Trade?> {
    fun findByTypeAndCloseDateBetweenOrderByOpenDateAsc(
        type: TradeType?,
        closeDateBegin: LocalDateTime?,
        closeDateEnd: LocalDateTime?
    ): List<Trade>

    @Query("SELECT t FROM Trade t WHERE  t.symbol = :symbol AND t.currency = :currency AND t.secType = :secType AND t.multiplier = :multiplier AND (t.closeDate >= :fillDate OR t.openPosition <> 0) ORDER BY t.openDate ASC")
    fun findTradesAffectedByExecution(
        @Param("symbol") symbol: String?,
        @Param("currency") currency: Currency?,
        @Param("secType") secType: SecType?,
        @Param("multiplier") multiplier: Double,
        @Param("fillDate") fillDate: LocalDateTime?
    ): List<Trade>

    @Query("SELECT DISTINCT t.underlying AS u FROM Trade t ORDER BY u")
    fun findAllUnderlyings(): List<String>

    @Query("SELECT DISTINCT t.underlying AS u FROM Trade t WHERE t.openPosition <> 0 ORDER BY u")
    fun findOpenUnderlyings(): List<String>

    @Query("SELECT COUNT(t) FROM Trade t WHERE t.openPosition <> 0")
    fun countOpenTrades(): Long

    @Query("SELECT COUNT(DISTINCT t.underlying) AS u FROM Trade t")
    fun countAllUnderlyings(): Long

    @Query("SELECT COUNT(DISTINCT t.underlying) AS u FROM Trade t WHERE t.openPosition <> 0")
    fun countOpenUnderlyings(): Long
}