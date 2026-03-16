package com.futuresedge.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futuresedge.R;
import com.futuresedge.model.Signal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvTotal, tvGolden, tvDeath;
    private final List<Signal> history = new ArrayList<>();
    private SignalAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_history, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        rv       = v.findViewById(R.id.rv_history);
        tvTotal  = v.findViewById(R.id.tv_total_signals);
        tvGolden = v.findViewById(R.id.tv_golden_count);
        tvDeath  = v.findViewById(R.id.tv_death_count);

        adapter = new SignalAdapter(requireContext(), history);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        v.findViewById(R.id.btn_clear_history).setOnClickListener(x -> clearHistory());

        loadHistory();
    }

    /** Call this from ScannerFragment after a successful scan to save signals */
    public static void appendSignals(android.content.Context ctx, List<Signal> signals) {
        SharedPreferences sp = ctx.getSharedPreferences("history", android.content.Context.MODE_PRIVATE);
        JSONArray arr;
        try { arr = new JSONArray(sp.getString("signals", "[]")); }
        catch (Exception e) { arr = new JSONArray(); }

        for (Signal sig : signals) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("pair",       sig.pair);
                obj.put("signalType", sig.signalType);
                obj.put("priceChange",sig.priceChange);
                obj.put("candlesAgo", sig.candlesAgo);
                obj.put("ts",         sig.timestampMs);
                obj.put("maType",     sig.maType);
                obj.put("tf",         sig.timeframe);
                obj.put("fast",       sig.fastPeriod);
                obj.put("slow",       sig.slowPeriod);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        // Keep last 500
        while (arr.length() > 500) arr.remove(0);
        sp.edit().putString("signals", arr.toString()).apply();
    }

    private void loadHistory() {
        SharedPreferences sp = requireContext()
                .getSharedPreferences("history", android.content.Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(sp.getString("signals", "[]"));
            history.clear();
            for (int i = arr.length() - 1; i >= 0; i--) { // newest first
                JSONObject o = arr.getJSONObject(i);
                history.add(new Signal(
                        o.getString("pair"),
                        o.getString("signalType"),
                        o.getDouble("priceChange"),
                        0,
                        o.getInt("candlesAgo"),
                        o.getLong("ts"),
                        o.getString("maType"),
                        o.getString("tf"),
                        o.getInt("fast"),
                        o.getInt("slow")
                ));
            }
        } catch (Exception ignored) {}

        adapter.notifyDataSetChanged();
        updateStats();
    }

    private void updateStats() {
        int golden = 0, death = 0;
        for (Signal s : history) {
            if (s.isGoldenCross()) golden++; else death++;
        }
        tvTotal .setText(String.valueOf(history.size()));
        tvGolden.setText(String.valueOf(golden));
        tvDeath .setText(String.valueOf(death));
    }

    private void clearHistory() {
        requireContext().getSharedPreferences("history", android.content.Context.MODE_PRIVATE)
                .edit().remove("signals").apply();
        history.clear();
        adapter.notifyDataSetChanged();
        updateStats();
    }
}
