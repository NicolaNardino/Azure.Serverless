package com.projects.azure.market_data;

import java.time.LocalDate;

public final class MarketData {
    private final LocalDate date;
    private final double close;
    private final double open;
    private final double high;
    private final double low;

    public MarketData(final LocalDate date, final double close, final double open, final double high, final double low) {
        this.date = date;
        this.close = close;
        this.open = open;
        this.high = high;
        this.low = low;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getClose() {
        return close;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    @Override
    public String toString() {
        return "MarketData{" +
                "priceDate=" + date +
                ", close=" + close +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                '}';
    }
}
