# TradeSense- AI

## Swing Trading Analysis & Recommendation Engine

### Overview

**TradeSense- AI** is a data-driven swing trading decision-support system that analyzes stock market data to generate:

- **Buy recommendations** (from market universe)
- **Sell recommendations** (only from user's portfolio)

The system focuses on 2–3 week swing trading strategies, using technical indicators and scoring models instead of unreliable price predictions.

### Objectives

- Provide actionable buy/sell signals
- Use data-driven scoring, not speculation
- Build a scalable, modular system
- Enable future expansion into:
  - Intraday trading
  - Machine learning models
  - Multi-broker integrations

### Core Idea

Instead of predicting stock prices, **TradeSense- AI**:

1. Collects historical + latest market data
2. Computes technical indicators
3. Scores and ranks stocks
4. Generates:
   - **Buy recommendations** (top-ranked stocks)
   - **Sell recommendations** (portfolio-only)

### System Architecture

```
Frontend (React / Next.js)
        ↓
Backend API (FastAPI / Spring Boot)
        ↓
-----------------------------------
|      Analysis Engine            |
|  - Indicator Computation        |
|  - Scoring Model               |
-----------------------------------
        ↓
-----------------------------------
|     Data Service Layer         |
|  - Cache + DB                 |
|  - Provider Abstraction       |
-----------------------------------
        ↓
-----------------------------------
|   Data Providers (Pluggable)   |
|  - Yahoo Finance              |
|  - Free NSE APIs              |
|  - ICICI Breeze               |
|  - Kite (future)              |
-----------------------------------
```

### Data Provider Abstraction

**Market Data Interface**

```java
public interface MarketDataProvider {
    List<OHLC> getHistoricalData(String symbol, LocalDate from, LocalDate to);
    OHLC getLatestData(String symbol);
    List<String> getSupportedSymbols();
}
```

**Portfolio Interface**

```java
public interface PortfolioProvider {
    List<Holding> getHoldings();
    List<Position> getPositions();
}
```

**Supported Providers**

| Provider | Status |
|----------|--------|
| YahooFinanceProvider | MVP |
| FreeNSEProvider | Supported |
| ICICIBreezeProvider | Supported |
| KiteProvider | Planned |

### Data Strategy

**Initial Load**

- Fetch 6–12 months historical data
- Store in database

**Daily Update (Scheduled Job)**

1. Fetch latest OHLC data
2. Append to DB
3. Update indicators
4. Recompute scores
5. Generate recommendations

**Caching Strategy**

- DB-first lookup
- Fetch only latest data
- Optional Redis for hot caching

### Data Models

**OHLC**

```java
class OHLC {
    String symbol;
    LocalDate date;
    double open;
    double high;
    double low;
    double close;
    long volume;
}
```

**Portfolio**

```java
class Holding {
    String symbol;
    int quantity;
    double avgPrice;
}

class Position {
    String symbol;
    int quantity;
    double pnl;
}
```

### Indicators Used

- RSI (Relative Strength Index)
- Moving Averages (20, 50, 200 DMA)
- MACD
- Volume trends
- Relative strength vs index

### Scoring Model

Each stock is ranked using weighted factors:

**Score** = Momentum (30%) + Trend Strength (25%) + Volume Confirmation (15%) + RSI Health (10%) + Relative Strength (20%)

### Buy Strategy (Market-wide)

**Criteria**

- Price above key moving averages (50/200 DMA)
- Healthy pullback (RSI 40–50)
- Breakout (optional)
- Volume confirmation

**Output**

- Top N stocks (e.g., Top 5–10)

### Sell Strategy (Portfolio Only)

- **IF** Stop Loss hit (-5% to -8%) → SELL
- **ELSE IF** Price < 50 DMA → SELL
- **ELSE IF** RSI > 70 → PARTIAL SELL
- **ELSE IF** Better opportunity exists → SELL (optional)
- **ELSE** → HOLD

### Configuration

```yaml
marketDataProvider: YAHOO
portfolioProvider: MOCK

analysis:
  topN: 5
  stopLossPercent: 7
```

### Fallback Strategy (Advanced)

Try Primary Provider → if fail → Secondary → if fail → Free API

### Tech Stack

| Layer | Technologies |
|-------|----------------|
| Backend | Python (FastAPI) / Java (Spring Boot), Pandas, NumPy |
| Frontend | React / Next.js |
| Database | PostgreSQL |
| Cache | Redis (optional) |
| Infrastructure | AWS / GCP, Cron jobs |

### MVP Roadmap

| Week | Focus |
|------|--------|
| Week 1 | Data ingestion (historical), DB setup |
| Week 2 | Indicator engine, Scoring system |
| Week 3 | Portfolio integration (mock or broker) |
| Week 4 | Frontend dashboard |

### Future Enhancements

**Phase 2**

- Backtesting engine
- Portfolio optimization
- Alerts (buy/sell signals)

**Phase 3**

- ML models (XGBoost, LSTM)
- News sentiment analysis
- Multi-broker support

**Phase 4**

- Intraday trading
- Real-time streaming (WebSockets)

### Risks & Considerations

- Data reliability (free APIs)
- Market unpredictability
- Overfitting in models
- API rate limits
- Regulatory compliance (if monetized)

### Key Differentiator

Most tools: *“Top stocks to buy”*

**TradeSense- AI**: *“Given YOUR portfolio, what should you buy and sell?”*

### Disclaimer

This project is for **educational purposes only**. It does not constitute financial advice.

### Contributing

Contributions are welcome. Areas to improve:

- New trading strategies
- Better scoring models
- Additional data providers
- UI/UX improvements

### Future Vision

Build **TradeSense- AI** into a personal AI trading assistant that helps retail investors make smarter, data-backed decisions.
