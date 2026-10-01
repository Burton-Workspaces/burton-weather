# Burton Weather

An Android weather client. Save cities, read a detailed forecast, remove places you no longer need, and watch a live RainViewer radar. There is no cloud account and no weather-vendor login.

Signed APKs are published on [GitHub Releases](https://github.com/Burton-Workspaces/burton-weather/releases). Droidify / F-Droid: [burton-sonos-fdroid](https://github.com/Burton-Workspaces/burton-sonos-fdroid) (`https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo`).

## What it does

- **Cities** — saved places with current temperature, condition, and high/low; tap to open the forecast; remove with the close control
- **Add city** — search Open-Meteo’s public geocoder, or use this phone’s coarse location
- **Detail** — current conditions, humidity, wind, pressure, UV, visibility, dew point, air quality, 24-hour strip, 14-day outlook
- **Radar** — animated RainViewer frames over a dark map, centered on a saved city
- **Settings** — °C/°F, km/h or mph, saved-city count, version

First launch hydrates saved cities from local cache, then refreshes forecasts.

## Requirements

- Android 8.0+ (API 26)
- Internet access for geocoding, forecasts, air quality, and radar tiles
- Optional coarse location to add “Current location”

## Docs

| Doc | Contents |
| --- | --- |
| [Using the app](docs/using.md) | Screens, permissions, and what lives on-device |
| [Architecture](docs/architecture.md) | Packages, Open-Meteo, RainViewer, caching |
| [Development](docs/development.md) | Build, run, test, project layout |
| [Build automation](docs/build-automation.md) | GitHub Actions, workflow permissions, signing secrets |
| [Releases](docs/releases.md) | SemVer, local build + publish walkthrough, GitHub Releases |
| [F-Droid / Droidify](docs/fdroid.md) | Self-hosted repo, Fingerprint, Pages publish script |
| [Contributing](CONTRIBUTING.md) | Conventional Commits (required) |

## Quick start (debug)

```bash
./gradlew :app:installDebug
```

Debug builds use application id `com.burton.weather.debug`. Release builds need a keystore; see [docs/releases.md](docs/releases.md).

```bash
./gradlew testDebugUnitTest
```

## License and scope

This is a household weather viewer. It does not replace a meteorological service, and it does not sign in to Apple, Google, AccuWeather, or other weather accounts.
