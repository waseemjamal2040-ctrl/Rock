# NearbyChat - Karakish

- Room name: **كراكيش**
- Open room: no PIN required
- Join/Host with Google Nearby Connections (works offline: BT/BLE/Wi‑Fi Direct)
- Build via GitHub Actions to get APK without Android Studio.

## Quick Start (GitHub)
1) Create a new GitHub repo and upload this project (all files).
2) Open the **Actions** tab → enable workflows if prompted.
3) Push to `main` (or `master`). The workflow **Android CI** will run.
4) After it finishes, open the run → **Artifacts** → download **app-debug-apk** → `app-debug.apk`.
5) Transfer APK to your Android phone and install (allow unknown sources).

## Notes
- If Play Services Nearby is blocked on some devices, ensure Google Play Services is up-to-date.
- Release signing is not configured (unsigned). For a signed release, add a keystore and secrets to the workflow.