package com.futuresedge.utils;

import android.util.Log;

import com.futuresedge.model.PatternResult;
import com.futuresedge.network.BinanceApiService;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class FullScannerEngine {

    private static final String TAG = "FullScanner";
    private final BinanceApiService api = new BinanceApiService();

    // Progress callback
    public interface ProgressCallback {
        void onProgress(int scanned, int total, String currentSymbol);
    }

    /**
     * Scans ALL Binance USDT perpetual pairs with no volume filter.
     * Returns PatternResult for every pair that has at least 1 signal.
     */
    public List<PatternResult> scanAll(String timeframe,
                                       int fastPeriod, int slowPeriod,
                                       MaCalculator.MaType maType,
                                       ProgressCallback cb) throws Exception {

        List<PatternResult> results = new ArrayList<>();

        // Get ALL USDT perp pairs (no volume filter → minVolume = 0)
        List<String> pairs = api.getUsdtPerpPairs(0);
        Log.d(TAG, "Total pairs to scan: " + pairs.size());

        String interval = timeframeToInterval(timeframe);
        // Need: slowPeriod + 60 candles for indicator history
        int limit = Math.min(slowPeriod + 60, 1500);

        for (int i = 0; i < pairs.size(); i++) {
            String symbol = pairs.get(i);
            if (cb != null) cb.onProgress(i + 1, pairs.size(), symbol);

            try {
                PatternResult r = analyzePair(symbol, interval, limit,
                        fastPeriod, slowPeriod, maType, timeframe);
                if (r != null && r.signalCount > 0) {
                    results.add(r);
                }
                Thread.sleep(60); // ~16 req/sec — safe for Binance rate limit
            } catch (Exception e) {
                Log.w(TAG, "Skip " + symbol + ": " + e.getMessage());
            }
        }

        // Sort by |compositeScore| descending
        results.sort((a, b) ->
                Double.compare(Math.abs(b.compositeScore), Math.abs(a.compositeScore)));

        return results;
    }

    private PatternResult analyzePair(String symbol, String interval, int limit,
                                      int fastPeriod, int slowPeriod,
                                      MaCalculator.MaType maType,
                                      String timeframe) throws Exception {

        // Fetch OHLCV data
        BinanceApiService.OhlcvData data = api.getOhlcv(symbol, interval, limit);
        if (data == null || data.closes.size() < slowPeriod + 5) return null;

        PatternResult r = new PatternResult();
        r.symbol     = symbol;
        r.timeframe  = timeframe;
        r.timestampMs = System.currentTimeMillis();
        r.lastPrice  = data.closes.get(data.closes.size() - 1);

        // ── MA Crossover ─────────────────────────────────────────────────
        double[] fast = MaCalculator.calculate(data.closes, fastPeriod, maType);
        double[] slow = MaCalculator.calculate(data.closes, slowPeriod, maType);
        MaCalculator.CrossoverResult cross = MaCalculator.findCrossover(fast, slow, 30);
        if (cross != null) {
            r.hasMaCross      = true;
            r.maCrossGolden   = cross.isGolden;
            r.maCrossAgo      = cross.candlesAgo;
            // Strength: how far apart the MAs are as % of price
            if (fast.length > 0 && slow.length > 0 && r.lastPrice > 0) {
                int fi = fast.length - 1, si = slow.length - 1;
                r.maCrossStrength = Math.abs(fast[fi] - slow[si]) / r.lastPrice * 1000;
                r.maCrossStrength = Math.min(r.maCrossStrength, 100);
            }
        }

        // ── MACD (12,26,9) ────────────────────────────────────────────────
        if (data.closes.size() >= 35) {
            IndicatorCalculator.MacdResult macd =
                    IndicatorCalculator.calculateMACD(data.closes, 12, 26, 9);
            if (macd.histogram != null && macd.histogram.length >= 2) {
                int hi = macd.histogram.length - 1;
                double prevHist = macd.histogram[hi - 1];
                double currHist = macd.histogram[hi];
                // Signal: histogram crosses zero
                if ((prevHist < 0 && currHist > 0) || (prevHist > 0 && currHist < 0)) {
                    r.hasMacdSignal    = true;
                    r.macdCrossGolden  = currHist > 0;
                    r.macdHistogram    = currHist;
                    r.macdStrength     = Math.min(Math.abs(currHist) / r.lastPrice * 10000, 100);
                }
            }
        }

        // ── RSI (14) ──────────────────────────────────────────────────────
        if (data.closes.size() >= 20) {
            double[] rsi = IndicatorCalculator.calculateRSI(data.closes, 14);
            if (rsi.length > 0) {
                double rsiVal = rsi[rsi.length - 1];
                r.rsiValue = rsiVal;
                if (rsiVal <= 30) {
                    r.hasRsiSignal = true;
                    r.rsiSignal    = "Oversold";
                    r.rsiStrength  = (30 - rsiVal) / 30.0 * 100;
                } else if (rsiVal >= 70) {
                    r.hasRsiSignal = true;
                    r.rsiSignal    = "Overbought";
                    r.rsiStrength  = (rsiVal - 70) / 30.0 * 100;
                }
                // Divergence: RSI trend vs price trend (last 5 bars)
                else if (rsi.length >= 5) {
                    double rsiSlope   = rsi[rsi.length-1] - rsi[rsi.length-5];
                    double priceSlope = data.closes.get(data.closes.size()-1)
                            - data.closes.get(data.closes.size()-5);
                    if (rsiSlope > 1 && priceSlope < 0) {
                        r.hasRsiSignal = true; r.rsiSignal = "Bullish Div"; r.rsiStrength = 65;
                    } else if (rsiSlope < -1 && priceSlope > 0) {
                        r.hasRsiSignal = true; r.rsiSignal = "Bearish Div"; r.rsiStrength = 65;
                    }
                }
            }
        }

        // ── Bollinger Bands (20, 2) ────────────────────────────────────────
        if (data.closes.size() >= 22) {
            IndicatorCalculator.BollingerResult bb =
                    IndicatorCalculator.calculateBollinger(data.closes, 20, 2.0);
            if (bb != null) {
                double price = r.lastPrice;
                if (price > bb.upper) {
                    r.hasBollingerSignal = true; r.bollingerSignal = "Breakout Up";
                    r.bollingerStrength  = Math.min((price - bb.upper) / bb.upper * 1000, 100);
                } else if (price < bb.lower) {
                    r.hasBollingerSignal = true; r.bollingerSignal = "Breakout Down";
                    r.bollingerStrength  = Math.min((bb.lower - price) / bb.lower * 1000, 100);
                } else if (bb.percentB >= 0.95) {
                    r.hasBollingerSignal = true; r.bollingerSignal = "Touch Upper"; r.bollingerStrength = 60;
                } else if (bb.percentB <= 0.05) {
                    r.hasBollingerSignal = true; r.bollingerSignal = "Touch Lower"; r.bollingerStrength = 60;
                } else if (bb.bandwidth < 3.0) {
                    r.hasBollingerSignal = true; r.bollingerSignal = "Squeeze"; r.bollingerStrength = 50;
                }
            }
        }

        // ── EMA Ribbon ────────────────────────────────────────────────────
        if (data.closes.size() >= 60) {
            IndicatorCalculator.RibbonResult ribbon =
                    IndicatorCalculator.calculateEmaRibbon(data.closes);
            if (ribbon != null && ribbon.aligned) {
                r.hasEmaRibbon       = true;
                r.emaRibbonBullish   = ribbon.bullish;
                r.emaRibbonStrength  = ribbon.strength;
            }
        }

        // ── Volume Spike ──────────────────────────────────────────────────
        if (data.volumes.size() >= 21) {
            double ratio = IndicatorCalculator.volumeSpike(data.volumes, 20);
            if (ratio >= 1.5) {
                r.hasVolumeSpike  = true;
                r.volumeRatio     = ratio;
                // Bullish if price closed higher than open on spike candle
                int last = data.closes.size() - 1;
                r.volumeBullish   = data.closes.get(last) > data.opens.get(last);
            }
        }

        // ── Candlestick Patterns ──────────────────────────────────────────
        if (data.closes.size() >= 3) {
            int n = data.closes.size();
            List<CandlestickDetector.Candle> candles = new ArrayList<>();
            // Use last 3 candles
            for (int i = Math.max(0, n - 3); i < n; i++) {
                candles.add(new CandlestickDetector.Candle(
                        data.opens.get(i), data.highs.get(i),
                        data.lows.get(i),  data.closes.get(i),
                        data.volumes.get(i)));
            }
            r.candlePatterns = CandlestickDetector.detect(candles);
        }

        // ── 24h ticker data ───────────────────────────────────────────────
        BinanceApiService.TickerData ticker = api.get24hTicker(symbol);
        r.priceChange24h = ticker.priceChangePercent;
        r.volume24h      = ticker.quoteVolume;

        // ── Composite score & prediction ──────────────────────────────────
        ScoreEngine.computeScore(r);

        return r;
    }

    private String timeframeToInterval(String tf) {
        switch (tf) {
            case "15m": return "15m";
            case "4h":  return "4h";
            case "1d":  return "1d";
            case "1w":  return "1w";
            default:    return "1h";
        }
    }
}
