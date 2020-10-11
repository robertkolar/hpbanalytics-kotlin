package com.highpowerbear.hpbanalytics.service;

import com.highpowerbear.hpbanalytics.common.HanUtil;
import com.highpowerbear.hpbanalytics.config.WsTopic;
import com.highpowerbear.hpbanalytics.database.*;
import com.highpowerbear.hpbanalytics.enums.Currency;
import com.highpowerbear.hpbanalytics.enums.StatisticsInterval;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by robertk on 4/26/2015.
 */
@Service
public class StatisticsCalculatorService {
    private static final Logger log = LoggerFactory.getLogger(StatisticsCalculatorService.class);

    private final TradeRepository tradeRepository;
    private final ExecutionRepository executionRepository;
    private final MessageService messageService;
    private final TradeCalculatorService tradeCalculatorService;

    private final Map<String, List<Statistics>> statisticsMap = new HashMap<>(); // caching statistics to prevent excessive recalculation

    private final String ALL = "ALL";

    @Autowired
    public StatisticsCalculatorService(TradeRepository tradeRepository,
                                       ExecutionRepository executionRepository,
                                       MessageService messageService,
                                       TradeCalculatorService tradeCalculatorService) {

        this.tradeRepository = tradeRepository;
        this.executionRepository = executionRepository;
        this.messageService = messageService;
        this.tradeCalculatorService = tradeCalculatorService;
    }

    public List<Statistics> getStatistics(StatisticsInterval interval, String tradeType, String secType, String currency, String underlying, Integer maxPoints) {

        List<Statistics> statisticsList = statisticsMap.get(statisticsKey(interval, tradeType, secType, currency, underlying));
        if (statisticsList == null) {
            return new ArrayList<>();
        }

        int size = statisticsList.size();

        if (maxPoints == null || size < maxPoints) {
            maxPoints = size;
        }

        int firstIndex = size - maxPoints;
        // copy because reverse will be performed on it

        return new ArrayList<>(statisticsList.subList(firstIndex, size));
    }

    @Async("taskExecutor")
    public void calculateStatistics(StatisticsInterval interval, String tradeType, String secType, String currency, String underlying) {
        log.info("BEGIN statistics calculation for interval=" + interval + ", tradeType=" + tradeType + ", secType=" + secType + ", currency=" + currency + ", undl=" + underlying);

        Example<Trade> filter = DataFilters.tradeFilterByExample(
                normalizeEnumParam(tradeType, TradeType.class),
                normalizeEnumParam(secType, Types.SecType.class),
                normalizeEnumParam(currency, Currency.class),
                ALL.equals(underlying) ? null : underlying);

        List<Trade> trades = tradeRepository.findAll(filter, Sort.by(Sort.Direction.ASC, "openDate"));

        List<Statistics> stats = doCalculate(trades, interval);
        statisticsMap.put(statisticsKey(interval, tradeType, secType, currency, underlying), stats);

        log.info("END statistics calculation for interval=" + interval + ", included " + trades.size() + " trades");

        messageService.sendWsReloadRequestMessage(WsTopic.STATISTICS);
    }

    private String statisticsKey(StatisticsInterval interval, String tradeType, String secType, String currency, String underlying) {

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

    private List<Statistics> doCalculate(List<Trade> trades, StatisticsInterval interval) {
        List<Statistics> stats = new ArrayList<>();

        if (trades == null || trades.isEmpty()) {
            return stats;
        }

        LocalDateTime firstDate = getFirstDate(trades);
        LocalDateTime lastDate = getLastDate(trades);

        LocalDateTime firstPeriodDate = toBeginOfPeriod(firstDate, interval);
        LocalDateTime lastPeriodDate = toBeginOfPeriod(lastDate, interval);
        LocalDateTime periodDate = firstPeriodDate;

        BigDecimal cumulProfitLoss = BigDecimal.ZERO;
        int statsCount = 1;

        while (!periodDate.isAfter(lastPeriodDate)) {
            List<Trade> tradesOpenedForPeriod = getTradesOpenedForPeriod(trades, periodDate, interval);
            List<Trade> tradesClosedForPeriod = getTradesClosedForPeriod(trades, periodDate, interval);

            int numExecs = getNumberExecutionsForPeriod(trades, periodDate, interval);
            int numOpened = tradesOpenedForPeriod.size();
            int numClosed = tradesClosedForPeriod.size();
            int numWinners = 0;
            int numLosers = 0;
            double pctWinners;
            BigDecimal winnersProfit = BigDecimal.ZERO;
            BigDecimal losersLoss = BigDecimal.ZERO;
            BigDecimal bigWinner = BigDecimal.ZERO;
            BigDecimal bigLoser = BigDecimal.ZERO;
            BigDecimal profitLoss;

            for (Trade t : tradesClosedForPeriod) {
                BigDecimal pl = tradeCalculatorService.calculatePlPortfolioBase(t);

                if (pl.doubleValue() >= 0) {
                    numWinners++;
                    winnersProfit = winnersProfit.add(pl);

                    if (pl.compareTo(bigWinner) > 0) {
                        bigWinner = pl;
                    }
                } else {
                    numLosers++;
                    losersLoss = losersLoss.add(pl);

                    if (pl.compareTo(bigLoser) < 0) {
                        bigLoser = pl;
                    }
                }
            }
            pctWinners = numClosed != 0 ? ((double) numWinners / (double) numClosed) * 100.0 : 0.0;
            profitLoss = winnersProfit.add(losersLoss);
            cumulProfitLoss = cumulProfitLoss.add(profitLoss);

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
                    profitLoss,
                    cumulProfitLoss
            );
            stats.add(s);

            if (StatisticsInterval.DAY.equals(interval)) {
                periodDate = periodDate.plusDays(1);

            } else if (StatisticsInterval.MONTH.equals(interval)) {
                periodDate = periodDate.plusMonths(1);

            } else if (StatisticsInterval.YEAR.equals(interval)) {
                periodDate = periodDate.plusYears(1);
            }
        }
        return stats;
    }

    private LocalDateTime getFirstDate(List<Trade> trades) {
        LocalDateTime firstDateOpened = trades.get(0).getOpenDate();
        for (Trade t: trades) {
            if (t.getOpenDate().isBefore(firstDateOpened)) {
                firstDateOpened = t.getOpenDate();
            }
        }
        return firstDateOpened;
    }

    private LocalDateTime getLastDate(List<Trade> trades) {
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

    private List<Trade> getTradesOpenedForPeriod(List<Trade> trades, LocalDateTime periodDate, StatisticsInterval interval) {
        return trades.stream()
                .filter(t -> toBeginOfPeriod(t.getOpenDate(), interval).isEqual(periodDate))
                .collect(Collectors.toList());
    }

    private List<Trade> getTradesClosedForPeriod(List<Trade> trades, LocalDateTime periodDate, StatisticsInterval interval) {
        return trades.stream()
                .filter(t -> t.getCloseDate() != null)
                .filter(t -> toBeginOfPeriod(t.getCloseDate(), interval).isEqual(periodDate))
                .collect(Collectors.toList());
    }

    private int getNumberExecutionsForPeriod(List<Trade> trades, LocalDateTime periodDate, StatisticsInterval interval) {

        return (int) trades.stream()
                .map(Trade::getExecutionIds)
                .map(executionRepository::findByIdInOrderByFillDateAsc)
                .flatMap(Collection::stream)
                .filter(execution -> toBeginOfPeriod(execution.getFillDate(), interval).isEqual(periodDate))
                .map(Execution::getId)
                .distinct()
                .count();
    }

    private LocalDateTime toBeginOfPeriod(LocalDateTime localDateTime, StatisticsInterval interval) {
        LocalDate localDate = localDateTime.toLocalDate();

        if (StatisticsInterval.YEAR.equals(interval)) {
            localDate = localDate.withDayOfYear(1);

        } else if (StatisticsInterval.MONTH.equals(interval)) {
            localDate = localDate.withDayOfMonth(1);
        }

        return localDate.atStartOfDay();
    }
}
