package com.projects.azure.market_data;

import java.time.LocalDate;

public final class MarketData {
    private final LocalDate priceDate;
    private final double close;
    private final double open;
    private final double high;
    private final double low;

    public MarketData(final LocalDate priceDate, final double close, final double open, final double high, final double low) {
        this.priceDate = priceDate;
        this.close = close;
        this.open = open;
        this.high = high;
        this.low = low;
    }

    public LocalDate getPriceDate() {
        return priceDate;
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
                "priceDate=" + priceDate +
                ", close=" + close +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                '}';
    }
}
