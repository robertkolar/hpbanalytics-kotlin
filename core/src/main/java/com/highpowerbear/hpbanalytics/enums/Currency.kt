package com.highpowerbear.hpbanalytics.enums;

/**
 * Created by robertk on 11/18/2017.
 */
public enum Currency {
    EUR,
    USD,
    AUD,
    GBP,
    CHF,
    JPY,
    KRW,
    HKD,
    SGD;

    public static Currency findByValue(String value) {
        Currency result = null;
        for (Currency currency : values()) {
            if (currency.name().equalsIgnoreCase(value)) {
                result = currency;
                break;
            }
        }
        return result;
    }
}
