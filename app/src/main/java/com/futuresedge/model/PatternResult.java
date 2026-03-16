package com.futuresedge.model;

import java.util.ArrayList;
import java.util.List;

public class PatternResult {

    public String symbol;
    public String timeframe;
    public long timestampMs;

    // ── MA Crossover ──────────────────────────────────────────────────────
    public boolean hasMaCross;
    public boolean maCrossGolden;   // true=golden, false=death
    public int     maCrossAgo;
    public double  maCrossStrength; // 0-100

    // ── RSI ───────────────────────────────────────────────────────────────
    public boolean hasRsiSignal;
    public double  rsiValue;
    public String  rsiSignal;       // "Oversold" / "Overbought" / "Bullish Div" / "Bearish Div"
    public double  rsiStrength;

    // ── MACD ──────────────────────────────────────────────────────────────
    public boolean hasMacdSignal;
    public boolean macdCrossGolden;
    public double  macdHistogram;
    public double  macdStrength;

    // ── Bollinger Bands ───────────────────────────────────────────────────
    public boolean hasBollingerSignal;
    public String  bollingerSignal; // "Squeeze" / "Breakout Up" / "Breakout Down" / "Touch Upper" / "Touch Lower"
    public double  bollingerStrength;

    // ── Volume Spike ──────────────────────────────────────────────────────
    public boolean hasVolumeSpike;
    public double  volumeRatio;     // current vol / avg vol
    public boolean volumeBullish;   // true=price up with vol, false=price down with vol

    // ── EMA Ribbon ────────────────────────────────────────────────────────
    public boolean hasEmaRibbon;
    public boolean emaRibbonBullish;
    public double  emaRibbonStrength;

    // ── Candlestick Patterns ──────────────────────────────────────────────
    public List<CandlePattern> candlePatterns = new ArrayList<>();

    // ── Price data ────────────────────────────────────────────────────────
    public double lastPrice;
    public double priceChange24h;
    public double volume24h;

    // ── Composite score & prediction ──────────────────────────────────────
    public double compositeScore;   // -100 (strong bear) to +100 (strong bull)
    public double predictedChangePercent; // estimated % move
    public String predictionLabel;  // "Strong Buy" / "Buy" / "Neutral" / "Sell" / "Strong Sell"
    public int    signalCount;      // how many signals triggered

    // ── Signal color strength 0.0-1.0 (for alpha) ─────────────────────────
    public float colorAlpha() {
        double abs = Math.abs(compositeScore);
        if (abs >= 70) return 1.0f;
        if (abs >= 50) return 0.82f;
        if (abs >= 30) return 0.62f;
        return 0.42f;
    }

    public boolean isBullish() { return compositeScore > 0; }

    public static class CandlePattern {
        public String name;       // "Hammer", "Doji", "Bullish Engulfing" etc.
        public boolean bullish;
        public double  strength;  // 0-100
        public int     barsAgo;

        public CandlePattern(String name, boolean bullish, double strength, int barsAgo) {
            this.name = name; this.bullish = bullish;
            this.strength = strength; this.barsAgo = barsAgo;
        }
    }
}
