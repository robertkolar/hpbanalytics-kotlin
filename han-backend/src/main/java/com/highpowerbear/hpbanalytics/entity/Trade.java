package com.highpowerbear.hpbanalytics.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.highpowerbear.hpbanalytics.common.HanUtil;
import com.highpowerbear.hpbanalytics.common.OptionUtil;
import com.highpowerbear.hpbanalytics.enums.Action;
import com.highpowerbear.hpbanalytics.enums.Currency;
import com.highpowerbear.hpbanalytics.enums.FuturePlMultiplier;
import com.highpowerbear.hpbanalytics.enums.SecType;
import com.highpowerbear.hpbanalytics.enums.TradeStatus;
import com.highpowerbear.hpbanalytics.enums.TradeType;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by robertk on 5/29/2017.
 */
@Entity
@Table(name = "trade", schema = "report", catalog = "hpbanalytics")
public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    @Enumerated(EnumType.STRING)
    private TradeType type;
    private String symbol;
    private String underlying;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "sectype")
    private SecType secType;
    @Column(name = "cumulativequantity")
    private Integer cumulativeQuantity;
    @Enumerated(EnumType.STRING)
    private TradeStatus status;
    @Column(name = "openposition")
    private Integer openPosition;
    @Column(name = "avgopenprice")
    private BigDecimal avgOpenPrice;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "opendate")
    private Calendar openDate;
    @Column(name = "avgcloseprice")
    private BigDecimal avgClosePrice;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "closedate")
    private Calendar closeDate;
    @Column(name = "profitloss")
    private BigDecimal profitLoss;
    @ManyToOne
    @JsonIgnore
    private Report report;
    @OneToMany(mappedBy = "trade", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("fillDate ASC")
    private List<SplitExecution> splitExecutions;

    @JsonProperty
    public Integer getReportId() {
        return this.report.getId();
    }

    @JsonProperty
    public String getDuration() {
        return (closeDate != null ? HanUtil.toDurationString(closeDate.getTimeInMillis() - openDate.getTimeInMillis()) : "");
    }

    public void calculate() {
        MathContext mc = new MathContext(7);
        this.report = splitExecutions.iterator().next().execution.getReport();
        this.type = (splitExecutions.iterator().next().getCurrentPosition() > 0 ? TradeType.LONG : TradeType.SHORT);
        this.symbol = (this.splitExecutions == null || this.splitExecutions.isEmpty() ? null : this.splitExecutions.iterator().next().execution.getSymbol());
        this.underlying = (this.splitExecutions == null || this.splitExecutions.isEmpty() ? null : this.splitExecutions.iterator().next().execution.getUnderlying());
        this.currency = (this.splitExecutions == null || this.splitExecutions.isEmpty() ? null : this.splitExecutions.iterator().next().execution.getCurrency());
        this.secType = (this.splitExecutions == null || this.splitExecutions.isEmpty() ? null : this.splitExecutions.iterator().next().execution.getSecType());
        this.openPosition = (this.splitExecutions == null || this.splitExecutions.isEmpty() ? null : this.splitExecutions.get(this.splitExecutions.size() - 1).getCurrentPosition());
        BigDecimal cumulativeOpenPrice = new BigDecimal(0.0);
        BigDecimal cumulativeClosePrice = new BigDecimal(0.0);
        this.cumulativeQuantity = 0;
        for (SplitExecution se : splitExecutions) {
            if ((this.type == TradeType.LONG && se.execution.getAction() == Action.BUY) || (this.type == TradeType.SHORT && se.execution.getAction() == Action.SELL)) {
                this.cumulativeQuantity += se.getSplitQuantity();
                cumulativeOpenPrice = cumulativeOpenPrice.add(new BigDecimal(se.getSplitQuantity()).multiply(se.execution.getFillPrice(), mc));
            }
            if (this.status == TradeStatus.CLOSED) {
                if ((this.type == TradeType.LONG && se.execution.getAction() == Action.SELL) || (this.type == TradeType.SHORT && se.execution.getAction() == Action.BUY)) {
                    cumulativeClosePrice = cumulativeClosePrice.add(new BigDecimal(se.getSplitQuantity()).multiply(se.execution.getFillPrice(), mc));
                }
            }
        }
        this.avgOpenPrice = cumulativeOpenPrice.divide(new BigDecimal(this.cumulativeQuantity), mc);
        this.openDate = this.getSplitExecutions().get(0).getExecution().getFillDate();
        if (this.status == TradeStatus.CLOSED) {
            this.avgClosePrice = cumulativeClosePrice.divide(new BigDecimal(this.cumulativeQuantity), mc);
            this.closeDate = this.getSplitExecutions().get(this.getSplitExecutions().size() - 1).getExecution().getFillDate();
            this.profitLoss = (TradeType.LONG.equals(this.type) ? cumulativeClosePrice.subtract(cumulativeOpenPrice, mc) : cumulativeOpenPrice.subtract(cumulativeClosePrice, mc));
            if (SecType.OPT.equals(getSecType())) {
                this.profitLoss = this.profitLoss.multiply((OptionUtil.isMini(symbol) ? new BigDecimal(10) : new BigDecimal(100)), mc);
            }
            if (SecType.FUT.equals(getSecType())) {
                this.profitLoss = this.profitLoss.multiply(new BigDecimal(FuturePlMultiplier.getMultiplierByUnderlying(underlying)), mc);
            }
        }
    }

    public String print() {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
        return (id + ", " + type + ", " + status + ", " + symbol + ", " + secType + ", " + (openDate != null ? df.format(openDate.getTime()) : "-") + ", " + (closeDate != null ? df.format(closeDate.getTime()) : "-") + ", " + profitLoss);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trade trade = (Trade) o;

        return !(id != null ? !id.equals(trade.id) : trade.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getUnderlying() {
        return underlying;
    }

    public void setUnderlying(String underlying) {
        this.underlying = underlying;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public SecType getSecType() {
        return secType;
    }

    public void setSecType(SecType secType) {
        this.secType = secType;
    }

    public Integer getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(Integer openPosition) {
        this.openPosition = openPosition;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TradeType getType() {
        return type;
    }

    public void setType(TradeType type) {
        this.type = type;
    }

    public Integer getCumulativeQuantity() {
        return cumulativeQuantity;
    }

    public void setCumulativeQuantity(Integer cummulativeQuantity) {
        this.cumulativeQuantity = cummulativeQuantity;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    public Calendar getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Calendar openDate) {
        this.openDate = openDate;
    }

    public Calendar getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(Calendar closeDate) {
        this.closeDate = closeDate;
    }

    public BigDecimal getAvgOpenPrice() {
        return avgOpenPrice;
    }

    public void setAvgOpenPrice(BigDecimal avgOpenPrice) {
        this.avgOpenPrice = avgOpenPrice;
    }

    public BigDecimal getAvgClosePrice() {
        return avgClosePrice;
    }

    public void setAvgClosePrice(BigDecimal avgClosePrice) {
        this.avgClosePrice = avgClosePrice;
    }

    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(BigDecimal profitLoss) {
        this.profitLoss = profitLoss;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report source) {
        this.report = source;
    }

    public List<SplitExecution> getSplitExecutions() {
        return splitExecutions;
    }

    public void setSplitExecutions(List<SplitExecution> splitExecutions) {
        for (SplitExecution se : splitExecutions) {
            se.setTrade(this);
        }
        this.splitExecutions = splitExecutions;
    }

    public SplitExecution getLastSplitExecution() {
        return this.getSplitExecutions().get(this.getSplitExecutions().size() - 1);
    }
    
    public Boolean getOpen() {
        return (status == TradeStatus.OPEN);
    }

    @Override
    public String toString() {
        return "Trade{" +
                "id=" + id +
                ", type=" + type +
                ", symbol='" + symbol + '\'' +
                ", underlying='" + underlying + '\'' +
                ", currency=" + currency +
                ", secType=" + secType +
                ", cumulativeQuantity=" + cumulativeQuantity +
                ", status=" + status +
                ", openPosition=" + openPosition +
                ", avgOpenPrice=" + avgOpenPrice +
                ", openDate=" + openDate +
                ", avgClosePrice=" + avgClosePrice +
                ", closeDate=" + closeDate +
                ", profitLoss=" + profitLoss +
                '}';
    }
}