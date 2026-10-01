# Using the app

Burton Weather keeps saved cities and unit preferences on this phone. Forecasts and radar frames come from public HTTPS APIs. There is no Burton account.

## Screens

### Cities

The home list. Each card shows the place name, current condition, temperature, and today’s high/low once a forecast has loaded.

- **Add** opens a full-screen search. Type a city name; pick **Add**. Already-saved rows show **Added**.
- **Use this location** asks for coarse location (if needed) and saves a “Current location” pin from the last known network/GPS fix.
- **Refresh** reloads every saved city.
- **Close** on a card removes that city immediately.
- Tap a card for the detailed forecast.

### Detail

Current temperature and WMO condition, then a grid of humidity, wind, gusts, pressure, cloud cover, precipitation, dew point, visibility, UV, rain, snow, and sunrise/sunset.

If Open-Meteo’s air-quality feed answers, you also get US AQI (and EU AQI when present), PM2.5, PM10, ozone, NO₂, SO₂, CO, and grass pollen.

The next 24 hours scroll sideways. The 14-day outlook lists condition, precip chance, rainfall, UV, and high/low.

Current temperature and WMO condition sit beside a live radar thumbnail. Tap the thumbnail for a full-screen radar locked to this city (chevron returns to detail; the bottom bar stays hidden). **Delete** removes the city and returns to the list.

### Radar

Animated RainViewer frames over a dark Carto/OSM base map. From the Radar tab, play/pause steps through past frames and nowcast, and city chips recenter the map. Refresh pulls a new RainViewer catalog (new timestamps about every 10 minutes). City detail opens the same map for one city only.

### Settings

Opened from the Cities gear.

| Row | What it does |
| --- | --- |
| Cities | How many places are saved |
| Temperature | Tap to switch °C and °F |
| Wind | Tap to switch km/h and mph |
| Burton Weather | App version from `version.txt` |

## Permissions

| Permission | Why |
| --- | --- |
| Internet / network state | Geocoding, forecast, air quality, radar tiles |
| Coarse location (optional) | “Use this location” only. Denied, the rest of the app still works |

Nothing is uploaded to a Burton server. Open-Meteo and RainViewer see the HTTPS requests this phone makes.

## What lives on-device

- Saved cities and unit preferences in DataStore (`burton_weather`)
- In-memory forecasts until the process is killed; the next launch refreshes them
- Vendored Leaflet files for the radar WebView (no CDN)

Removing a city deletes it from the saved list. Uninstalling the app clears the catalog.
