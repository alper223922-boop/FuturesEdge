package com.futuresedge.utils;

import java.util.List;

public class IndicatorCalculator {

    // ── RSI ───────────────────────────────────────────────────────────────────
    public static double[] calculateRSI(List<Double> closes, int period) {
        if (closes.size() < period + 1) return new double[0];
        double[] rsi = new double[closes.size() - period];

        double gainSum = 0, lossSum = 0;
        for (int i = 1; i <= period; i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            if (diff > 0) gainSum += diff; else lossSum -= diff;
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        rsi[0] = avgLoss == 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss));

        for (int i = 1; i < rsi.length; i++) {
            double diff = closes.get(period + i) - closes.get(period + i - 1);
            double gain = diff > 0 ? diff : 0;
            double loss = diff < 0 ? -diff : 0;
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
            rsi[i] = avgLoss == 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss));
        }
        return rsi;
    }

    // ── MACD ──────────────────────────────────────────────────────────────────
    public static class MacdResult {
        public double[] macdLine;
        public double[] signalLine;
        public double[] histogram;
    }

    public static MacdResult calculateMACD(List<Double> closes,
                                           int fastPeriod, int slowPeriod, int signalPeriod) {
        double[] fastEma = MaCalculator.calculateEMA(closes, fastPeriod);
        double[] slowEma = MaCalculator.calculateEMA(closes, slowPeriod);

        // align: slowEma is shorter
        int offset = slowPeriod - fastPeriod;
        int len = slowEma.length;
        double[] macdLine = new double[len];
        for (int i = 0; i < len; i++) {
            macdLine[i] = fastEma[i + offset] - slowEma[i];
        }

        // Signal = EMA of macdLine
        java.util.List<Double> macdList = new java.util.ArrayList<>();
        for (double v : macdLine) macdList.add(v);
        double[] signal = MaCalculator.calculateEMA(macdList, signalPeriod);

        int sigOffset = macdLine.length - signal.length;
        double[] histogram = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            histogram[i] = macdLine[sigOffset + i] - signal[i];
        }

        MacdResult r = new MacdResult();
        r.macdLine  = macdLine;
        r.signalLine = signal;
        r.histogram  = histogram;
        return r;
    }

    // ── Bollinger Bands ───────────────────────────────────────────────────────
    public static class BollingerResult {
        public double upper, middle, lower, bandwidth, percentB;
    }

    public static BollingerResult calculateBollinger(List<Double> closes, int period, double multiplier) {
        if (closes.size() < period) return null;
        int last = closes.size();
        double sum = 0;
        for (int i = last - period; i < last; i++) sum += closes.get(i);
        double sma = sum / period;

        double variance = 0;
        for (int i = last - period; i < last; i++) {
            double d = closes.get(i) - sma;
            variance += d * d;
        }
        double std = Math.sqrt(variance / period);

        BollingerResult r = new BollingerResult();
        r.middle    = sma;
        r.upper     = sma + multiplier * std;
        r.lower     = sma - multiplier * std;
        r.bandwidth = (r.upper - r.lower) / r.middle * 100;

        double currentPrice = closes.get(last - 1);
        r.percentB  = std == 0 ? 0.5 : (currentPrice - r.lower) / (r.upper - r.lower);
        return r;
    }

    // ── EMA Ribbon ────────────────────────────────────────────────────────────
    // Uses 8, 13, 21, 34, 55 EMAs — bullish if all aligned up (8>13>21>34>55)
    public static class RibbonResult {
        public boolean bullish;
        public boolean aligned;  // all in order
        public double  strength; // % spread between fastest and slowest
    }

    public static RibbonResult calculateEmaRibbon(List<Double> closes) {
        int[] periods = {8, 13, 21, 34, 55};
        double[] vals = new double[periods.length];

        for (int p = 0; p < periods.length; p++) {
            double[] ema = MaCalculator.calculateEMA(closes, periods[p]);
            if (ema.length == 0) return null;
            vals[p] = ema[ema.length - 1];
        }

        // Check if strictly decreasing (bullish: 8>13>21>34>55)
        boolean bullishAligned = true, bearishAligned = true;
        for (int i = 0; i < vals.length - 1; i++) {
            if (vals[i] <= vals[i + 1]) bullishAligned = false;
            if (vals[i] >= vals[i + 1]) bearishAligned = false;
        }

        RibbonResult r = new RibbonResult();
        r.bullish  = bullishAligned;
        r.aligned  = bullishAligned || bearishAligned;
        r.strength = vals[0] != 0 ? Math.abs(vals[0] - vals[vals.length - 1]) / vals[0] * 100 : 0;
        return r;
    }

    // ── Volume Spike ──────────────────────────────────────────────────────────
    /** Returns ratio of last volume vs average of previous `avgPeriod` candles */
    public static double volumeSpike(List<Double> volumes, int avgPeriod) {
        if (volumes.size() < avgPeriod + 1) return 1.0;
        int last = volumes.size() - 1;
        double sum = 0;
        for (int i = last - avgPeriod; i < last; i++) sum += volumes.get(i);
        double avg = sum / avgPeriod;
        return avg == 0 ? 1.0 : volumes.get(last) / avg;
    }
}
