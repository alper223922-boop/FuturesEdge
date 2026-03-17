package com.futuresedge.network;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScanService extends Service {

    private static final String TAG = "ScanService";
    public static final String CHANNEL_ID = "ma_scanner_channel";
    public static final String ACTION_SCAN_RESULT = "com.futuresedge.SCAN_RESULT";
    public static final String EXTRA_SIGNAL_COUNT = "signal_count";

    private ScheduledExecutorService scheduler; // Timer yerine daha güvenli olan Scheduler
    private ScannerEngine engine;

    private long intervalMs = 5 * 60 * 1000L; 

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
        
        // Android 14+ uyumluluğu için Foreground Service başlatma
        Notification notification = buildNotification("Tarayıcı Aktif - Arka planda çalışıyor...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            intervalMs = intent.getLongExtra("interval_ms", intervalMs);
        }
        scheduleScans();
        return START_STICKY; // Sistem servisi kapatırsa otomatik yeniden başlatır
    }

    private void scheduleScans() {
        if (scheduler != null) scheduler.shutdownNow();
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Timer yerine ScheduledExecutor kullanmak "NetworkOnMainThread" hatalarını önler
        scheduler.scheduleAtFixedRate(this::runScan, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void runScan() {
        try {
            Log.d(TAG, "Tarama başlatılıyor...");
            List<Signal> signals = engine.scan(
                    fastPeriod, slowPeriod,
                    MaCalculator.MaType.valueOf(maType),
                    timeframe, minVolume);

            // Sonuçları bildirmek için yayın yap
            Intent broadcast = new Intent(ACTION_SCAN_RESULT);
            broadcast.putExtra(EXTRA_SIGNAL_COUNT, signals.size());
            sendBroadcast(broadcast);

            if (alertsEnabled && !signals.isEmpty()) {
                Signal first = signals.get(0);
                notifyUser(first.pair + " " + first.signalType,
                       signals.size() + " yeni sinyal tespit edildi!");
            }

            updateForegroundNotification(signals.size() + " aktif sinyal bulundu.");

        } catch (Exception e) {
            Log.e(TAG, "Tarama hatası: " + e.getMessage());
            updateForegroundNotification("Bağlantı hatası, tekrar deneniyor...");
        }
    }

    private void notifyUser(String title, String body) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_scanner)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build();
        
        nm.notify((int) System.currentTimeMillis(), n);
    }

    private void updateForegroundNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(1, buildNotification(text));
        }
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_scanner)
                .setContentTitle("FuturesEdge MA Scanner")
                .setContentText(text)
                .setContentIntent(pi)
                .setOngoing(true) // Bildirimin kaydırılarak silinmesini engeller
                .setSilent(true)  // Sürekli bildirim sesi çıkmasını engeller
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Piyasa Tarama Bildirimleri",
                    NotificationManager.IMPORTANCE_LOW); // Sürekli ses çıkmaması için LOW
            
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) scheduler.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
