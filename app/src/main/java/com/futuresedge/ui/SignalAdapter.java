package com.futuresedge.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.futuresedge.R;
import com.futuresedge.model.Signal;

import java.util.List;
import java.util.Locale;

public class SignalAdapter extends RecyclerView.Adapter<SignalAdapter.VH> {

    private final List<Signal> items;
    private final Context ctx;

    public SignalAdapter(Context ctx, List<Signal> items) {
        this.ctx = ctx;
        this.items = items;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_signal, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Signal s = items.get(pos);

        // Format pair: BTCUSDT → BTC/USDT
        String displayPair = s.pair.endsWith("USDT")
                ? s.pair.replace("USDT", "/USDT")
                : s.pair;
        h.tvPair.setText(displayPair);
        h.tvSignalType.setText(s.signalType);

        int green = ContextCompat.getColor(ctx, R.color.green_signal);
        int red   = ContextCompat.getColor(ctx, R.color.red_signal);

        int signalColor = s.isGoldenCross() ? green : red;
        h.tvPair.setTextColor(signalColor);
        h.tvSignalType.setTextColor(signalColor);
        h.dotSignal.setBackgroundResource(
                s.isGoldenCross() ? R.drawable.bg_dot_green : R.drawable.bg_dot_red);

        // Price change
        String priceStr = String.format(Locale.US, "%+.2f%% %s",
                s.priceChange, s.priceChange >= 0 ? "↗" : "↘");
        h.tvPriceChange.setText(priceStr);
        h.tvPriceChange.setTextColor(s.priceChange >= 0 ? green : red);

        // Volume change placeholder
        h.tvVolumeChange.setText("");
        h.tvVolumeChange.setVisibility(View.GONE);

        // Candles ago
        h.tvCandlesAgo.setText(s.candlesAgo + " candles ago (" + s.getTimeAgoString() + ")");
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPair, tvSignalType, tvPriceChange, tvVolumeChange, tvCandlesAgo;
        View dotSignal;

        VH(View v) {
            super(v);
            tvPair        = v.findViewById(R.id.tv_pair);
            tvSignalType  = v.findViewById(R.id.tv_signal_type);
            tvPriceChange = v.findViewById(R.id.tv_price_change);
            tvVolumeChange= v.findViewById(R.id.tv_volume_change);
            tvCandlesAgo  = v.findViewById(R.id.tv_candles_ago);
            dotSignal     = v.findViewById(R.id.dot_signal);
        }
    }
}
