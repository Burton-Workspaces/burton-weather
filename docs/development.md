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

Radar tiles come from RainViewer and CARTO. A blank map usually means the WebView loaded but tiles were blocked; check device network and the CARTO key, not Leaflet itself (it is vendored).

## CARTO Basemaps API key

The radar base map is CARTO Dark Matter raster tiles (`dark_all`) from `basemaps.cartocdn.com`. Requests without a key are watermarked. Request a key, restrict it to this app, then put it on the Leaflet URL.

### Request a key

1. Open [Get your Basemaps API key](https://carto.com/basemaps/apikey/) or the [dashboard](https://dashboard.basemaps.carto.com/).
2. Fill the form (email, name, commercial vs not, what you are building). Accept the [Basemaps terms](https://carto.com/legal/basemap-terms/).
3. CARTO emails a magic-link sign-in and the key. There is no password. The key works immediately.
4. Later: **Already have a key? Sign in** on the same pages to see usage, add restrictions, create more keys, or delete them.

Non-commercial use is free up to 5M tile requests per calendar month (UTC) across every key on the account. Commercial use is free up to 1M. Attribution (`© OpenStreetMap contributors, © CARTO`) must stay visible on the map. Do not share one key across unrelated projects.

If the form does not load, email [support-basemaps@carto.com](mailto:support-basemaps@carto.com).

### Restrict the key to this app

On the dashboard, under **Restrictions → Restrict to mobile apps → Android**, add **package name + app signing SHA-1**. CARTO checks `X-Android-Package` and `X-Android-Cert`. Every restriction you enable must be met.

Use both rows if debug and release installs should both load tiles:

| Build | Package name | SHA-1 |
| --- | --- | --- |
| Release (`release.jks`, alias `burton`) | `com.burton.weather` | `50:4F:F7:44:C5:5C:87:2E:57:80:28:9C:49:EF:E3:38:63:BF:D5:69` |
| Debug (`~/.android/debug.keystore`) | `com.burton.weather.debug` | `BA:27:66:9E:A8:37:A2:98:B2:B3:EC:63:24:8E:5D:9D:B7:8D:8D:64` |

This app is signed with `release.jks` for GitHub Releases and the F-Droid catalog (no Play App Signing), so the release SHA-1 above is the cert on the shipped APK.

Re-print fingerprints after a keystore change:

```bash
keytool -list -v -keystore release.jks -alias burton | grep SHA1
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
```

Do not enable website Referer or origin-IP restrictions for this app. The map loads from a `file://` WebView on the phone.

`CartoBasemap` intercepts those tile requests in the radar WebView and adds `X-Android-Package` (the running `applicationId`) and `X-Android-Cert` (SHA-1 hex, no colons) so a key locked to this app still loads.

### Put the key on the tile URL

The live key is on the Leaflet URL in [`app/src/main/assets/radar/index.html`](../app/src/main/assets/radar/index.html) (`?key=` on every `basemaps.cartocdn.com` tile). It is visible in the APK. Package + SHA-1 restrictions are what stop other clients from using it. Keep `keystore.properties` and the JKS out of git; the basemap key is not a signing secret.

## Versioning while developing

Do not hand-edit `CHANGELOG.md` or `version.txt` on feature branches. Those are owned by [release-please](releases.md) from Conventional Commits on `master`.

Commit subjects must follow Conventional Commits. Install the hook once:

```bash
./scripts/install-git-hooks.sh
```

See [CONTRIBUTING.md](../CONTRIBUTING.md).
