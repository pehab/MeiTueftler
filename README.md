# MeiTüftler

Eine offline spielbare Android-Erfinderwerkstatt für Kinder: Murmeln, Holzrampen, Trampoline, Blöcke und eigene Ideen.

**Version 0.2.1 · versionCode 3 · Android 8 oder neuer · Paket `de.haberland.meitueftler`**

## Spielen

- 16 frei wählbare Aufgaben und ein freier Bauplatz mit bis zu 20 Bauteilen.
- Mit **＋ Brett**, **＋ Trampolin** oder **＋ Block** ein Bauteil hinzufügen, antippen und mit dem Finger verschieben.
- **Bretter** leiten die Murmel weiter. **Trampoline** geben einen Rückstoß nach oben; ihre Neigung verändert die Sprungrichtung. **Blöcke** sind dicke Begrenzungen, die auch als kurze Rampen dienen.
- Aufgaben 11–16 führen Sprünge, Blöcke und Kombinationen ein. Die ursprünglichen Aufgaben und gespeicherten Bauwerke bleiben erhalten.
- Das ausgewählte Bauteil in 5°-Schritten drehen und in 40er-Schritten verlängern/verkürzen.
- **Ausprobieren** startet die Murmel. **Weiterbauen** setzt sie zurück; der Aufbau bleibt erhalten.
- **Zurück** macht die letzte Änderung rückgängig. **Neu bauen** entfernt alle Bauteile nach Rückfrage.
- **Hinweis** zeigt einen möglichen Aufbau als gestrichelte Bauteile. Es gibt auch andere Lösungen.
- Bis zu drei Sterne: Ziel erreicht, mit wenigen Bauteilen gelöst, ohne Hinweis gelöst. Keine gesperrten Aufgaben, kein Zeitdruck, keine Leben.
- Fortschritt, Aufbauten und Toneinstellung werden auf dem Gerät gespeichert. Töne lassen sich ausschalten.
- Hoch- und Querformat werden unterstützt; auf Tablets empfiehlt sich Querformat für die große Baufläche.

Eine Murmel, drei Bauteilarten, feste Hindernisse und ein Zielkorb bilden den Baukasten. Hebel, bewegliche Maschinen, Motoren und mehrere Murmeln sind noch nicht enthalten. Die vereinfachte Physik simuliert Schwerkraft, Kollisionen und Rollverluste; sie ist kein wissenschaftliches Messwerkzeug.

## APK herunterladen

Die direkt installierbare APK liegt unter **[Releases](https://github.com/pehab/MeiTueftler/releases)**. Jeder erfolgreiche Build auf `main` veröffentlicht automatisch ein Testrelease mit APK. Die Veröffentlichung wartet auf Tests, Lint, Build und Emulator-Smoke-Test.

Tags haben das Format `v0.2.1-build.N`. Testreleases sind als Vorabversion markiert und verwenden die Android-Debug-Signatur. Eine feste private Testsignatur kann über das GitHub-Actions-Secret `MEITUEFTLER_TEST_KEYSTORE_BASE64` bereitgestellt werden. Die CI dekodiert den Schlüssel nur in ihr temporäres Verzeichnis. Ein Keystore darf nicht in das öffentliche Repository gelangen. Veröffentlichte Builds auf `main` verlangen diesen festen Schlüssel; bei fehlendem Secret bricht die CI ab. Pull-Request-Builds ohne Secret können weiterhin mit einer temporären Debugsignatur geprüft werden.

**Wechsel von den ersten Testbuilds:** Deren Signaturen wechselten ungewollt zwischen CI-Läufen. Bei einem Signaturwechsel muss die bisherige Test-App deinstalliert werden; dadurch wird der lokale Fortschritt gelöscht. Die neuen Speicherformate lesen weiterhin alte Bauwerke, wenn die App-Daten erhalten bleiben. Nach einmaliger Einrichtung des festen Schlüssels bleiben zukünftige Signaturen gleich. APK-Updates benötigen außerdem einen höheren `versionCode`.

## Entwickeln

Android Studio, JDK 17 und Android SDK 37 verwenden. Der Gradle-Wrapper ist enthalten:

```bash
bash gradlew testDebugUnitTest lintDebug assembleDebug
```

- `game/`: Android-unabhängiges Java-Modell, Level und Physik mit festen Schritten (240 Hz).
- `MainActivity`: Navigation, Bedienelemente und lokale Speicherung.
- `WorkshopView`: skalierbare Canvas-Grafik und Drag-Gesten; die Bildschirmauflösung verändert die Physik nicht.
- Unit-Tests prüfen insbesondere alle 16 Referenzlösungen, Federrückstoß und dicke Blöcke, schnelle Kollisionen, Wiederholbarkeit und Baurücksetzung.
- GitHub Actions startet zusätzlich die APK auf einem Android-35-Emulator. Der Test bedient die App anhand ihres Accessibility-Baums, verschiebt ein Brett, löst die erste Aufgabe und eine Trampolin-Aufgabe und prüft die neue Werkzeugleiste, gespeicherte Bauteilarten, Rückgängig, Hochformat und die System-Zurück-Taste. Screenshots dokumentieren Menü, Aufgaben, Baufläche und Erfolg.

Die App verwendet keine Netzberechtigung, Konten, Werbung, In-App-Käufe oder Analysedienste. Die Grafiken werden direkt mit Canvas gezeichnet.
