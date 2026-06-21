# CosmoHID — keyboard relay

Turn the **Cosmo Communicator** (Android 9) into a real **Bluetooth keyboard** for
another device (e.g. **OPPO Find N2**), so the Cosmo's physical keys give
**real-time control** on the host — spacebar play/pause, arrow scrub, Ctrl
shortcuts — fully **offline** (pure Bluetooth HID, no Wi‑Fi/internet, no app on
the host, no root).

## How it works
- `HidManager` grabs the `BluetoothHidDevice` profile (API 28+) and advertises a
  standard 8‑byte boot‑keyboard SDP record, then sends HID reports to a bonded host.
- `RelayImeService` is an input method that captures the Cosmo's real
  `onKeyDown`/`onKeyUp` and forwards them as HID reports (true keydown/keyup, not
  text injection).
- `HidService` is a foreground service so the connection survives while you're in
  the editor on the host.

## Build
```bash
source .env-android        # JDK 17 + Android SDK
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## First-run test (on the Cosmo)
1. Install the APK on the Cosmo. Open **CosmoHID**, grant Bluetooth permission.
2. In Android **Bluetooth settings**, pair the Cosmo with the Find N2.
3. In the app: **1. Start HID service (register)**.
4. **2. Refresh paired devices** → tap **Connect: <Find N2>**. Status should show
   `CONNECTED`.
5. Quick check without the IME: tap **Send TEST: Space** — the N2 should react as
   if space was pressed (e.g. play/pause in a player).
6. Enable the relay keyboard: **Enable CosmoHID Relay in settings** → turn it on →
   **Switch to CosmoHID Relay (picker)** → pick it.
7. Tap the **Capture box**, then type on the Cosmo physical keyboard. Keys are
   relayed live to whatever is focused on the N2.

## Known v1 limits
- You must keep a focused field (the capture box) on the Cosmo for the IME to
  receive hardware keys. (A dedicated always-on capture activity is a v2 idea.)
- Keymap covers letters/numbers/symbols/F‑keys/arrows/nav + modifiers. Media keys
  and the Cosmo Fn layer are not mapped yet.
- Single host at a time; reconnect by tapping Connect again.

## Related — other Planet Cosmo Communicator fixes

- **[cosmo-standby-battery-fix](https://github.com/tathome2025/cosmo-standby-battery-fix)**
  — diagnose & fix the V19 standby battery drain (a cover-display wake lock keeps
  the SoC awake), including a no-Windows V19→V23 fastboot upgrade guide.

Made to help fellow Cosmo owners keep these great little devices useful. Issues
and PRs welcome.
