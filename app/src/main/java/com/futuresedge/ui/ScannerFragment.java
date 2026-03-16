package com.futuresedge.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.futuresedge.R;
import com.futuresedge.model.Signal;
import com.futuresedge.network.BinanceApiService;
import com.futuresedge.network.ScanService;
import com.futuresedge.utils.MaCalculator;
import com.futuresedge.utils.ScannerEngine;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerFragment extends Fragment {

    // UI
    private android.widget.EditText etFast, etSlow, etFreq;
    private Button btnEma, btnSma, btnWma;
    private Button btn15m, btn1h, btn4h, btn1d, btn1w;
    private SwitchMaterial switchAutoScan, switchAlerts;
    private SeekBar seekVolume;
    private TextView tvVolumeLabel, tvFreqLabel, tvMatchesCount, tvLastUpdated, tvSignalSummary;
    private RecyclerView rvSignals;
    private Button btnStartScan;

    // State
    private String selectedMaType = "EMA";
    private String selectedTimeframe = "1h";
    private final List<Signal> signalList = new ArrayList<>();
    private SignalAdapter adapter;
    private double minVolume = 10_000_000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final double[] VOLUME_STEPS = {
        1_000_000, 5_000_000, 10_000_000, 25_000_000,
        50_000_000, 100_000_000, 250_000_000, 500_000_000
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        bindViews(v);
        setupMaTypeButtons();
        setupTimeframeButtons();
        setupVolumeSeekbar();
        setupSwitches();
        setupStartButton();

        adapter = new SignalAdapter(requireContext(), signalList);
        rvSignals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSignals.setAdapter(adapter);
        rvSignals.setNestedScrollingEnabled(false);
    }

    private void bindViews(View v) {
        etFast          = v.findViewById(R.id.et_fast_period);
        etSlow          = v.findViewById(R.id.et_slow_period);
        etFreq          = v.findViewById(R.id.et_scan_frequency);
        btnEma          = v.findViewById(R.id.btn_ema);
        btnSma          = v.findViewById(R.id.btn_sma);
        btnWma          = v.findViewById(R.id.btn_wma);
        btn15m          = v.findViewById(R.id.btn_15m);
        btn1h           = v.findViewById(R.id.btn_1h);
        btn4h           = v.findViewById(R.id.btn_4h);
        btn1d           = v.findViewById(R.id.btn_1d);
        btn1w           = v.findViewById(R.id.btn_1w);
        switchAutoScan  = v.findViewById(R.id.switch_auto_scan);
        switchAlerts    = v.findViewById(R.id.switch_alerts);
        seekVolume      = v.findViewById(R.id.seekbar_volume);
        tvVolumeLabel   = v.findViewById(R.id.tv_volume_label);
        tvFreqLabel     = v.findViewById(R.id.tv_scan_freq_label);
        tvMatchesCount  = v.findViewById(R.id.tv_matches_count);
        tvLastUpdated   = v.findViewById(R.id.tv_last_updated);
        tvSignalSummary = v.findViewById(R.id.tv_signal_summary);
        rvSignals       = v.findViewById(R.id.rv_signals);
        btnStartScan    = v.findViewById(R.id.btn_start_scan);
    }

    // ── MA Type buttons ──────────────────────────────────────────────────────

    private void setupMaTypeButtons() {
        View.OnClickListener l = view -> {
            int id = view.getId();
            if      (id == R.id.btn_ema) selectMaType("EMA");
            else if (id == R.id.btn_sma) selectMaType("SMA");
            else if (id == R.id.btn_wma) selectMaType("WMA");
        };
        btnEma.setOnClickListener(l);
        btnSma.setOnClickListener(l);
        btnWma.setOnClickListener(l);
        selectMaType("EMA");
    }

    private void selectMaType(String type) {
        selectedMaType = type;
        int goldColor  = requireContext().getColor(R.color.accent_gold);
        int grayColor  = requireContext().getColor(R.color.text_secondary);

        btnEma.setTextColor("EMA".equals(type) ? goldColor : grayColor);
        btnSma.setTextColor("SMA".equals(type) ? goldColor : grayColor);
        btnWma.setTextColor("WMA".equals(type) ? goldColor : grayColor);

        // stroke highlight via tag workaround — background tint is already bg_card
        // Simple approach: set alpha on non-selected buttons
        btnEma.setAlpha("EMA".equals(type) ? 1f : 0.55f);
        btnSma.setAlpha("SMA".equals(type) ? 1f : 0.55f);
        btnWma.setAlpha("WMA".equals(type) ? 1f : 0.55f);
    }

    // ── Timeframe buttons ────────────────────────────────────────────────────

    private void setupTimeframeButtons() {
        View.OnClickListener l = view -> {
            int id = view.getId();
            if      (id == R.id.btn_15m) selectTimeframe("15m");
            else if (id == R.id.btn_1h)  selectTimeframe("1h");
            else if (id == R.id.btn_4h)  selectTimeframe("4h");
            else if (id == R.id.btn_1d)  selectTimeframe("1d");
            else if (id == R.id.btn_1w)  selectTimeframe("1w");
        };
        btn15m.setOnClickListener(l); btn1h.setOnClickListener(l);
        btn4h.setOnClickListener(l);  btn1d.setOnClickListener(l);
        btn1w.setOnClickListener(l);
        selectTimeframe("1h");
    }

    private void selectTimeframe(String tf) {
        selectedTimeframe = tf;
        int gold = requireContext().getColor(R.color.accent_gold);
        int gray = requireContext().getColor(R.color.text_secondary);
        btn15m.setTextColor("15m".equals(tf) ? gold : gray);
        btn1h .setTextColor("1h" .equals(tf) ? gold : gray);
        btn4h .setTextColor("4h" .equals(tf) ? gold : gray);
        btn1d .setTextColor("1d" .equals(tf) ? gold : gray);
        btn1w .setTextColor("1w" .equals(tf) ? gold : gray);

        btn15m.setAlpha("15m".equals(tf) ? 1f : 0.55f);
        btn1h .setAlpha("1h" .equals(tf) ? 1f : 0.55f);
        btn4h .setAlpha("4h" .equals(tf) ? 1f : 0.55f);
        btn1d .setAlpha("1d" .equals(tf) ? 1f : 0.55f);
        btn1w .setAlpha("1w" .equals(tf) ? 1f : 0.55f);
    }

    // ── Volume seekbar ───────────────────────────────────────────────────────

    private void setupVolumeSeekbar() {
        seekVolume.setMax(VOLUME_STEPS.length - 1);
        seekVolume.setProgress(2); // default: 10M
        updateVolumeLabel(2);
        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) { updateVolumeLabel(p); }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void updateVolumeLabel(int progress) {
        minVolume = VOLUME_STEPS[progress];
        if (minVolume >= 1_000_000_000)
            tvVolumeLabel.setText("> " + (long)(minVolume/1_000_000_000) + "B USDT");
        else if (minVolume >= 1_000_000)
            tvVolumeLabel.setText("> " + (long)(minVolume/1_000_000) + "M USDT");
        else
            tvVolumeLabel.setText("> " + (long)(minVolume/1_000) + "K USDT");
    }

    // ── Switches ─────────────────────────────────────────────────────────────

    private void setupSwitches() {
        switchAlerts.setOnCheckedChangeListener((b, checked) ->
                ScanService.alertsEnabled = checked);
    }

    // ── Start Scan ───────────────────────────────────────────────────────────

    private void setupStartButton() {
        btnStartScan.setOnClickListener(v -> startScan());
    }

    private void startScan() {
        int fast, slow;
        try {
            fast = Integer.parseInt(etFast.getText().toString().trim());
            slow = Integer.parseInt(etSlow.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid period values", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fast >= slow) {
            Toast.makeText(requireContext(), "Fast period must be less than slow", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update service params
        ScanService.fastPeriod = fast;
        ScanService.slowPeriod = slow;
        ScanService.maType = selectedMaType;
        ScanService.timeframe = selectedTimeframe;
        ScanService.minVolume = minVolume;

        btnStartScan.setEnabled(false);
        btnStartScan.setText("⏳  SCANNING…");
        signalList.clear();
        adapter.notifyDataSetChanged();
        tvMatchesCount.setText("LIVE MATCHES (…)");
        tvSignalSummary.setText("Scanning pairs…");

        final int fFast = fast, fSlow = slow;
        final String fMaType = selectedMaType, fTf = selectedTimeframe;
        final double fVol = minVolume;

        executor.submit(() -> {
            try {
                ScannerEngine engine = new ScannerEngine();
                List<Signal> results = engine.scan(
                        fFast, fSlow,
                        MaCalculator.MaType.valueOf(fMaType),
                        fTf, fVol);

                mainHandler.post(() -> {
                    signalList.clear();
                    signalList.addAll(results);
                    adapter.notifyDataSetChanged();

                    String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                    tvMatchesCount.setText("LIVE MATCHES (" + results.size() + ")");
                    tvLastUpdated.setText("Last updated: " + ts);
                    tvSignalSummary.setText(results.size() + " signal found");

                    btnStartScan.setEnabled(true);
                    btnStartScan.setText("▶  START SCAN");
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(),
                            "Scan error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnStartScan.setEnabled(true);
                    btnStartScan.setText("▶  START SCAN");
                });
            }
        });

        // Start background service if auto-scan enabled
        if (switchAutoScan.isChecked()) {
            startScanService();
        }
    }

    private void startScanService() {
        String freqStr = etFreq.getText().toString().trim();
        long intervalMs = parseFrequencyToMs(freqStr);
        Intent intent = new Intent(requireContext(), ScanService.class);
        intent.putExtra("interval_ms", intervalMs);
        requireContext().startService(intent);
    }

    private long parseFrequencyToMs(String freq) {
        if (freq.isEmpty()) return 5 * 60 * 1000L;
        try {
            freq = freq.toLowerCase().trim();
            if (freq.endsWith("m")) return Long.parseLong(freq.replace("m","")) * 60 * 1000L;
            if (freq.endsWith("h")) return Long.parseLong(freq.replace("h","")) * 3600 * 1000L;
            return Long.parseLong(freq) * 60 * 1000L;
        } catch (Exception e) {
            return 5 * 60 * 1000L;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }
}
