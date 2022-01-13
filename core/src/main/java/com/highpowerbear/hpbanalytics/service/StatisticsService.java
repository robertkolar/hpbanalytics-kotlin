package com.highpowerbear.hpbanalytics.service;

import com.hazelcast.core.HazelcastInstance;
import com.highpowerbear.hpbanalytics.common.HanUtil;
import com.highpowerbear.hpbanalytics.config.HanSettings;
import com.highpowerbear.hpbanalytics.config.WsTopic;
import com.highpowerbear.hpbanalytics.database.DataFilters;
import com.highpowerbear.hpbanalytics.database.Execution;
import com.highpowerbear.hpbanalytics.database.Trade;
import com.highpowerbear.hpbanalytics.database.TradeRepository;
import com.highpowerbear.hpbanalytics.enums.Currency;
import com.highpowerbear.hpbanalytics.enums.TradeType;
import com.highpowerbear.hpbanalytics.model.Statistics;
import com.ib.client.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by robertk on 4/26/2015.
 */
@Service
public class StatisticsService {
    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private final TradeRepository tradeRepository;
    private final HazelcastInstance hanHazelcastInstance;
    private final MessageService messageService;
    private final TradeCalculationService tradeCalculationService;
    private final ExchangeRateService exchangeRateService;

    private final String ALL = "ALL";

    @Autowired
    public StatisticsService(TradeRepository tradeRepository,
                             HazelcastInstance hanHazelcastInstance,
                             MessageService messageService,
                             TradeCalculationService tradeCalculationService,
                             ExchangeRateService exchangeRateService) {

        this.tradeRepository = tradeRepository;
        this.hanHazelcastInstance = hanHazelcastInstance;
        this.messageService = messageService;
        this.tradeCalculationService = tradeCalculationService;
        this.exchangeRateService = exchangeRateService;
    }

    public List<Statistics> getStatistics(ChronoUnit interval, String tradeType, String secType, String currency, String underlying, Integer maxPoints) {

        List<Statistics> statisticsList = statisticsMap().get(statisticsKey(interval, tradeType, secType, currency, underlying));
        if (statisticsList == null) {
            return Collections.emptyList();
        }

        int size = statisticsList.size();
        if (maxPoints == null || size < maxPoints) {
            maxPoints = size;
        }
        int firstIndex = size - maxPoints;
        return new ArrayList<>(statisticsList.subList(firstIndex, size)); // copy because reverse will be performed on it
    }

    @Async("taskExecutor")
    public void calculateStatistics(ChronoUnit interval, String tradeType, String secType, String currency, String underlying) {
        log.info("BEGIN statistics calculation for interval=" + interval + ", tradeType=" + tradeType + ", secType=" + secType + ", currency=" + currency + ", undl=" + underlying);

        Example<Trade> filter = DataFilters.tradeFilterByExample(
                normalizeEnumParam(tradeType, TradeType.class),
                normalizeEnumParam(secType, Types.SecType.class),
                normalizeEnumParam(currency, Currency.class),
                ALL.equals(underlying) ? null : underlying);

        List<Trade> trades = tradeRepository.findAll(filter, Sort.by(Sort.Direction.ASC, "openDate"));

        log.info("found " + trades.size() + " trades matching the filter criteria, calculating statistics...");
        List<Statistics> stats = calculate(trades, interval);
        statisticsMap().put(statisticsKey(interval, tradeType, secType, currency, underlying), stats);

        log.info("END statistics calculation for interval=" + interval + ", included " + trades.size() + " trades");

        messageService.sendWsReloadRequestMessage(WsTopic.STATISTICS);
    }

    private String statisticsKey(ChronoUnit interval, String tradeType, String secType, String currency, String underlying) {

        String intervalKey = interval.name();
        String tradeTypeKey = tradeType == null || ALL.equals(tradeType) ? ALL : tradeType;
        String secTypeKey = secType == null || ALL.equals(secType) ? ALL : secType;
        String currencyKey = currency == null || ALL.equals(currency) ? ALL : currency;
        String underlyingKey = underlying == null ? ALL : underlying;

        return intervalKey + "_" + tradeTypeKey + "_" + secTypeKey + "_" + currencyKey + "_" + underlyingKey;
    }

    private <T extends Enum<T>> T normalizeEnumParam(String param, Class<T> enumType) {

        return param == null || ALL.equals(param) ? null : T.valueOf(enumType, param);
    }

    private List<Statistics> calculate(List<Trade> trades, ChronoUnit interval) {
        List<Statistics> stats = new ArrayList<>();

        if (trades == null || trades.isEmpty()) {
            return stats;
        }

        LocalDateTime firstPeriodDate = toBeginOfPeriod(firstDate(trades), interval);
        LocalDateTime lastPeriodDate = toBeginOfPeriod(lastDate(trades), interval);
        LocalDateTime periodDate = firstPeriodDate;

        BigDecimal cumulProfitLoss = BigDecimal.ZERO;
        int statsCount = 1;

        while (!periodDate.isAfter(lastPeriodDate)) {
            List<Trade> tradesOpenedForPeriod = getTradesOpenedForPeriod(trades, periodDate, interval);
            List<Trade> tradesClosedForPeriod = getTradesClosedForPeriod(trades, periodDate, interval);

            List<Execution> executionsForPeriod = getExecutionsForPeriod(trades, periodDate, interval);
            int numExecs = executionsForPeriod.size();
            int numOpened = tradesOpenedForPeriod.size();
            int numClosed = tradesClosedForPeriod.size();
            int numWinners = 0;
            int numLosers = 0;
            double pctWinners;
            BigDecimal bigWinner = BigDecimal.ZERO;
            BigDecimal bigLoser = BigDecimal.ZERO;
            BigDecimal winnersProfit = BigDecimal.ZERO;
            BigDecimal losersLoss = BigDecimal.ZERO;
            BigDecimal profitLoss = BigDecimal.ZERO;
            BigDecimal profitLossTaxReport = BigDecimal.ZERO;

            for (Trade trade : tradesClosedForPeriod) {
                BigDecimal pl = tradeCalculationService.calculatePlPortfolioBaseCloseOnly(trade);
                profitLoss = profitLoss.add(pl);

                if (HanUtil.isDerivative(trade.getSecType())) {
                    BigDecimal plTr = tradeCalculationService.calculatePlPortfolioBaseOpenClose(trade);
                    profitLossTaxReport = profitLossTaxReport.add(plTr);
                }

                if (pl.doubleValue() >= 0) {
                    numWinners++;
                    winnersProfit = winnersProfit.add(pl);

                    if (pl.compareTo(bigWinner) > 0) {
                        bigWinner = pl;
                    }
                } else {
                    numLosers++;
                    losersLoss = losersLoss.add(pl.negate());

                    if (pl.compareTo(bigLoser) < 0) {
                        bigLoser = pl.negate();
                    }
                }
            }
            pctWinners = numClosed != 0 ? ((double) numWinners / (double) numClosed) * 100.0 : 0.0;
            cumulProfitLoss = cumulProfitLoss.add(profitLoss);

            BigDecimal valueBought = executionsForPeriod.stream()
                    .filter(e -> e.getAction() == Types.Action.BUY)
                    .map(this::calculateExecutionValueBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal valueSold = executionsForPeriod.stream()
                    .filter(e -> e.getAction() == Types.Action.SELL)
                    .map(this::calculateExecutionValueBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal timeValueBought = executionsForPeriod.stream()
                    .filter(e -> e.getAction() == Types.Action.BUY)
                    .filter(e -> e.getTimeValue() != null)
                    .map(this::calculateExecutionTimeValueBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal timeValueSold = executionsForPeriod.stream()
                    .filter(e -> e.getAction() == Types.Action.SELL)
                    .filter(e -> e.getTimeValue() != null)
                    .map(this::calculateExecutionTimeValueBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Statistics s = new Statistics(
                    statsCount++,
                    periodDate,
                    numExecs,
                    numOpened,
                    numClosed,
                    numWinners,
                    numLosers,
                    HanUtil.round2(pctWinners),
                    bigWinner,
                    bigLoser,
                    winnersProfit,
                    losersLoss,
                    valueBought,
                    valueSold,
                    timeValueBought,
                    timeValueSold,
                    profitLoss,
                    profitLossTaxReport,
                    cumulProfitLoss
            );
            stats.add(s);
            periodDate = periodDate.plus(1, interval);
        }
        return stats;
    }

    private BigDecimal calculateExecutionValueBase(Execution execution) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(execution.getFillDate().toLocalDate(), execution.getCurrency());
        return execution.getValue().divide(exchangeRate, HanSettings.DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateExecutionTimeValueBase(Execution execution) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(execution.getFillDate().toLocalDate(), execution.getCurrency());
        return execution.getTimeValue().divide(exchangeRate, HanSettings.DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private LocalDateTime firstDate(List<Trade> trades) {
        return Objects.requireNonNull(trades.stream()
                .map(Trade::getOpenDate)
                .min(LocalDateTime::compareTo)
                .orElse(null));
    }

    private LocalDateTime lastDate(List<Trade> trades) {
        LocalDateTime lastDate;
        LocalDateTime lastDateOpened = trades.get(0).getOpenDate();
        LocalDateTime lastDateClosed = trades.get(0).getCloseDate();

        for (Trade t: trades) {
            if (t.getOpenDate().isAfter(lastDateOpened)) {
                lastDateOpened = t.getOpenDate();
            }
        }
        for (Trade t: trades) {
            if (t.getCloseDate() != null && (lastDateClosed == null || t.getCloseDate().isAfter(lastDateClosed))) {
                lastDateClosed = t.getCloseDate();
            }
        }
        lastDate = (lastDateClosed == null || lastDateOpened.isAfter(lastDateClosed) ? lastDateOpened : lastDateClosed);
        return lastDate;
    }

    private List<Trade> getTradesOpenedForPeriod(List<Trade> trades, LocalDateTime periodDate, ChronoUnit interval) {
        return trades.stream()
                .filter(t -> toBeginOfPeriod(t.getOpenDate(), interval).isEqual(periodDate))
                .collect(Collectors.toList());
    }

    private List<Trade> getTradesClosedForPeriod(List<Trade> trades, LocalDateTime periodDate, ChronoUnit interval) {
        return trades.stream()
                .filter(t -> t.getCloseDate() != null)
                .filter(t -> toBeginOfPeriod(t.getCloseDate(), interval).isEqual(periodDate))
                .collect(Collectors.toList());
    }

    private List<Execution> getExecutionsForPeriod(List<Trade> trades, LocalDateTime periodDate, ChronoUnit interval) {
        return trades.stream()
                .flatMap(t -> t.getExecutions().stream())
                .filter(e -> toBeginOfPeriod(e.getFillDate(), interval).isEqual(periodDate))
                .distinct()
                .collect(Collectors.toList());
    }

    private LocalDateTime toBeginOfPeriod(LocalDateTime localDateTime, ChronoUnit interval) {
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

    private Map<String, List<Statistics>> statisticsMap() {
        return hanHazelcastInstance.getMap(HanSettings.HAZELCAST_STATISTICS_MAP_NAME);
    }
}
