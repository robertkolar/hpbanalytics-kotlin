package com.highpowerbear.hpbanalytics.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 * Created by robertk on 4/26/2015.
 */
public class Statistics implements Serializable {
    private static final long serialVersionUID = 8464224239795026258L;

    private int id;
    private LocalDateTime periodDate;
    private int numExecs;
    private int numOpened;
    private int numClosed;
    private int numWinners;
    private int numLosers;
    private double pctWinners;
    private BigDecimal bigWinner;
    private BigDecimal bigLoser;
    private BigDecimal winnersProfit;
    private BigDecimal losersLoss;
    private BigDecimal timeValueBought;
    private BigDecimal timeValueSold;
    private BigDecimal timeValueSum;
    private BigDecimal profitLoss;
    private BigDecimal profitLossTaxReport;
    private BigDecimal cumulProfitLoss;

    public int getId() {
        return id;
    }

    public Statistics setId(int id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getPeriodDate() {
        return periodDate;
    }

    public Statistics setPeriodDate(LocalDateTime periodDate) {
        this.periodDate = periodDate;
        return this;
    }

    public int getNumExecs() {
        return numExecs;
    }

    public Statistics setNumExecs(int numExecs) {
        this.numExecs = numExecs;
        return this;
    }

    public int getNumOpened() {
        return numOpened;
    }

    public Statistics setNumOpened(int numOpened) {
        this.numOpened = numOpened;
        return this;
    }

    public int getNumClosed() {
        return numClosed;
    }

    public Statistics setNumClosed(int numClosed) {
        this.numClosed = numClosed;
        return this;
    }

    public int getNumWinners() {
        return numWinners;
    }

    public Statistics setNumWinners(int numWinners) {
        this.numWinners = numWinners;
        return this;
    }

    public int getNumLosers() {
        return numLosers;
    }

    public Statistics setNumLosers(int numLosers) {
        this.numLosers = numLosers;
        return this;
    }

    public double getPctWinners() {
        return pctWinners;
    }

    public Statistics setPctWinners(double pctWinners) {
        this.pctWinners = pctWinners;
        return this;
    }

    public BigDecimal getBigWinner() {
        return bigWinner;
    }

    public Statistics setBigWinner(BigDecimal bigWinner) {
        this.bigWinner = bigWinner;
        return this;
    }

    public BigDecimal getBigLoser() {
        return bigLoser;
    }

    public Statistics setBigLoser(BigDecimal bigLoser) {
        this.bigLoser = bigLoser;
        return this;
    }

    public BigDecimal getWinnersProfit() {
        return winnersProfit;
    }

    public Statistics setWinnersProfit(BigDecimal winnersProfit) {
        this.winnersProfit = winnersProfit;
        return this;
    }

    public BigDecimal getLosersLoss() {
        return losersLoss;
    }

    public Statistics setLosersLoss(BigDecimal losersLoss) {
        this.losersLoss = losersLoss;
        return this;
    }

    public BigDecimal getTimeValueBought() {
        return timeValueBought;
    }

    public Statistics setTimeValueBought(BigDecimal timeValueBought) {
        this.timeValueBought = timeValueBought;
        return this;
    }

    public BigDecimal getTimeValueSold() {
        return timeValueSold;
    }

    public Statistics setTimeValueSold(BigDecimal timeValueSold) {
        this.timeValueSold = timeValueSold;
        return this;
    }

    public BigDecimal getTimeValueSum() {
        return timeValueSum;
    }

    public Statistics setTimeValueSum(BigDecimal timeValueSum) {
        this.timeValueSum = timeValueSum;
        return this;
    }

    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    public Statistics setProfitLoss(BigDecimal profitLoss) {
        this.profitLoss = profitLoss;
        return this;
    }

    public BigDecimal getProfitLossTaxReport() {
        return profitLossTaxReport;
    }

    public Statistics setProfitLossTaxReport(BigDecimal profitLossTaxReport) {
        this.profitLossTaxReport = profitLossTaxReport;
        return this;
    }

    public BigDecimal getCumulProfitLoss() {
        return cumulProfitLoss;
    }

    public Statistics setCumulProfitLoss(BigDecimal cumulProfitLoss) {
        this.cumulProfitLoss = cumulProfitLoss;
        return this;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "id=" + id +
                ", periodDate=" + periodDate +
                ", numExecs=" + numExecs +
                ", numOpened=" + numOpened +
                ", numClosed=" + numClosed +
                ", numWinners=" + numWinners +
                ", numLosers=" + numLosers +
                ", pctWinners=" + pctWinners +
                ", bigWinner=" + bigWinner +
                ", bigLoser=" + bigLoser +
                ", winnersProfit=" + winnersProfit +
                ", losersLoss=" + losersLoss +
                ", timeValueBought=" + timeValueBought +
                ", timeValueSold=" + timeValueSold +
                ", timeValueSum=" + timeValueSum +
                ", profitLoss=" + profitLoss +
                ", profitLossTaxReport=" + profitLossTaxReport +
                ", cumulProfitLoss=" + cumulProfitLoss +
                '}';
    }
}
