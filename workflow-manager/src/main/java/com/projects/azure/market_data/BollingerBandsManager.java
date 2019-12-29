package com.projects.azure.market_data;

import java.util.Arrays;

/**
 * It contains the logic for creating the Bollinger Bands and it's based on the following definition:
 *
 * <ul>
 *     <li>Middle Band: X Days moving average</li>
 *     <li>Lower Band: Middle Band  – (2 * X Day Moving Standard Deviation)</li>
 *     <li>Upper Band: Middle Band  + (2 * X Day Moving Standard Deviation)</li>
 * </ul>
 *
 * Where X is an application setting.
 *
 * */
public final class BollingerBandsManager {

    private final MarketData[] closePricesTimeSeries;
    private final int monthlyAverageLength;
    private int timeSeriesIndex;

    public BollingerBandsManager(final MarketData[] closePriceTimeSeries, final int monthlyAverageLength) {
        this.closePricesTimeSeries = closePriceTimeSeries;
        this.monthlyAverageLength = monthlyAverageLength;
        this.timeSeriesIndex = monthlyAverageLength;
    }

    /**
     * It builds the bands by extracting X close prices from the input time series. Each time this method gets called, it builds a new close prices slice by rolling over by 1 the input time series.
     * For instance, given monthlyAverageLength = 30:
     *
     * <ul>
     *     <li>Call 1: timeSeriesIndex = 100, slice lower bound = (100 - 30) = 70, upper bound = 100 --> slice is a array of 30 items, having sliced the input time series indexes from 70 to 100(excluded).</li>
     *     <li>Call 2: timeSeriesIndex = 101, slice lower bound = (101 - 30) = 71, upper bound = 101 --> slice is again of 30 items. </li>
     * </ul>
     *
     * Given that, in order for the strategy to work, it needs to have X days historical data, it assumes the first X days in the input time series as pure historical data.
     *
     * @return Bollinger Bands.
     *
     * @see BollingerBands
     *
     * */
    public BollingerBands buildBands() {
        final int from = timeSeriesIndex - monthlyAverageLength;
        final int to = timeSeriesIndex;
        final MarketData[] xDaysPrices = Arrays.copyOfRange(closePricesTimeSeries, from, to);
        final double middleBand = Arrays.stream(xDaysPrices).map(MarketData::getClosePrice).mapToDouble(Double::valueOf).average().getAsDouble();
        final double standardDeviation = Math.sqrt(Arrays.stream(xDaysPrices).map(MarketData::getClosePrice).mapToDouble(price -> Math.pow(price.doubleValue() - middleBand, 2.0)).sum() / (xDaysPrices.length - 1));
        final double upperBand = middleBand + (2 * standardDeviation);
        final double lowerBand = middleBand - (2 * standardDeviation);
        timeSeriesIndex++;
        return new BollingerBands(middleBand, lowerBand, upperBand);
    }
}
