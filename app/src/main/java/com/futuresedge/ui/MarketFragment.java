package com.futuresedge.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.futuresedge.R;
import com.futuresedge.model.MarketTicker;
import com.futuresedge.network.BinanceApiService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MarketFragment extends Fragment {

    private RecyclerView rv;
    private SwipeRefreshLayout swipe;
    private Button btnAll, btnGainers, btnLosers, btnVolume;

    private final List<MarketTicker> allTickers = new ArrayList<>();
    private final List<MarketTicker> displayed  = new ArrayList<>();
    private MarketAdapter adapter;

    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String currentFilter = "ALL";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_market, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        rv     = v.findViewById(R.id.rv_market);
        swipe  = v.findViewById(R.id.swipe_refresh_market);
        btnAll     = v.findViewById(R.id.btn_all);
        btnGainers = v.findViewById(R.id.btn_gainers);
        btnLosers  = v.findViewById(R.id.btn_losers);
        btnVolume  = v.findViewById(R.id.btn_volume);

        adapter = new MarketAdapter(requireContext(), displayed);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        swipe.setColorSchemeColors(requireContext().getColor(R.color.accent_gold));
        swipe.setOnRefreshListener(this::loadMarket);

        View.OnClickListener fl = view -> {
            int id = view.getId();
            if      (id == R.id.btn_all)     setFilter("ALL");
            else if (id == R.id.btn_gainers) setFilter("GAINERS");
            else if (id == R.id.btn_losers)  setFilter("LOSERS");
            else if (id == R.id.btn_volume)  setFilter("VOLUME");
        };
        btnAll.setOnClickListener(fl); btnGainers.setOnClickListener(fl);
        btnLosers.setOnClickListener(fl); btnVolume.setOnClickListener(fl);

        v.findViewById(R.id.btn_refresh_market).setOnClickListener(x -> loadMarket());

        loadMarket();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        int gold = requireContext().getColor(R.color.accent_gold);
        int gray = requireContext().getColor(R.color.text_secondary);
        btnAll    .setTextColor("ALL"    .equals(filter) ? gold : gray);
        btnGainers.setTextColor("GAINERS".equals(filter) ? gold : gray);
        btnLosers .setTextColor("LOSERS" .equals(filter) ? gold : gray);
        btnVolume .setTextColor("VOLUME" .equals(filter) ? gold : gray);
        applyFilter();
    }

    private void applyFilter() {
        displayed.clear();
        List<MarketTicker> tmp = new ArrayList<>(allTickers);
        switch (currentFilter) {
            case "GAINERS":
                tmp.sort((a, b) -> Double.compare(b.priceChangePercent, a.priceChangePercent));
                break;
            case "LOSERS":
                tmp.sort(Comparator.comparingDouble(a -> a.priceChangePercent));
                break;
            case "VOLUME":
                tmp.sort((a, b) -> Double.compare(b.volume, a.volume));
                break;
            default:
                tmp.sort((a, b) -> Double.compare(b.volume, a.volume));
                break;
        }
        displayed.addAll(tmp.subList(0, Math.min(tmp.size(), 100)));
        adapter.notifyDataSetChanged();
    }

    private void loadMarket() {
        swipe.setRefreshing(true);
        exec.submit(() -> {
            try {
                BinanceApiService api = new BinanceApiService();
                List<MarketTicker> tickers = api.getAllTickers(5_000_000);
                handler.post(() -> {
                    allTickers.clear();
                    allTickers.addAll(tickers);
                    applyFilter();
                    swipe.setRefreshing(false);
                });
            } catch (Exception e) {
                handler.post(() -> swipe.setRefreshing(false));
            }
        });
    }

    @Override public void onDestroyView() { super.onDestroyView(); exec.shutdownNow(); }
}
