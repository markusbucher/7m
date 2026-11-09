# Kirchheim Handball (Android & iOS)

Zeigt für die SG HD-Kirchheim (Männer Bezirksklasse Staffel 2 Rhein-Neckar, Saison 2026/27):
Tabelle · Gesamtspielplan · Spielplan Kirchheim · Torschützenliste (aus den PDF-Spielberichten).

Die App holt Tabelle und Spielplan beim Handball4All-Dienst (derselbe, den die BWHV-Seite nutzt) und liest neue
PDF-Spielberichte direkt auf dem Gerät aus. Elf bereits ausgewertete Spiele sind als Startdaten
(`shared/src/commonMain/composeResources/files/seed_goals.json`) enthalten. Button „Aktualisieren“ oben rechts.
Beim ersten Start ohne Daten wird automatisch geladen (Internet nötig); danach funktioniert die App mit dem
gespeicherten Stand auch offline. Ein fehlerhafter Spielbericht wird höchstens alle 12 Stunden erneut geladen.

## Aufbau (Kotlin Multiplatform)

| Ordner | Inhalt |
|---|---|
| `shared/src/commonMain` | Gemeinsamer Code für Android und iOS: Datenmodell, JSON- und Spielbericht-Auswertung, Torschützenliste, Speicherung, Oberfläche (Compose Multiplatform) |
| `shared/src/androidMain` | Android-spezifisch: PDF-Text mit PDFBox-Android, Dateien |
| `shared/src/iosMain` | iOS-spezifisch: PDF-Text mit PDFKit, Dateien, Einstieg `MainViewController()` |
| `shared/src/commonTest` | Tests (laufen auf Android/JVM und im iOS-Simulator) |
| `app` | Android-App (nur `MainActivity`, Icon, Manifest) |
| `iosApp` | iOS-App (SwiftUI-Hülle); das Xcode-Projekt wird mit [XcodeGen](https://github.com/yonaskolb/XcodeGen) aus `project.yml` erzeugt |

Mannschaft und Liga sind fest eingestellt: `Team.kt` und `ApiParser.LEAGUE_URL`.

## Tests

```bash
./gradlew :shared:testAndroidHostTest
```

Auf einem Mac zusätzlich im iOS-Simulator:

```bash
./gradlew :shared:iosSimulatorArm64Test
```

## Android: APK bauen

**Variante A – Android Studio:** Projekt öffnen, Gradle-Sync abwarten, *Build → Build APK(s)*.
Die Datei liegt unter `app/build/outputs/apk/debug/app-debug.apk`.

**Variante B – GitHub:** Reiter *Actions → Bauen und testen → Run workflow*. Nach einigen Minuten steht unter
*Artifacts* die Datei `KirchheimHandball-APK`.

Installieren: APK aufs Handy übertragen, antippen, Installation aus dieser Quelle einmalig erlauben.

Für eine Release-Version mit eigenem Schlüssel die Umgebungsvariablen `SIGNING_STORE_FILE`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` und `SIGNING_KEY_PASSWORD` setzen und `./gradlew :app:assembleRelease`
ausführen; ohne sie wird mit dem Debug-Schlüssel signiert.

## iOS: App bauen (macOS mit Xcode nötig)

```bash
brew install xcodegen
```

```bash
cd iosApp && xcodegen && open KirchheimHandball.xcodeproj
```

In Xcode unter *Signing & Capabilities* das eigene Team wählen und auf Gerät oder Simulator starten.
Der Kotlin-Teil wird dabei automatisch von Gradle gebaut. Die GitHub Action baut die App zur Kontrolle für den
Simulator (ohne Signatur).

## Technik

Kotlin 2.4, Compose Multiplatform 1.12, Ktor (HTTP), kotlinx.serialization (JSON), PDFBox-Android (PDF auf
Android), PDFKit (PDF auf iOS). Mindestens Android 8.0 bzw. iOS 16.
