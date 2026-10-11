# MeiTüftler-Maschinendateien

Dateiname: `<Name>.meitueftler.json`, MIME-Typ `application/json`, UTF-8. Das Format enthält eine einzelne gespeicherte Konstruktion für den freien Bauplatz. Lokale UUIDs, Sterne, Fortschritt und App-Einstellungen werden nicht exportiert.

```json
{
  "format": "MeiTueftler-Maschine",
  "version": 1,
  "workshopVersion": 1,
  "category": "marbles",
  "name": "Meine Kugelbahn",
  "parts": [
    {"kind": "PLANK", "x": 500, "y": 300, "length": 480, "angle": 20, "power": 2, "channel": 1}
  ]
}
```

| Werkstatt | `category` | Bauteiltypen | Weitere Felder |
| --- | --- | --- | --- |
| Kugelbahnen | `marbles` | `PLANK`, `TRAMPOLINE`, `BLOCK`, `FAN`, `SWITCH`, `DOOR` | `length`, `power`, `channel` |
| Fahrzeuge | `vehicles` | `MOTOR`, `FRAME`, `WHEEL`, `WEIGHT`, `HINGE`, `DRIVE`, `FOOT` | `size`, `power`, `direction` |
| Mechanik | `mechanics` | `BEAM`, `WEIGHT`, `ROPE`, `WINCH`, `PULLEY` | `size`, `power`, `direction`, `bx`, `by`, `guide` |

Alle Bauteile enthalten `kind`, `x`, `y` und `angle`. Koordinaten beziehen sich auf die Werkbank mit 1000 × 600 Einheiten; Winkel sind Grad. `power` ist 1–3, `channel` 1–4, `direction` −1 oder 1. `guide` ist −1 für ein direktes Seil oder der nullbasierte Index eines festen Schraubpunkts im freien Mechanik-Bauplatz. Die beiden Seilenden sind `(x,y)` und `(bx,by)`.

`version` beschreibt das Dateischema, `workshopVersion` die zugehörige Bauplatz-Geometrie. Beide werden geprüft; zukünftige Versionen erfordern eine kompatible App. Einstellungen für nicht aktive Bauteilfunktionen werden mitgespeichert. Unbekannte Felder werden nicht in die lokale Konstruktion übernommen.

Der Import prüft die gesamte Datei vor dem Speichern: Format, unterstützte Versionen, Kategorie, Name mit 1–40 Zeichen, Zahlenbereiche, höchstens 60 bekannte Bauteile, genau einen Fahrzeugmotor, gültige Seilenden und Befestigungsindizes. Maximal 128 KiB und zwölf Verschachtelungsebenen sind erlaubt. Sehr große oder beschädigte Dateien verändern keine Spielstände. Fehlende optionale Einstellungen verwenden dieselben Standardwerte wie gespeicherte Aufbauten; alte Murmelteile ohne Typ bleiben als Holzbrett exportierbar.

Nach der Vorschau und Bestätigung erhält die Maschine eine neue lokale UUID. Gleiche Namen erhalten einen Import-Zusatz. Bei einer vollen Galerie (20 Einträge) wird der Import abgewiesen, ohne einen Eintrag zu ersetzen. Die Werkstatt wird aus dem Dateiinhalt bestimmt, unabhängig davon, aus welcher Galerie die Datei ausgewählt wurde.

Android nutzt `ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT` für die vom Benutzer ausgewählte Datei. Die Teilauswahl erhält eine `content://`-URI über den eigenen FileProvider und eine zeitweilige Leseberechtigung. Nur `cache/machine-exports/` ist dafür freigegeben; Spielstände und Diagnose-Dateien sind nicht zugänglich. Dateizugriffe laufen außerhalb des UI-Threads. Kein zusätzlicher Speicherzugriff und kein Server sind erforderlich.

Referenzen: [Android-Dateiauswahl](https://developer.android.com/training/data-storage/shared/documents-files), [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider).

Ab 0.11.0 enthält ein freier Aufbau optional `layout` und verwendet `workshopVersion: 2` (das Dateischema bleibt `version: 1`). Alte Dateien mit `workshopVersion: 1` werden weiterhin geladen. Lokal werden neue freie Aufbauten als `{ "parts": [...], "layout": {...} }` gespeichert; alte Bauteilarrays bleiben lesbar.

| Werkstatt | Layoutfelder |
| --- | --- |
| Kugelbahnen | `sx`, `sy` (Start), `gx`, `gy` (Korb) |
| Fahrzeuge | `gx`, `gy`, `surfaces`: bis zu 40 Arrays `[ax, ay, bx, by, twoSided]`; Motorposition bleibt im Motorbauteil |
| Mechanik | `sx`, `sy` (Kiste), `gx`, `gy`, `mounts`: Arrays `[x, y]` in der bisherigen Schraubpunktreihenfolge |

Layoutkoordinaten sind endliche Zahlen innerhalb der Werkbank; der Editor begrenzt Marker zusätzlich auf den sichtbaren Innenbereich. Die Anzahl der Mechanik-Schraubpunkte bleibt unverändert, sodass gespeicherte Seilführungsindizes gültig bleiben. Unbekannte Layoutfelder werden nicht gespeichert.
