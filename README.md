# DevxyzLogin

DevxyzLogin — GoLogin-style antidetect browser by JepongDevxyz.

- **Android app** (`android/`) — Kotlin/Jetpack Compose, package `com.devxyz.login`. Profile manager + isolated browser per profile.
- **Web dashboard** (`web/index.html`) — single-file GoLogin-style profiles dashboard.
- **Shared profile schema** — see `SHARED_SCHEMA.md` (same fields on web and Android).

Build: GitHub Actions (`.github/workflows/android.yml`, committed via web UI) runs `gradle assembleDebug` in `android/` and uploads the `DevxyzLogin-debug-apk` artifact.
