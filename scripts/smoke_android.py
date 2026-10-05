"""Exercise the release APK using Android's accessibility tree, not fixed screen coordinates."""
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "de.haberland.meitueftler"
OUTPUT = Path("screenshots")
OUTPUT.mkdir(exist_ok=True)
launcher_recoveries = 0


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)


def tree():
    global launcher_recoveries
    adb("shell", "uiautomator", "dump", "/sdcard/workshop-ui.xml")
    root = ET.fromstring(adb("shell", "cat", "/sdcard/workshop-ui.xml"))
    # Some cold emulator boots show an ANR from Pixel Launcher after wm resize.
    # Recover only this exact system app once. Never dismiss a MeiTüftler ANR.
    if any(i.get("package") == "android" and i.get("text") == "Pixel Launcher isn't responding"
           for i in root.iter("node")):
        screenshot("emulator-launcher-anr")
        if launcher_recoveries:
            raise AssertionError("Pixel Launcher failed repeatedly on the emulator")
        close = next(i for i in root.iter("node")
                     if i.get("resource-id") == "android:id/aerr_close")
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", close.get("bounds")))
        adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
        launcher_recoveries += 1
        print("Recovered the emulator's Pixel Launcher ANR; continuing app checks")
        adb("shell", "uiautomator", "dump", "/sdcard/workshop-ui.xml")
        root = ET.fromstring(adb("shell", "cat", "/sdcard/workshop-ui.xml"))
    return root


def node(prefix, attr="text", timeout=15):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        for item in tree().iter("node"):
            if item.get(attr, "").strip().casefold().startswith(prefix.strip().casefold()):
                return item
        time.sleep(0.5)
    screenshot("failure")
    (OUTPUT / "failure-crash.log").write_text(adb("logcat", "-d", "-b", "crash"))
    (OUTPUT / "failure-ui.xml").write_text(adb("shell", "cat", "/sdcard/workshop-ui.xml"))
    (OUTPUT / "failure-system.log").write_text(adb("logcat", "-d", "-t", "400"))
    (OUTPUT / "failure-activities.log").write_text(adb("shell", "dumpsys", "activity", "activities"))
    raise AssertionError(f"Missing {attr}: {prefix}")


def tap(prefix):
    # Toolbars can scroll on smaller screens as the construction kit grows.
    root = tree()
    if not any(i.get("text", "").strip().casefold().startswith(prefix.strip().casefold()) for i in root.iter("node")):
        scrolls = [i for i in root.iter("node") if i.get("scrollable") == "true"]
        if scrolls:
            x1,y1,x2,y2=map(int,re.findall(r"\d+",scrolls[-1].get("bounds")))
            for _ in range(3):
                adb("shell","input","swipe",str((x1+x2)//2),str(y1+30),str((x1+x2)//2),str(y2-30),"250")
    for _ in range(7):
        root = tree()
        found = [i for i in root.iter("node") if i.get("text", "").strip().casefold().startswith(prefix.strip().casefold())]
        if found:
            break
        scrolls = [i for i in root.iter("node") if i.get("scrollable") == "true"]
        if not scrolls:
            break
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", scrolls[-1].get("bounds")))
        adb("shell", "input", "swipe", str((x1+x2)//2), str(y2-30), str((x1+x2)//2), str(y1+30), "350")
    item = node(prefix)
    assert item.get("enabled") == "true", f"Disabled: {prefix}"
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", item.get("bounds")))
    assert x2 > x1 and y2 > y1, f"Not visible: {prefix}"
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(0.35)


def screenshot(name):
    with (OUTPUT / f"{name}.png").open("wb") as image:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=image, check=True)


def open_first_level():
    tap("24 Murmel-Aufgaben")
    node("  Deine Murmel-Aufgaben")
    tap("1 · Die erste Rampe")
    node("Baufläche", "content-desc")


adb("shell", "wm", "size", "1280x800")
adb("shell", "wm", "density", "160")
adb("shell", "input", "keyevent", "224")
adb("shell", "wm", "dismiss-keyguard")
time.sleep(1)
adb("logcat", "-c")
adb("install", "-r", "apk/app-debug.apk")
adb("shell", "pm", "clear", PACKAGE)
# wm resize may leave Pixel Launcher busy or keep its menu above the launched app.
# This is a disposable emulator; stop only the observed launcher before starting the game.
adb("shell", "am", "force-stop", "com.google.android.apps.nexuslauncher")
launch = adb("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN",
             "-c", "android.intent.category.LAUNCHER", "-n", f"{PACKAGE}/.MainActivity")
print(launch, flush=True)
assert "Error" not in launch and "Status: ok" in launch, launch
node("MeiTüftler", timeout=30)
screenshot("menu")
tap("24 Murmel-Aufgaben")
node("  Deine Murmel-Aufgaben")
screenshot("levels")
tap("1 · Die erste Rampe")
board = node("Baufläche", "content-desc")
screenshot("workshop")
tap("＋ Brett")
node("Brett 1 · 20° · 480")
x1, y1, x2, y2 = map(int, re.findall(r"\d+", board.get("bounds")))
scale = min((x2-x1-12)/1000, (y2-y1-12)/600)
ox = x1 + (x2-x1-1000*scale)/2
oy = y1 + (y2-y1-600*scale)/2
coords = [round(ox+500*scale), round(oy+300*scale), round(ox+300*scale), round(oy+230*scale)]
adb("shell", "input", "swipe", *map(str, coords), "650")
tap("＋ Länger")
node("Brett 1 · 20° · 520")
screenshot("built")
tap("▶ Ausprobieren")
node("Geschafft!", timeout=20)
screenshot("solved")
tap("Weiter tüfteln")
node("Brett 1 · 20° · 520")
time.sleep(1)
adb("shell", "am", "force-stop", PACKAGE)
# A 0.1.0 build has no kind field. Its construction and stars must survive the migration.
legacy = ET.fromstring(adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/workshop.xml"))
for entry in legacy.findall("string"):
    if entry.get("name") == "build_0":
        old_build = json.loads(entry.text)
        for piece in old_build:
            piece.pop("kind", None)
        entry.text = json.dumps(old_build)
subprocess.run(["adb", "shell", "run-as", PACKAGE, "tee", "shared_prefs/workshop.xml"],
               input=ET.tostring(legacy), stdout=subprocess.DEVNULL, check=True)
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
node("Ohne Zeitdruck. Jede Idee darf ausprobiert werden.   ·   3 / 72 Sterne")
open_first_level()
node("Bauteile: 1 / 3")
screenshot("restored")
adb("shell", "wm", "size", "800x1280")
node("Baufläche", "content-desc")
time.sleep(1)
screenshot("portrait")
# The system Back action must return to the menu, not exit from the workshop.
adb("shell", "input", "keyevent", "4")
node("MeiTüftler")
assert adb("shell", "pidof", PACKAGE).strip()
# The second toolbox must be usable, not merely drawn on screen.
adb("shell", "wm", "size", "1280x800")
tap("24 Murmel-Aufgaben")
for _ in range(5):
    if any(i.get("text", "").startswith("11 · Hoch hinaus") for i in tree().iter("node")):
        break
    adb("shell", "input", "swipe", "600", "660", "600", "250", "500")
tap("11 · Hoch hinaus")
board = node("Baufläche", "content-desc")
tap("＋ Trampolin")
node("Trampolin 1 · 0° · 160")
tap("＋ Länger")
tap("＋ Länger")
for _ in range(3):
    tap("Drehen ↷")
node("Trampolin 1 · 15° · 240")
x1, y1, x2, y2 = map(int, re.findall(r"\d+", board.get("bounds")))
scale = min((x2-x1-12)/1000, (y2-y1-12)/600)
ox = x1 + (x2-x1-1000*scale)/2
oy = y1 + (y2-y1-600*scale)/2
coords = [round(ox+500*scale), round(oy+300*scale), round(ox+220*scale), round(oy+400*scale)]
adb("shell", "input", "swipe", *map(str, coords), "650")
screenshot("spring-built")
tap("▶ Ausprobieren")
node("Geschafft!", timeout=20)
screenshot("spring-solved")
tap("Weiter tüfteln")
tap("‹ Menü")
tap("Freier Bauplatz")
tap("＋ Trampolin")
tap("＋ Block")
node("Block 2 · 0° · 160")
tap("Drehen ↷")
tap("＋ Länger")
node("Block 2 · 5° · 200")
screenshot("new-toolbox")
tap("↶ Zurück")
node("Bauteile: 2 / 20")
tap("‹ Menü")
time.sleep(1)
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
node("Ohne Zeitdruck. Jede Idee darf ausprobiert werden.   ·   6 / 72 Sterne")
tap("Freier Bauplatz")
node("Bauteile: 2 / 20")
board = node("Baufläche", "content-desc")
x1, y1, x2, y2 = map(int, re.findall(r"\d+", board.get("bounds")))
adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
node("Block 2 · 5° · 160")
screenshot("new-elements-restored")
# New air mechanic, then named inventions survive a full process restart.
tap("‹ Menü")
tap("24 Murmel-Aufgaben")
for _ in range(6):
    if any(i.get("text", "").startswith("17 · Frischer Wind") for i in tree().iter("node")):
        break
    adb("shell", "input", "swipe", "600", "660", "600", "250", "500")
tap("17 · Frischer Wind")
board = node("Baufläche", "content-desc")
tap("＋ Ventilator")
node("Ventilator 1 · -60° · 320")
node("Stärke: mittel")
x1, y1, x2, y2 = map(int, re.findall(r"\d+", board.get("bounds")))
scale = min((x2-x1-12)/1000, (y2-y1-12)/600)
ox = x1+(x2-x1-1000*scale)/2
oy = y1+(y2-y1-600*scale)/2
coords=[round(ox+500*scale),round(oy+300*scale),round(ox+120*scale),round(oy+300*scale)]
adb("shell", "input", "swipe", *map(str,coords), "650")
screenshot("fan-built")
# Verify the saved release point before simulation; a short last MOVE must not shift the fan.
fan_prefs=ET.fromstring(adb("shell","run-as",PACKAGE,"cat","shared_prefs/workshop.xml"))
fan_build=json.loads(next(i.text for i in fan_prefs.findall("string") if i.get("name")=="build_16"))
assert abs(fan_build[0]["x"]-120)<=2 and abs(fan_build[0]["y"]-300)<=2, fan_build
tap("▶ Ausprobieren")
node("Geschafft!", timeout=20)
screenshot("fan-solved")
tap("Weiter tüfteln")
tap("‹ Menü")
tap("Freier Bauplatz")
tap("＋ Ventilator")
tap("Stärke: mittel")
node("Stärke: kräftig")
tap("Speichern")
node("Erfindung speichern")
root=tree()
field=next(i for i in root.iter("node") if i.get("class")=="android.widget.EditText")
x1,y1,x2,y2=map(int,re.findall(r"\d+",field.get("bounds")))
adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
adb("shell","input","keyevent","KEYCODE_MOVE_END")
for _ in range(45):
    adb("shell","input","keyevent","KEYCODE_DEL")
adb("shell","input","text","Windmaschine")
# Dialog Save, not the identically named header button beneath the modal.
root=tree()
confirm=next(i for i in root.iter("node") if i.get("resource-id")=="android:id/button1")
x1,y1,x2,y2=map(int,re.findall(r"\d+",confirm.get("bounds")))
adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
time.sleep(1)
tap("‹ Menü")
tap("Meine Erfindungen")
node("Windmaschine")
screenshot("inventions")
tap("Windmaschine")
node("Bauteile: 3 / 20")
board=node("Baufläche","content-desc")
x1,y1,x2,y2=map(int,re.findall(r"\d+",board.get("bounds")))
adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
node("Ventilator 3 · -60° · 320")
node("Stärke: kräftig")
tap("Stärke: kräftig")
node("Stärke: sanft")
tap("Speichern")
tap("Aktualisieren")
tap("Speichern")
tap("Als Kopie")
tap("‹ Menü")
time.sleep(1)
adb("shell","am","force-stop",PACKAGE)
adb("shell","am","start","-W","-n",f"{PACKAGE}/.MainActivity")
tap("Meine Erfindungen")
node("Windmaschine (Kopie)")
tap("Windmaschine")
board=node("Baufläche","content-desc")
x1,y1,x2,y2=map(int,re.findall(r"\d+",board.get("bounds")))
adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
node("Ventilator 3 · -60° · 320")
node("Stärke: sanft")
screenshot("invention-restored")
# Pair a user-built switch and door, save, then check their channels after restart.
tap("＋ Schalter")
tap("Verbindung: Rot 1")
node("Verbindung: Blau 2")
board=node("Baufläche","content-desc")
x1,y1,x2,y2=map(int,re.findall(r"\d+",board.get("bounds")))
scale=min((x2-x1-12)/1000,(y2-y1-12)/600)
ox=x1+(x2-x1-1000*scale)/2;oy=y1+(y2-y1-600*scale)/2
adb("shell","input","swipe",str(round(ox+500*scale)),str(round(oy+300*scale)),str(round(ox+250*scale)),str(round(oy+110*scale)),"650")
tap("＋ Tür")
tap("Verbindung: Rot 1")
node("Verbindung: Blau 2")
screenshot("switch-door-built")
tap("Speichern")
tap("Aktualisieren")
tap("‹ Menü")
time.sleep(1)
adb("shell","am","force-stop",PACKAGE)
adb("shell","am","start","-W","-n",f"{PACKAGE}/.MainActivity")
tap("Meine Erfindungen")
tap("Windmaschine")
node("Bauteile: 5 / 20")
board=node("Baufläche","content-desc")
x1,y1,x2,y2=map(int,re.findall(r"\d+",board.get("bounds")))
adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
node("Tür 5 · 0° · 160")
node("Verbindung: Blau 2")
screenshot("switch-door-restored")
# Solve the introductory fixed switch/door puzzle through the real UI.
tap("‹ Menü")
tap("24 Murmel-Aufgaben")
tap("21 · Sesam, öffne dich!")
board=node("Baufläche","content-desc")
tap("＋ Brett")
x1,y1,x2,y2=map(int,re.findall(r"\d+",board.get("bounds")))
scale=min((x2-x1-12)/1000,(y2-y1-12)/600)
ox=x1+(x2-x1-1000*scale)/2;oy=y1+(y2-y1-600*scale)/2
adb("shell","input","swipe",str(round(ox+500*scale)),str(round(oy+300*scale)),str(round(ox+300*scale)),str(round(oy+230*scale)),"650")
tap("＋ Länger")
screenshot("door-puzzle-built")
tap("▶ Ausprobieren")
node("Geschafft!",timeout=20)
screenshot("door-puzzle-solved")
tap("Weiter tüfteln")
tap("‹ Menü")
# Solve the new underpass route through the real controls and verify its revised build storage.
tap("24 Murmel-Aufgaben")
for _ in range(6):
    if any(i.get("text", "").startswith("13 · Unten durch, oben ankommen") for i in tree().iter("node")):
        break
    adb("shell", "input", "swipe", "600", "660", "600", "250", "500")
tap("13 · Unten durch, oben ankommen")
board=node("Baufläche", "content-desc")
screenshot("underpass-empty")
def place_piece(x, y):
    x1,y1,x2,y2=map(int,re.findall(r"\d+",board.get("bounds")))
    scale=min((x2-x1-12)/1000,(y2-y1-12)/600)
    ox=x1+(x2-x1-1000*scale)/2;oy=y1+(y2-y1-600*scale)/2
    adb("shell","input","swipe",str(round(ox+500*scale)),str(round(oy+300*scale)),
        str(round(ox+x*scale)),str(round(oy+y*scale)),"650")
tap("＋ Brett")
for _ in range(5): tap("Drehen ↷")
for _ in range(5): tap("− Kürzer")
node("Brett 1 · 45° · 280")
place_piece(221,151.421)
tap("＋ Trampolin")
for _ in range(4): tap("＋ Länger")
node("Trampolin 2 · 0° · 320")
place_piece(436,555)
screenshot("underpass-built")
tap("▶ Ausprobieren")
node("Geschafft!",timeout=20)
screenshot("underpass-solved")
tap("Weiter tüfteln")
tap("‹ Menü")
time.sleep(1)
adb("shell","am","force-stop",PACKAGE)
adb("shell","am","start","-W","-n",f"{PACKAGE}/.MainActivity")
root=ET.fromstring(adb("shell","run-as",PACKAGE,"cat","shared_prefs/workshop.xml"))
assert any(i.get("name")=="build_12_v2" and len(json.loads(i.text))==2 for i in root.findall("string"))
assert any(i.get("name")=="build_0" for i in root.findall("string"))
node("MeiTüftler")

# No Firebase provider may start, including when the SDK is linked but unconfigured.
assert "FirebaseInitProvider" not in adb("shell","dumpsys","package",PACKAGE)
assert "com.google.firebase.analytics" not in adb("shell","dumpsys","package",PACKAGE)
log = adb("logcat", "-d", "-b", "crash")
(OUTPUT / "crash.log").write_text(log)
assert "FATAL EXCEPTION" not in log, log
print("PASS: original puzzle, trampoline puzzle, new toolbox, typed persistence, legacy builds, undo, rotation, fan puzzle, named inventions, switch-door puzzle, saved channels, opt-in Firebase manifest and Back", flush=True)

# Exercise consent in the parent UI. Only a synthetic, confirmed test crash is expected.
def parent_info():
    tap("Für Eltern · Info")
    root=tree()
    question=next(i.get("text") for i in root.iter("node") if "Wie viel ist" in i.get("text", ""))
    a,b=map(int,re.search(r"(\d+) × (\d+)",question).groups())
    field=node("Antwort für Eltern","content-desc")
    x1,y1,x2,y2=map(int,re.findall(r"\d+",field.get("bounds")))
    adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
    adb("shell","input","text",str(a*b))
    tap("Weiter")
    node("Info und Datenschutz")

adb("shell","setprop","log.tag.FirebaseCrashlytics","DEBUG")
adb("shell","setprop","log.tag.TransportRuntime.CctTransportBackend","DEBUG")
adb("shell","setprop","log.tag.TransportRuntime.Uploader","DEBUG")
parent_info()
root=tree()
assert any("Diagnose: aus" in i.get("text", "") for i in root.iter("node"))
screenshot("parents-diagnostics-off")
tap("Diagnose erlauben")
time.sleep(5)
parent_info()
root=tree()
assert any("Diagnose: an" in i.get("text", "") for i in root.iter("node"))
screenshot("parents-diagnostics-on")
tap("Test-Absturz")
tap("Test auslösen")
time.sleep(4)
crash=adb("logcat","-d","-b","crash")
assert "MeiTueftler manual Crashlytics test" in crash, crash
(OUTPUT/"expected-diagnostic-crash.log").write_text(crash)
adb("shell","am","start","-W","-n",f"{PACKAGE}/.MainActivity")
node("MeiTüftler",timeout=30)
# Give Crashlytics/Android DataTransport time to schedule and upload the test report.
end=time.monotonic()+90
queued=False;uploaded=False
while time.monotonic()<end:
    diagnostics=adb("logcat","-d","-s","FirebaseCrashlytics","TransportRuntime.CctTransportBackend","TransportRuntime.Uploader")
    queued="successfully enqueued to DataTransport" in diagnostics
    uploaded="Status Code: 200" in diagnostics or "Status Code: 202" in diagnostics
    if queued and uploaded:break
    time.sleep(3)
(OUTPUT/"diagnostics-delivery.log").write_text(diagnostics)
assert queued,"Crashlytics did not enqueue the synthetic report"
print("Crashlytics synthetic report enqueued; HTTP upload acknowledgement:",uploaded,flush=True)
parent_info()
tap("Diagnose ausschalten")
parent_info()
assert any("Diagnose: aus" in i.get("text", "") for i in tree().iter("node"))
tap("Schließen")
adb("shell","am","force-stop",PACKAGE)
adb("logcat","-c")
adb("shell","am","start","-W","-n",f"{PACKAGE}/.MainActivity")
node("MeiTüftler",timeout=30)
time.sleep(2)
assert "Initializing Firebase Crashlytics" not in adb("logcat","-d","-s","FirebaseCrashlytics")
print("PASS: parent consent, confirmed synthetic crash, SDK report queue, consent withdrawal and startup without Firebase",flush=True)
