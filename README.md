# MeiTüftler

Eine offline spielbare Android-Erfinderwerkstatt für Kinder: Murmeln, Holzrampen, Trampoline, Blöcke, Wind und eigene Ideen.

**Version 0.5.0 · versionCode 7 · Android 8 oder neuer · Paket `de.haberland.meitueftler`**

## Spielen

- 24 frei wählbare Aufgaben und ein freier Bauplatz mit bis zu 20 Bauteilen.
- Mit **＋ Brett**, **＋ Trampolin**, **＋ Block** oder **＋ Ventilator** ein Bauteil hinzufügen, antippen und mit dem Finger verschieben.
- **Bretter** leiten die Murmel weiter. **Trampoline** geben einen Rückstoß nach oben; ihre Neigung verändert die Sprungrichtung. **Blöcke** sind dicke Begrenzungen, die auch als kurze Rampen dienen.
- Aufgaben 11–16 führen Sprünge, Blöcke und Kombinationen ein. Aufgaben 17–20 führen den Ventilator ein. Die ursprünglichen Aufgaben und gespeicherten Bauwerke bleiben erhalten.
- **Ventilatoren** wirken im sichtbaren Luftkegel. Drehen ist rundherum möglich; **Stärke** wechselt zwischen sanft, mittel und kräftig. Kürzer/länger verändert die Reichweite. Der runde Ventilatorkörper ist ein Hindernis; der Luftkegel bleibt durchlässig.
- Im freien Bauplatz **Speichern** wählen und einen Namen eingeben. **Meine Erfindungen** öffnet die Galerie mit bis zu 20 Maschinen. Geladene Maschinen lassen sich aktualisieren oder als Kopie speichern; Löschen verlangt eine Rückfrage. Der freie Entwurf bleibt zusätzlich automatisch gespeichert.
- Eine warme Holzwerkbank mit Maserung, Schrauben, kleinen Randdetails, Bretter-Astlöchern, drehenden Propellern, Luftspuren, Federbewegung und wackelndem Zielkorb macht die Werkstatt lebendiger.
- Das ausgewählte Bauteil in 5°-Schritten drehen und in 40er-Schritten verlängern/verkürzen.
- **Ausprobieren** startet die Murmel. **Weiterbauen** setzt sie zurück; der Aufbau bleibt erhalten.
- **Zurück** macht die letzte Änderung rückgängig. **Neu bauen** entfernt alle Bauteile nach Rückfrage.
- **Hinweis** zeigt einen möglichen Aufbau als gestrichelte Bauteile. Es gibt auch andere Lösungen.
- Bis zu drei Sterne: Ziel erreicht, mit wenigen Bauteilen gelöst, ohne Hinweis gelöst. Keine gesperrten Aufgaben, kein Zeitdruck, keine Leben.
- Fortschritt, Aufbauten und Toneinstellung werden auf dem Gerät gespeichert. Töne lassen sich ausschalten.
- Hoch- und Querformat werden unterstützt; auf Tablets empfiehlt sich Querformat für die große Baufläche.

Eine Murmel, sechs Bauteilarten, feste Hindernisse und ein Zielkorb bilden den Baukasten. Hebel, bewegliche Maschinen, Motoren und mehrere Murmeln sind noch nicht enthalten. Die vereinfachte Physik simuliert Schwerkraft, Kollisionen und Rollverluste; sie ist kein wissenschaftliches Messwerkzeug. Luft ist ein vereinfachter Kraftkegel; Bauteile schirmen ihn in dieser Variante nicht ab.

## Abwechslungsreiche Wege ab 0.5.0

18 der 24 Aufgaben haben neue Hindernisaufbauten und Hinweise. Die einfachen Einführungen bleiben erhalten; dazwischen gibt es niedrige Tunnel, versetzte Fenster, eine S-Kurve, einen Hin-und-zurück-Weg, hohe Galerien und Schalter in mehrstufigen Wegen. Aufgabe 13 führt von links oben unter einer mittleren Sperre hindurch zum Korb rechts oben.

Die Referenzlösungen verwenden bei den neuen Aufgaben zwei bis vier Bauteile. Andere Lösungen bleiben ausdrücklich erlaubt. Tests prüfen echte Lösbarkeit, kleine Platzierungsabweichungen und viele einfache Ein-Bauteil-Aufbauten für die mehrstufigen Aufgaben. Das ist keine mathematische Garantie gegen jede kreative Abkürzung.

Sterne, freie Bauwerke und benannte Erfindungen bleiben erhalten. Neue Level-Geometrien bekommen eigene Aufbau-Speicherplätze; alte Level-Aufbauten werden nicht gelöscht und nicht in unpassende neue Hindernisse geladen.

## Schalter und Türen

- **＋ Schalter** setzt einen durchlässigen Berührungssensor; **＋ Tür** eine geschlossene Barriere.
- Ein Bauteil auswählen und über **Verbindung** Rot 1, Blau 2, Gelb 3 oder Grün 4 wählen. Gleichfarbige Türen öffnen bei Berührung eines Schalters und bleiben bis zum nächsten Versuch offen.
- Farbe und Zahl sind beide sichtbar; beim Auswählen verbinden gestrichelte Linien passende Bauteile. Mehrere Türen können denselben Schalter nutzen.
- Aufgaben 21–24 führen Schalter mit Rampen, Trampolinen und Wind ein. Die ersten haben feste Schalter im Weg; in Aufgaben 23 und 24 wird der Schalter selbst gebaut und zugeordnet.
- **Für Eltern · Info** zeigt Version, lokale Datenspeicherung und die optionale Diagnose nach einer Erwachsenenabfrage.

## Firebase und Play Store

Die Firebase-Konfiguration für MeiTüftler ist integriert. Die optionale Absturzdiagnose bleibt standardmäßig aus und wird erst im Elternbereich erlaubt. Play-In-App-Updates lassen sich im Release-Build dort prüfen.

Icon, Feature-Grafik, deutsche Texte und sechs Store-Bilder mit echten Spielansichten liegen unter [docs/play-store](docs/play-store).

Die konkrete Einrichtung und der lokale Release-Ablauf stehen in [docs/RELEASE.md](docs/RELEASE.md). Die Store-Beschreibung steht in [docs/STORE-LISTING.md](docs/STORE-LISTING.md); [docs/PRIVACY-DRAFT.md](docs/PRIVACY-DRAFT.md) ist ein zu vervollständigender Datenschutzentwurf.

## APK herunterladen

Die direkt installierbare APK liegt unter **[Releases](https://github.com/pehab/MeiTueftler/releases)**. Jeder erfolgreiche Build auf `main` veröffentlicht automatisch ein Testrelease mit APK. Die Veröffentlichung wartet auf Tests, Lint, Build und Emulator-Smoke-Test.

Tags haben das Format `v0.5.0-build.N`. Testreleases sind als Vorabversion markiert und verwenden die Android-Debug-Signatur. Eine feste private Testsignatur kann über das GitHub-Actions-Secret `MEITUEFTLER_TEST_KEYSTORE_BASE64` bereitgestellt werden. Die CI dekodiert den Schlüssel nur in ihr temporäres Verzeichnis. Ein Keystore darf nicht in das öffentliche Repository gelangen. Veröffentlichte Builds auf `main` verlangen diesen festen Schlüssel; bei fehlendem Secret bricht die CI ab. Pull-Request-Builds ohne Secret können weiterhin mit einer temporären Debugsignatur geprüft werden.

**Wechsel von den ersten Testbuilds:** Deren Signaturen wechselten ungewollt zwischen CI-Läufen. Bei einem Signaturwechsel muss die bisherige Test-App deinstalliert werden; dadurch wird der lokale Fortschritt gelöscht. Die neuen Speicherformate lesen weiterhin alte Bauwerke, wenn die App-Daten erhalten bleiben. Nach einmaliger Einrichtung des festen Schlüssels bleiben zukünftige Signaturen gleich. APK-Updates benötigen außerdem einen höheren `versionCode`.

## Entwickeln

Android Studio, JDK 17 und Android SDK 37 verwenden. Der Gradle-Wrapper ist enthalten:

```bash
bash gradlew testDebugUnitTest lintDebug assembleDebug
```

- `game/`: Android-unabhängiges Java-Modell, Level und Physik mit festen Schritten (240 Hz).
- `MainActivity`: Navigation, Bedienelemente und lokale Speicherung.
- `WorkshopView`: skalierbare Canvas-Grafik und Drag-Gesten; die Bildschirmauflösung verändert die Physik nicht.
- Unit-Tests prüfen insbesondere alle 24 Referenzlösungen, Federrückstoß, dicke Blöcke und gerichteten Wind, schnelle Kollisionen, Wiederholbarkeit und Baurücksetzung.
- GitHub Actions startet zusätzlich die APK auf einem Android-35-Emulator. Der Test bedient die App anhand ihres Accessibility-Baums, verschiebt ein Brett, löst eine Brett-, eine Trampolin- und eine Ventilator-Aufgabe und prüft die neue Werkzeugleiste, gespeicherte Bauteilarten, benannte Erfindungen samt Kopien und Änderungen nach einem Neustart, Rückgängig, Hochformat und die System-Zurück-Taste. Screenshots dokumentieren Menü, Aufgaben, Baufläche und Erfolg.

Die App verwendet keine Konten, Werbung, In-App-Käufe oder Google Analytics. Das Firebase-SDK bringt eine Netzberechtigung mit; Firebase wird erst bei eingerichteter Konfiguration und ausdrücklicher Zustimmung im Elternbereich gestartet. Spielen funktioniert offline. Die Grafiken werden direkt mit Canvas gezeichnet.
