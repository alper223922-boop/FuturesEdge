package com.futuresedge.utils;

import com.futuresedge.model.Signal;
import com.futuresedge.network.BinanceApiService;

import java.util.ArrayList;
import java.util.List;

public class ScannerEngine {

    private final BinanceApiService api = new BinanceApiService();

    /**
     * Scans all USDT perp pairs and returns crossover signals.
     * Runs on background thread.
     */
    public List<Signal> scan(int fastPeriod, int slowPeriod,
                             MaCalculator.MaType maType,
                             String timeframe, double minVolume) throws Exception {

        List<Signal> results = new ArrayList<>();
        List<String> pairs = api.getUsdtPerpPairs(minVolume);

        // Need enough candles: slowPeriod + lookback
        int lookback = 30;
        int limit = slowPeriod + lookback + 5;
        limit = Math.min(limit, 1500);

        String interval = timeframeToInterval(timeframe);

        for (String symbol : pairs) {
            try {
                List<Double> closes = api.getClosePrices(symbol, interval, limit);
                if (closes.size() < slowPeriod + 2) continue;

                double[] fast = MaCalculator.calculate(closes, fastPeriod, maType);
                double[] slow = MaCalculator.calculate(closes, slowPeriod, maType);

                MaCalculator.CrossoverResult cross =
                        MaCalculator.findCrossover(fast, slow, lookback);
                if (cross == null) continue;

                // Get price/volume change
                BinanceApiService.TickerData ticker = api.get24hTicker(symbol);

                Signal signal = new Signal(
                        symbol,
                        cross.isGolden ? "Golden Cross" : "Death Cross",
                        ticker.priceChangePercent,
                        0, // volume change vs yesterday not available in basic ticker
                        cross.candlesAgo,
                        System.currentTimeMillis(),
                        maType.name(),
                        timeframe,
                        fastPeriod,
                        slowPeriod
                );
                results.add(signal);

                // Safety: avoid hammering API too fast
                Thread.sleep(80);

            } catch (Exception e) {
                // skip this pair silently
            }
        }

        return results;
    }

    private String timeframeToInterval(String tf) {
        switch (tf) {
            case "15m": return "15m";
            case "1h":  return "1h";
            case "4h":  return "4h";
            case "1d":  return "1d";
            case "1w":  return "1w";
            default:    return "1h";
        }
    }
}
