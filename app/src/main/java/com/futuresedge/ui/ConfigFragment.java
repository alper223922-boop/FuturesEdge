package com.futuresedge.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.futuresedge.R;
import com.futuresedge.network.ScanService;

public class ConfigFragment extends Fragment {

    private EditText etApiKey, etApiSecret;
    private SwitchMaterial switchAutoPairs, switchShowVolume, switchShowCandles;
    private TextView tvPairsCount;

    private SharedPreferences prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_config, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        prefs = requireContext().getSharedPreferences("config", android.content.Context.MODE_PRIVATE);

        etApiKey          = v.findViewById(R.id.et_api_key);
        etApiSecret       = v.findViewById(R.id.et_api_secret);
        switchAutoPairs   = v.findViewById(R.id.switch_auto_pairs);
        switchShowVolume  = v.findViewById(R.id.switch_show_volume);
        switchShowCandles = v.findViewById(R.id.switch_show_candles);
        tvPairsCount      = v.findViewById(R.id.tv_pairs_count);

        loadPrefs();

        v.findViewById(R.id.btn_save_config).setOnClickListener(x -> savePrefs());

        switchAutoPairs.setOnCheckedChangeListener((b, checked) ->
                tvPairsCount.setText(checked ? "100 pairs loaded" : "Manual pairs mode"));
    }

    private void loadPrefs() {
        // Never show saved API keys in plaintext — just indicate if set
        boolean hasKey = !prefs.getString("api_key", "").isEmpty();
        etApiKey.setHint(hasKey ? "API Key saved ✓" : "Enter API Key (optional)");
        etApiSecret.setHint(hasKey ? "Secret saved ✓" : "Enter API Secret (optional)");

        switchAutoPairs  .setChecked(prefs.getBoolean("auto_pairs",    true));
        switchShowVolume .setChecked(prefs.getBoolean("show_volume",   true));
        switchShowCandles.setChecked(prefs.getBoolean("show_candles",  true));

        int pairsCount = prefs.getInt("pairs_count", 100);
        tvPairsCount.setText(pairsCount + " pairs loaded");
    }

    private void savePrefs() {
        SharedPreferences.Editor ed = prefs.edit();

        String key    = etApiKey.getText().toString().trim();
        String secret = etApiSecret.getText().toString().trim();
        if (!key.isEmpty())    ed.putString("api_key",    key);
        if (!secret.isEmpty()) ed.putString("api_secret", secret);

        ed.putBoolean("auto_pairs",   switchAutoPairs.isChecked());
        ed.putBoolean("show_volume",  switchShowVolume.isChecked());
        ed.putBoolean("show_candles", switchShowCandles.isChecked());
        ed.apply();

        Toast.makeText(requireContext(), "Configuration saved ✓", Toast.LENGTH_SHORT).show();

        // Clear fields for security
        etApiKey.setText("");
        etApiSecret.setText("");
    }
}
