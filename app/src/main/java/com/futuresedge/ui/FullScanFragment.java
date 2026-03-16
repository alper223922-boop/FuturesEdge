package com.futuresedge.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futuresedge.R;
import com.futuresedge.model.PatternResult;
import com.futuresedge.utils.FullScannerEngine;
import com.futuresedge.utils.MaCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FullScanFragment extends Fragment {

    private Button btnScan;
    private Button fs15m, fs1h, fs4h, fs1d, fs1w;
    private EditText etFast, etSlow;
    private LinearLayout llProgress;
    private ProgressBar progressBar;
    private TextView tvProgressText, tvProgressPct, tvFsCount, tvFsUpdated;
    private RecyclerView rvResults;

    private String selectedTf = "1h";
    private final List<PatternResult> results = new ArrayList<>();
    private PatternResultAdapter adapter;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_full_scan, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        btnScan       = v.findViewById(R.id.btn_full_scan);
        fs15m         = v.findViewById(R.id.fs_btn_15m);
        fs1h          = v.findViewById(R.id.fs_btn_1h);
        fs4h          = v.findViewById(R.id.fs_btn_4h);
        fs1d          = v.findViewById(R.id.fs_btn_1d);
        fs1w          = v.findViewById(R.id.fs_btn_1w);
        etFast        = v.findViewById(R.id.fs_et_fast);
        etSlow        = v.findViewById(R.id.fs_et_slow);
        llProgress    = v.findViewById(R.id.ll_progress);
        progressBar   = v.findViewById(R.id.progress_bar);
        tvProgressText= v.findViewById(R.id.tv_progress_text);
        tvProgressPct = v.findViewById(R.id.tv_progress_pct);
        tvFsCount     = v.findViewById(R.id.tv_fs_count);
        tvFsUpdated   = v.findViewById(R.id.tv_fs_updated);
        rvResults     = v.findViewById(R.id.rv_full_scan);

        adapter = new PatternResultAdapter(requireContext(), results);
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(adapter);

        View.OnClickListener tfListener = view -> {
            int id = view.getId();
            if      (id == R.id.fs_btn_15m) selectTf("15m");
            else if (id == R.id.fs_btn_1h)  selectTf("1h");
            else if (id == R.id.fs_btn_4h)  selectTf("4h");
            else if (id == R.id.fs_btn_1d)  selectTf("1d");
            else if (id == R.id.fs_btn_1w)  selectTf("1w");
        };
        fs15m.setOnClickListener(tfListener); fs1h.setOnClickListener(tfListener);
        fs4h.setOnClickListener(tfListener);  fs1d.setOnClickListener(tfListener);
        fs1w.setOnClickListener(tfListener);
        selectTf("1h");

        btnScan.setOnClickListener(x -> startFullScan());
    }

    private void selectTf(String tf) {
        selectedTf = tf;
        int gold = requireContext().getColor(R.color.accent_gold);
        int gray = requireContext().getColor(R.color.text_secondary);
        fs15m.setTextColor("15m".equals(tf) ? gold : gray);
        fs1h .setTextColor("1h" .equals(tf) ? gold : gray);
        fs4h .setTextColor("4h" .equals(tf) ? gold : gray);
        fs1d .setTextColor("1d" .equals(tf) ? gold : gray);
        fs1w .setTextColor("1w" .equals(tf) ? gold : gray);
        fs15m.setAlpha("15m".equals(tf)?1f:0.55f); fs1h.setAlpha("1h".equals(tf)?1f:0.55f);
        fs4h .setAlpha("4h" .equals(tf)?1f:0.55f); fs1d.setAlpha("1d".equals(tf)?1f:0.55f);
        fs1w .setAlpha("1w" .equals(tf)?1f:0.55f);
    }

    private void startFullScan() {
        int fast, slow;
        try {
            fast = Integer.parseInt(etFast.getText().toString().trim());
            slow = Integer.parseInt(etSlow.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Geçersiz MA değeri", Toast.LENGTH_SHORT).show();
            return;
        }

        btnScan.setEnabled(false);
        btnScan.setText("⏳  Taranıyor…");
        llProgress.setVisibility(View.VISIBLE);
        results.clear();
        adapter.notifyDataSetChanged();
        tvFsCount.setText("RESULTS (…)");

        final String tf     = selectedTf;
        final int fFast     = fast;
        final int fSlow     = slow;

        exec.submit(() -> {
            try {
                FullScannerEngine engine = new FullScannerEngine();
                List<PatternResult> found = engine.scanAll(
                        tf, fFast, fSlow,
                        MaCalculator.MaType.EMA,
                        (scanned, total, symbol) -> handler.post(() -> {
                            int pct = (int)(scanned * 100.0 / total);
                            progressBar.setProgress(pct);
                            tvProgressPct.setText(pct + "%");
                            tvProgressText.setText(scanned + "/" + total + "  " + symbol);
                        })
                );

                handler.post(() -> {
                    results.clear();
                    results.addAll(found);
                    adapter.notifyDataSetChanged();

                    String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                    tvFsCount.setText("RESULTS (" + found.size() + ")");
                    tvFsUpdated.setText(ts);
                    llProgress.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                    btnScan.setText("🔍  SCAN ALL PAIRS");

                    // Save to history
                    // (PatternResult signals saved separately if needed)
                });

            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(requireContext(),
                            "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    llProgress.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                    btnScan.setText("🔍  SCAN ALL PAIRS");
                });
            }
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        exec.shutdownNow();
    }
}
