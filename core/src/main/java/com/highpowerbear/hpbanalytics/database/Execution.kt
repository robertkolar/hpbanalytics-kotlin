package com.highpowerbear.hpbanalytics.database

import com.fasterxml.jackson.annotation.JsonIgnore
import com.highpowerbear.hpbanalytics.config.HanSettings
import com.ib.client.Types.SecType
import java.time.LocalDateTime
import java.math.BigDecimal
import com.highpowerbear.hpbanalytics.enums.Currency
import com.ib.client.Types
import java.io.Serializable
import javax.persistence.*

/**
 * Created by robertk on 5/29/2017.
 */
@Entity
@Table(name = "execution", schema = HanSettings.DB_SCHEMA, catalog = HanSettings.DB_DATABASE)
class Execution : Serializable {
    @Id
    @SequenceGenerator(
        name = "execution_generator",
        sequenceName = "execution_seq",
        schema = HanSettings.DB_SCHEMA,
        catalog = HanSettings.DB_DATABASE,
        allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "execution_generator")
    var id: Long? = null
    var reference: String? = null

    @Enumerated(EnumType.STRING)
    var action: Types.Action? = null
    var quantity: Double = 0.0
    var symbol: String = ""
    var underlying: String? = null

    @Enumerated(EnumType.STRING)
    var currency: Currency = Currency.EUR

    @Enumerated(EnumType.STRING)
    var secType: SecType = SecType.FOP
    var multiplier: Double = 1.0
    var fillDate: LocalDateTime = LocalDateTime.now()
    var fillPrice: BigDecimal = BigDecimal.ZERO
    var inTheMoney: BigDecimal? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    var trade: Trade? = null
    val value: BigDecimal
        get() {
            var value = fillPrice
                .multiply(BigDecimal.valueOf(multiplier))
                .multiply(BigDecimal.valueOf(quantity))
            if (action == Types.Action.SELL) {
                value = value.negate()
            }
            return value
        }
    val timeValue: BigDecimal
        get() {
            if (inTheMoney == null) {
                return BigDecimal.ZERO
            }
            var timeValue = fillPrice
                .subtract(inTheMoney)
                .multiply(BigDecimal.valueOf(multiplier))
                .multiply(BigDecimal.valueOf(quantity))
            if (action == Types.Action.SELL) {
                timeValue = timeValue.negate()
            }
            return timeValue
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val execution = other as Execution
        return id == execution.id
    }

    override fun hashCode(): Int {
        return if (id != null) id.hashCode() else 0
    }

    val tradeId: Long?
        get() = if (trade != null) trade!!.id else null

    override fun toString(): String {
        return "Execution{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", action=" + action +
                ", quantity=" + quantity +
                ", symbol='" + symbol + '\'' +
                ", underlying='" + underlying + '\'' +
                ", currency=" + currency +
                ", secType=" + secType +
                ", multiplier=" + multiplier +
                ", fillDate=" + fillDate +
                ", fillPrice=" + fillPrice +
                ", inTheMoney=" + inTheMoney +
                ", tradeId=" + tradeId +
                '}'
    }

    companion object {
        private const val serialVersionUID = 2067980957084297540L
    }
}