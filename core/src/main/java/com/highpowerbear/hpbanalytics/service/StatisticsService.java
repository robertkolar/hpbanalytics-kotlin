package com.highpowerbear.hpbanalytics.service;

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
import com.highpowerbear.hpbanalytics.service.helper.StatisticsHelper;
import com.ib.client.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.highpowerbear.hpbanalytics.config.HanSettings.ALL;

/**
 * Created by robertk on 4/26/2015.
 */
@Service
public class StatisticsService {
    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private final TradeRepository tradeRepository;
    private final MessageService messageService;
    private final TradeCalculationService tradeCalculationService;
    private final StatisticsHelper helper;

    @Autowired
    public StatisticsService(TradeRepository tradeRepository,
                             MessageService messageService,
                             TradeCalculationService tradeCalculationService,
                             StatisticsHelper helper) {

        this.tradeRepository = tradeRepository;
        this.messageService = messageService;
        this.tradeCalculationService = tradeCalculationService;
        this.helper = helper;
    }

    public List<Statistics> getStatistics(ChronoUnit interval, String tradeType, String secType, String currency, String underlying, Integer maxPoints) {

        List<Statistics> statisticsList = helper.statisticsMap().get(helper.statisticsKey(interval, tradeType, secType, currency, underlying));
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

    public List<Statistics> getCurrentStatistics(String tradeType, String secType, String currency, String underlying) {
        List<Statistics> currentStatisticsList = helper.currentStatisticsMap().get(helper.statisticsKey(null, tradeType, secType, currency, underlying));
        return Objects.requireNonNullElse(currentStatisticsList, Collections.emptyList());
    }

    @Async("taskExecutor")
    public void calculateStatistics(ChronoUnit interval, String tradeType, String secType, String currency, String underlying) {
        log.info("BEGIN statistics calculation for interval=" + interval + ", tradeType=" + tradeType + ", secType=" + secType + ", currency=" + currency + ", undl=" + underlying);

        Example<Trade> tradeExample = DataFilters.tradeExample(
                helper.normalizeEnumParam(tradeType, TradeType.class),
                helper.normalizeEnumParam(secType, Types.SecType.class),
                helper.normalizeEnumParam(currency, Currency.class),
                ALL.equals(underlying) ? null : underlying);

        List<Trade> trades = tradeRepository.findAll(tradeExample, Sort.by(Sort.Direction.ASC, "openDate"));

        log.info("found " + trades.size() + " trades matching the filter criteria, calculating statistics...");
        List<Statistics> statisticsList = calculate(trades, interval);
        helper.statisticsMap().put(helper.statisticsKey(interval, tradeType, secType, currency, underlying), statisticsList);

        log.info("END statistics calculation for interval=" + interval + ", included " + trades.size() + " trades");
        messageService.sendWsReloadRequestMessage(WsTopic.STATISTICS);
    }

    public void calculateCurrentStatistics(String tradeType, String secType, String currency, String underlying, boolean reload) {
        log.info("BEGIN current statistics calculation for tradeType=" + tradeType + ", secType=" + secType + ", currency=" + currency + ", undl=" + underlying);

        LocalDateTime cutoffDate = helper.toBeginOfPeriod(LocalDateTime.now(), ChronoUnit.YEARS);
        Specification<Trade> tradeSpecification = DataFilters.tradeSpecification(
                helper.normalizeEnumParam(tradeType, TradeType.class),
                helper.normalizeEnumParam(secType, Types.SecType.class),
                helper.normalizeEnumParam(currency, Currency.class),
                ALL.equals(underlying) ? null : underlying,
                cutoffDate
        );
        List<Trade> trades = tradeRepository.findAll(tradeSpecification, Sort.by(Sort.Direction.ASC, "openDate"));

        Statistics daily = calculateCurrent(trades, ChronoUnit.DAYS);
        Statistics monthly = calculateCurrent(trades, ChronoUnit.MONTHS);
        Statistics yearly = calculateCurrent(trades, ChronoUnit.YEARS);

        helper.currentStatisticsMap().put(helper.statisticsKey(null, tradeType, secType, currency, underlying), List.of(daily, monthly, yearly));

        log.info("END current statistics calculation, included " + trades.size() + " trades");
        if (reload) {
            messageService.sendWsReloadRequestMessage(WsTopic.CURRENT_STATISTICS);
        }
    }

    public void calculateCurrentStatisticsOnExecution(Execution execution) {
        String all = HanSettings.ALL;
        String secType = execution.getSecType().name();
        String undl = execution.getUnderlying();

        Stream.of(all, secType).forEach(st ->
            Stream.of(all, undl).forEach(u ->
                calculateCurrentStatistics(all, st, all, u, false)));

        messageService.sendWsReloadRequestMessage(WsTopic.CURRENT_STATISTICS);
    }

    private List<Statistics> calculate(List<Trade> trades, ChronoUnit interval) {
        List<Statistics> statisticsList = new ArrayList<>();
        if (trades == null || trades.isEmpty()) {
            return statisticsList;
        }

        LocalDateTime firstPeriodDate = helper.toBeginOfPeriod(helper.firstDate(trades), interval);
        LocalDateTime lastPeriodDate = helper.toBeginOfPeriod(helper.lastDate(trades), interval);
        LocalDateTime periodDate = firstPeriodDate;

        BigDecimal cumulProfitLoss = BigDecimal.ZERO;
        int statsCount = 1;

        while (!periodDate.isAfter(lastPeriodDate)) {
            Statistics statistics = calculatePeriod(trades, interval, periodDate);

            cumulProfitLoss = cumulProfitLoss.add(statistics.getProfitLoss());
            statistics
                    .setId(statsCount++)
                    .setCumulProfitLoss(cumulProfitLoss);

            statisticsList.add(statistics);
            periodDate = periodDate.plus(1, interval);
        }
        return statisticsList;
    }

    private Statistics calculateCurrent(List<Trade> trades, ChronoUnit interval) {
        Statistics statistics = calculatePeriod(trades, interval, helper.toBeginOfPeriod(LocalDateTime.now(), interval));
        int id = interval == ChronoUnit.DAYS ? 1 : (interval == ChronoUnit.MONTHS ? 2 : 3);

        return statistics
                .setId(id)
                .setCumulProfitLoss(statistics.getProfitLoss());
    }

    private Statistics calculatePeriod(List<Trade> trades, ChronoUnit interval, LocalDateTime periodDate) {
        if (trades == null || trades.isEmpty()) {
            return new Statistics();
        }

        List<Trade> tradesOpenedForPeriod = helper.getTradesOpenedForPeriod(trades, periodDate, interval);
        List<Trade> tradesClosedForPeriod = helper.getTradesClosedForPeriod(trades, periodDate, interval);
        List<Execution> executionsForPeriod = helper.getExecutionsForPeriod(trades, periodDate, interval);

        int numWinners = 0;
        int numLosers = 0;
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
        double pctWinners = !tradesClosedForPeriod.isEmpty() ? ((double) numWinners / (double) tradesClosedForPeriod.size()) * 100.0 : 0.0;

        return new Statistics()
                .setPeriodDate(periodDate)
                .setNumExecs(executionsForPeriod.size())
                .setNumOpened(tradesOpenedForPeriod.size())
                .setNumClosed(tradesClosedForPeriod.size())
                .setNumWinners(numWinners)
                .setNumLosers(numLosers)
                .setPctWinners(HanUtil.round2(pctWinners))
                .setBigWinner(bigWinner)
                .setBigLoser(bigLoser)
                .setWinnersProfit(winnersProfit)
                .setLosersLoss(losersLoss)
                .setValueBought(helper.valueSum(executionsForPeriod, Types.Action.BUY))
                .setValueSold(helper.valueSum(executionsForPeriod, Types.Action.SELL))
                .setTimeValueBought(helper.timeValueSum(executionsForPeriod, Types.Action.BUY))
                .setTimeValueSold(helper.timeValueSum(executionsForPeriod, Types.Action.SELL))
                .setProfitLoss(profitLoss)
                .setProfitLossTaxReport(profitLossTaxReport);
    }
}
