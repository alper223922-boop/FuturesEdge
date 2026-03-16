package com.futuresedge.model;

public class Signal {
    public String pair;
    public String signalType;   // "Golden Cross" or "Death Cross"
    public double priceChange;  // 24h %
    public double volumeChange; // 24h vol %
    public int candlesAgo;
    public long timestampMs;
    public String maType;       // EMA / SMA / WMA
    public String timeframe;
    public int fastPeriod;
    public int slowPeriod;

    public Signal(String pair, String signalType, double priceChange,
                  double volumeChange, int candlesAgo, long timestampMs,
                  String maType, String timeframe, int fastPeriod, int slowPeriod) {
        this.pair = pair;
        this.signalType = signalType;
        this.priceChange = priceChange;
        this.volumeChange = volumeChange;
        this.candlesAgo = candlesAgo;
        this.timestampMs = timestampMs;
        this.maType = maType;
        this.timeframe = timeframe;
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
    }

    public boolean isGoldenCross() {
        return "Golden Cross".equals(signalType);
    }

    /** Human-readable "Xh ago" based on timeframe & candlesAgo */
    public String getTimeAgoString() {
        int minutes = timeframeToMinutes(timeframe);
        int totalMin = minutes * candlesAgo;
        if (totalMin < 60) return totalMin + "m ago";
        if (totalMin < 1440) return (totalMin / 60) + "h ago";
        return (totalMin / 1440) + "d ago";
    }

    private int timeframeToMinutes(String tf) {
        switch (tf) {
            case "15m": return 15;
            case "1h":  return 60;
            case "4h":  return 240;
            case "1d":  return 1440;
            case "1w":  return 10080;
            default:    return 60;
        }
    }
}
