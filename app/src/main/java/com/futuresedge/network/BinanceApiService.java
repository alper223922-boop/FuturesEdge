package com.futuresedge.network;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BinanceApiService {

    private static final String TAG = "BinanceApi";
    private static final String BASE = "https://fapi.binance.com";

    private final OkHttpClient client;

    public BinanceApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    // ── Kline (candle) data ──────────────────────────────────────────────────

    /**
     * Returns closing prices for the given symbol + interval.
     * limit = number of candles (max 1500).
     */
    public List<Double> getClosePrices(String symbol, String interval, int limit) throws IOException {
        String url = BASE + "/fapi/v1/klines?symbol=" + symbol
                + "&interval=" + interval
                + "&limit=" + limit;
        String body = get(url);
        List<Double> closes = new ArrayList<>();
        if (body == null) return closes;
        try {
            JSONArray arr = new JSONArray(body);
            for (int i = 0; i < arr.length(); i++) {
                JSONArray candle = arr.getJSONArray(i);
                closes.add(Double.parseDouble(candle.getString(4))); // index 4 = close
            }
        } catch (Exception e) {
            Log.e(TAG, "getClosePrices parse error: " + e.getMessage());
        }
        return closes;
    }

    // ── Full OHLCV data ──────────────────────────────────────────────────────

    public static class OhlcvData {
        public List<Double> opens   = new ArrayList<>();
        public List<Double> highs   = new ArrayList<>();
        public List<Double> lows    = new ArrayList<>();
        public List<Double> closes  = new ArrayList<>();
        public List<Double> volumes = new ArrayList<>();
    }

    public OhlcvData getOhlcv(String symbol, String interval, int limit) throws IOException {
        String url = BASE + "/fapi/v1/klines?symbol=" + symbol
                + "&interval=" + interval + "&limit=" + limit;
        String body = get(url);
        OhlcvData data = new OhlcvData();
        if (body == null) return data;
        try {
            JSONArray arr = new JSONArray(body);
            for (int i = 0; i < arr.length(); i++) {
                JSONArray c = arr.getJSONArray(i);
                data.opens  .add(Double.parseDouble(c.getString(1)));
                data.highs  .add(Double.parseDouble(c.getString(2)));
                data.lows   .add(Double.parseDouble(c.getString(3)));
                data.closes .add(Double.parseDouble(c.getString(4)));
                data.volumes.add(Double.parseDouble(c.getString(5)));
            }
        } catch (Exception e) {
            Log.e(TAG, "getOhlcv parse error: " + e.getMessage());
        }
        return data;
    }

    // ── 24h ticker ──────────────────────────────────────────────────────────

    public static class TickerData {
        public double priceChangePercent;
        public double quoteVolume;
    }

    public TickerData get24hTicker(String symbol) throws IOException {
        String url = BASE + "/fapi/v1/ticker/24hr?symbol=" + symbol;
        String body = get(url);
        TickerData td = new TickerData();
        if (body == null) return td;
        try {
            JSONObject obj = new JSONObject(body);
            td.priceChangePercent = obj.getDouble("priceChangePercent");
            td.quoteVolume = obj.getDouble("quoteVolume");
        } catch (Exception e) {
            Log.e(TAG, "get24hTicker parse error: " + e.getMessage());
        }
        return td;
    }

    // ── All USDT perpetual pairs ─────────────────────────────────────────────

    public List<String> getUsdtPerpPairs(double minVolume24hUsdt) throws IOException {
        String url = BASE + "/fapi/v1/ticker/24hr";
        String body = get(url);
        List<String> pairs = new ArrayList<>();
        if (body == null) return pairs;
        try {
            JSONArray arr = new JSONArray(body);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String symbol = obj.getString("symbol");
                if (!symbol.endsWith("USDT")) continue;
                double vol = obj.getDouble("quoteVolume");
                if (vol >= minVolume24hUsdt) {
                    pairs.add(symbol);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getUsdtPerpPairs parse error: " + e.getMessage());
        }
        return pairs;
    }

    /** Full 24h ticker list for Market screen */
    public List<com.futuresedge.model.MarketTicker> getAllTickers(double minVol) throws IOException {
        String url = BASE + "/fapi/v1/ticker/24hr";
        String body = get(url);
        List<com.futuresedge.model.MarketTicker> list = new ArrayList<>();
        if (body == null) return list;
        try {
            JSONArray arr = new JSONArray(body);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String symbol = o.getString("symbol");
                if (!symbol.endsWith("USDT")) continue;
                double price  = o.getDouble("lastPrice");
                double chgPct = o.getDouble("priceChangePercent");
                double vol    = o.getDouble("quoteVolume");
                if (vol >= minVol) {
                    list.add(new com.futuresedge.model.MarketTicker(symbol, price, chgPct, vol));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllTickers error: " + e.getMessage());
        }
        return list;
    }

    // ── HTTP helper ──────────────────────────────────────────────────────────

    private String get(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP " + response.code() + " for " + url);
                return null;
            }
            return response.body() != null ? response.body().string() : null;
        }
    }
}
