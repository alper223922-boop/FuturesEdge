package com.futuresedge.utils;

import java.util.List;

/**
 * Moving average calculations: EMA, SMA, WMA
 */
public class MaCalculator {

    public enum MaType { EMA, SMA, WMA }

    /** Returns the last N MA values (size = closes.size() - period + 1) */
    public static double[] calculate(List<Double> closes, int period, MaType type) {
        switch (type) {
            case EMA: return calculateEMA(closes, period);
            case SMA: return calculateSMA(closes, period);
            case WMA: return calculateWMA(closes, period);
            default:  return calculateEMA(closes, period);
        }
    }

    // ── EMA ──────────────────────────────────────────────────────────────────
    public static double[] calculateEMA(List<Double> closes, int period) {
        if (closes.size() < period) return new double[0];
        double[] ema = new double[closes.size() - period + 1];
        double multiplier = 2.0 / (period + 1);

        // seed with SMA
        double sum = 0;
        for (int i = 0; i < period; i++) sum += closes.get(i);
        ema[0] = sum / period;

        for (int i = 1; i < ema.length; i++) {
            ema[i] = (closes.get(period - 1 + i) - ema[i - 1]) * multiplier + ema[i - 1];
        }
        return ema;
    }

    // ── SMA ──────────────────────────────────────────────────────────────────
    public static double[] calculateSMA(List<Double> closes, int period) {
        if (closes.size() < period) return new double[0];
        double[] sma = new double[closes.size() - period + 1];
        double sum = 0;
        for (int i = 0; i < period; i++) sum += closes.get(i);
        sma[0] = sum / period;
        for (int i = 1; i < sma.length; i++) {
            sum += closes.get(period - 1 + i) - closes.get(i - 1);
            sma[i] = sum / period;
        }
        return sma;
    }

    // ── WMA ──────────────────────────────────────────────────────────────────
    public static double[] calculateWMA(List<Double> closes, int period) {
        if (closes.size() < period) return new double[0];
        double[] wma = new double[closes.size() - period + 1];
        double weightSum = period * (period + 1) / 2.0;
        for (int i = 0; i < wma.length; i++) {
            double val = 0;
            for (int j = 0; j < period; j++) {
                val += closes.get(i + j) * (j + 1);
            }
            wma[i] = val / weightSum;
        }
        return wma;
    }

    /**
     * Finds the most recent crossover.
     * Returns candlesAgo (0 = current candle) or -1 if no crossover found
     * within lookback candles.
     * isGolden = fast crossed above slow
     */
    public static CrossoverResult findCrossover(double[] fast, double[] slow, int lookback) {
        int len = Math.min(fast.length, slow.length);
        if (len < 2) return null;

        for (int i = len - 1; i >= Math.max(1, len - lookback); i--) {
            boolean currFastAbove = fast[i] > slow[i];
            boolean prevFastAbove = fast[i - 1] > slow[i - 1];

            if (currFastAbove && !prevFastAbove) {
                // Golden cross
                return new CrossoverResult(true, len - 1 - i);
            } else if (!currFastAbove && prevFastAbove) {
                // Death cross
                return new CrossoverResult(false, len - 1 - i);
            }
        }
        return null;
    }

    public static class CrossoverResult {
        public final boolean isGolden;
        public final int candlesAgo;

        public CrossoverResult(boolean isGolden, int candlesAgo) {
            this.isGolden = isGolden;
            this.candlesAgo = candlesAgo;
        }
    }
}
