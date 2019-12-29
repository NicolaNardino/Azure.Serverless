package com.projects.azure.market_data;

/**
 * POJO wrapping the 3 Bollinger Bands: middle, lower and upper.
 *
 * */
public final class BollingerBands {
    private final double middleBand;
    private final double lowerBand;
    private final double upperBand;

    public BollingerBands(final double middleBand, final double lowerBand, final double upperBand) {
        this.middleBand = middleBand;
        this.lowerBand = lowerBand;
        this.upperBand = upperBand;
    }

    public double getMiddleBand() {
        return middleBand;
    }

    public double getLowerBand() {
        return lowerBand;
    }

    public double getUpperBand() {
        return upperBand;
    }

    @Override
    public String toString() {
        return "BollingerBands{" + "middleBand=" + middleBand + ", lowerBand=" + lowerBand + ", upperBand=" + upperBand + "}";
    }
}
