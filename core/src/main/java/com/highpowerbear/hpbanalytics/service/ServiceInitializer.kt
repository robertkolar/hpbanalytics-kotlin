package com.highpowerbear.hpbanalytics.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Created by robertk on 10/25/2020.
 */
@Component
class ServiceInitializer @Autowired constructor(
    initializingServices: List<InitializingService>) {

    init {
        initializingServices.forEach { s -> s.initialize() }
    }
}