package com.highpowerbear.hpbanalytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.List;

/**
 * Created by robertk on 4/5/2020.
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private final String ecbExchangeRateUrl;
    private final List<String> underlyingsPermanent;

    public ApplicationProperties(String ecbExchangeRateUrl, List<String> underlyingsPermanent) {
        this.ecbExchangeRateUrl = ecbExchangeRateUrl;
        this.underlyingsPermanent = underlyingsPermanent;
    }

    public String getEcbExchangeRateUrl() {
        return ecbExchangeRateUrl;
    }

    public List<String> getUnderlyingsPermanent() {
        return underlyingsPermanent;
    }
}
