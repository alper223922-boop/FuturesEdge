package com.futuresedge.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.futuresedge.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FullScanFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScanAdapter adapter;
    private List<ScanResult> allResults = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_full_scan, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_full_scan);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Verileri yükle ve filtrele
        loadAndFilterResults();
        
        return view;
    }

    private void loadAndFilterResults() {
        // 1. Mevcut zamanı ve 24 saat öncesini hesapla
        long currentMillis = System.currentTimeMillis();
        long twentyFourHoursAgo = currentMillis - (24 * 60 * 60 * 1000);

        List<ScanResult> filteredList = new ArrayList<>();

        // 2. allResults içindeki verileri kontrol et (Bu veriler API'den veya Servisten gelir)
        for (ScanResult result : allResults) {
            // result.getTimestamp() değerinin milisaniye cinsinden olduğunu varsayıyoruz
            if (result.getTimestamp() >= twentyFourHoursAgo) {
                filteredList.add(result);
            }
        }

        // 3. Listeyi güncelle
        adapter = new ScanAdapter(filteredList);
        recyclerView.setAdapter(adapter);
    }
}
