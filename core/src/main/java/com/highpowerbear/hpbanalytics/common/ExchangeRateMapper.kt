package com.highpowerbear.hpbanalytics.common

import com.highpowerbear.hpbanalytics.database.ExchangeRate
import com.highpowerbear.shared.ExchangeRateDTO
import org.mapstruct.Mapper

/**
 * Created by robertk on 11/1/2020.
 */
@Mapper(componentModel = "spring")
interface ExchangeRateMapper {
    fun entityToDto(entity: ExchangeRate): ExchangeRateDTO
}