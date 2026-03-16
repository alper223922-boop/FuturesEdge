package com.futuresedge.utils;

import com.futuresedge.model.PatternResult;
import com.futuresedge.model.PatternResult.CandlePattern;

/**
 * Converts all detected signals into a composite score (-100 to +100)
 * and a weighted price prediction.
 *
 * Weights (total = 100):
 *   MA Crossover       25
 *   MACD               20
 *   RSI                15
 *   EMA Ribbon         15
 *   Candlestick        15
 *   Bollinger          10
 *   Volume Spike        5  (modifier, not directional)
 */
public class ScoreEngine {

    private static final double W_MA        = 25;
    private static final double W_MACD      = 20;
    private static final double W_RSI       = 15;
    private static final double W_RIBBON    = 15;
    private static final double W_CANDLE    = 15;
    private static final double W_BOLLINGER = 10;
    private static final double W_VOLUME    =  5; // multiplier

    public static void computeScore(PatternResult r) {
        double score = 0;
        int signalCount = 0;

        // ── MA Crossover ─────────────────────────────────────────────────
        if (r.hasMaCross) {
            double dir = r.maCrossGolden ? 1 : -1;
            // Decay with candles ago (max lookback 30)
            double decay = Math.max(0.3, 1.0 - r.maCrossAgo * 0.03);
            score += dir * W_MA * (r.maCrossStrength / 100.0) * decay;
            signalCount++;
        }

        // ── MACD ─────────────────────────────────────────────────────────
        if (r.hasMacdSignal) {
            double dir = r.macdCrossGolden ? 1 : -1;
            score += dir * W_MACD * (r.macdStrength / 100.0);
            signalCount++;
        }

        // ── RSI ──────────────────────────────────────────────────────────
        if (r.hasRsiSignal) {
            double rsiDir = 0;
            if ("Oversold".equals(r.rsiSignal) || "Bullish Div".equals(r.rsiSignal))  rsiDir =  1;
            if ("Overbought".equals(r.rsiSignal) || "Bearish Div".equals(r.rsiSignal)) rsiDir = -1;
            score += rsiDir * W_RSI * (r.rsiStrength / 100.0);
            signalCount++;
        }

        // ── EMA Ribbon ───────────────────────────────────────────────────
        if (r.hasEmaRibbon) {
            double dir = r.emaRibbonBullish ? 1 : -1;
            score += dir * W_RIBBON * Math.min(r.emaRibbonStrength / 5.0, 1.0);
            signalCount++;
        }

        // ── Candlestick patterns ──────────────────────────────────────────
        if (!r.candlePatterns.isEmpty()) {
            double candleScore = 0;
            double maxStr = 0;
            for (CandlePattern cp : r.candlePatterns) {
                double dir = cp.bullish ? 1 : -1;
                candleScore += dir * cp.strength;
                maxStr = Math.max(maxStr, cp.strength);
            }
            // Average and normalize
            candleScore /= r.candlePatterns.size();
            score += (candleScore / 100.0) * W_CANDLE;
            signalCount++;
        }

        // ── Bollinger ────────────────────────────────────────────────────
        if (r.hasBollingerSignal) {
            double dir = 0;
            switch (r.bollingerSignal) {
                case "Breakout Up":   dir =  1.0; break;
                case "Touch Lower":   dir =  0.7; break;
                case "Squeeze":       dir =  0.0; break; // neutral
                case "Touch Upper":   dir = -0.7; break;
                case "Breakout Down": dir = -1.0; break;
            }
            score += dir * W_BOLLINGER * (r.bollingerStrength / 100.0);
            signalCount++;
        }

        // ── Volume spike modifier ─────────────────────────────────────────
        // If volume spike exists and confirms direction → boost by up to 5%
        if (r.hasVolumeSpike) {
            double volBoost = Math.min(r.volumeRatio / 5.0, 1.0) * W_VOLUME;
            double currentDir = score >= 0 ? 1 : -1;
            double volDir = r.volumeBullish ? 1 : -1;
            if (currentDir == volDir) {
                score += volDir * volBoost; // confirms trend
            } else {
                score -= Math.abs(volBoost) * 0.5; // contradicts → slight penalty
            }
        }

        // Clamp to [-100, +100]
        score = Math.max(-100, Math.min(100, score));

        r.compositeScore = score;
        r.signalCount    = signalCount;

        // ── Price prediction ──────────────────────────────────────────────
        // Base: 1% per 20 score points, scaled by signal count confidence
        double confidence = Math.min(signalCount / 5.0, 1.0);
        double basePct = (score / 20.0) * confidence;

        // Boost if volume confirms
        if (r.hasVolumeSpike && r.volumeRatio > 2.0) {
            basePct *= (1 + Math.min(r.volumeRatio / 10.0, 0.5));
        }

        r.predictedChangePercent = basePct;

        // Label
        if      (score >=  70) r.predictionLabel = "Strong Buy";
        else if (score >=  35) r.predictionLabel = "Buy";
        else if (score >=  10) r.predictionLabel = "Weak Buy";
        else if (score >  -10) r.predictionLabel = "Neutral";
        else if (score > -35)  r.predictionLabel = "Weak Sell";
        else if (score > -70)  r.predictionLabel = "Sell";
        else                   r.predictionLabel = "Strong Sell";
    }
}
