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

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as ExchangeRate
        return date == that.date
    }

    override fun hashCode(): Int {
        return date.hashCode()
    }

    fun setDate(date: String): ExchangeRate {
        this.date = date
        return this
    }

    fun setEurUsd(eurUsd: Double): ExchangeRate {
        this.eurUsd = eurUsd
        return this
    }

    fun setEurGbp(eurGbp: Double): ExchangeRate {
        this.eurGbp = eurGbp
        return this
    }

    fun setEurChf(eurChf: Double): ExchangeRate {
        this.eurChf = eurChf
        return this
    }

    fun setEurAud(eurAud: Double): ExchangeRate {
        this.eurAud = eurAud
        return this
    }

    fun setEurJpy(eurJpy: Double): ExchangeRate {
        this.eurJpy = eurJpy
        return this
    }

    fun setEurKrw(eurKrw: Double): ExchangeRate {
        this.eurKrw = eurKrw
        return this
    }

    fun setEurHkd(eurHkd: Double): ExchangeRate {
        this.eurHkd = eurHkd
        return this
    }

    fun setEurSgd(eurSgd: Double): ExchangeRate {
        this.eurSgd = eurSgd
        return this
    }

    companion object {
        private const val serialVersionUID = 539031756808205732L
    }
}