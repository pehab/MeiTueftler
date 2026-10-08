# Firebase und lokaler Play-Store-Release

Stand: 8. Oktober 2026. Version 0.6.1, versionCode 9, Paket de.haberland.meitueftler.

## Firebase einrichten

1. In einem eigenen Firebase-Projekt eine Android-App mit genau `de.haberland.meitueftler` registrieren. Google Analytics ist für dieses Projekt nicht erforderlich und nicht als SDK eingebaut.
2. Crashlytics in der Firebase-Konsole öffnen. `google-services.json` herunterladen und lokal unter `app/google-services.json` ablegen. Die öffentliche Firebase-Client-Konfiguration für genau MeiTüftler ist im Repository enthalten; private Service-Account-Schlüssel oder Keystores gehören nicht hinein.
3. Für GitHub-APKs optional das Actions-Secret `MEITUEFTLER_GOOGLE_SERVICES_JSON` mit dem vollständigen JSON-Inhalt setzen. CI schreibt es während des Builds in app/google-services.json. Ohne Secret wird die eingecheckte MeiTüftler-Konfiguration verwendet.
4. Android Studio synchronisieren. Google-Services- und Crashlytics-Gradle-Plugins werden nur mit vorhandener Konfiguration angewandt. SDK: Firebase BoM 35.0.0, Crashlytics; keine Analytics-Abhängigkeit.
5. Auf einem eigenen Testgerät „Für Eltern · Info“ öffnen, Erwachsenenabfrage beantworten und die Datenerklärung lesen. „Diagnose erlauben“ aktivieren. Vorher wird Firebase nicht initialisiert; der automatische FirebaseInitProvider ist aus dem Manifest entfernt und die Sammlung standardmäßig deaktiviert.
6. Nur im Debug-Build erscheint bei erlaubter Diagnose „Test-Absturz“. Rückfrage bestätigen, App neu öffnen, Bericht in der Crashlytics-Konsole prüfen. Die CI führt einen bestätigten synthetischen Test-Absturz aus und kontrolliert die Crashlytics-Queue sowie, soweit zeitnah vorhanden, den HTTP-Upload-Acknowledgement. Die Anzeige des Berichts in der Firebase-Konsole muss separat kontrolliert werden.
7. Diagnose wieder ausschalten prüfen: automatische Meldung deaktiviert, noch nicht gesendete Berichte werden zum Löschen vorgemerkt, bei künftigen Starts ohne Zustimmung wird Firebase nicht gestartet. Bereits gesendete Daten werden nicht zurückgerufen. Keine Erfindungsnamen, Bauwerke oder Nutzer-IDs als Custom Keys oder Logs anhängen.

## Lokales AAB

GitHub baut und veröffentlicht weiterhin ausschließlich eine Debug-APK. Kein AAB und kein Play-Upload sind im Workflow eingerichtet.

1. In Android Studio „Generate Signed App Bundle / APK“ → Android App Bundle → release.
2. Einen privaten Release-/Upload-Keystore nutzen, sicher außerhalb des Repositories verwahren und sichern. Nicht den GitHub-Testschlüssel verwenden; Passwörter nicht in Gradle oder Git speichern.
3. Play App Signing in der Play Console einrichten und zunächst einen internen Test veröffentlichen. Jede spätere Version braucht einen höheren versionCode.
4. Die GitHub-Test-APK ist mit einer Debugsignatur signiert. Der Wechsel zur Play-App kann wegen anderer App-Signatur eine Deinstallation erfordern und lokale Daten löschen. Keine nahtlose Migration versprechen. Vorher Maschinen ggf. auf dem Testgerät behalten; einen Export gibt es noch nicht.
5. Release-Build lokal testen: Modusauswahl, freie Fahrzeugmontage, Motorbegrenzung, Fahren, Schwerpunkt, Fahrzeuggalerie und gespeicherte Kugelbahnen sowie neue Hinderniswege, freie Schalter/Türen, Zuordnung, gespeicherte Erfindungen, Bildschirmwechsel und Elternzustimmung. Mit Firebase-Konfiguration auch einen Diagnosebericht aus einem Testbuild prüfen.

## Vor Veröffentlichung vervollständigen

- Store-Kurz-/Langbeschreibung aus STORE-LISTING.md sowie eigene Screenshots und Feature-Grafik hochladen.
- Zielgruppe passend zum tatsächlichen Angebot auswählen (u. a. 5 Jahre und jünger sowie 6–8); Families-Anforderungen prüfen. Erwachsenenabfrage ist ein Bedienhindernis und allein kein Nachweis gesetzlicher Einwilligung.
- IARC-Fragebogen wahrheitsgemäß ausfüllen; keine Werbung, Käufe oder Konten vorhanden.
- Datenschutzentwurf um verantwortliche Person, Kontakt, konkrete Rechtsgrundlage und veröffentlichte URL ergänzen. Firebase-Vertrag, internationale Verarbeitung und SDK-Eignung für Kinder vor Veröffentlichung prüfen.
- Datensicherheit anhand des tatsächlichen Release-Builds angeben: mit Crashlytics optionale Absturzprotokolle/Diagnosedaten und Geräte- oder andere IDs für App-Funktion/Stabilität. „Keine Datenerhebung“ passt nicht zu einem Build, in dem Eltern Crashlytics erlauben können. Google unterscheidet im Formular Auftragsverarbeitung und Weitergabe; die konkrete Konfiguration prüfen.
- App-Zugriff: alle Spielaufgaben ohne Konto offen; Elternbereich mit sichtbarer Rechenaufgabe erreichbar. Prüfhinweis entsprechend angeben.

Offizielle Quellen:
- https://firebase.google.com/docs/crashlytics/android/get-started
- https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
- https://firebase.google.com/support/privacy
- https://support.google.com/googleplay/android-developer/answer/9893335
- https://support.google.com/googleplay/android-developer/answer/10787469

## Play-In-App-Updates

Im Elternbereich gibt es im Release-Build „Nach Updates suchen“. Nur eine über Google Play installierte App startet den flexiblen Update-Dialog; Debug-/GitHub-Builds kontaktieren Play nicht. Heruntergeladene Updates werden nach Bestätigung und Speicherung des Aufbaus mit Neustart abgeschlossen. Prüfen im internen Play-Test mit zwei Versionen und höherem versionCode; ein lokaler Build allein kann diesen Store-Ablauf nicht Ende zu Ende testen.

Quelle: https://developer.android.com/guide/playcore/in-app-updates/kotlin-java
