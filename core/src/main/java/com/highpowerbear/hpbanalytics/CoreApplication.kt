package com.highpowerbear.hpbanalytics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.context.properties.EnableConfigurationProperties
import com.highpowerbear.hpbanalytics.config.ApplicationProperties
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Created by robertk on 5/29/2017.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ApplicationProperties::class)
@EnableJpaRepositories
open class CoreApplication