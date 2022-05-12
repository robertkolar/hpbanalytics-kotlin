package com.highpowerbear.hpbanalytics.config

import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Created by robertk on 4/5/2020.
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "application")
class ApplicationProperties(val ecbExchangeRateUrl: String, val underlyingsPermanent: List<String>)