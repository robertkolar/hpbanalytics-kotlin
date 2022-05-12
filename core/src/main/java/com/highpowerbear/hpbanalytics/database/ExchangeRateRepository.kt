package com.highpowerbear.hpbanalytics.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by robertk on 4/13/2020.
 */
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, String> {

    List<ExchangeRate> findFirstByOrderByDateDesc();
}
