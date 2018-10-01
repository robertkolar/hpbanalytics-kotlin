package com.highpowerbear.hpbanalytics.common.model;

import java.io.Serializable;

/**
 * Created by robertk on 3/21/2018.
 */
public class ExecutionDto implements Serializable {
    private static final long serialVersionUID = -1776809755883405517L;

    private final String acctNumber;
    private final long permId;
    private final String side;
    private final int cumQty;
    private final String symbol;
    private final String localSymbol;
    private final String currency;
    private final String secType;
    private final double price;

    public ExecutionDto(String acctNumber, long permId, String side, int cumQty, String symbol, String localSymbol, String currency, String secType, double price) {
        this.acctNumber = acctNumber;
        this.permId = permId;
        this.side = side;
        this.cumQty = cumQty;
        this.symbol = symbol;
        this.localSymbol = localSymbol;
        this.currency = currency;
        this.secType = secType;
        this.price = price;
    }

    public String getAcctNumber() {
        return acctNumber;
    }

    public long getPermId() {
        return permId;
    }

    public String getSide() {
        return side;
    }

    public int getCumQty() {
        return cumQty;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getLocalSymbol() {
        return localSymbol;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSecType() {
        return secType;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "ExecutionDto{" +
                "acctNumber='" + acctNumber + '\'' +
                ", permId=" + permId +
                ", side='" + side + '\'' +
                ", cumQty=" + cumQty +
                ", symbol='" + symbol + '\'' +
                ", localSymbol='" + localSymbol + '\'' +
                ", currency='" + currency + '\'' +
                ", secType='" + secType + '\'' +
                ", price=" + price +
                '}';
    }
}