package com.projects.azure.market_data;

public final class TradingStrategyInput {
    private final double marketPrice;
    private final BollingerBands bollingerBands;

    public TradingStrategyInput(final double marketPrice, final BollingerBands bollingerBands) {
        this.marketPrice = marketPrice;
        this.bollingerBands = bollingerBands;
    }

    public double getMarketPrice() {
        return marketPrice;
    }

    public BollingerBands getBollingerBands() {
        return bollingerBands;
    }

    @Override
    public String toString() {
        return "TradingStrategyInput{" +
                "marketPrice=" + marketPrice +
                ", bollingerBands=" + bollingerBands +
                '}';
    }
}
