package com.futuresedge.network;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.futuresedge.R;
import com.futuresedge.model.Signal;
import com.futuresedge.ui.MainActivity;
import com.futuresedge.utils.MaCalculator;
import com.futuresedge.utils.ScannerEngine;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScanService extends Service {

    private static final String TAG = "ScanService";
    public static final String CHANNEL_ID = "ma_scanner_channel";
    public static final String ACTION_SCAN_RESULT = "com.futuresedge.SCAN_RESULT";
    public static final String EXTRA_SIGNAL_COUNT = "signal_count";

    private Timer timer;
    private ScannerEngine engine;

    // Scan interval in ms — updated from activity
    private long intervalMs = 5 * 60 * 1000L; // default 5 min

    public static int fastPeriod = 50;
    public static int slowPeriod = 200;
    public static String maType = "EMA";
    public static String timeframe = "1h";
    public static double minVolume = 10_000_000;
    public static boolean alertsEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        engine = new ScannerEngine();
        createNotificationChannel();
        startForeground(1, buildNotification("Scanning…"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            intervalMs = intent.getLongExtra("interval_ms", intervalMs);
        }
        scheduleScans();
        return START_STICKY;
    }

    private void scheduleScans() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runScan();
            }
        }, 0, intervalMs);
    }

    private void runScan() {
        try {
            List<Signal> signals = engine.scan(
                    fastPeriod, slowPeriod,
                    MaCalculator.MaType.valueOf(maType),
                    timeframe, minVolume);

            // Broadcast result count
            Intent broadcast = new Intent(ACTION_SCAN_RESULT);
            broadcast.putExtra(EXTRA_SIGNAL_COUNT, signals.size());
            sendBroadcast(broadcast);

            // Notify if alerts enabled
            if (alertsEnabled && !signals.isEmpty()) {
                Signal first = signals.get(0);
                notify(first.pair + " " + first.signalType,
                       signals.size() + " signal(s) detected");
            }

            updateForegroundNotification(signals.size() + " signals found");

        } catch (Exception e) {
            Log.e(TAG, "Scan error: " + e.getMessage());
        }
    }

    private void notify(String title, String body) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_scanner)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
        nm.notify((int) System.currentTimeMillis(), n);
    }

    private void updateForegroundNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, buildNotification(text));
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_scanner)
                .setContentTitle("MA Scanner Pro")
                .setContentText(text)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "MA Scanner Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
