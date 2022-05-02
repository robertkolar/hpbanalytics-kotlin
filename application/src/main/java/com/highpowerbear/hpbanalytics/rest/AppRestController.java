package com.highpowerbear.hpbanalytics.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highpowerbear.hpbanalytics.config.ApplicationProperties;
import com.highpowerbear.hpbanalytics.database.*;
import com.highpowerbear.hpbanalytics.enums.TradeStatus;
import com.highpowerbear.hpbanalytics.enums.TradeType;
import com.highpowerbear.hpbanalytics.model.DataFilterItem;
import com.highpowerbear.hpbanalytics.model.Statistics;
import com.highpowerbear.hpbanalytics.rest.model.CalculateStatisticsRequest;
import com.highpowerbear.hpbanalytics.rest.model.CloseTradeRequest;
import com.highpowerbear.hpbanalytics.rest.model.GenericList;
import com.highpowerbear.hpbanalytics.service.AnalyticsService;
import com.highpowerbear.hpbanalytics.service.StatisticsService;
import com.highpowerbear.hpbanalytics.service.TaxReportService;
import com.ib.client.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by robertk on 12/21/2017.
 */
@RestController
@RequestMapping("/")
public class AppRestController {

    private final ExecutionRepository executionRepository;
    private final TradeRepository tradeRepository;
    private final StatisticsService statisticsService;
    private final AnalyticsService analyticsService;
    private final TaxReportService taxReportService;
    private final ApplicationProperties applicationProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AppRestController(ExecutionRepository executionRepository,
                             TradeRepository tradeRepository,
                             StatisticsService statisticsService,
                             AnalyticsService analyticsService,
                             TaxReportService taxReportService,
                             ApplicationProperties applicationProperties) {

        this.executionRepository = executionRepository;
        this.tradeRepository = tradeRepository;
        this.statisticsService = statisticsService;
        this.analyticsService = analyticsService;
        this.taxReportService = taxReportService;
        this.applicationProperties = applicationProperties;
    }

    @RequestMapping("execution")
    public GenericList<Execution> getFilteredExecutions(
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam(required = false, value = "filter") String jsonFilter) throws Exception {

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "fillDate"));

        List<Execution> executions;
        long numExecutions;

        if (jsonFilter != null) {
            List<DataFilterItem> filter = Arrays.asList(objectMapper.readValue(jsonFilter, DataFilterItem[].class));

            executions = executionRepository.findAll(DataFilters.executionSpecification(filter), pageable).getContent();
            numExecutions = executionRepository.count(DataFilters.executionSpecification(filter));

        } else {
            executions = executionRepository.findAll(pageable).getContent();
            numExecutions = executionRepository.count();
        }
        return new GenericList<>(executions, (int) numExecutions);
    }

    @RequestMapping(method = RequestMethod.POST, value = "execution")
    public void addExecution(@RequestBody Execution execution) {

        execution.setId(null);
        analyticsService.addExecution(execution);
        statisticsService.calculateCurrentStatisticsOnExecution(execution);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "execution/{executionId}")
    public void deleteExecution(@PathVariable("executionId") long executionId) {

        Execution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "execution not found, id=" + executionId);
        }

        analyticsService.deleteExecution(executionId);
        statisticsService.calculateCurrentStatisticsOnExecution(execution);
    }

    @RequestMapping(method = RequestMethod.POST, value = "trade/regenerate-all")
    public void regenerateAllTrades() {
        analyticsService.regenerateAllTrades();
    }

    @RequestMapping("trade")
    public GenericList<Trade> getFilteredTrades(
            @RequestParam("page") int page,
            @RequestParam("limit") int limit,
            @RequestParam(required = false, value = "filter") String jsonFilter) throws Exception {

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "openDate"));
        List<Trade> trades;
        long numTrades;

        if (jsonFilter != null) {
            List<DataFilterItem> filter = Arrays.asList(objectMapper.readValue(jsonFilter, DataFilterItem[].class));

            trades = tradeRepository.findAll(DataFilters.tradeSpecification(filter), pageable).getContent();
            numTrades = tradeRepository.count(DataFilters.tradeSpecification(filter));

        } else {
            trades = tradeRepository.findAll(pageable).getContent();
            numTrades = tradeRepository.count();
        }
        return new GenericList<>(trades, (int) numTrades);
    }

    @RequestMapping("trade/statistics")
    public ResponseEntity<?> getTradeStatistics() {
        return ResponseEntity.ok(analyticsService.getTradeStatistics());
    }

    @RequestMapping(method = RequestMethod.POST, value = "trade/{tradeId}/close")
    public void manualCloseTrade(
            @PathVariable("tradeId") long tradeId,
            @RequestBody CloseTradeRequest r) {

        Trade trade = tradeRepository.findById(tradeId).orElse(null);

        if (trade == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "trade not found, id=" + tradeId);

        } else if (!TradeStatus.OPEN.equals(trade.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trade not open, id=" + tradeId);
        }

        Execution execution = new Execution()
                .setReference(r.getExecutionReference())
                .setAction(trade.getType() == TradeType.LONG ? Types.Action.SELL : Types.Action.BUY)
                .setQuantity(Math.abs(trade.getOpenPosition()))
                .setSymbol(trade.getSymbol())
                .setUnderlying(trade.getUnderlying())
                .setCurrency(trade.getCurrency())
                .setSecType(trade.getSecType())
                .setMultiplier(trade.getMultiplier())
                .setFillDate(r.getCloseDate())
                .setFillPrice(r.getClosePrice());

        analyticsService.addExecution(execution);
        statisticsService.calculateCurrentStatisticsOnExecution(execution);
    }

    @RequestMapping("statistics")
    public GenericList<Statistics> getStatistics(
            @RequestParam("interval") ChronoUnit interval,
            @RequestParam(required = false, value = "tradeType") String tradeType,
            @RequestParam(required = false, value = "secType") String secType,
            @RequestParam(required = false, value = "currency") String currency,
            @RequestParam(required = false, value = "underlying") String underlying,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit) {

        List<Statistics> items = statisticsService.getStatistics(interval, tradeType, secType, currency, underlying, null);
        Collections.reverse(items);
        int total = items.size();

        return new GenericList<>(page(items, start, limit, total), total);
    }

    @RequestMapping("statistics/current")
    public GenericList<Statistics> getCurrentStatistics(
            @RequestParam(required = false, value = "tradeType") String tradeType,
            @RequestParam(required = false, value = "secType") String secType,
            @RequestParam(required = false, value = "currency") String currency,
            @RequestParam(required = false, value = "underlying") String underlying,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit) {

        List<Statistics> items = statisticsService.getCurrentStatistics(tradeType, secType, currency, underlying);
        int total = items.size();

        return new GenericList<>(page(items, start, limit, total), total);
    }

    @RequestMapping("statistics/underlyings")
    public List<String> getUnderlyings(@RequestParam(required = false, value = "openOnly") boolean openOnly) {

        List<String> underlyings;
        if (openOnly) {
            underlyings = tradeRepository.findOpenUnderlyings();
            applicationProperties.getUnderlyingsPermanent().stream()
                    .filter(up -> !underlyings.contains(up))
                    .forEach(underlyings::add);
        } else {
            underlyings = tradeRepository.findAllUnderlyings();
        }
        return underlyings;
    }

    @RequestMapping(method = RequestMethod.POST, value = "statistics")
    public void calculateStatistics(@RequestBody CalculateStatisticsRequest r) {
        statisticsService.calculateStatistics(r.getInterval(), r.getTradeType(), r.getSecType(), r.getCurrency(), r.getUnderlying());
    }

    @RequestMapping(method = RequestMethod.POST, value = "statistics/current")
    public void calculateCurrentStatistics(@RequestBody CalculateStatisticsRequest r) {
        statisticsService.calculateCurrentStatistics(r.getTradeType(), r.getSecType(), r.getCurrency(), r.getUnderlying(), true);
    }

    @RequestMapping("statistics/charts")
    public GenericList<Statistics> getCharts(
            @RequestParam("interval") ChronoUnit interval,
            @RequestParam(required = false, value = "tradeType") String tradeType,
            @RequestParam(required = false, value = "secType") String secType,
            @RequestParam(required = false, value = "currency") String currency,
            @RequestParam(required = false, value = "underlying") String underlying) {

        List<Statistics> statistics = statisticsService.getStatistics(interval, tradeType, secType, currency, underlying, 120);
        return new GenericList<>(statistics, statistics.size());
    }

    @RequestMapping("statistics/ifi/years")
    public List<Integer> getIfiYears() {
        return taxReportService.getIfiYears();
    }

    @RequestMapping("statistics/ifi/csv")
    public String getIfiCsv(
            @RequestParam("year") int year,
            @RequestParam("endMonth") int endMonth,
            @RequestParam("tradeType") TradeType tradeType) {

        return taxReportService.generate(year, endMonth, tradeType);
    }

    private <T> List<T> page(List<T> items, int start, int limit, int total) {
        List<T> itemsPaged;

        if (!items.isEmpty()) {
            int fromIndex = Math.min(start, total - 1);
            int toIndex = Math.min(fromIndex + limit, total);
            itemsPaged = items.subList(fromIndex, toIndex);
        } else {
            itemsPaged = Collections.emptyList();
        }
        return itemsPaged;
    }
}
