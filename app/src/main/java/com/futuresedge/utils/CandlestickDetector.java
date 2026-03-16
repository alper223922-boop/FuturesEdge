package com.futuresedge.utils;

import com.futuresedge.model.PatternResult.CandlePattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Candlestick pattern detector.
 * All patterns use last 3 candles (index: -3, -2, -1 = current).
 */
public class CandlestickDetector {

    public static class Candle {
        public double open, high, low, close, volume;
        public Candle(double o, double h, double l, double c, double v) {
            open=o; high=h; low=l; close=c; volume=v;
        }
        public double body()      { return Math.abs(close - open); }
        public double range()     { return high - low; }
        public double upperWick() { return high - Math.max(open, close); }
        public double lowerWick() { return Math.min(open, close) - low; }
        public boolean isBullish(){ return close > open; }
        public boolean isBearish(){ return close < open; }
    }

    public static List<CandlePattern> detect(List<Candle> candles) {
        List<CandlePattern> results = new ArrayList<>();
        int n = candles.size();
        if (n < 3) return results;

        Candle c0 = candles.get(n - 3); // 3 bars ago
        Candle c1 = candles.get(n - 2); // 2 bars ago
        Candle c2 = candles.get(n - 1); // current (last closed)

        // ── Single candle patterns ────────────────────────────────────────────

        // Doji (body < 10% of range)
        if (c2.range() > 0 && c2.body() / c2.range() < 0.10) {
            boolean bull = c2.close >= c2.open;
            results.add(new CandlePattern("Doji", bull, 55, 0));
        }

        // Hammer (bullish): small body at top, long lower wick >= 2x body, tiny upper wick
        if (c2.body() > 0 && c2.lowerWick() >= 2 * c2.body()
                && c2.upperWick() <= 0.3 * c2.body()
                && c2.isBullish()) {
            results.add(new CandlePattern("Hammer", true, 70, 0));
        }

        // Inverted Hammer (bullish reversal after downtrend)
        if (c2.body() > 0 && c2.upperWick() >= 2 * c2.body()
                && c2.lowerWick() <= 0.3 * c2.body()
                && c2.isBullish()) {
            results.add(new CandlePattern("Inverted Hammer", true, 60, 0));
        }

        // Hanging Man (bearish): same shape as hammer but after uptrend
        if (c2.body() > 0 && c2.lowerWick() >= 2 * c2.body()
                && c2.upperWick() <= 0.3 * c2.body()
                && c2.isBearish()) {
            results.add(new CandlePattern("Hanging Man", false, 65, 0));
        }

        // Shooting Star (bearish): long upper wick, small body at bottom
        if (c2.body() > 0 && c2.upperWick() >= 2 * c2.body()
                && c2.lowerWick() <= 0.3 * c2.body()
                && c2.isBearish()) {
            results.add(new CandlePattern("Shooting Star", false, 70, 0));
        }

        // Marubozu Bullish: no wicks, strong body
        if (c2.range() > 0 && c2.body() / c2.range() > 0.95 && c2.isBullish()) {
            results.add(new CandlePattern("Bullish Marubozu", true, 75, 0));
        }

        // Marubozu Bearish
        if (c2.range() > 0 && c2.body() / c2.range() > 0.95 && c2.isBearish()) {
            results.add(new CandlePattern("Bearish Marubozu", false, 75, 0));
        }

        // Spinning Top: small body, wicks on both sides
        if (c2.range() > 0 && c2.body() / c2.range() < 0.30
                && c2.upperWick() > c2.body() && c2.lowerWick() > c2.body()) {
            results.add(new CandlePattern("Spinning Top", c2.isBullish(), 40, 0));
        }

        // ── Two candle patterns ───────────────────────────────────────────────

        // Bullish Engulfing: bearish c1, bullish c2 that engulfs c1
        if (c1.isBearish() && c2.isBullish()
                && c2.open < c1.close && c2.close > c1.open) {
            results.add(new CandlePattern("Bullish Engulfing", true, 80, 0));
        }

        // Bearish Engulfing
        if (c1.isBullish() && c2.isBearish()
                && c2.open > c1.close && c2.close < c1.open) {
            results.add(new CandlePattern("Bearish Engulfing", false, 80, 0));
        }

        // Bullish Harami: big bearish c1, small bullish c2 inside c1 body
        if (c1.isBearish() && c2.isBullish()
                && c2.open > c1.close && c2.close < c1.open
                && c2.body() < c1.body() * 0.5) {
            results.add(new CandlePattern("Bullish Harami", true, 60, 0));
        }

        // Bearish Harami
        if (c1.isBullish() && c2.isBearish()
                && c2.open < c1.close && c2.close > c1.open
                && c2.body() < c1.body() * 0.5) {
            results.add(new CandlePattern("Bearish Harami", false, 60, 0));
        }

        // Piercing Line (bullish): bearish c1, bullish c2 opens below c1 low, closes > 50% of c1
        if (c1.isBearish() && c2.isBullish()
                && c2.open < c1.low
                && c2.close > (c1.open + c1.close) / 2
                && c2.close < c1.open) {
            results.add(new CandlePattern("Piercing Line", true, 70, 0));
        }

        // Dark Cloud Cover (bearish): bullish c1, bearish c2 opens above c1 high, closes below midpoint
        if (c1.isBullish() && c2.isBearish()
                && c2.open > c1.high
                && c2.close < (c1.open + c1.close) / 2
                && c2.close > c1.open) {
            results.add(new CandlePattern("Dark Cloud Cover", false, 70, 0));
        }

        // Tweezer Bottom (bullish): c1 and c2 have same low
        if (c1.isBearish() && c2.isBullish()
                && Math.abs(c1.low - c2.low) / c1.close < 0.002) {
            results.add(new CandlePattern("Tweezer Bottom", true, 65, 0));
        }

        // Tweezer Top (bearish)
        if (c1.isBullish() && c2.isBearish()
                && Math.abs(c1.high - c2.high) / c1.close < 0.002) {
            results.add(new CandlePattern("Tweezer Top", false, 65, 0));
        }

        // ── Three candle patterns ─────────────────────────────────────────────

        // Morning Star (bullish): bearish c0, doji/small c1, bullish c2
        if (c0.isBearish() && c2.isBullish()
                && c1.body() < c0.body() * 0.3
                && c2.close > (c0.open + c0.close) / 2) {
            results.add(new CandlePattern("Morning Star", true, 85, 0));
        }

        // Evening Star (bearish)
        if (c0.isBullish() && c2.isBearish()
                && c1.body() < c0.body() * 0.3
                && c2.close < (c0.open + c0.close) / 2) {
            results.add(new CandlePattern("Evening Star", false, 85, 0));
        }

        // Three White Soldiers (bullish): 3 consecutive bullish candles, each closes higher
        if (c0.isBullish() && c1.isBullish() && c2.isBullish()
                && c1.close > c0.close && c2.close > c1.close
                && c1.open > c0.open && c2.open > c1.open) {
            results.add(new CandlePattern("Three White Soldiers", true, 90, 0));
        }

        // Three Black Crows (bearish)
        if (c0.isBearish() && c1.isBearish() && c2.isBearish()
                && c1.close < c0.close && c2.close < c1.close
                && c1.open < c0.open && c2.open < c1.open) {
            results.add(new CandlePattern("Three Black Crows", false, 90, 0));
        }

        // Three Inside Up (bullish): bearish c0, bullish harami c1, bullish c2 above c0 open
        if (c0.isBearish() && c1.isBullish()
                && c1.open > c0.close && c1.close < c0.open
                && c2.isBullish() && c2.close > c0.open) {
            results.add(new CandlePattern("Three Inside Up", true, 78, 0));
        }

        // Three Inside Down (bearish)
        if (c0.isBullish() && c1.isBearish()
                && c1.open < c0.close && c1.close > c0.open
                && c2.isBearish() && c2.close < c0.open) {
            results.add(new CandlePattern("Three Inside Down", false, 78, 0));
        }

        // Abandoned Baby (bullish): bearish c0, doji c1 gapped below, bullish c2 gapped above
        if (c0.isBearish() && c2.isBullish()
                && c1.body() / Math.max(c1.range(), 0.0001) < 0.10
                && c1.high < c0.low && c1.low < c2.open) {
            results.add(new CandlePattern("Abandoned Baby", true, 92, 0));
        }

        return results;
    }
}
