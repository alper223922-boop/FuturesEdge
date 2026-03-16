package com.futuresedge.model;

public class MarketTicker {
    public String symbol;
    public double lastPrice;
    public double priceChangePercent;
    public double volume;       // quote volume in USDT

    public MarketTicker(String symbol, double lastPrice,
                        double priceChangePercent, double volume) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
        this.priceChangePercent = priceChangePercent;
        this.volume = volume;
    }

    public String getDisplayPair() {
        if (symbol.endsWith("USDT")) {
            return symbol.replace("USDT", "/USDT");
        }
        return symbol;
    }

    public String getFormattedPrice() {
        if (lastPrice >= 1000) return String.format("$%,.0f", lastPrice);
        if (lastPrice >= 1)    return String.format("$%.2f", lastPrice);
        return String.format("$%.6f", lastPrice);
    }

    public String getFormattedVolume() {
        if (volume >= 1_000_000_000) return String.format("%.1fB", volume / 1_000_000_000);
        if (volume >= 1_000_000)     return String.format("%.1fM", volume / 1_000_000);
        return String.format("%.0fK", volume / 1_000);
    }
}
