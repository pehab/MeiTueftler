# MeiTüftler

Eine offline spielbare Android-Erfinderwerkstatt für Kinder: Murmeln, Holzrampen und eigene Ideen.

**Version 0.1.0 · versionCode 1 · Android 8 oder neuer · Paket `de.haberland.meitueftler`**

## Spielen

- Zehn frei wählbare Aufgaben und ein freier Bauplatz mit bis zu zwölf Brettern.
- Mit **＋ Brett** ein Brett hinzufügen, antippen und mit dem Finger verschieben.
- Das ausgewählte Brett in 5°-Schritten drehen und in 40er-Schritten verlängern/verkürzen.
- **Ausprobieren** startet die Murmel. **Weiterbauen** setzt sie zurück; der Aufbau bleibt erhalten.
- **Zurück** macht die letzte Änderung rückgängig. **Neu bauen** entfernt die Bretter nach Rückfrage.
- **Hinweis** zeigt einen möglichen Aufbau als gestrichelte Bretter. Es gibt auch andere Lösungen.
- Bis zu drei Sterne: Ziel erreicht, mit wenigen Brettern gelöst, ohne Hinweis gelöst. Keine gesperrten Aufgaben, kein Zeitdruck, keine Leben.
- Fortschritt, Aufbauten und Toneinstellung werden auf dem Gerät gespeichert. Töne lassen sich ausschalten.
- Hoch- und Querformat werden unterstützt; auf Tablets empfiehlt sich Querformat für die große Baufläche.

Eine Murmel, feste Hindernisse und ein Zielkorb bilden den ersten Baukasten. Hebel, bewegliche Maschinen, Motoren und mehrere Murmeln sind noch nicht enthalten. Die vereinfachte Physik simuliert Schwerkraft, Kollisionen und Rollverluste; sie ist kein wissenschaftliches Messwerkzeug.

## APK herunterladen

Die direkt installierbare APK liegt unter **[Releases](https://github.com/pehab/MeiTueftler/releases)**. Jeder erfolgreiche Build auf `main` veröffentlicht automatisch ein Testrelease mit APK. Die Veröffentlichung wartet auf Tests, Lint, Build und Emulator-Smoke-Test.

Tags haben das Format `v0.1.0-build.N`. Testreleases sind als Vorabversion markiert und verwenden die Android-Debug-Signatur. Die CI behält den Debug-Schlüssel über einen Cache für Updates bei. Wird dieser Cache gelöscht, kann sich die Signatur ändern; produktive Store-Releases benötigen später einen separat verwalteten Signaturschlüssel. APK-Updates benötigen außerdem einen höheren `versionCode`.

## Entwickeln

Android Studio, JDK 17 und Android SDK 37 verwenden. Der Gradle-Wrapper ist enthalten:

```bash
bash gradlew testDebugUnitTest lintDebug assembleDebug
```

- `game/`: Android-unabhängiges Java-Modell, Level und Physik mit festen Schritten (240 Hz).
- `MainActivity`: Navigation, Bedienelemente und lokale Speicherung.
- `WorkshopView`: skalierbare Canvas-Grafik und Drag-Gesten; die Bildschirmauflösung verändert die Physik nicht.
- Unit-Tests prüfen insbesondere alle zehn Referenzlösungen, schnelle Kollisionen, Wiederholbarkeit und Baurücksetzung.
- GitHub Actions startet zusätzlich die APK auf einem Android-35-Emulator. Der Test bedient die App anhand ihres Accessibility-Baums, verschiebt ein Brett, löst die erste Aufgabe und prüft gespeicherten Fortschritt, Hochformat und die System-Zurück-Taste. Screenshots dokumentieren Menü, Aufgaben, Baufläche und Erfolg.

Die App verwendet keine Netzberechtigung, Konten, Werbung, In-App-Käufe oder Analysedienste. Die Grafiken werden direkt mit Canvas gezeichnet.
