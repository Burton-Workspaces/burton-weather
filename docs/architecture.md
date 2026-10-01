# Architecture

The app is a single Gradle module (`:app`), Kotlin, Jetpack Compose, Hilt, and OkHttp. UI never talks HTTP directly; screens collect `WeatherRepository` state.

```
ui/          Compose screens and ViewModels (Hilt)
domain/      SavedCity, WeatherBundle, RadarCatalog, units, WMO labels
data/
  parse      tiny JSON
  geocode    Open-Meteo geocoding
  weather    Open-Meteo forecast + air quality
  radar      RainViewer weather-maps catalog
  location   LocationManager last-known fix (no Play Services)
  repository WeatherRepository + LocalPrefs (DataStore)
di/          OkHttp client
```

## Forecasts and places

`Geocoder` queries `https://geocoding-api.open-meteo.com/v1/search`. Hits become `CityHit` rows; subscribe stores a `SavedCity` (Open-Meteo id when present).

`OpenMeteo` loads forecast and air quality in parallel:

- `https://api.open-meteo.com/v1/forecast` — current, hourly (24+), daily (14)
- `https://air-quality-api.open-meteo.com/v1/air-quality` — AQI and particulates (best-effort)

Values stay metric in the model. Compose formats °C/°F and km/h/mph from `Units` in the snapshot.

## Radar

`RainViewer` reads `https://api.rainviewer.com/public/weather-maps.json`. Each frame path becomes a tile template:

`{host}{path}/256/{z}/{x}/{y}/2/1_1.png`

`RadarScreen` hosts a WebView on `file:///android_asset/radar/index.html` (vendored Leaflet). Compose owns play/pause and frame index; JavaScript swaps overlay opacity.

The base map is CARTO Dark Matter raster tiles (`https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png?key=`). `CartoBasemap` adds the Android package and signing SHA-1 headers those keys expect. How to request and restrict a key is in [development.md](development.md).

## Snapshot and cache

`WeatherRepository` is a process singleton. `start()`:

1. Hydrates DataStore (`burton_weather`) so Cities is not an empty spinner
2. Refreshes every saved city

Removing a city drops it from the list and from the in-memory weather map, then persists.

## UI shell

`MainActivity` hosts a `NavHost` and a persistent bottom bar. Tab order is Cities → Radar. City detail is a nested destination (bar hidden). Tapping the detail radar thumbnail opens `city/{id}/radar`, still with the bar hidden and no city chips; the Radar tab keeps multi-city selection. Add-city and Settings are **FullScreenModal** overlays from Cities.
