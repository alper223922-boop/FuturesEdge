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
import com.futuresedge.model.MarketTicker;

import java.util.List;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.VH> {

    private final List<MarketTicker> items;
    private final Context ctx;

    public MarketAdapter(Context ctx, List<MarketTicker> items) {
        this.ctx = ctx;
        this.items = items;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_market, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MarketTicker t = items.get(pos);
        h.tvPair.setText(t.getDisplayPair());
        h.tvVolume.setText("Vol: " + t.getFormattedVolume() + " USDT");
        h.tvPrice.setText(t.getFormattedPrice());

        String chgStr = String.format("%+.2f%%", t.priceChangePercent);
        h.tvChange.setText(chgStr);

        int color = t.priceChangePercent >= 0
                ? ContextCompat.getColor(ctx, R.color.green_signal)
                : ContextCompat.getColor(ctx, R.color.red_signal);
        h.tvChange.setTextColor(color);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPair, tvVolume, tvPrice, tvChange;
        VH(View v) {
            super(v);
            tvPair   = v.findViewById(R.id.tv_market_pair);
            tvVolume = v.findViewById(R.id.tv_market_volume);
            tvPrice  = v.findViewById(R.id.tv_market_price);
            tvChange = v.findViewById(R.id.tv_market_change);
        }
    }
}
