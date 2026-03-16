package com.futuresedge.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.futuresedge.R;
import com.futuresedge.model.PatternResult;
import com.futuresedge.model.PatternResult.CandlePattern;

import java.util.List;
import java.util.Locale;

public class PatternResultAdapter extends RecyclerView.Adapter<PatternResultAdapter.VH> {

    private final List<PatternResult> items;
    private final Context ctx;

    // Base colors
    private static final int COLOR_BULL_BASE = Color.parseColor("#00C853");
    private static final int COLOR_BEAR_BASE = Color.parseColor("#FF3D3D");
    private static final int COLOR_NEUTRAL   = Color.parseColor("#F0A500");

    public PatternResultAdapter(Context ctx, List<PatternResult> items) {
        this.ctx = ctx; this.items = items;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_pattern_result, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        PatternResult r = items.get(pos);

        // ── Coin name with strength-based alpha ───────────────────────────
        String displayPair = r.symbol.endsWith("USDT")
                ? r.symbol.replace("USDT", "/USDT") : r.symbol;
        h.tvCoinName.setText(displayPair);

        int baseColor  = r.compositeScore > 5  ? COLOR_BULL_BASE
                       : r.compositeScore < -5 ? COLOR_BEAR_BASE
                       : COLOR_NEUTRAL;
        float alpha    = r.colorAlpha();
        h.tvCoinName.setTextColor(alphaColor(baseColor, alpha));

        // ── Score badge ───────────────────────────────────────────────────
        String scoreStr = (r.compositeScore >= 0 ? "+" : "") +
                String.format(Locale.US, "%.0f", r.compositeScore);
        h.tvScore.setText(scoreStr);
        h.tvScore.setTextColor(baseColor);

        // ── Prediction label ──────────────────────────────────────────────
        h.tvPredictionLabel.setText(r.predictionLabel);
        h.tvPredictionLabel.setTextColor(predictionColor(r.predictionLabel));

        // ── Price change ──────────────────────────────────────────────────
        h.tvPriceChange.setText(String.format(Locale.US, "24h: %+.2f%%", r.priceChange24h));
        h.tvPriceChange.setTextColor(r.priceChange24h >= 0 ? COLOR_BULL_BASE : COLOR_BEAR_BASE);

        // ── Predicted change ──────────────────────────────────────────────
        String pred = String.format(Locale.US, "Target: %+.1f%%", r.predictedChangePercent);
        h.tvPredictedChange.setText(pred);
        h.tvPredictedChange.setTextColor(
                r.predictedChangePercent >= 0 ? COLOR_BULL_BASE : COLOR_BEAR_BASE);

        // ── Signal count ──────────────────────────────────────────────────
        h.tvSignalCount.setText(r.signalCount + " signal" + (r.signalCount != 1 ? "s" : ""));

        // ── Signal chips ──────────────────────────────────────────────────
        h.llChips.removeAllViews();
        addChipIfTrue(h.llChips, r.hasMaCross,
                r.maCrossGolden ? "MA ↑" : "MA ↓", r.maCrossGolden);
        addChipIfTrue(h.llChips, r.hasMacdSignal,
                r.macdCrossGolden ? "MACD ↑" : "MACD ↓", r.macdCrossGolden);
        addChipIfTrue(h.llChips, r.hasRsiSignal, "RSI " + r.rsiSignal,
                "Oversold".equals(r.rsiSignal) || "Bullish Div".equals(r.rsiSignal));
        addChipIfTrue(h.llChips, r.hasBollingerSignal, "BB " + r.bollingerSignal,
                r.bollingerSignal != null && (r.bollingerSignal.contains("Up") ||
                        r.bollingerSignal.contains("Lower") || r.bollingerSignal.contains("Squeeze")));
        addChipIfTrue(h.llChips, r.hasEmaRibbon,
                r.emaRibbonBullish ? "Ribbon ↑" : "Ribbon ↓", r.emaRibbonBullish);
        if (r.hasVolumeSpike) {
            addChip(h.llChips,
                    String.format(Locale.US, "Vol %.1fx", r.volumeRatio),
                    r.volumeBullish);
        }

        // ── Candlestick patterns ──────────────────────────────────────────
        if (!r.candlePatterns.isEmpty()) {
            StringBuilder sb = new StringBuilder("📊 ");
            for (int i = 0; i < r.candlePatterns.size(); i++) {
                CandlePattern cp = r.candlePatterns.get(i);
                if (i > 0) sb.append(" · ");
                sb.append(cp.bullish ? "▲" : "▼").append(" ").append(cp.name);
            }
            h.tvCandlePatterns.setText(sb.toString());
            h.tvCandlePatterns.setVisibility(View.VISIBLE);
        } else {
            h.tvCandlePatterns.setVisibility(View.GONE);
        }
    }

    private void addChipIfTrue(LinearLayout parent, boolean condition, String label, boolean bull) {
        if (!condition) return;
        addChip(parent, label, bull);
    }

    private void addChip(LinearLayout parent, String label, boolean bull) {
        TextView chip = new TextView(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(6);
        chip.setLayoutParams(lp);
        chip.setText(label);
        chip.setTextSize(10f);
        chip.setTextColor(bull ? COLOR_BULL_BASE : COLOR_BEAR_BASE);
        chip.setBackground(ctx.getDrawable(R.drawable.bg_chip));
        chip.setPadding(dp(8), dp(3), dp(8), dp(3));
        parent.addView(chip);
    }

    private int dp(int value) {
        return (int) (value * ctx.getResources().getDisplayMetrics().density);
    }

    private int alphaColor(int color, float alpha) {
        int a = (int) (alpha * 255);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int predictionColor(String label) {
        if (label == null) return COLOR_NEUTRAL;
        switch (label) {
            case "Strong Buy":  return Color.parseColor("#00E676");
            case "Buy":         return COLOR_BULL_BASE;
            case "Weak Buy":    return Color.parseColor("#69F0AE");
            case "Strong Sell": return Color.parseColor("#FF1744");
            case "Sell":        return COLOR_BEAR_BASE;
            case "Weak Sell":   return Color.parseColor("#FF6E6E");
            default:            return COLOR_NEUTRAL;
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCoinName, tvScore, tvPredictionLabel,
                 tvPriceChange, tvPredictedChange, tvSignalCount, tvCandlePatterns;
        LinearLayout llChips;

        VH(View v) {
            super(v);
            tvCoinName        = v.findViewById(R.id.tv_coin_name);
            tvScore           = v.findViewById(R.id.tv_score);
            tvPredictionLabel = v.findViewById(R.id.tv_prediction_label);
            tvPriceChange     = v.findViewById(R.id.tv_price_change);
            tvPredictedChange = v.findViewById(R.id.tv_predicted_change);
            tvSignalCount     = v.findViewById(R.id.tv_signal_count);
            tvCandlePatterns  = v.findViewById(R.id.tv_candle_patterns);
            llChips           = v.findViewById(R.id.ll_signal_chips);
        }
    }
}
