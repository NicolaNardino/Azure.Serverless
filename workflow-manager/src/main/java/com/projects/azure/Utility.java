package com.projects.azure;

import com.projects.azure.market_data.MarketData;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public final class Utility {
    static final List<String> tickers = Arrays.asList("GOOG", "AMEX", "FB", "MSFT");

    public static Map<String, List<MarketData>> buildMarketData(final List<String> tickers, final LocalDate startDate, final LocalDate endDate) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return tickers.stream().collect(Collectors.toMap(k -> k, v -> getDatesBetween(startDate, endDate).sorted().
                map(date -> new MarketData(date, round(random.nextDouble(0, 1), 2), round(random.nextDouble(0, 1), 2), round(random.nextDouble(0, 1), 2), round(random.nextDouble(0, 1), 2))).collect(toList())));
    }

    private static Stream<LocalDate> getDatesBetween(final LocalDate startDate, final LocalDate endDate) {
        return IntStream.iterate(0, i -> i + 1).limit(ChronoUnit.DAYS.between(startDate, endDate)).mapToObj(startDate::plusDays);
    }

    public static String printStackTrace(final Exception e) {
        final StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static double round(final double value, final int decimalPlaces) {
        return BigDecimal.valueOf(value).setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
    }
}
