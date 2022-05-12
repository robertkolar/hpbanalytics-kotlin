package com.highpowerbear.hpbanalytics.database

import org.springframework.data.jpa.repository.JpaRepository

/**
 * Created by robertk on 4/13/2020.
 */
interface ExchangeRateRepository : JpaRepository<ExchangeRate?, String?> {
    fun findFirstByOrderByDateDesc(): List<ExchangeRate>
}