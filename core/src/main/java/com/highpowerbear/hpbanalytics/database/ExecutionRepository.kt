package com.highpowerbear.hpbanalytics.database

import com.highpowerbear.hpbanalytics.enums.Currency
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.LocalDateTime
import com.ib.client.Types.SecType
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import javax.transaction.Transactional

/**
 * Created by robertk on 4/13/2020.
 */
interface ExecutionRepository : JpaRepository<Execution?, Long?>, JpaSpecificationExecutor<Execution?> {

    fun findAllByOrderByFillDateAsc(): List<Execution>
    fun existsByFillDate(fillDate: LocalDateTime?): Boolean

    @Query("SELECT e FROM Execution e WHERE e.symbol = :symbol AND e.currency = :currency AND e.secType = :secType AND e.multiplier = :multiplier AND e.fillDate >= :cutoffDate ORDER BY e.fillDate ASC")
    fun findExecutionsToAnalyzeAgain(
        @Param("symbol") symbol: String?,
        @Param("currency") currency: Currency?,
        @Param("secType") secType: SecType?,
        @Param("multiplier") multiplier: Double,
        @Param("cutoffDate") cutoffDate: LocalDateTime?
    ): List<Execution>

    @Modifying
    @Transactional
    @Query("update Execution e set e.trade = null")
    fun disassociateAllExecutions(): Int
}