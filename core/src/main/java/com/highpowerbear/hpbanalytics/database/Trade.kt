package com.highpowerbear.hpbanalytics.database

import com.highpowerbear.hpbanalytics.config.HanSettings
import com.highpowerbear.hpbanalytics.enums.Currency
import com.highpowerbear.hpbanalytics.enums.TradeStatus
import com.highpowerbear.hpbanalytics.enums.TradeType
import com.ib.client.Types

import javax.persistence.*
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Created by robertk on 5/29/2017.
 */
@Entity
@Table(name = "trade", schema = HanSettings.DB_SCHEMA, catalog = HanSettings.DB_DATABASE)
class Trade : Serializable {

    @Id
    @SequenceGenerator(name = "trade_generator",
        sequenceName = "trade_seq",
        schema = HanSettings.DB_SCHEMA,
        catalog = HanSettings.DB_DATABASE,
        allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trade_generator")
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    var type: TradeType? = null
    var symbol: String? = null
    var underlying: String? = null

    @Enumerated(EnumType.STRING)
    var currency: Currency? = null

    @Enumerated(EnumType.STRING)
    var secType: Types.SecType? = null
    var multiplier: Double? = null
    var cumulativeQuantity: Double? = null

    @Enumerated(EnumType.STRING)
    var status: TradeStatus? = null
    var openPosition: Double? = null
    var avgOpenPrice: BigDecimal? = null
    var openDate: LocalDateTime? = null
    var avgClosePrice: BigDecimal? = null
    var closeDate: LocalDateTime? = null
    var profitLoss: BigDecimal? = null
    var timeValueSum: BigDecimal? = null

    @OneToMany(mappedBy = "trade", fetch = FetchType.EAGER)
    @OrderBy("fillDate ASC")
    var executions: MutableList<Execution> = mutableListOf()

    fun getValueSum(): BigDecimal {
        return executions
                .map { it.value }
                .fold(BigDecimal.ZERO, BigDecimal::add)
    }

    fun getExecutionIds(): String {
        return executions.joinToString { it.id.toString() }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val trade = other as Trade
        return id == trade.id
    }

    override fun hashCode(): Int {
        return if (id != null) id.hashCode() else 0
    }

    @Override
    override fun toString() : String {
        return "Trade{" +
                "id=" + id +
                ", type=" + type +
                ", symbol='" + symbol + '\'' +
                ", secType=" + secType +
                ", openDate=" + openDate +
                ", closeDate=" + closeDate +
                ", profitLoss=" + profitLoss +
                ", executionIds=" + getExecutionIds() +
                '}'
    }

    companion object {
        private const val serialVersionUID = 3978501428965359313L
    }
}
