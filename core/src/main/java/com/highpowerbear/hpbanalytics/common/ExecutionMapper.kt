package com.highpowerbear.hpbanalytics.common

import com.highpowerbear.hpbanalytics.database.Execution
import com.highpowerbear.shared.ExecutionDTO
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

/**
 * Created by robertk on 10/4/2020.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ExecutionMapper {
    fun dtoToEntity(dto: ExecutionDTO): Execution
}