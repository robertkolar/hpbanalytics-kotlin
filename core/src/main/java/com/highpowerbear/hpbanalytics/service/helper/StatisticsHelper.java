package com.highpowerbear.hpbanalytics.service.helper;

import com.hazelcast.core.HazelcastInstance;
import com.highpowerbear.hpbanalytics.config.HanSettings;
import com.highpowerbear.hpbanalytics.database.Execution;
import com.highpowerbear.hpbanalytics.database.Trade;
import com.highpowerbear.hpbanalytics.enums.Currency;
import com.highpowerbear.hpbanalytics.model.Statistics;
import com.highpowerbear.hpbanalytics.service.ExchangeRateService;
import com.ib.client.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.highpowerbear.hpbanalytics.config.HanSettings.ALL;

/**
 * Created by robertk on 1/18/2022.
 */
@Component
public class StatisticsHelper {

    private final HazelcastInstance hanHazelcastInstance;
    private final ExchangeRateService exchangeRateService;

    @Autowired
    public StatisticsHelper(HazelcastInstance hanHazelcastInstance,
                            ExchangeRateService exchangeRateService) {

        this.hanHazelcastInstance = hanHazelcastInstance;
        this.exchangeRateService = exchangeRateService;
    }

    public String statisticsKey(ChronoUnit interval, String tradeType, String secType, String currency, String underlying) {

        String intervalKey = interval != null ? interval.name() : null;
        String tradeTypeKey = tradeType == null || ALL.equals(tradeType) ? ALL : tradeType;
        String secTypeKey = secType == null || ALL.equals(secType) ? ALL : secType;
        String currencyKey = currency == null || ALL.equals(currency) ? ALL : currency;
        String underlyingKey = underlying == null ? ALL : underlying;

        return (intervalKey != null ? intervalKey + "_" : "") + tradeTypeKey + "_" + secTypeKey + "_" + currencyKey + "_" + underlyingKey;
    }

    public <T extends Enum<T>> T normalizeEnumParam(String param, Class<T> enumType) {

        return param == null || ALL.equals(param) ? null : T.valueOf(enumType, param);
    }

    public BigDecimal valueSum(List<Execution> executions, Types.Action action) {
        return executions.stream()
                .filter(e -> e.getAction() == action)
                .map(e -> valueBase(e.getValue(), e.getFillDate().toLocalDate(), e.getCurrency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal timeValueSum(List<Execution> executions, Types.Action action) {
        return executions.stream()
                .filter(e -> e.getAction() == action)
                .filter(e -> e.getTimeValue() != null)
                .map(e -> valueBase(e.getTimeValue(), e.getFillDate().toLocalDate(), e.getCurrency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal valueBase(BigDecimal value, LocalDate date, Currency currency) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(date, currency);
        return value.divide(exchangeRate, HanSettings.DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public LocalDateTime firstDate(List<Trade> trades) {
        return trades.stream()
                .flatMap(t -> t.getExecutions().stream())
                .map(Execution::getFillDate)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    public LocalDateTime lastDate(List<Trade> trades) {
        return trades.stream()
                .flatMap(t -> t.getExecutions().stream())
                .map(Execution::getFillDate)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    public List<Trade> getTradesOpenedForPeriod(List<Trade> trades, LocalDateTime periodDate, ChronoUnit interval) {
        return trades.stream()
                .filter(t -> toBeginOfPeriod(t.getOpenDate(), interval).isEqual(periodDate))
                .collect(Collectors.toList());
    }

    public List<Trade> getTradesClosedForPeriod(List<Trade> trades, LocalDateTime periodDate, ChronoUnit interval) {
        return trades.stream()
                .filter(t -> t.getCloseDate() != null)
                .filter(t -> toBeginOfPeriod(t.getCloseDate(), interval).isEqual(periodDate))
                .collect(Collectors.toList());
    }

    public List<Execution> getExecutionsForPeriod(List<Trade> trades, LocalDateTime periodDate, ChronoUnit interval) {
        return trades.stream()
                .flatMap(t -> t.getExecutions().stream())
                .filter(e -> toBeginOfPeriod(e.getFillDate(), interval).isEqual(periodDate))
                .distinct()
                .collect(Collectors.toList());
    }

    public LocalDateTime toBeginOfPeriod(LocalDateTime localDateTime, ChronoUnit interval) {
        LocalDate localDate = localDateTime.toLocalDate();

        if (ChronoUnit.YEARS.equals(interval)) {
            localDate = localDate.withDayOfYear(1);

        } else if (ChronoUnit.MONTHS.equals(interval)) {
            localDate = localDate.withDayOfMonth(1);

        } else if (!ChronoUnit.DAYS.equals(interval)) {
            throw new IllegalStateException("unsupported statistics interval " + interval);
        }

        return localDate.atStartOfDay();
    }

    public Map<String, List<Statistics>> statisticsMap() {
        return hanHazelcastInstance.getMap(HanSettings.HAZELCAST_STATISTICS_MAP_NAME);
    }

    public Map<String, List<Statistics>> currentStatisticsMap() {
        return hanHazelcastInstance.getMap(HanSettings.HAZELCAST_CURRENT_STATISTICS_MAP_NAME);
    }
}
