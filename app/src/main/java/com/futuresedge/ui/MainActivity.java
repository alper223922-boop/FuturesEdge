package com.futuresedge.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.futuresedge.R;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final ScannerFragment    scannerFragment  = new ScannerFragment();
    private final MarketFragment     marketFragment   = new MarketFragment();
    private final FullScanFragment   fullScanFragment = new FullScanFragment();
    private final HistoryFragment    historyFragment  = new HistoryFragment();
    private final ConfigFragment     configFragment   = new ConfigFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottom_navigation);

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
