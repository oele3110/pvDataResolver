# PV Monitor — Android App Context for Claude Code

## Project overview

Native Android app for a self-hosted PV monitoring system. The backend runs on a Raspberry Pi 5
and is publicly accessible via Cloudflare Tunnel. The app shows live energy data and historical
statistics for 2 users (owner + wife).

## Tech stack

- **Kotlin**
- **Jetpack Compose** — UI framework
- **OkHttp + Retrofit** — REST API calls
- **OkHttp WebSocket** — live data connection
- **Hilt** — dependency injection
- **EncryptedSharedPreferences** — secure token storage

## Backend URLs (production)

- REST API: `https://pv.dennislampert.de/api/`
- WebSocket: `wss://pv.dennislampert.de/ws`
- Swagger UI: `https://pv.dennislampert.de/docs`

## Authentication

- `POST /api/login` with body `{"username": "...", "password": "..."}` returns
  `{"access_token": "...", "token_type": "bearer"}`
- JWT token is valid for 30 days
- All REST endpoints require `Authorization: Bearer <token>` header
- WebSocket requires token as query parameter: `wss://pv.dennislampert.de/ws?token=<jwt>`

## WebSocket — live data payload (~1s interval)

```json
{
  "timestamp": "2026-04-05T14:30:00Z",
  "inverter": {
    "power_ac_w": 3500,
    "power_dc_w": 3600,
    "home_consumption_from_pv_w": 2300
  },
  "smartmeter": {
    "grid_power_w": 1200,
    "home_consumption_w": 3500,
    "home_consumption_from_grid_w": 0,
    "home_consumption_from_battery_w": 0
  },
  "wallbox": {
    "power_w": 7400,
    "power_pv_w": 5000,
    "power_battery_w": 0,
    "power_grid_w": 2400,
    "session_energy_wh": 12300,
    "session_duration_min": 45.0,
    "active_charge_mode": 1
  },
  "battery": {
    "power_w": -1000,
    "state_of_charge_pct": 82
  },
  "heater": {
    "power_w": 2000,
    "temp1_c": 65.3,
    "temp2_c": 61.1
  },
  "consumers": {
    "power_oven": 0,
    "power_bathroom_heater_top_floor": 0,
    "power_bathroom_heater_ground_floor": 598,
    "power_dishwasher": 2.9,
    "power_kwl": 12.8,
    "power_fridge": 10.8,
    "power_fridge_hwr": 34.2,
    "power_tv": 16.7,
    "power_tv_accessory": 28,
    "power_dryer": 0,
    "power_washing_machine": 8.9,
    "power_water_softening": 17.2,
    "temperature_hot_water": 78.5
  },
  "calculated": {
    "self_consumption_w": 2300,
    "self_consumption_rate_pct": 65.7,
    "autarky_rate_pct": 82.3
  }
}
```

Notes:

- `grid_power_w` positive = feed-in to grid, negative = consuming from grid
- `battery.power_w` positive = charging, negative = discharging
- All power values in Watts, energy in Wh, temperature in Celsius
- Fields can be `null` if the device has not reported yet

## REST API — Historical data

### GET /api/history

Query parameters:

- `range`: `today` | `month` | `year`
- `device`: `all` | `inverter` | `smartmeter` | `wallbox` | `battery` | `heater` | `consumers`
- `month`: `YYYY-MM` (required when range=month)
- `year`: `YYYY` (required when range=year)

Response: array of `{"time": "ISO8601", "sensor": "sensor_name", "value": 123.4}`

Examples:

- `GET /api/history?range=today&device=all`
- `GET /api/history?range=month&month=2026-04&device=inverter`
- `GET /api/history?range=year&year=2026`

### GET /api/status

Returns device availability and last_seen timestamps:

```json
{
  "status": "ok",
  "uptime_s": 3600.0,
  "devices": [
    {"name": "modbus", "available": true, "last_seen": "2026-04-05T14:30:00Z"},
    {"name": "heater_rod", "available": true, "last_seen": "2026-04-05T14:30:00Z"},
    {"name": "mqtt", "available": true, "last_seen": "2026-04-05T14:30:00Z"}
  ]
}
```

## Project structure

```
pv-monitor-app/
├── app/src/main/java/de/pvmonitor/
│   ├── data/
│   │   ├── api/
│   │   │   ├── PvApiService.kt         # Retrofit interface (REST endpoints)
│   │   │   └── WebSocketManager.kt     # WebSocket connection + auto-reconnect
│   │   ├── auth/
│   │   │   ├── AuthRepository.kt       # Login, token storage (EncryptedSharedPrefs)
│   │   │   └── TokenInterceptor.kt     # OkHttp interceptor: attach JWT to every request
│   │   └── model/
│   │       ├── EnergyData.kt           # Live data model
│   │       └── Statistics.kt          # Historical aggregations
│   ├── ui/
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt      # Main view: live energy flow
│   │   │   └── DashboardViewModel.kt
│   │   ├── history/
│   │   │   ├── HistoryScreen.kt        # Daily/monthly/yearly charts
│   │   │   └── HistoryViewModel.kt
│   │   ├── login/
│   │   │   └── LoginScreen.kt
│   │   └── components/
│   │       ├── EnergyFlowDiagram.kt    # Animated energy flow diagram
│   │       ├── PowerGauge.kt           # Current power as gauge
│   │       └── ConsumptionCard.kt      # Individual consumer card
│   └── PvMonitorApp.kt                 # Navigation, Hilt DI setup
└── build.gradle.kts
```

## Coding guidelines

- Kotlin: use coroutines and Flow for async operations
- Jetpack Compose: follow unidirectional data flow (ViewModel → UI)
- WebSocket: implement auto-reconnect with exponential backoff
- Token storage: always use EncryptedSharedPreferences, never plain SharedPreferences
- Null safety: all WebSocket fields can be null — handle gracefully in UI
