package com.futuresedge.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.futuresedge.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final ScannerFragment scannerFragment = new ScannerFragment();
    private final MarketFragment marketFragment = new MarketFragment();
    private final FullScanFragment fullScanFragment = new FullScanFragment();
    private final HistoryFragment historyFragment = new HistoryFragment();
    private final ConfigFragment configFragment = new ConfigFragment();
    private Fragment activeFragment;

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Bildirim İzinlerini Kontrol Et (Android 13+ için şart)
        checkNotificationPermission();

        // 2. Arka Plan Tarama Servisini Başlat
        startScannerService();

        bottomNav = findViewById(R.id.bottom_navigation);

        // Fragmentları ekle ve gizle (Sadece scannerFragment görünür başlar)
        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, configFragment,   "config")  .hide(configFragment)
            .add(R.id.fragment_container, historyFragment,  "history") .hide(historyFragment)
            .add(R.id.fragment_container, fullScanFragment, "fullscan").hide(fullScanFragment)
            .add(R.id.fragment_container, marketFragment,   "market")  .hide(marketFragment)
            .add(R.id.fragment_container, scannerFragment,  "scanner")
            .commit();

        activeFragment = scannerFragment;
        bottomNav.setSelectedItemId(R.id.nav_scanner);
        setNavColors(R.id.nav_scanner);

        // Navigasyon Tıklama Olayları
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment target;
            
            if      (id == R.id.nav_market)   target = marketFragment;
            else if (id == R.id.nav_scanner)  target = scannerFragment;
            else if (id == R.id.nav_fullscan) target = fullScanFragment;
            else if (id == R.id.nav_history)  target = historyFragment;
            else if (id == R.id.nav_config)   target = configFragment;
            else return false;

            if (target == activeFragment) return true;

            getSupportFragmentManager().beginTransaction()
                .hide(activeFragment).show(target)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

            activeFragment = target;
            setNavColors(id);
            return true;
        });
    }

    // Tarama Servisini Başlatma Fonksiyonu
    private void startScannerService() {
        Intent serviceIntent = new Intent(this, com.futuresedge.service.ScannerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // Android 13 ve üzeri için Bildirim İzni İsteme
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bildirim izni verildi.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Uyarı: Bildirim izni verilmedi, sinyalleri göremezsiniz!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setNavColors(int selectedId) {
        int gold = getColor(R.color.accent_gold);
        int gray = getColor(R.color.nav_unselected);
        
        for (int i = 0; i < bottomNav.getMenu().size(); i++)
            bottomNav.getMenu().getItem(i).setChecked(
                bottomNav.getMenu().getItem(i).getItemId() == selectedId);
        
        int[][] states = { new int[]{android.R.attr.state_checked}, new int[]{} };
        android.content.res.ColorStateList csl =
            new android.content.res.ColorStateList(states, new int[]{gold, gray});
        
        bottomNav.setItemIconTintList(csl);
        bottomNav.setItemTextColor(csl);
    }

    @Override
    public void onBackPressed() {
        if (activeFragment != scannerFragment)
            bottomNav.setSelectedItemId(R.id.nav_scanner);
        else super.onBackPressed();
    }
}
