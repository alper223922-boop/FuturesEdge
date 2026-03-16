# FuturesEdge

Binance Futures MA crossover scanner for Android.

## Features
- **EMA / SMA / WMA** selectable MA type
- Timeframes: 15M, 1H, 4H, 1D, 1W
- Golden Cross / Death Cross detection
- Auto-Scan background service
- Push notifications for signals
- 4 screens: Market, Scanner, History, Config

## Build Instructions

### Prerequisites
- Android Studio (Hedgehog or newer)  
- JDK 17
- Internet connection (Gradle downloads dependencies)

### Steps
1. Clone / copy this project folder
2. Open in Android Studio → **File → Open**
3. Wait for Gradle sync to complete
4. Click **Run ▶** or **Build → Build APK**
5. APK output: `app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions (CI)
Push to `main` branch → APK automatically built → download from **Actions → Artifacts**

**Required one-time setup:**
```
cd project_root
gradle wrapper   # generates gradle-wrapper.jar
```
Or open in Android Studio — it handles this automatically.

## MA Calculation
- **EMA**: Exponential (multiplier = 2/(period+1))
- **SMA**: Simple rolling average
- **WMA**: Weighted (recent candles get higher weight)

## API
Uses Binance Futures public API (no key required for scanning):
- `GET /fapi/v1/klines` — candle data
- `GET /fapi/v1/ticker/24hr` — price/volume
