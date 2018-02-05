package com.highpowerbear.hpbanalytics.rest;

import com.highpowerbear.hpbanalytics.common.CoreUtil;
import com.highpowerbear.hpbanalytics.common.MessageSender;
import com.highpowerbear.hpbanalytics.common.OptionInfoVO;
import com.highpowerbear.hpbanalytics.dao.ReportDao;
import com.highpowerbear.hpbanalytics.dao.filter.ExecutionFilter;
import com.highpowerbear.hpbanalytics.dao.filter.FilterParser;
import com.highpowerbear.hpbanalytics.dao.filter.TradeFilter;
import com.highpowerbear.hpbanalytics.entity.Execution;
import com.highpowerbear.hpbanalytics.entity.Report;
import com.highpowerbear.hpbanalytics.entity.Trade;
import com.highpowerbear.hpbanalytics.enums.SecType;
import com.highpowerbear.hpbanalytics.enums.StatisticsInterval;
import com.highpowerbear.hpbanalytics.enums.TradeStatus;
import com.highpowerbear.hpbanalytics.enums.TradeType;
import com.highpowerbear.hpbanalytics.report.IfiCsvGenerator;
import com.highpowerbear.hpbanalytics.report.ReportProcessor;
import com.highpowerbear.hpbanalytics.report.StatisticsCalculator;
import com.highpowerbear.hpbanalytics.report.StatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static com.highpowerbear.hpbanalytics.common.CoreSettings.WS_TOPIC_REPORT;

/**
 * Created by robertk on 12/21/2017.
 */
@RestController
@RequestMapping("/report")
public class ReportRestController {

    @Autowired ReportDao reportDao;
    @Autowired private StatisticsCalculator statisticsCalculator;
    @Autowired private ReportProcessor reportProcessor;
    @Autowired private FilterParser filterParser;
    @Autowired private IfiCsvGenerator ifiCsvGenerator;
    @Autowired private MessageSender messageSender;

    @RequestMapping("/reports")
    public ResponseEntity<?> getReports() {
        List<Report> reports = reportDao.getReports();

        reports.forEach(report -> report.setReportInfo(reportDao.getReportInfo(report.getId())));

        return ResponseEntity.ok(reports);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/reports")
    public ResponseEntity<?> updateReport(
            @RequestBody Report report) {

        Report reportDb = reportDao.findReport(report.getId());
        if (reportDb == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reportDao.updateReport(report));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/reports/{id}")
    public ResponseEntity<?> analyzeReport(
            @PathVariable("id") int id) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        reportProcessor.analyzeAll(id);
        messageSender.sendWsMessage(WS_TOPIC_REPORT, "report " + id + " analyzed");

        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/reports/{id}")
    public ResponseEntity<?> deleteReport(
            @PathVariable("id") int id) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        reportProcessor.deleteReport(id);
        messageSender.sendWsMessage(WS_TOPIC_REPORT, "report " + id + " deleted");

        return ResponseEntity.ok().build();
    }

    @RequestMapping("/reports/{id}/executions")
    public ResponseEntity<?> getFilteredExecutions(
            @PathVariable("id") int id,
            @RequestParam(required = false, value = "filter") String jsonFilter,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        ExecutionFilter filter = filterParser.parseExecutionFilter(jsonFilter);
        List<Execution> executions = reportDao.getFilteredExecutions(id, filter, start, limit);
        Long numExecutions = reportDao.getNumFilteredExecutions(id, filter);

        return ResponseEntity.ok(new RestList<>(executions, numExecutions));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/reports/{id}/executions")
    public ResponseEntity<?> createExecution(
            @PathVariable("id") int reportId,
            @RequestBody Execution execution) {

        Report report = reportDao.findReport(reportId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        execution.setId(null);
        execution.setReport(report);
        // fix fill date timezone, JAXB JSON converter sets it to UTC
        execution.getFillDate().setTimeZone(TimeZone.getTimeZone("America/New_York"));
        execution.setReceivedDate(Calendar.getInstance());

        reportProcessor.newExecution(execution);
        messageSender.sendWsMessage(WS_TOPIC_REPORT, "new execution processed");

        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/reports/{id}/executions/{executionid}")
    public ResponseEntity<?> deleteExecution(
            @PathVariable("id") int reportId,
            @PathVariable("executionid") long executionId) {

        Execution execution = reportDao.findExecution(executionId);
        if (execution == null || reportId != execution.getReportId()) {
            return ResponseEntity.notFound().build();
        }
        reportProcessor.deleteExecution(executionId);
        messageSender.sendWsMessage(WS_TOPIC_REPORT, "execution " + executionId + " deleted");

        return ResponseEntity.ok().build();
    }

    @RequestMapping("/reports/{id}/trades")
    public ResponseEntity<?> getFilteredTrades(
            @PathVariable("id") int id,
            @RequestParam(required = false, value = "filter") String jsonFilter,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        TradeFilter filter = filterParser.parseTradeFilter(jsonFilter);
        List<Trade> trades = reportDao.getFilteredTrades(id, filter, start, limit);
        Long numTrades = reportDao.getNumFilteredTrades(id, filter);

        return ResponseEntity.ok(new RestList<>(trades, numTrades));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/reports/{id}/trades/{tradeid}/close")
    public ResponseEntity<?> closeTrade(
            @PathVariable("id") int id,
            @PathVariable("tradeid") long tradeId,
            @RequestBody CloseTrade closeTrade) {

        Report report = reportDao.findReport(id);
        Trade trade = reportDao.findTrade(tradeId);

        if (report == null || trade == null) {
            return ResponseEntity.notFound().build();
        }

        if (!TradeStatus.OPEN.equals(trade.getStatus())) {
            return ResponseEntity.badRequest().build();
        }

        // fix fill date timezone, JAXB JSON converter sets it to UTC
        closeTrade.getCloseDate().setTimeZone(TimeZone.getTimeZone("America/New_York"));

        reportProcessor.closeTrade(trade, closeTrade.getCloseDate(), closeTrade.getClosePrice());
        messageSender.sendWsMessage(WS_TOPIC_REPORT, "trade " + tradeId + " closed");

        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/reports/{id}/trades/{tradeid}/expire")
    public ResponseEntity<?> expireTrade(
            @PathVariable("id") int id,
            @PathVariable("tradeid") long tradeId) {

        Report report = reportDao.findReport(id);
        Trade trade = reportDao.findTrade(tradeId);

        if (report == null || trade == null) {
            return ResponseEntity.notFound().build();
        }

        if (!SecType.OPT.equals(trade.getSecType()) || !TradeStatus.OPEN.equals(trade.getStatus())) {
            return ResponseEntity.badRequest().build();
        }
        reportProcessor.expireTrade(trade);
        messageSender.sendWsMessage(WS_TOPIC_REPORT, "trade " + tradeId + " expired");

        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/reports/{id}/trades/{tradeid}/assign")
    public ResponseEntity<?> assignTrade(
            @PathVariable("id") int id,
            @PathVariable("tradeid") long tradeId) {

        Report report = reportDao.findReport(id);
        Trade trade = reportDao.findTrade(tradeId);

        if (report == null || trade == null) {
            return ResponseEntity.notFound().build();
        }

        if (!SecType.OPT.equals(trade.getSecType()) || !TradeStatus.OPEN.equals(trade.getStatus())) {
            return ResponseEntity.badRequest().build();
        }
        reportProcessor.assignTrade(trade);
        messageSender.sendWsMessage(WS_TOPIC_REPORT, "trade " + tradeId + " assigned");

        return ResponseEntity.ok().build();
    }

    @RequestMapping("/reports/{id}/statistics/{interval}")
    public ResponseEntity<?> getStatistics(
            @PathVariable("id") int id,
            @PathVariable("interval") StatisticsInterval interval,
            @RequestParam(required = false, value = "underlying") String underlying,
            @RequestParam("start") int start,
            @RequestParam("limit") int limit) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        List<StatisticsVO> statistics = statisticsCalculator.getStatistics(report, interval, underlying, 180);
        Collections.reverse(statistics);
        List<StatisticsVO> statisticsPage = new ArrayList<>();

        for (int i = 0; i < statistics.size(); i++) {
            if (i >= start && i < (start + limit)) {
                statisticsPage.add(statistics.get(i));
            }
        }
        return ResponseEntity.ok(new RestList<>(statisticsPage, (long) statistics.size()));
    }

    @RequestMapping("reports/{id}/charts/{interval}")
    public ResponseEntity<?> getCharts(
            @PathVariable("id") int id,
            @PathVariable("interval") StatisticsInterval interval,
            @RequestParam(required = false, value = "underlying") String underlying) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        List<StatisticsVO> statistics = statisticsCalculator.getStatistics(report, interval, underlying, 180);

        return ResponseEntity.ok(new RestList<>(statistics, (long) statistics.size()));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/reports/{id}/statistics/{interval}")
    public ResponseEntity<?> calculateStatistics(
            @PathVariable("id") int id,
            @PathVariable("interval") StatisticsInterval interval,
            @RequestParam(required = false, value = "underlying") String underlying) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        statisticsCalculator.calculateStatistics(id, interval, underlying);

        return ResponseEntity.ok().build();
    }

    @RequestMapping("/reports/{id}/underlyings")
    public ResponseEntity<?> getUnderlyings(
            @PathVariable("id") int id) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reportDao.getUnderlyings(id));
    }

    @RequestMapping("/optionutil/parse")
    public ResponseEntity<?> optionUtilParse(
            @RequestParam("optionsymbol") String optionSymbol) {

        OptionInfoVO optionInfo = CoreUtil.parseOptionSymbol(optionSymbol);
        if (optionInfo == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(optionInfo);
    }

    @RequestMapping("/reports/{id}/ificsv/{year}/{tradetype}")
    public ResponseEntity<?> getIfiCsv(
            @PathVariable("id") int id,
            @PathVariable("year") int year,
            @PathVariable("tradetype") TradeType tradeType) {

        Report report = reportDao.findReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ifiCsvGenerator.generate(id, year, tradeType));
    }

    private static class CloseTrade {
        private Calendar closeDate;
        private BigDecimal closePrice;

        private CloseTrade(Calendar closeDate, BigDecimal closePrice) {
            this.closeDate = closeDate;
            this.closePrice = closePrice;
        }

        private Calendar getCloseDate() {
            return closeDate;
        }

        private BigDecimal getClosePrice() {
            return closePrice;
        }
    }
}