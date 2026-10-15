# TradeMind AI — Connected Starter Project

This is a native Android starter with a multi-screen trading workspace and initial public-data integrations.

## Connected now
- **Crypto quote:** Binance public ticker endpoint for BTC/USDT (no API key required for public market data).
- **FX reference rate:** Frankfurter API for EUR/USD (reference exchange rate, not a broker quote; not suitable for execution).
- Background-thread network requests and refresh button.
- Chart/indicator/strategy/AI Lab UI, risk/reward calculator and educational indicator cards.

## Not yet connected — requires configuration or further implementation
- **AI analysis:** requires an AI provider account/API key and preferably a secure backend. Do not put a secret API key in the Android APK. Set up a small backend endpoint and configure `AI_BACKEND_URL` in the app when you have one.
- **Screenshot analysis:** image selection/upload UI is not connected to an AI vision backend.
- **Gold, indices and oil live quotes:** need a provider that supports those instruments and has appropriate licensing/terms. Current gold/index/oil entries are clearly marked illustrative.
- **Real chart candles and calculated indicators:** chart is a visual placeholder; indicator pages are educational descriptions, not computed from live candles.
- Broker execution, account sync, alerts and backtesting are not implemented.

Public endpoints may change, be rate-limited, or be unavailable in some regions. Displayed FX reference rates are not tradable prices. Do not use this prototype for financial decisions.

## Build APK through GitHub Actions
1. Extract the ZIP.
2. Upload the extracted files and folders into the root of your GitHub repository.
3. Commit to `main`.
4. Open **Actions** → **Build Android APK**.
5. Download artifact `TradeMind-AI-debug-apk` from the successful run.
6. Extract the artifact ZIP and install `app-debug.apk`.

## Local build
Requires JDK 17 and Gradle 8.9:
```bash
gradle assembleDebug
```

Never commit provider secrets. A production deployment should use a backend to protect secrets, validate data, implement rate limits, and provide proper privacy and risk disclosures.
