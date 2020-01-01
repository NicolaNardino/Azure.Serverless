package com.projects.azure.market_data;

import java.time.LocalDate;

public final class TradingStrategyInput {
    private final LocalDate marketDataDate;
    private final BollingerBands bollingerBands;

    public TradingStrategyInput(final LocalDate marketDataDate, final BollingerBands bollingerBands) {
        this.marketDataDate = marketDataDate;
        this.bollingerBands = bollingerBands;
    }

    @Override
    public String toString() {
        return "TradingStrategyInput{" +
                "marketDataDate=" + marketDataDate +
                ", bollingerBands=" + bollingerBands +
                '}';
    }
}
