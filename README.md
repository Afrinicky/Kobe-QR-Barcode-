# Kobe QR & Barcode

A completely offline QR and barcode utility for Android. Scan, generate, organise —
all on the phone. No account, no backend, no analytics, and **no `INTERNET`
permission in the manifest**, so the app physically cannot phone home. The network is
only ever touched by the app you hand a link to.

Part of the Kobe series: native Kotlin, offline-first, shipped as an installable
APK/AAB.

---

## Features

### Scan
- Live camera scanning with automatic detection (no shutter button).
- QR plus the 1D symbologies people actually meet: **EAN-13, EAN-8, UPC-A, UPC-E,
  Code 128, Code 39, Code 93, ITF, Codabar**, and also Data Matrix, Aztec and PDF417.
- Flashlight toggle, dimmed scan window with corner brackets and a sweep indicator.
- Scan from an image in the gallery (photo picker — no storage permission needed).
- Vibration and optional beep on a hit.

### Smart Result Actions
The decoded payload is interpreted, not just displayed:

| Scanned | Shown as | Offered actions |
|---|---|---|
| `https://example.com` | **Website** — example.com | Open Website · Copy · Share |
| `+233201234567` | **Phone Number** | Call · Send SMS · Copy · Share |
| `WIFI:S:HomeWiFi;T:WPA;P:…;;` | **Wi-Fi Network** — network, security, password | Connect · Copy · Share |
| vCard / MECARD | **Contact** — name, org, phones, emails | Add Contact · Call · Email |
| `geo:5.6037,-0.1870?q=Accra` | **Location** | Open Map · Copy · Share |
| VEVENT | **Event** — start, end, location | Add to Calendar |
| `mailto:` / MATMSG | **Email** — subject, body | Send Email |
| `smsto:` | **SMS** — message body | Send SMS · Call |
| EAN/UPC digits | **Product** — symbology, digit count | Copy · Share |
| anything else | **Text** | Copy · Share |

Wi-Fi "Connect" uses `WifiNetworkSuggestion` + the system add-network dialog on
Android 11+, and falls back to opening Wi-Fi settings with the password copied.

### Generate QR
Ten content types — Text, Website, Phone, Email, SMS, Wi-Fi, Contact (vCard),
Location, Event, Social — each with its own form and validation.

Customisation: size (512–2048 px), error correction (L/M/Q/H), foreground and
background colour (swatches, RGB sliders or hex), quiet-zone margin, and an optional
centre logo. Adding a logo automatically raises error correction to **H** so the code
stays readable. Output: save PNG to the gallery, share, or print.

### Generate Barcode
Code 128, Code 39, EAN-13, EAN-8, UPC-A, UPC-E, ITF and Codabar, with per-format
input rules: numeric-only filtering, length checks, alphabet checks, even-digit ITF,
Codabar start/stop wrapping, and **automatic check-digit calculation** for EAN/UPC (a
wrong check digit is reported with the correct one). Human-readable digits are drawn
under the bars in their conventional groups.

### History & Favourites
Room-backed local history of everything scanned and created. Filter by All / Scanned /
Created / Favourites, full-text search, per-row copy, share, favourite and delete.
Tapping an entry re-renders the code from its stored style and shows the same smart
actions.

### Batch Scanner
Continuous scanning for shops, warehouses, pharmacies, labs and stock-taking. Each new
symbol drops into a live list, with duplicate detection (or duplicate counting when you
turn it off), a rescan guard, pause/resume, and export to **CSV** or **TXT**, copy-all,
or bulk-save to history.

### Settings
Theme (System/Light/Dark), Material You dynamic colour, vibration, beep,
auto-open URLs, keep scan history, batch duplicate handling, default QR style, and
clear-all-data.

---

## Stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM with a small clean-ish core/data/ui split |
| Camera | CameraX |
| Scanning | ML Kit Barcode Scanning (**bundled** model — works with no Play Services download) |
| Generation | ZXing core |
| Storage | Room |
| Preferences | DataStore |
| DI | Hilt |
| Build | Gradle Kotlin DSL + version catalog |
| Min SDK | 26 (Android 8.0) · Target/Compile SDK 35 |

No backend, no API, no cloud, no Firebase.

## Project layout

```
app/src/main/java/com/kobe/qrbarcode/
├── core/
│   ├── model/       CodeFormat, ContentKind, SmartAction, ParsedContent …
│   ├── parser/      ContentParser (decode → meaning), ContentBuilder (form → payload)
│   ├── generate/    CodeGenerator (ZXing → Bitmap), BarcodeValidator, CodeStyle
│   └── util/        ActionRunner (intents), ImageStore, TextExport, Feedback, TimeFormat
├── data/
│   ├── db/          Room entity, DAO, database
│   ├── repo/        CodeRepository + domain record mapping
│   └── prefs/       DataStore settings
├── di/              Hilt module
└── ui/
    ├── theme/       Kobe colour scheme, typography
    ├── nav/         routes + bottom destinations
    ├── components/  shared composables (rows, badges, colour picker, code preview)
    ├── home/ scan/ batch/ generate/ history/ detail/ settings/
```

The `core` package is pure Kotlin logic wherever possible, which is what the unit
tests exercise.

## Building

Requires JDK 17+ and the Android SDK (platform 35).

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # parser / validator / builder tests
./gradlew :app:lintDebug            # Android lint
./gradlew :app:assembleRelease      # minified release APK (R8)
./gradlew :app:bundleRelease        # AAB for Play
```

Outputs land in `app/build/outputs/`.

### Signing a release

Release builds are signed automatically if a keystore is configured; otherwise they are
produced unsigned. Create `keystore.properties` in the project root (git-ignored):

```properties
storeFile=kobe-release.jks
storePassword=…
keyAlias=kobe
keyPassword=…
```

The same values are read from `KOBE_STORE_FILE`, `KOBE_STORE_PASSWORD`,
`KOBE_KEY_ALIAS` and `KOBE_KEY_PASSWORD` for CI.

## Permissions

| Permission | Why |
|---|---|
| `CAMERA` | live scanning; frames are analysed on device and never stored |
| `VIBRATE` | scan confirmation |
| `WRITE_EXTERNAL_STORAGE` (maxSdk 28) | saving PNGs on Android 9 and older |

Gallery scanning uses the system photo picker, so no media permission is requested.
There is deliberately **no `INTERNET` permission**.

## Tests

`app/src/test/` covers the logic that decides what a code means and what is legal to
encode:

- `ContentParserTest` — URL/phone/email/SMS/Wi-Fi/vCard/geo detection, escaped Wi-Fi
  separators, and EAN digits reading as a product rather than a phone number.
- `BarcodeValidatorTest` — check-digit generation and rejection, ITF parity, Code 39
  and Code 128 alphabets, Codabar wrapping.
- `ContentBuilderTest` — round trips: what the generator writes, the parser reads back.
