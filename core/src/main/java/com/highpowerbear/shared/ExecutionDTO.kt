package com.highpowerbear.shared

import com.ib.client.Types.SecType
import java.time.LocalDateTime
import java.math.BigDecimal
import com.ib.client.Types
import java.io.Serializable

/**
 * Created by robertk on 10/3/2020.
 */
class ExecutionDTO : Serializable {
    var reference: String = ""
    var action: Types.Action = Types.Action.BUY
    var quantity: Double = 0.0
    var symbol: String = ""
    var underlying: String = ""
    var currency: String = ""
    var secType: SecType = SecType.FOP
    var multiplier: Double = 0.0
    var fillDate: LocalDateTime = LocalDateTime.MIN
    var fillPrice: BigDecimal = BigDecimal.ZERO
    var inTheMoney: BigDecimal = BigDecimal.ZERO

    override fun toString(): String {
        return "ExecutionDTO{" +
                "reference='" + reference + '\'' +
                ", action=" + action +
                ", quantity=" + quantity +
                ", symbol='" + symbol + '\'' +
                ", underlying='" + underlying + '\'' +
                ", currency='" + currency + '\'' +
                ", secType=" + secType +
                ", multiplier=" + multiplier +
                ", fillDate=" + fillDate +
                ", fillPrice=" + fillPrice +
                ", inTheMoney=" + inTheMoney +
                '}'
    }

    companion object {
        private const val serialVersionUID = -7595159006402413521L
    }
}