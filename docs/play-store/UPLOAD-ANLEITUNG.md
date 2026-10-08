# Play-Store-Paket · MeiTüftler 0.6.2

- icon-512.png: App-Symbol, 512×512 Pixel, PNG.
- feature-1024x500.png: Feature-Grafik, 1024×500 Pixel, RGB-PNG ohne Transparenz.
- 01–06 … 1920x1080.png: sechs Store-Bilder im 16:9-Querformat, RGB-PNG. Sie zeigen echte Spielansichten mit ergänzenden Bildunterschriften.
- tablet-screenshots/: dieselben authentischen Spielansichten ohne Rahmen und Zusatztexte, 1280×800 Pixel; für Tablet-Screenshot-Felder.
- store-texte-de.txt: Name, Kurzbeschreibung und Langbeschreibung auf Deutsch.
- release-notes-de.txt: kurze Versionshinweise.

Die bestehenden Werbebilder zeigen Kugelbahnen aus dem geprüften Emulatorlauf von 0.4.0. Sie bilden die neue Kategorieauswahl und die Fahrzeugwerkstatt von 0.6.2 noch nicht ab.

Die Feature-Grafik ist eine Werbeillustration; sie ist kein Screenshot der Spielgrafik.

Das AAB wie vereinbart lokal in Android Studio signieren. GitHub veröffentlicht weiterhin nur die Test-APK. Support-E-Mail, vollständige Datenschutzerklärungs-URL und App-Inhalte/Datensicherheit in der Play Console ergänzen. Der Datenschutztext im Repository ist ein noch zu vervollständigender Entwurf.

Vorgaben geprüft am 5. Oktober 2026 anhand https://support.google.com/googleplay/android-developer/answer/9866151?hl=de

## Screenshots für 0.6.2

18 Level wurden überarbeitet. Die vorhandenen Store-Bilder stammen aus 0.4.x. Bild 02 zeigt die alte Aufgabenliste und muss vor dem Upload von 0.6.2 durch eine aktuelle Aufnahme ersetzt werden. Die unveränderten Einführungsaufgaben in Bildern 01, 03, 04 und 05 bleiben repräsentativ. Aktuelle Screenshots der Aufgabenliste und des neuen Unterpass-Level liegen beim erfolgreichen CI-Lauf im Artefakt `interface-screenshots`.

Für 0.6.2 zusätzlich aktuelle Aufnahmen von Kategorieauswahl, Fahrzeug-Baufläche, verbundenem Fahrzeug, Fahrzeug-Erfolg und Fahrzeug-Galerie verwenden. Die CI legt diese im Artefakt `interface-screenshots` ab (`menu`, `vehicle-menu`, `vehicle-built`, `vehicle-solved`, `vehicle-inventions`). Die bisherigen Werbegrafiken werden durch diese Codeänderung nicht neu gerendert.
