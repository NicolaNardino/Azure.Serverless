package com.projects.azure.market_data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class MarketData {
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("YYYYMMdd");

    private final LocalDate closePriceDate;
    private final double closePrice;
    private final double openPrice;
    private final double highPrice;
    private final double lowPrice;

    public MarketData(final LocalDate closePriceDate, final double closePrice, final double openPrice, final double highPrice, final double lowPrice) {
        this.closePriceDate = closePriceDate;
        this.closePrice = closePrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
    }

    public LocalDate getClosePriceDate() {
        return closePriceDate;
    }

    public double getClosePrice() {
        return closePrice;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public double getHighPrice() {
        return highPrice;
    }

    public double getLowPrice() {
        return lowPrice;
    }

    public static String toCSV(final MarketData marketData, final String separator) {
        return marketData.getClosePriceDate().format(fmt)+separator+ String.format("%.2f", marketData.getClosePrice())+separator+String.format("%.2f", marketData.getHighPrice())+separator+
                String.format("%.2f", marketData.getLowPrice())+separator+String.format("%.2f", marketData.getOpenPrice());
    }

    @Override
    public String toString() {
        return "MarketData{" +
                "closePriceDate=" + closePriceDate +
                ", closePrice=" + closePrice +
                ", openPrice=" + openPrice +
                ", highPrice=" + highPrice +
                ", lowPrice=" + lowPrice +
                '}';
    }
}
