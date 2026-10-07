# NfcEvAuthApp - simple Android app (admin + phone-as-card)

## What it does
1. **Admin enroll**: enter username+email -> `create-pending` -> shows 6-digit claim code.
   On ESP32 serial: `enroll <code>` then tap blank Classic card. ESP32 claims + writes sector 8.
2. **Phone pass**: enter enrolled email -> `request-pass` -> 32-char code, served via HCE (AID `F039413031`).
   MVP verify: on ESP32 serial `phone <code>`. Full HCE auto-read is Phase 2 (see `HceService.kt` + `pollPhone()` stub in firmware).

## Open in Android Studio
1. Open folder `NfcEvAuthApp`, let Gradle sync (needs internet first time).
2. `minSdk 26` (Android 8+, HCE requirement). NFC permission declared; HCE optional.
3. Edit default URL/secret in `activity_main.xml` + `MainActivity.kt` apikey if you rotate keys.
4. Run on physical Android phone (emulator has no NFC/HCE).

## Supabase wiring
- Functions: `https://ihtmnshkyskidewdxihd.supabase.co/functions/v1/nfc-admin`, `verify-tap`
- Headers: `x-device-secret: dev-device-secret-change-me`, `apikey: sb_publishable_...`
- Set real `MASTER_KEY` + `DEVICE_SECRET` in Supabase Dashboard > Edge Functions > Secrets. Dev defaults work but rotate before any real deployment.

## Security note
Classic is clone-detecting, not clone-proof (Crypto1 broken). Protection = server HMAC tag + rotating counter + revocation + logs. For production move to NTAG 424 DNA / DESFire.
