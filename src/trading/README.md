# MiniTrader — Java OOP Console Trading Application

Aplicatie consola construita pentru cursul **Programare avansata pe obiecte** in **Java**!! Simuleaza un exchange simplificat cu active spot și perpetual, potrivire orders, gestionare portofoliu și urmarire pozitii.
## Features

- **Spot trading** — buy/sell assets ca BTC, ETH against USDT
- **Perpetual trading** — leveraged long/short positions pe perp markets
- **Order book** — sorted buy/sell orders cu matching automat
- **Portfolio management** — deposit, withdraw, view balances si positions
- **Database persistence** — SQLite via JDBC cu operatii CRUD
- **Audit logging** — toate actiunile logate in `audit.csv` cu ISO-8601 timestamps
- **Unit tests** — 10 de teste JUnit5 acopera serviciile si validarile

## Necesare pentru rularea Mini Trader

- **Java 17+** (tested with OpenJDK 17)
- **SQLite JDBC driver** — `sqlite-jdbc-3.46.1.3.jar` (in `lib/`)
- **JUnit 5** (for tests) — `junit-platform-console-standalone-1.10.2.jar` (in `lib/`)

## Structura proiectului

```
src/trading/
├── model/
│   ├── enums/
│   │   ├── AssetType.java          — SPOT, PERPETUAL
│   │   ├── OrderSide.java          — BUY, SELL
│   │   ├── OrderStatus.java        — OPEN, FILLED, CANCELLED
│   │   └── PositionSide.java       — LONG, SHORT
│   ├── Printable.java              — interface with toPrettyString()
│   ├── User.java                   — trader account
│   ├── Asset.java                  — abstract base (symbol, name, type)
│   ├── SpotAsset.java              — extends Asset (circulatingSupply)
│   ├── PerpAsset.java              — extends Asset (fundingRate, maxLeverage)
│   ├── Order.java                  — abstract base (Comparable by price)
│   ├── SpotOrder.java              — extends Order
│   ├── PerpOrder.java              — extends Order (leverage, positionSide)
│   ├── Position.java               — open perp position with PnL
│   ├── Portfolio.java              — balances (Map) + positions (List)
│   ├── Trade.java                  — executed trade record
│   ├── Market.java                 — trading pair with price/volume
│   └── OrderBook.java              — TreeSet-based sorted order book
├── exception/
│   └── TradingException.java       — custom runtime exception
├── database/
│   └── DatabaseConnection.java     — singleton JDBC connection + schema init
├── dao/
│   ├── UserDao.java                — interface
│   ├── UserDaoImpl.java            — JDBC implementation
│   ├── AssetDao.java               — interface
│   ├── AssetDaoImpl.java           — JDBC implementation
│   ├── MarketDao.java              — interface
│   ├── MarketDaoImpl.java          — JDBC implementation
│   ├── OrderDao.java               — interface
│   ├── OrderDaoImpl.java           — JDBC implementation
│   ├── TradeDao.java               — interface
│   └── TradeDaoImpl.java           — JDBC implementation
├── service/
│   ├── UserService.java            — singleton, user + portfolio operations
│   ├── MarketService.java          — singleton, market + order book operations
│   ├── TradingService.java         — singleton, order placement + matching
│   └── AuditService.java           — singleton, CSV audit logging
└── Main.java                       — console menu (14 actions)

test/trading/service/
├── UserServiceTest.java            — 9 tests
├── MarketServiceTest.java          — 4 tests
└── TradingServiceTest.java         — 7 tests
```

## Database Schema

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    users     │     │    assets    │     │   markets    │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id     (PK)  │     │ symbol (PK)  │◄────│ base_asset   │
│ username     │     │ name         │     │ symbol (PK)  │
│ email        │     │ asset_type   │     │ quote_asset  │
└──────┬───────┘     │ circ_supply  │     │ last_price   │
       │             │ funding_rate │     │ volume_24h   │
       │             │ max_leverage │     └──────┬───────┘
       │             └──────────────┘            │
       │                                         │
       ▼                                         ▼
┌──────────────┐                        ┌──────────────┐
│    orders    │                        │    trades    │
├──────────────┤                        ├──────────────┤
│ id     (PK)  │                        │ id     (PK)  │
│ user_id (FK) │                        │ buyer_id(FK) │
│ market  (FK) │                        │ seller_id(FK)│
│ side         │                        │ market  (FK) │
│ price        │                        │ price        │
│ quantity     │                        │ quantity     │
│ status       │                        │ created_at   │
│ order_type   │                        └──────────────┘
│ leverage     │
│ position_side│
│ created_at   │
└──────────────┘
```

## Cum sa compilezi si rulezi Mini Trader

```bash
# Compile
javac -cp "lib/sqlite-jdbc-3.46.1.3.jar" -d out \
  src/trading/model/enums/*.java \
  src/trading/model/*.java \
  src/trading/exception/*.java \
  src/trading/database/*.java \
  src/trading/dao/*.java \
  src/trading/service/*.java \
  src/trading/Main.java

# Run
java -cp "out:lib/sqlite-jdbc-3.46.1.3.jar" trading.Main
```

Baza de date SQLite (`minitrader.db`) se creeaza automat la prima rulare. Market-urile sunt adaugate apriori: `BTC/USDT`, `ETH/USDT`, `BTC-PERP/USDT`, `ETH-PERP/USDT`.

## Cum sa rulezi testele

```bash
# Compile tests
javac -cp "out:lib/sqlite-jdbc-3.46.1.3.jar:lib/junit-platform-console-standalone-1.10.2.jar" -d out \
  test/trading/service/UserServiceTest.java \
  test/trading/service/MarketServiceTest.java \
  test/trading/service/TradingServiceTest.java

# Run tests
java -jar lib/junit-platform-console-standalone-1.10.2.jar \
  --class-path "out:lib/sqlite-jdbc-3.46.1.3.jar" \
  --scan-class-path out
```

Testele folosesc SQLite in-memory (`jdbc:sqlite::memory:`) ca sa nu afecteze datele persistente din db.

## Comenzi in meniu — Exemple

```
=== Welcome to MiniTrader ===

--- Menu ---
 1. Register user
 2. Deposit funds
 3. Withdraw funds
 4. Place spot order
 5. Place perp order
 6. Cancel order
 7. View portfolio
 8. View open orders
 9. View trade history
10. View positions
11. View order book
12. Filter trades by market
13. View orders sorted by price
14. Market summary
 0. Exit
```

### Sesiune exemplu

```
Choose an option: 1
Username: alice
Email: alice@example.com
Registered: User[id=1, username=alice, email=alice@example.com]

Choose an option: 2
Username: alice
Asset symbol (e.g. USDT, BTC): USDT
Amount: 100000
Deposited 100000.0000 USDT for alice.

Choose an option: 4
Username: alice
Available markets:
  BTC/USDT
  ETH/USDT
  BTC-PERP/USDT
  ETH-PERP/USDT
Market symbol: BTC/USDT
Side (BUY/SELL): BUY
Price: 67000
Quantity: 1
Order placed: SpotOrder[id=1, BUY BTC/USDT 1.0000 @ 67000.00, status=OPEN, user=alice]

Choose an option: 7
Username: alice
=== Portfolio of alice ===
Balances:
  USDT: 100000.0000
Open Positions (0):
  (none)

Choose an option: 14
=== Market Summary ===
Market[BTC/USDT | price=67000.00 | vol24h=0.00 | type=SPOT]
Market[ETH/USDT | price=3500.00 | vol24h=0.00 | type=SPOT]
Market[BTC-PERP/USDT | price=67000.00 | vol24h=0.00 | type=PERPETUAL]
Market[ETH-PERP/USDT | price=3500.00 | vol24h=0.00 | type=PERPETUAL]
```

## Audit Log

Orice actiune este logata in `audit.csv`:
```
REGISTER_USER,2026-06-02T14:23:11.123456
DEPOSIT_FUNDS,2026-06-02T14:23:15.654321
PLACE_SPOT_ORDER,2026-06-02T14:23:20.111222
VIEW_PORTFOLIO,2026-06-02T14:23:25.333444
MARKET_SUMMARY,2026-06-02T14:23:30.555666
```

## OOP Concepts Used

| Concept                  | Where                                                                                         |
|--------------------------|-----------------------------------------------------------------------------------------------|
| **Mostenire**            | Asset → SpotAsset/PerpAsset, Order → SpotOrder/PerpOrder                                      |
| **Interfata**            | `Printable` implementat de toate clasele model                                                |
| **Enum**                 | AssetType, OrderSide, OrderStatus, PositionSide                                               |
| **Colectii**             | List (orders, trades), Set (users), Map (portfolios, markets, balances), TreeSet (order book) |
| **Colectii sortate**     | `TreeSet<Order>` in OrderBook cu comparator custom                                            |
| **Encapsulare**          | Campuri private, getters/setters                                                              |
| **Tratare exceptii**     | Custom TradingException + try/catch in Main                                                   |
| **Singleton**            | UserService, MarketService, TradingService, AuditService, DatabaseConnection                  |
| **DAO pattern**          | Interfete/implementari separate DAO pentru 5 entitati                                         |
| **Persistenta via JDBC** | SQLite cu PreparedStatement, CRUD pentru toate tabelele                                       |
| **Serviciu Audit**       | CSV logging cu ISO-8601 timestamps                                                            |
| **JUnit 5**              | 20 unit tests pe 3 clase test                                                                 |
