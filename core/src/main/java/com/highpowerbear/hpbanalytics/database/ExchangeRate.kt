package com.highpowerbear.hpbanalytics.database

import com.highpowerbear.hpbanalytics.config.HanSettings
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * Created by robertk on 5/29/2017.
 */
@Entity
@Table(name = "exchange_rate", schema = HanSettings.DB_SCHEMA, catalog = HanSettings.DB_DATABASE)
class ExchangeRate : Serializable {
    @Id
    var date // yyyy-MM-dd
            : String = ""
    var eurUsd: Double = 0.0
    var eurGbp: Double = 0.0
    var eurChf: Double = 0.0
    var eurAud: Double = 0.0
    var eurJpy: Double = 0.0
    var eurKrw: Double = 0.0
    var eurHkd: Double = 0.0
    var eurSgd: Double = 0.0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ExchangeRate
        return date == that.date
    }

    override fun hashCode(): Int {
        return date.hashCode()
    }

    companion object {
        private const val serialVersionUID = 539031756808205732L
    }
}