package com.highpowerbear.hpbanalytics.common

import com.highpowerbear.hpbanalytics.database.Execution
import com.highpowerbear.shared.ExecutionDTO
import org.mapstruct.Mapper

/**
 * Created by robertk on 10/4/2020.
 */
@Mapper(componentModel = "spring")
interface ExecutionMapper {
    fun dtoToEntity(dto: ExecutionDTO): Execution
}