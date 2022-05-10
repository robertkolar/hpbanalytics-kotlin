package com.highpowerbear.hpbanalytics.model

import com.highpowerbear.hpbanalytics.database.Execution
import com.highpowerbear.hpbanalytics.enums.Currency
import com.ib.client.Types.SecType

/**
 * Created by robertk on 10/23/2020.
 */
class ExecutionContract private constructor(private val execution: Execution) {
    fun execution(): Execution {
        return execution
    }

    fun symbol(): String {
        return execution.symbol
    }

    fun currency(): Currency {
        return execution.currency
    }

    fun secType(): SecType {
        return execution.secType
    }

    fun multiplier(): Double {
        return execution.multiplier
    }

    override fun toString(): String {
        return cid(execution)
    }

    companion object {
        fun forExecution(e: Execution): ExecutionContract {
            return ExecutionContract(e)
        }

        fun cid(e: Execution): String {
            return e.symbol + "_" + e.currency + "_" + e.secType + "_" + e.multiplier.toString().replace(".", "_")
        }
    }
}