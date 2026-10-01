# Development

## Tooling

- JDK **17**
- Android SDK compile/target **35**, min **26**
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01
- Hilt 2.53.1 (KSP)

Point Gradle at the SDK with `local.properties` (`sdk.dir=…`). That file is gitignored.

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew testDebugUnitTest
```

Release assemble is blocked unless `keystore.properties` exists and `storeFile` points at a real keystore. Copy [`keystore.properties.example`](../keystore.properties.example) and keep `keystore.properties`, `*.jks`, and `*.keystore` out of git (see `.gitignore`). GitHub Actions signing is [build automation](build-automation.md).

Debug application id is `com.burton.weather.debug` so it can sit next to a signed install.

## Layout

```
app/src/main/java/com/burton/weather/
  MainActivity.kt              nav, location permission
  data/parse/                  tiny JSON
  data/geocode/                Open-Meteo places
  data/weather/                forecast + air quality
  data/radar/                  RainViewer catalog
  data/location/               last-known LocationManager fix
  data/repository/             WeatherRepository, DataStore
  domain/                      models, WMO codes, units
  ui/cities, search, detail, radar, settings, components, theme
app/src/main/assets/radar      Leaflet + radar map page
app/src/test/java/…            parser and codec tests (no device)
```

Parser tests cover geocoding JSON, Open-Meteo forecast/air, RainViewer frames, and city catalog round-trip. Run those before changing network parsing.

## Network while debugging

Search, forecast, and radar need the internet. The emulator is fine. All upstream APIs are HTTPS.

If a city search is empty: try a larger place name, or an ASCII spelling. Open-Meteo geocoding needs at least two characters.

Radar tiles come from RainViewer and Carto. A blank map usually means the WebView loaded but tiles were blocked; check device network, not Leaflet itself (it is vendored).

## Versioning while developing

Do not hand-edit `CHANGELOG.md` or `version.txt` on feature branches. Those are owned by [release-please](releases.md) from Conventional Commits on `master`.

Commit subjects must follow Conventional Commits. Install the hook once:

```bash
./scripts/install-git-hooks.sh
```

See [CONTRIBUTING.md](../CONTRIBUTING.md).
